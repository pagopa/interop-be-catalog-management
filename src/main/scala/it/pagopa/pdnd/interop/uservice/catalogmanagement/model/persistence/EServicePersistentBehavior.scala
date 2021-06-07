package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.EService

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.language.postfixOps

@SuppressWarnings(Array("org.wartremover.warts.Equals"))
object EServicePersistentBehavior {

  def commandHandler(
    shard: ActorRef[ClusterSharding.ShardCommand],
    context: ActorContext[Command]
  ): (State, Command) => Effect[Event, State] = { (state, command) =>
    val idleTimeout =
      context.system.settings.config.getDuration("pdnd-interop-uservice-catalog-management.idle-timeout")
    context.setReceiveTimeout(idleTimeout.get(ChronoUnit.SECONDS) seconds, Idle)
    command match {
      case AddEService(newEService, replyTo) =>
        val eService: Option[EService] = for {
          id    <- newEService.id
          found <- state.eServices.get(id.toString)
        } yield found

        eService
          .map { es =>
            replyTo ! StatusReply.Error[String](
              s"E-Service ${es.id.map(_.toString).getOrElse("UNKNOWN")} already exists"
            )
            Effect.none[EServiceAdded, State]
          }
          .getOrElse {
            Effect
              .persist(EServiceAdded(newEService))
              .thenRun((_: State) => replyTo ! StatusReply.Success(newEService.id.map(_.toString).getOrElse("UNKNOWN")))
          }

      case GetEService(eServiceId, replyTo) =>
        val eService: Option[EService] = state.eServices.get(eServiceId)
        replyTo ! StatusReply.Success[Option[EService]](eService)
        Effect.none[Event, State]

      case ListServices(from, to, _, _, status, replyTo) =>
        val eServices = state.eServices.filter(_._2.status == status).values.toSeq.slice(from, to)
        replyTo ! eServices
        Effect.none[Event, State]

      case Idle =>
        shard ! ClusterSharding.Passivate(context.self)
        context.log.error(s"Passivate shard: ${shard.path.name}")
        Effect.none[Event, State]
    }
  }

  val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case EServiceAdded(eService) => state.add(eService)
    }

  val TypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("pdnd-interop-uservice-catalog-management-persistence-eservice")

  def apply(shard: ActorRef[ClusterSharding.ShardCommand], persistenceId: PersistenceId): Behavior[Command] = {
    Behaviors.setup { context =>
      context.log.error(s"Starting EService Shard ${persistenceId.id}")
      val numberOfEvents =
        context.system.settings.config
          .getInt("pdnd-interop-uservice-catalog-management.number-of-events-before-snapshot")
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
