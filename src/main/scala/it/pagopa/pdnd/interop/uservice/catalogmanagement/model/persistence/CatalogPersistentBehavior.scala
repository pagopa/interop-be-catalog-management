package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.CatalogItem

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.language.postfixOps

@SuppressWarnings(Array("org.wartremover.warts.Equals"))
object CatalogPersistentBehavior {

  def commandHandler(
    shard: ActorRef[ClusterSharding.ShardCommand],
    context: ActorContext[Command]
  ): (State, Command) => Effect[Event, State] = { (state, command) =>
    val idleTimeout =
      context.system.settings.config.getDuration("uservice-catalog-management.idle-timeout")
    context.setReceiveTimeout(idleTimeout.get(ChronoUnit.SECONDS) seconds, Idle)
    command match {
      case AddCatalogItem(newCatalogItem, replyTo) =>
        val catalogItem: Option[CatalogItem] = state.items.get(newCatalogItem.id.toString)

        catalogItem
          .map { ci =>
            replyTo ! StatusReply.Error[CatalogItem](s"E-Service ${ci.id.toString} already exists")
            Effect.none[CatalogItemAdded, State]
          }
          .getOrElse {
            Effect
              .persist(CatalogItemAdded(newCatalogItem))
              .thenRun((_: State) => replyTo ! StatusReply.Success(newCatalogItem))
          }

      case GetCatalogItem(itemId, replyTo) =>
        val catalogItem: Option[CatalogItem] = state.items.get(itemId)
        replyTo ! StatusReply.Success[Option[CatalogItem]](catalogItem)
        Effect.none[Event, State]

      case ListCatalogItem(from, to, producerId, _, status, replyTo) =>
        val catalogItems: Seq[CatalogItem] = state.items
          .filter { case (_, v) =>
            (if (producerId.isDefined) producerId.contains(v.producerId.toString) else true) &&
              (if (status.isDefined) v.descriptors.exists(descriptor => status.contains(descriptor.status.stringify))
               else true)
          }
          .values
          .toSeq
          .slice(from, to)
        replyTo ! catalogItems
        Effect.none[Event, State]

      case Idle =>
        shard ! ClusterSharding.Passivate(context.self)
        context.log.error(s"Passivate shard: ${shard.path.name}")
        Effect.none[Event, State]
    }
  }

  val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case CatalogItemAdded(catalogItem) => state.add(catalogItem)
    }

  val TypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("uservice-catalog-management-persistence-catalog")

  def apply(shard: ActorRef[ClusterSharding.ShardCommand], persistenceId: PersistenceId): Behavior[Command] = {
    Behaviors.setup { context =>
      context.log.error(s"Starting EService Shard ${persistenceId.id}")
      val numberOfEvents =
        context.system.settings.config
          .getInt("uservice-catalog-management.number-of-events-before-snapshot")
      EventSourcedBehavior[Command, Event, State](
        persistenceId = persistenceId,
        emptyState = State.empty,
        commandHandler = commandHandler(shard, context),
        eventHandler = eventHandler
      ).withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = numberOfEvents, keepNSnapshots = 1))
        .withTagger(_ => Set(persistenceId.id))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200 millis, 5 seconds, 0.1))
    }
  }
}
