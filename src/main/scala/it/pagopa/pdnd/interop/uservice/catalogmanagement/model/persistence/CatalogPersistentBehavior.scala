package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import akka.Done
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.{CatalogDescriptor, CatalogDocument, CatalogItem}

import java.time.temporal.ChronoUnit
import java.util.UUID
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

      case AddClonedCatalogItem(newCatalogItem, replyTo) =>
        val catalogItem: Option[CatalogItem] = state.items.get(newCatalogItem.id.toString)
        catalogItem
          .map { ci =>
            replyTo ! StatusReply.Error[CatalogItem](s"E-Service ${ci.id.toString} already exists")
            Effect.none[ClonedCatalogItemAdded, State]
          }
          .getOrElse {
            Effect
              .persist(ClonedCatalogItemAdded(newCatalogItem))
              .thenRun((_: State) => replyTo ! StatusReply.Success(newCatalogItem))
          }

      case UpdateCatalogItem(modifiedCatalogItem, replyTo) =>
        val catalogItem: Option[CatalogItem] = state.items.get(modifiedCatalogItem.id.toString)

        catalogItem
          .map { _ =>
            Effect
              .persist(CatalogItemUpdated(modifiedCatalogItem))
              .thenRun((_: State) => replyTo ! Some(modifiedCatalogItem))
          }
          .getOrElse {
            replyTo ! None
            Effect.none[CatalogItemUpdated, State]
          }

      case UpdateDocument(eServiceId, descriptorId, documentId, modifiedDocument, replyTo) =>
        val catalogDocument: Option[CatalogDocument] =
          for {
            service    <- state.items.get(eServiceId)
            descriptor <- service.descriptors.find(_.id.toString == descriptorId)
            interface = descriptor.interface.fold(Seq.empty[CatalogDocument])(doc => Seq(doc))
            document <- (interface ++: descriptor.docs).find(_.id.toString == documentId)
          } yield document

        catalogDocument
          .map { _ =>
            Effect
              .persist(DocumentUpdated(eServiceId, descriptorId, documentId, modifiedDocument))
              .thenRun((_: State) => replyTo ! Some(modifiedDocument))
          }
          .getOrElse {
            replyTo ! None
            Effect.none[DocumentUpdated, State]
          }
      //

      case DeleteCatalogItemWithDescriptor(deletedCatalogItem, descriptorId, replyTo) =>
        val descriptorToDelete: Option[CatalogDescriptor] =
          state.items
            .get(deletedCatalogItem.id.toString)
            .flatMap(_.descriptors.find(_.id == UUID.fromString(descriptorId)))

        descriptorToDelete
          .map { _ =>
            Effect
              .persist(CatalogItemDeleted(deletedCatalogItem, descriptorId))
              .thenRun((_: State) => replyTo ! StatusReply.Success(Done))
          }
          .getOrElse {
            replyTo ! StatusReply.Error[Done](s"Draft version not found.")
            Effect.none[CatalogItemDeleted, State]
          }

      case GetCatalogItem(itemId, replyTo) =>
        val catalogItem: Option[CatalogItem] = state.items.get(itemId)
        replyTo ! catalogItem
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
      case CatalogItemAdded(catalogItem)                 => state.add(catalogItem)
      case ClonedCatalogItemAdded(catalogItem)           => state.add(catalogItem)
      case CatalogItemUpdated(catalogItem)               => state.update(catalogItem)
      case CatalogItemDeleted(catalogItem, descriptorId) => state.delete(catalogItem, descriptorId)
      case DocumentUpdated(eServiceId, descriptorId, documentId, modifiedDocument) =>
        state.updateDocument(eServiceId, descriptorId, documentId, modifiedDocument)
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
