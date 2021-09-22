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
import it.pagopa.pdnd.interop.uservice.catalogmanagement.error._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.{FileManager, UUIDSupplier, VersionGenerator}

import java.io.{ByteArrayOutputStream, File}
import java.nio.file.Paths
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
        found         <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
        catalogItem   <- found.toFuture(EServiceNotFoundError(eServiceId))
        document      <- extractDocument(catalogItem, descriptorId, documentId)
        extractedFile <- extractFile(document)
        outputStream  <- fileManager.get(extractedFile.path.toString)
      } yield (extractedFile.contentType, outputStream)

    onComplete(result) {
      case Success(tuple) =>
        complete(HttpEntity(tuple._1, tuple._2.toByteArray))
      case Failure(exception) =>
        getEService400(Problem(Option(exception.getMessage), status = 400, s"EService $eServiceId not found"))
    }
  }

  private def extractDocument(
    catalogItem: CatalogItem,
    descriptorId: String,
    documentId: String
  ): Future[CatalogDocument] = {

    def lookupDocument(catalogDescriptor: CatalogDescriptor): Option[CatalogDocument] = {
      val interface = catalogDescriptor.interface.fold(Seq.empty[CatalogDocument])(doc => Seq(doc))
      (interface ++: catalogDescriptor.docs).find(_.id.toString == documentId)
    }

    catalogItem.descriptors
      .find(_.id.toString == descriptorId)
      .flatMap(lookupDocument)
      .toFuture(DocumentNotFoundError(catalogItem.id.toString, descriptorId, documentId))
  }

  private def extractFile(catalogDocument: CatalogDocument)(implicit ec: ExecutionContext): Future[ExtractedFile] = {
    val contentType: Future[ContentType] = ContentType
      .parse(catalogDocument.contentType)
      .fold(ex => Future.failed(ContentTypeParsingError(catalogDocument, ex)), Future.successful)

    contentType.map(ExtractedFile(_, Paths.get(catalogDocument.path)))
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
    eServiceDescriptorSeed: UpdateEServiceDescriptorSeed
  )(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {

    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    def mergeChanges(
      descriptor: CatalogDescriptor,
      eServiceDescriptorSeed: UpdateEServiceDescriptorSeed
    ): Either[ValidationError, CatalogDescriptor] = {
      val newStatus = validateDescriptorStatus(eServiceDescriptorSeed.status)
      newStatus match {
        case Invalid(nel) => Left(ValidationError(nel.toList))
        case Valid(status) =>
          Right(
            descriptor
              .copy(
                description = eServiceDescriptorSeed.description,
                audience = eServiceDescriptorSeed.audience,
                voucherLifespan = eServiceDescriptorSeed.voucherLifespan,
                status = status
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
          updateDescriptor400(
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

  /** Code: 201, Message: EService Descriptor created., DataType: EServiceDescriptor
    * Code: 400, Message: Invalid input, DataType: Problem
    * Code: 404, Message: Not found, DataType: Problem
    * Code: 500, Message: Not found, DataType: Problem
    */
  override def createDescriptor(eServiceId: String, eServiceDescriptorSeed: EServiceDescriptorSeed)(implicit
    toEntityMarshallerEServiceDescriptor: ToEntityMarshaller[EServiceDescriptor],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    val result: Future[CatalogDescriptor] = for {
      current     <- retrieveCatalogItem(commander, eServiceId)
      nextVersion <- VersionGenerator.next(current.currentVersion).toFuture
      createdCatalogDescriptor = CatalogDescriptor(
        id = uuidSupplier.get,
        description = eServiceDescriptorSeed.description,
        version = nextVersion,
        interface = None,
        docs = Seq.empty[CatalogDocument],
        status = Draft,
        voucherLifespan = eServiceDescriptorSeed.voucherLifespan,
        audience = eServiceDescriptorSeed.audience
      )
      _ <- commander.ask(ref => UpdateCatalogItem(current.addDescriptor(createdCatalogDescriptor), ref))
    } yield createdCatalogDescriptor

    onComplete(result) {
      case Success(descriptor) => createDescriptor200(descriptor.toApi)
      case Failure(exception) =>
        createDescriptor400(Problem(Option(exception.getMessage), status = 400, "some error"))
    }
  }

  /** Code: 204, Message: Document deleted.
    * Code: 404, Message: E-Service descriptor document not found, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */
  override def deleteEServiceDocument(eServiceId: String, descriptorId: String, documentId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    val result: Future[Option[CatalogItem]] = for {
      found         <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
      catalogItem   <- found.toFuture(EServiceNotFoundError(eServiceId))
      document      <- extractDocument(catalogItem, descriptorId, documentId)
      extractedFile <- extractFile(document)
      _             <- fileManager.delete(extractedFile.path.toString)
      updatedEservice = catalogItem.removeDocument(descriptorId, documentId)
      updated <- commander.ask(ref => UpdateCatalogItem(updatedEservice, ref))
    } yield updated

    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold(
          deleteEServiceDocument400(
            Problem(
              None,
              status = 404,
              s"Error on deletion of $documentId on descriptor $descriptorId on E-Service $eServiceId"
            )
          )
        )(_ => deleteEServiceDocument204)
      case Failure(exception) =>
        exception match {
          case ex @ (_: EServiceNotFoundError | _: EServiceDescriptorNotFoundError) =>
            deleteEServiceDocument404(
              Problem(
                Option(ex.getMessage),
                status = 404,
                s"$documentId on descriptor $descriptorId on E-Service $eServiceId not found"
              )
            )
          case ex =>
            deleteEServiceDocument400(
              Problem(
                Option(ex.getMessage),
                status = 400,
                s"Error on deletion of $documentId on descriptor $descriptorId on E-Service $eServiceId"
              )
            )
        }
    }
  }

  /** Code: 204, Message: EService Descriptor status archived
    * Code: 400, Message: Invalid input, DataType: Problem
    * Code: 404, Message: Not found, DataType: Problem
    */
  override def archiveDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val result = updateDescriptorStatus(eServiceId, descriptorId, Archived)
    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold(
          archiveDescriptor400(
            Problem(None, status = 404, s"Error on archiving of $descriptorId on E-Service $eServiceId")
          )
        )(_ => archiveDescriptor204)
      case Failure(ex) =>
        archiveDescriptor400(
          Problem(Option(ex.getMessage), status = 400, s"Error on archiving of $descriptorId on E-Service $eServiceId")
        )
    }
  }

  /** Code: 204, Message: EService Descriptor status deprecated
    * Code: 400, Message: Invalid input, DataType: Problem
    * Code: 404, Message: Not found, DataType: Problem
    */
  override def deprecateDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val result = updateDescriptorStatus(eServiceId, descriptorId, Deprecated)
    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold(
          deprecateDescriptor400(
            Problem(None, status = 404, s"Error on deprecating of $descriptorId on E-Service $eServiceId")
          )
        )(_ => deprecateDescriptor204)
      case Failure(ex) =>
        deprecateDescriptor400(
          Problem(
            Option(ex.getMessage),
            status = 400,
            s"Error on deprecating of $descriptorId on E-Service $eServiceId"
          )
        )
    }
  }

  /** Code: 204, Message: EService Descriptor status changed in draft
    * Code: 400, Message: Invalid input, DataType: Problem
    * Code: 404, Message: Not found, DataType: Problem
    */
  override def draftDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val result = updateDescriptorStatus(eServiceId, descriptorId, Draft)
    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold(
          draftDescriptor400(
            Problem(None, status = 404, s"Error on change state to draft of $descriptorId on E-Service $eServiceId")
          )
        )(_ => draftDescriptor204)
      case Failure(ex) =>
        draftDescriptor400(
          Problem(
            Option(ex.getMessage),
            status = 400,
            s"Error on change state to draft of $descriptorId on E-Service $eServiceId"
          )
        )
    }
  }

  /** Code: 204, Message: EService Descriptor status published.
    * Code: 400, Message: Invalid input, DataType: Problem
    * Code: 404, Message: Not found, DataType: Problem
    */
  override def publishDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val result = updateDescriptorStatus(eServiceId, descriptorId, Published)

    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold(
          publishDescriptor400(
            Problem(None, status = 404, s"Error on publication of $descriptorId on E-Service $eServiceId")
          )
        )(_ => publishDescriptor204)
      case Failure(ex) =>
        publishDescriptor400(
          Problem(
            Option(ex.getMessage),
            status = 400,
            s"Error on publication of $descriptorId on E-Service $eServiceId"
          )
        )
    }
  }

  /* utility method for descriptor status updates
   */
  private def updateDescriptorStatus(
    eServiceId: String,
    descriptorId: String,
    status: CatalogDescriptorStatus
  ): Future[Option[CatalogItem]] = {

    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    def mergeChanges(
      descriptor: CatalogDescriptor,
      updateEServiceDescriptorStatus: CatalogDescriptorStatus
    ): Either[ValidationError, CatalogDescriptor] = {
      Right(descriptor.copy(status = updateEServiceDescriptorStatus))
    }

    for {
      retrieved <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
      current   <- retrieved.toFuture(EServiceNotFoundError(eServiceId))
      toUpdateDescriptor <- current.descriptors
        .find(_.id.toString == descriptorId)
        .toFuture(EServiceDescriptorNotFoundError(eServiceId, descriptorId))
      updatedDescriptor <- mergeChanges(toUpdateDescriptor, status).toFuture
      updatedItem = current.copy(descriptors =
        current.descriptors.filter(_.id.toString != descriptorId) :+ updatedDescriptor
      )
      updated <- commander.ask(ref => UpdateCatalogItem(updatedItem, ref))
    } yield updated
  }

  /** Code: 200, Message: Updated EService document, DataType: File
    * Code: 404, Message: EService not found, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */
  override def updateEServiceDocument(
    eServiceId: String,
    descriptorId: String,
    documentId: String,
    updateEServiceDescriptorDocumentSeed: UpdateEServiceDescriptorDocumentSeed
  )(implicit
    toEntityMarshallerEServiceDoc: ToEntityMarshaller[EServiceDoc],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    val result: Future[Option[CatalogDocument]] = for {
      found       <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
      catalogItem <- found.toFuture(EServiceNotFoundError(eServiceId))
      document    <- extractDocument(catalogItem, descriptorId, documentId)
      updatedDocument = document.copy(description = updateEServiceDescriptorDocumentSeed.description)
      updated <- commander.ask(ref =>
        UpdateDocument(
          eServiceId = eServiceId,
          descriptorId = descriptorId,
          documentId = documentId,
          updatedDocument,
          ref
        )
      )
    } yield updated

    onComplete(result) {
      case Success(document) =>
        document.fold(
          updateEServiceDocument404(
            Problem(
              None,
              status = 404,
              s"Error on update of $documentId on descriptor $descriptorId on E-Service $eServiceId"
            )
          )
        )(catalogDocument => updateEServiceDocument200(catalogDocument.toApi))
      case Failure(exception) =>
        exception match {
          case ex @ (_: EServiceNotFoundError | _: EServiceDescriptorNotFoundError) =>
            deleteEServiceDocument404(
              Problem(
                Option(ex.getMessage),
                status = 404,
                s"$documentId on descriptor $descriptorId on E-Service $eServiceId not found"
              )
            )
          case ex =>
            deleteEServiceDocument400(
              Problem(
                Option(ex.getMessage),
                status = 400,
                s"Error on deletion of $documentId on descriptor $descriptorId on E-Service $eServiceId"
              )
            )
        }
    }
  }

  /** Code: 200, Message: Cloned EService with a new draft descriptor updated., DataType: EService
    * Code: 400, Message: Invalid input, DataType: Problem
    * Code: 404, Message: Not found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def cloneEServiceByDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {
    contexts.foreach(println)

    val shard = getShard(eServiceId)

    val result: Future[StatusReply[CatalogItem]] =
      for {
        service        <- getCommander(shard).ask(ref => GetCatalogItem(eServiceId, ref))
        serviceToClone <- service.toFuture(EServiceNotFoundError(eServiceId))
        descriptorToClone <- serviceToClone.descriptors
          .find(_.id.toString == descriptorId)
          .toFuture(EServiceDescriptorNotFoundError(eServiceId, descriptorId))
        clonedInterface <-
          descriptorToClone.interface.fold(Future.successful[Option[CatalogDocument]](None))(interface =>
            interface.cloneDocument(fileManager)(uuidSupplier.get, eServiceId, descriptorId).map(d => Some(d))
          )
        clonedDocuments <- Future.traverse(descriptorToClone.docs)(
          _.cloneDocument(fileManager)(uuidSupplier.get, eServiceId, descriptorId)
        )
        clonedService <- cloneItemAsNewDraft(serviceToClone, descriptorToClone, clonedInterface, clonedDocuments)
        cloned        <- getCommander(getShard(clonedService.id.toString)).ask(ref => AddClonedCatalogItem(clonedService, ref))
      } yield cloned

    onComplete(result) {
      case Success(document) =>
        document match {
          case statusReply if statusReply.isSuccess =>
            cloneEServiceByDescriptor200(statusReply.getValue.toApi)
          case statusReply if statusReply.isError =>
            cloneEServiceByDescriptor400(Problem(Option(statusReply.getError.getMessage), status = 405, "some error"))
        }

      case Failure(exception) =>
        exception match {
          case ex @ (_: EServiceNotFoundError | _: EServiceDescriptorNotFoundError) =>
            cloneEServiceByDescriptor404(
              Problem(Option(ex.getMessage), status = 404, s"$descriptorId on E-Service $eServiceId not found")
            )
          case ex =>
            cloneEServiceByDescriptor400(
              Problem(
                Option(ex.getMessage),
                status = 400,
                s"Error on cloning descriptor $descriptorId on E-Service $eServiceId"
              )
            )
        }
    }
  }

  private def cloneItemAsNewDraft(
    serviceToClone: CatalogItem,
    descriptorToClone: CatalogDescriptor,
    clonedInterface: Option[CatalogDocument],
    clonedDocuments: Seq[CatalogDocument]
  ): Future[CatalogItem] = {

    for {
      version <- VersionGenerator.next(None).toFuture
      descriptor = CatalogDescriptor(
        id = uuidSupplier.get,
        version = version,
        description = descriptorToClone.description,
        interface = clonedInterface,
        docs = clonedDocuments,
        status = Draft,
        audience = descriptorToClone.audience,
        voucherLifespan = descriptorToClone.voucherLifespan
      )
    } yield CatalogItem(
      id = uuidSupplier.get,
      producerId = serviceToClone.producerId,
      name = serviceToClone.name,
      description = serviceToClone.description,
      technology = serviceToClone.technology,
      attributes = serviceToClone.attributes,
      descriptors = Seq(descriptor)
    )
  }

  /** Code: 204, Message: EService deleted
    * Code: 400, Message: Invalid input, DataType: Problem
    * Code: 404, Message: Not found, DataType: Problem
    */
  override def deleteEService(
    eServiceId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {
    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    val result: Future[StatusReply[Done]] = for {
      retrieved <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
      service   <- retrieved.toFuture(EServiceNotFoundError(eServiceId))
      _         <- canBeDeleted(service)
      deleted   <- commander.ask(ref => DeleteCatalogItem(service.id.toString, ref))
    } yield deleted

    onComplete(result) {
      case Success(statusReply) =>
        if (statusReply.isSuccess) deleteEService204
        else deleteEService400(Problem(Option(statusReply.getError.getMessage), status = 400, "some error"))
      case Failure(exception) =>
        deleteEService400(Problem(Option(exception.getMessage), status = 400, "some error"))
    }
  }

  private def canBeDeleted(catalogItem: CatalogItem): Future[Boolean] = {
    catalogItem.descriptors match {
      case Nil => Future.successful(true)
      case _   => Future.failed(new RuntimeException(s"E-Service ${catalogItem.id.toString} cannot be deleted"))
    }
  }

}
