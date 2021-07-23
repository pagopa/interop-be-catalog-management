package it.pagopa.pdnd.interop.uservice.catalogmanagement.api.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives.{complete, onComplete, onSuccess}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.EServiceApiService
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common.system._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.{FileManager, UUIDSupplier}

import java.io.File
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}
@SuppressWarnings(
  Array(
    "org.wartremover.warts.ImplicitParameter",
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.Recursion"
  )
)
class EServiceApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]],
  uuidSupplier: UUIDSupplier,
  fileManager: FileManager
)(implicit ec: ExecutionContext)
    extends EServiceApiService {

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
  }

  @inline private def getShard(id: String): String = Math.abs(id.hashCode % settings.numberOfShards).toString

  private def getCommander(id: String): EntityRef[Command] =
    sharding.entityRefFor(CatalogPersistentBehavior.TypeKey, getShard(id))

  /** Code: 200, Message: EService created, DataType: EService
    * Code: 400, Message: Invalid input, DataType: Problem
    */
  override def createEService(eServiceSeed: EServiceSeed)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {
    contexts.foreach(println)

    val result: Future[StatusReply[CatalogItem]] =
      for {
        catalogItem <- CatalogItem.create(eServiceSeed, uuidSupplier)
        added       <- getCommander(catalogItem.id.toString).ask(ref => AddCatalogItem(catalogItem, ref))
      } yield added

    onSuccess(result) {
      case statusReply if statusReply.isSuccess =>
        createEService200(statusReply.getValue.toApi)
      case statusReply if statusReply.isError =>
        createEService400(Problem(Option(statusReply.getError.getMessage), status = 405, "some error"))
    }
  }

  /** Code: 200, Message: EService Interface created, DataType: EService
    * Code: 400, Message: Invalid input, DataType: Problem
    */
  override def createEServiceInterface(
    eServiceId: String,
    descriptorId: String,
    description: String,
    technology: String,
    idl: (FileInfo, File)
  )(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {
    val commander: EntityRef[Command] = getCommander(eServiceId)

    val result: Future[Option[CatalogItem]] = for {
      retrieved  <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
      current    <- retrieved.toFuture(new RuntimeException("EService not found"))
      verified   <- fileManager.verifyFile(idl, current)
      openapiDoc <- fileManager.store(uuidSupplier.get, eServiceId, descriptorId, idl)
      updated    <- commander.ask(ref => UpdateCatalogItem(verified.updateFile(descriptorId, openapiDoc), ref))
    } yield updated

    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold(createEServiceInterface404(Problem(None, status = 404, "some error")))(ci =>
          createEServiceInterface200(ci.toApi)
        )
      case Failure(exception) =>
        createEServiceInterface400(Problem(Option(exception.getMessage), status = 400, "some error"))
    }

  }

  /** Code: 200, Message: EService Descriptor published, DataType: EService
    * Code: 400, Message: Invalid input, DataType: Problem
    */
  override def publishDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {
    val commander: EntityRef[Command] = getCommander(eServiceId)

    val result: Future[Option[CatalogItem]] = for {
      retrieved <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
      current   <- retrieved.toFuture(new RuntimeException("EService non found"))
      updated   <- commander.ask(ref => UpdateCatalogItem(current.publish(descriptorId), ref))
    } yield updated

    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold(publishDescriptor404(Problem(None, status = 404, "some error")))(ci =>
          publishDescriptor200(ci.toApi)
        )
      case Failure(exception) =>
        publishDescriptor400(Problem(Option(exception.getMessage), status = 400, "some error"))
    }

  }

  /** Code: 200, Message: EService retrieved, DataType: EService
    * Code: 404, Message: EService not found, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */
  override def getEService(eServiceId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {
    contexts.foreach(println)
    val result: Future[Option[CatalogItem]] = getCommander(eServiceId).ask(ref => GetCatalogItem(eServiceId, ref))

    onSuccess(result) {
      case Some(catalogItem) => getEService200(catalogItem.toApi)
      case None              => getEService404(Problem(None, status = 404, "some error"))
    }
  }

  /** Code: 200, Message: A list of EService, DataType: Seq[EService]
    */
  override def getEServices(producerId: Option[String], consumerId: Option[String], status: Option[String])(implicit
    toEntityMarshallerEServicearray: ToEntityMarshaller[Seq[EService]],
    contexts: Seq[(String, String)]
  ): Route = {
    contexts.foreach(println)
    val sliceSize = 100
    def getSlice(commander: EntityRef[Command], from: Int, to: Int): LazyList[CatalogItem] = {
      val slice: Seq[CatalogItem] = Await
        .result(commander.ask(ref => ListCatalogItem(from, to, producerId, consumerId, status, ref)), Duration.Inf)

      if (slice.isEmpty)
        LazyList.empty[CatalogItem]
      else
        getSlice(commander, to, to + sliceSize) #::: slice.to(LazyList)
    }
    val commanders: Seq[EntityRef[Command]] =
      (0 until settings.numberOfShards).map(shard => getCommander(shard.toString))
    val catalogItem: Seq[CatalogItem] = commanders.flatMap(ref => getSlice(ref, 0, sliceSize))

    getEServices200(catalogItem.map(_.toApi))

  }

  /** Code: 200, Message: EService document retrieved, DataType: File
    * Code: 404, Message: EService not found, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */
  override def getEServiceDocument(eServiceId: String, descriptorId: String, documentId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerFile: ToEntityMarshaller[File],
    contexts: Seq[(String, String)]
  ): Route = {
    contexts.foreach(println)
    val commander: EntityRef[Command] = getCommander(eServiceId)

    val result: Future[Option[CatalogItem]] = commander.ask(ref => GetCatalogItem(eServiceId, ref))

    onSuccess(result) {
      case Some(catalogItem) =>
        val fileInfo = catalogItem.extractFile(descriptorId = descriptorId, documentId = documentId)

        fileInfo.fold(getEServiceDocument404(Problem(None, status = 404, s"Document $documentId not found"))) {
          case (contentType, file) =>
            complete(HttpEntity.fromFile(contentType, file.toFile))
        }
      case None => getEService400(Problem(None, status = 400, s"EService $eServiceId not found"))
    }
  }

}
