package it.pagopa.interop.catalogmanagement.model.persistence

import akka.Done
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.pattern.StatusReply
import akka.pattern.StatusReply.success
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.interop.catalogmanagement.model._

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.language.postfixOps
import it.pagopa.interop.catalogmanagement.error.CatalogManagementErrors._

object CatalogPersistentBehavior {

  def commandHandler(
    shard: ActorRef[ClusterSharding.ShardCommand],
    context: ActorContext[Command]
  ): (State, Command) => Effect[Event, State] = { (state, command) =>
    val idleTimeout =
      context.system.settings.config.getDuration("catalog-management.idle-timeout")
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
              .thenRun((_: State) => replyTo ! success(Some(modifiedCatalogItem)))
          }
          .getOrElse {
            replyTo ! success(None)
            Effect.none[CatalogItemUpdated, State]
          }

      case AddCatalogItemDocument(eServiceId, descriptorId, document, isInterface, serverUrls, replyTo) =>
        val catalogItemDescriptor: Option[CatalogDescriptor] =
          state.items.get(eServiceId).flatMap(_.descriptors.find(_.id.toString == descriptorId))

        catalogItemDescriptor
          .map { _ =>
            Effect
              .persist(CatalogItemDocumentAdded(eServiceId, descriptorId, document, isInterface, serverUrls))
              .thenRun((_: State) => replyTo ! Some(document))
          }
          .getOrElse {
            replyTo ! None
            Effect.none[CatalogItemDocumentAdded, State]
          }

      case DeleteCatalogItemDocument(eServiceId, descriptorId, documentId, replyTo) =>
        val catalogItemDescriptor: Option[CatalogDescriptor] =
          state.items
            .get(eServiceId)
            .flatMap(_.descriptors.find(_.id.toString == descriptorId))
        catalogItemDescriptor
          .map { descriptor =>
            val interface: Option[CatalogDocument] = descriptor.interface.filter(_.id.toString == documentId)
            val document: Option[CatalogDocument]  = descriptor.docs.find(_.id.toString == documentId)
            (interface, document) match {
              case (None, None) =>
                replyTo ! StatusReply.Error[Done](s"Document not found.")
                Effect.none[CatalogItemDocumentDeleted, State]
              case _            =>
                Effect
                  .persist(CatalogItemDocumentDeleted(eServiceId, descriptorId, documentId))
                  .thenRun((_: State) => replyTo ! StatusReply.Success(Done))
            }
          }
          .getOrElse {
            replyTo ! StatusReply.Error[Done](s"Descriptor not found.")
            Effect.none[CatalogItemDocumentDeleted, State]
          }

      case AddCatalogItemRiskAnalysis(eServiceId, catalogRiskAnalysis, replyTo) =>
        val catalogItem: Option[CatalogItem] = state.items.get(eServiceId)

        catalogItem
          .map { _ =>
            Effect
              .persist(CatalogItemRiskAnalysisAdded(eServiceId, catalogRiskAnalysis))
              .thenRun((_: State) => replyTo ! success(Some(catalogRiskAnalysis)))
          }
          .getOrElse {
            replyTo ! success(None)
            Effect.none[CatalogItemRiskAnalysisAdded, State]
          }

      case UpdateCatalogItemRiskAnalysis(eServiceId, catalogRiskAnalysis, replyTo) =>
        val catalogItemRiskAnalysis: Option[CatalogRiskAnalysis] =
          state.items.get(eServiceId).flatMap(_.riskAnalysis.find(_.id == catalogRiskAnalysis.id))

        catalogItemRiskAnalysis
          .map { _ =>
            Effect
              .persist(CatalogItemRiskAnalysisUpdated(eServiceId, catalogRiskAnalysis))
              .thenRun((_: State) => replyTo ! success(Some(catalogRiskAnalysis)))
          }
          .getOrElse {
            replyTo ! success(None)
            Effect.none[CatalogItemRiskAnalysisUpdated, State]
          }

