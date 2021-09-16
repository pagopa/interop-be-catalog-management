package it.pagopa.pdnd.interop.uservice.catalogmanagement.api.impl

import akka.Done
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.{ContentType, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, onComplete, onSuccess}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.pattern.StatusReply
import cats.data.Validated.{Invalid, Valid}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.EServiceApiService
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common.system._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.error.{
  EServiceDescriptorNotFoundError,
  EServiceNotFoundError,
  ValidationError
}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.{FileManager, UUIDSupplier}

import java.io.{ByteArrayOutputStream, File}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.ImplicitParameter",
    "org.wartremover.warts.ToString",
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.Recursion",
    "org.wartremover.warts.Equals"
  )
)
class EServiceApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]],
  uuidSupplier: UUIDSupplier,
  fileManager: FileManager
)(implicit ec: ExecutionContext)
    extends EServiceApiService
    with Validation {

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
  }

  @inline private def getShard(id: String): String = Math.abs(id.hashCode % settings.numberOfShards).toString

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
        shard = getShard(catalogItem.id.toString)
        added <- getCommander(shard).ask(ref => AddCatalogItem(catalogItem, ref))
      } yield added

    onSuccess(result) {
      case statusReply if statusReply.isSuccess =>
        createEService200(statusReply.getValue.toApi)
      case statusReply if statusReply.isError =>
        createEService400(Problem(Option(statusReply.getError.getMessage), status = 405, "some error"))
    }
  }

  /** Code: 200, Message: EService Document created, DataType: EService
    * Code: 400, Message: Invalid input, DataType: Problem
    * Code: 404, Message: Not found, DataType: Problem
    */
  override def createEServiceDocument(
    eServiceId: String,
    descriptorId: String,
    kind: String,
    description: String,
    doc: (FileInfo, File)
  )(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {

    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    val isInterface: Boolean = kind match {
      case "interface" => true
      case "document"  => false
    }

    val result: Future[Option[CatalogItem]] = for {
      current  <- retrieveCatalogItem(commander, eServiceId)
      verified <- FileManager.verify(doc, current, descriptorId, isInterface)
      openapiDoc <- fileManager.store(
        id = uuidSupplier.get,
        eServiceId = eServiceId,
        descriptorId = descriptorId,
        description = description,
        interface = isInterface,
        fileParts = doc
      )
      updated <- commander.ask(ref =>
        UpdateCatalogItem(verified.updateFile(descriptorId, openapiDoc, isInterface), ref)
      )
    } yield updated

    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold(createEServiceDocument404(Problem(None, status = 404, "some error")))(ci =>
          createEServiceDocument200(ci.toApi)
        )
      case Failure(exception) =>
        createEServiceDocument400(Problem(Option(exception.getMessage), status = 400, "some error"))
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

    val shard: String                       = getShard(eServiceId)
    val result: Future[Option[CatalogItem]] = getCommander(shard).ask(ref => GetCatalogItem(eServiceId, ref))

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

    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    val result: Future[(ContentType, ByteArrayOutputStream)] =
      for {
        found       <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
        catalogItem <- found.toFuture(new RuntimeException(""))
        fileInfo <- catalogItem
          .extractFile(descriptorId = descriptorId, documentId = documentId)
          .toFuture(new RuntimeException(""))
        outputStream <- fileManager.get(fileInfo._3.toString)
      } yield (fileInfo._2, outputStream)

    onComplete(result) {
      case Success(tuple) =>
        complete(HttpEntity(tuple._1, tuple._2.toByteArray))
      case Failure(exception) =>
        getEService400(Problem(Option(exception.getMessage), status = 400, s"EService $eServiceId not found"))
    }
  }

  override def deleteDraft(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {

    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    val result: Future[StatusReply[Done]] = for {
      retrieved <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
      current   <- retrieved.toFuture(new RuntimeException("EService non found"))
      _         <- descriptorDeletable(current, descriptorId)
      _         <- current.getInterfacePath(descriptorId).fold(Future.successful(true))(path => fileManager.delete(path))
      _ <- current
        .getDocumentPaths(descriptorId)
        .fold(Future.successful(Seq.empty[Boolean]))(path => Future.traverse(path)(fileManager.delete))
      deleted <- commander.ask(ref => DeleteCatalogItemWithDescriptor(current, descriptorId, ref))
    } yield deleted

    onComplete(result) {
      case Success(statusReply) =>
        if (statusReply.isSuccess) deleteDraft204
        else deleteDraft400(Problem(Option(statusReply.getError.getMessage), status = 400, "some error"))
      case Failure(exception) =>
        deleteDraft400(Problem(Option(exception.getMessage), status = 400, "some error"))
    }
  }

  /** Code: 200, Message: EService Descriptor published, DataType: EService
    * Code: 400, Message: Invalid input, DataType: Problem
    * Code: 404, Message: Not found, DataType: Problem
    */
  override def updateDescriptor(
    eServiceId: String,
    descriptorId: String,
    eServiceDescriptorSeed: EServiceDescriptorSeed
  )(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {

    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    def mergeChanges(
      descriptor: CatalogDescriptor,
      eServiceDescriptorSeed: EServiceDescriptorSeed
    ): Either[ValidationError, CatalogDescriptor] = {
      val newStatus = eServiceDescriptorSeed.status.map(validateDescriptorStatus)

      newStatus match {
        case Some(Invalid(nel)) => Left(ValidationError(nel.toList))
        case Some(Valid(status)) =>
          Right(
            descriptor
              .copy(
                description = eServiceDescriptorSeed.description.orElse(descriptor.description),
                audience = eServiceDescriptorSeed.audience.getOrElse(descriptor.audience),
                voucherLifespan = eServiceDescriptorSeed.voucherLifespan.getOrElse(descriptor.voucherLifespan),
                status = status
              )
          )
        case None =>
          Right(
            descriptor.copy(
              description = eServiceDescriptorSeed.description.orElse(descriptor.description),
              audience = eServiceDescriptorSeed.audience.getOrElse(descriptor.audience),
              voucherLifespan = eServiceDescriptorSeed.voucherLifespan.getOrElse(descriptor.voucherLifespan)
            )
          )
      }

    }

    val result: Future[Option[CatalogItem]] = for {
      retrieved <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
      current   <- retrieved.toFuture(EServiceNotFoundError(eServiceId))
      toUpdateDescriptor <- current.descriptors
        .find(_.id.toString == descriptorId)
        .toFuture(EServiceDescriptorNotFoundError(eServiceId, descriptorId))
      updatedDescriptor <- mergeChanges(toUpdateDescriptor, eServiceDescriptorSeed).toFuture
      updatedItem = current.copy(descriptors =
        current.descriptors.filter(_.id.toString != descriptorId) :+ updatedDescriptor
      )
      updated <- commander.ask(ref => UpdateCatalogItem(updatedItem, ref))
    } yield updated

    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold(
          updateDescriptor500(
            Problem(None, status = 500, s"Error on update of descriptor $descriptorId on E-Service $eServiceId")
          )
        )(ci => updateDescriptor200(ci.toApi))
      case Failure(exception) =>
        exception match {
          case ex @ (_: EServiceNotFoundError | _: EServiceDescriptorNotFoundError) =>
            updateDescriptor404(
              Problem(
                Option(ex.getMessage),
                status = 404,
                s"Error on update of descriptor $descriptorId on E-Service $eServiceId"
              )
            )
          case ex =>
            updateDescriptor400(
              Problem(
                Option(ex.getMessage),
                status = 400,
                s"Error on update of descriptor $descriptorId on E-Service $eServiceId"
              )
            )
        }

    }
  }

  /** Code: 200, Message: E-Service updated, DataType: EService
    * Code: 404, Message: E-Service not found, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */
  override def updateEServiceById(eServiceId: String, updateEServiceSeed: UpdateEServiceSeed)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {
    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    val result: Future[Option[CatalogItem]] = for {
      current         <- retrieveCatalogItem(commander, eServiceId)
      updated         <- current.mergeWithSeed(updateEServiceSeed)
      updatedResponse <- commander.ask(ref => UpdateCatalogItem(updated, ref))
    } yield updatedResponse

    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold(updateEServiceById404(Problem(None, status = 404, "some error")))(ci =>
          updateEServiceById200(ci.toApi)
        )
      case Failure(exception) =>
        updateEServiceById400(Problem(Option(exception.getMessage), status = 400, "some error"))
    }
  }

  private def descriptorDeletable(catalogItem: CatalogItem, descriptorId: String): Future[Boolean] = {
    if (catalogItem.descriptors.exists(descriptor => descriptor.id.toString == descriptorId && descriptor.isDraft))
      Future.successful(true)
    else
      Future.failed(new RuntimeException(s"Descriptor $descriptorId cannot be deleted"))
  }

  private def getCommander(shard: String): EntityRef[Command] =
    sharding.entityRefFor(CatalogPersistentBehavior.TypeKey, shard)

  private def retrieveCatalogItem(commander: EntityRef[Command], eServiceId: String): Future[CatalogItem] = {
    for {
      retrieved <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
      current   <- retrieved.toFuture(new RuntimeException("EService non found"))
    } yield current
  }
}
