package it.pagopa.pdnd.interop.uservice.catalogmanagement.api.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.{ContentType, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, onSuccess}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.EServiceApiService
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common.system._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.{FileManager, UUIDSupplier}

import java.io.File
import java.nio.file.Path
import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
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

  /** Code: 200, Message: EService created, DataType: EService
    * Code: 400, Message: Invalid input, DataType: Problem
    */
  override def createEService(
    name: String,
    description: String,
    audience: Seq[String],
    voucherLifespan: Int,
    technology: String,
    idl: (FileInfo, File)
  )(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {
    contexts.foreach(println)

    val firstVersion: String = "1"

    val id: UUID   = uuidSupplier.get
    val producerId = uuidSupplier.get //TODO from jwt

    val catalogItem: Future[CatalogItem] = Future.fromTry {
      for {
        openapiDoc <- fileManager.store(uuidSupplier.get, producerId.toString, firstVersion, idl)
      } yield CatalogItem(
        id = id,
        producerId = producerId,
        name = name,
        audience = audience,
        status = "active",
        descriptors = Seq(
          CatalogDescriptor(
            id = uuidSupplier.get,
            description = description,
            version = firstVersion,
            docs = Seq(openapiDoc),
            voucherLifespan = voucherLifespan,
            technology = technology,
            status = Draft
          )
        )
      )
    }

    val commander: EntityRef[Command] = sharding.entityRefFor(CatalogPersistentBehavior.TypeKey, getShard(id.toString))

    val result: Future[StatusReply[CatalogItem]] =
      catalogItem.flatMap(ci => commander.ask(ref => AddCatalogItem(ci, ref)))
    onSuccess(result) {
      case statusReply if statusReply.isSuccess => createEService200(statusReply.getValue.toApi)
      case statusReply if statusReply.isError =>
        createEService400(Problem(Option(statusReply.getError.getMessage), status = 405, "some error"))
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
    val commander: EntityRef[Command]                    = sharding.entityRefFor(CatalogPersistentBehavior.TypeKey, getShard(eServiceId))
    val result: Future[StatusReply[Option[CatalogItem]]] = commander.ask(ref => GetCatalogItem(eServiceId, ref))
    onSuccess(result) {
      case statusReply if statusReply.isSuccess =>
        statusReply.getValue.fold(getEService404(Problem(None, status = 404, "some error")))(catalogItem =>
          getEService200(catalogItem.toApi)
        )
      case statusReply if statusReply.isError =>
        getEService400(Problem(Option(statusReply.getError.getMessage), status = 400, "some error"))
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
    val commanders: Seq[EntityRef[Command]] = (0 until settings.numberOfShards).map(shard =>
      sharding.entityRefFor(CatalogPersistentBehavior.TypeKey, getShard(shard.toString))
    )
    val catalogItem: Seq[CatalogItem] = commanders.flatMap(ref => getSlice(ref, 0, sliceSize))

    getEServices200(catalogItem.map(_.toApi))

  }

  /** Code: 200, Message: EService document retrieved, DataType: File
    * Code: 404, Message: EService not found, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */
  override def getEServiceDocument(eServiceId: String, documentId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerFile: ToEntityMarshaller[File],
    contexts: Seq[(String, String)]
  ): Route = {
    contexts.foreach(println)
    val commander: EntityRef[Command] = sharding.entityRefFor(CatalogPersistentBehavior.TypeKey, getShard(eServiceId))

    val result: Future[StatusReply[Option[CatalogItem]]] = commander.ask(ref => GetCatalogItem(eServiceId, ref))

    onSuccess(result) {
      case statusReply if statusReply.isSuccess =>
        val fileInfo: Option[(ContentType, Path)] = for {
          catalogItem <- statusReply.getValue
          fileInfo    <- catalogItem.extractFile(UUID.fromString(documentId))
        } yield fileInfo

        fileInfo.fold(getEServiceDocument404(Problem(None, status = 404, "some error"))) { case (contentType, file) =>
          complete(HttpEntity.fromFile(contentType, file.toFile))
        }
      case statusReply if statusReply.isError =>
        getEService400(Problem(Option(statusReply.getError.getMessage), status = 400, "some error"))
    }
  }

}