      case AddCatalogItemDescriptor(eServiceId, catalogDescriptor, replyTo) =>
        val catalogItem: Option[CatalogItem] = state.items.get(eServiceId)

        catalogItem
          .map { _ =>
            Effect
              .persist(CatalogItemDescriptorAdded(eServiceId, catalogDescriptor))
              .thenRun((_: State) => replyTo ! success(Some(catalogDescriptor)))
          }
          .getOrElse {
            replyTo ! success(None)
            Effect.none[CatalogItemDescriptorAdded, State]
          }

      case UpdateCatalogItemDescriptor(eServiceId, catalogDescriptor, replyTo) =>
        val catalogItemDescriptor: Option[CatalogDescriptor] =
          state.items.get(eServiceId).flatMap(_.descriptors.find(_.id == catalogDescriptor.id))

        catalogItemDescriptor
          .map { _ =>
            Effect
              .persist(CatalogItemDescriptorUpdated(eServiceId, catalogDescriptor))
              .thenRun((_: State) => replyTo ! success(Some(catalogDescriptor)))
          }
          .getOrElse {
            replyTo ! success(None)
            Effect.none[CatalogItemDescriptorUpdated, State]
          }

      case UpdateCatalogItemDocument(eServiceId, descriptorId, documentId, modifiedDocument, serverUrls, replyTo) =>
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
              .persist(CatalogItemDocumentUpdated(eServiceId, descriptorId, documentId, modifiedDocument, serverUrls))
              .thenRun((_: State) => replyTo ! success(Some(modifiedDocument)))
          }
          .getOrElse {
            replyTo ! success(None)
            Effect.none[CatalogItemDocumentUpdated, State]
          }

      case DeleteCatalogItemDescriptor(deletedCatalogItem, descriptorId, replyTo) =>
        val descriptorToDelete: Option[CatalogDescriptor] =
          state.items
            .get(deletedCatalogItem.id.toString)
            .flatMap(_.descriptors.find(_.id.toString == descriptorId))

        descriptorToDelete
          .map { _ =>
            Effect
              .persist(CatalogItemWithDescriptorsDeleted(deletedCatalogItem, descriptorId))
              .thenRun((_: State) => replyTo ! StatusReply.Success(Done))
          }
          .getOrElse {
            replyTo ! StatusReply.Error[Done](s"Descriptor not found.")
            Effect.none[CatalogItemWithDescriptorsDeleted, State]
          }

      case DeleteCatalogItem(eServiceId, replyTo) =>
        val catalogItem: Option[CatalogItem] = state.items.get(eServiceId)
        catalogItem
          .map { eService =>
            Effect
              .persist(CatalogItemDeleted(eService.id.toString))
              .thenRun((_: State) => replyTo ! StatusReply.Success(Done))
          }
          .getOrElse {
            replyTo ! StatusReply.Error[Done](s"E-Service not found.")
            Effect.none[CatalogItemDeleted, State]
          }

      case GetCatalogItem(itemId, replyTo) =>
        val catalogItem: Option[CatalogItem] = state.items.get(itemId)
        replyTo ! success(catalogItem)
        Effect.none[Event, State]

      case ListCatalogItem(from, to, producerId, attributeId, status, replyTo) =>
        val catalogItems: Seq[CatalogItem] = state.items.values
          .filter(catalogItem => producerId.forall(_ == catalogItem.producerId.toString))
          .filter(catalogItem => attributeId.forall(containsAttribute(catalogItem, _)))
          .filter(catalogItem => status.forall(s => catalogItem.descriptors.exists(_.state == s)))
          .toSeq
          .slice(from, to)
        replyTo ! catalogItems
        Effect.none[Event, State]

      case MoveAttributesFromEserviceToDescriptors(eServiceId, replyTo) =>
        state.items
          .get(eServiceId)
          .fold {
            replyTo ! StatusReply.error[Done](EServiceNotFound(eServiceId))
            Effect.none[MovedAttributesFromEserviceToDescriptors, State]
          } { eService =>
            val newEservice: CatalogItem = moveAttributesToDescriptor(eService)
            Effect
              .persist(MovedAttributesFromEserviceToDescriptors(newEservice))
              .thenRun((_: State) => replyTo ! StatusReply.Success(Done))
          }

      case Idle =>
        shard ! ClusterSharding.Passivate(context.self)
        context.log.debug(s"Passivate shard: ${shard.path.name}")
        Effect.none[Event, State]
    }
  }

  def moveAttributesToDescriptor(catalogItem: CatalogItem): CatalogItem = {
    def updateSingleDescriptor(d: CatalogDescriptor): CatalogDescriptor =
      catalogItem.attributes.fold(d)(attrs => d.copy(attributes = attrs.combine(d.attributes)))

    catalogItem.copy(attributes = None, descriptors = catalogItem.descriptors.map(updateSingleDescriptor))
  }

  def containsAttribute(catalogItem: CatalogItem, attributeId: String): Boolean =
    catalogItem.descriptors
      .map(_.attributes)
      .toList
      .flatMap(a => a.certified.flatten ++ a.declared.flatten ++ a.verified.flatten)
      .exists(_.id.toString == attributeId)

  val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case CatalogItemAdded(catalogItem)                                => state.add(catalogItem)
      case ClonedCatalogItemAdded(catalogItem)                          => state.add(catalogItem)
      case CatalogItemUpdated(catalogItem)                              => state.update(catalogItem)
      case CatalogItemWithDescriptorsDeleted(catalogItem, descriptorId) =>
        state.deleteDescriptor(catalogItem, descriptorId)
      case CatalogItemDeleted(catalogItemId)                            => state.deleteEService(catalogItemId)
      case CatalogItemDocumentUpdated(eServiceId, descriptorId, documentId, modifiedDocument, serverUrls) =>
        state.updateDocument(eServiceId, descriptorId, documentId, modifiedDocument, serverUrls)
      case CatalogItemDocumentAdded(eServiceId, descriptorId, openapiDoc, isInterface, serverUrls)        =>
        state.addItemDocument(eServiceId, descriptorId, openapiDoc, isInterface, serverUrls)
      case CatalogItemDocumentDeleted(eServiceId, descriptorId, documentId)                               =>
        state.deleteDocument(eServiceId, descriptorId, documentId)
      case CatalogItemDescriptorAdded(eServiceId, catalogDescriptor)                                      =>
        state.addDescriptor(eServiceId, catalogDescriptor)
      case CatalogItemDescriptorUpdated(eServiceId, catalogDescriptor)                                    =>
        state.updateDescriptor(eServiceId, catalogDescriptor)
      case MovedAttributesFromEserviceToDescriptors(catalogItem)           => state.update(catalogItem)
      case CatalogItemRiskAnalysisAdded(eServiceId, catalogRiskAnalysis)   =>
        state.addRiskAnalysis(eServiceId, catalogRiskAnalysis)
      case CatalogItemRiskAnalysisUpdated(eServiceId, catalogRiskAnalysis) =>
        state.updateRiskAnalysis(eServiceId, catalogRiskAnalysis)
    }

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("interop-be-catalog-management-persistence")

  def apply(
    shard: ActorRef[ClusterSharding.ShardCommand],
    persistenceId: PersistenceId,
    projectionTag: String
  ): Behavior[Command] = Behaviors.setup { context =>
    context.log.debug(s"Starting EService Shard ${persistenceId.id}")
    val numberOfEvents = context.system.settings.config.getInt("catalog-management.number-of-events-before-snapshot")
    EventSourcedBehavior[Command, Event, State](
      persistenceId = persistenceId,
      emptyState = State.empty,
      commandHandler = commandHandler(shard, context),
      eventHandler = eventHandler
    ).withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = numberOfEvents, keepNSnapshots = 1))
      .withTagger(_ => Set(projectionTag))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200 millis, 5 seconds, 0.1))
  }

}
