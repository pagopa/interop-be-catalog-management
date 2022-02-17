package it.pagopa.pdnd.interop.uservice.catalogmanagement.api.impl

import akka.Done
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.{ContentType, StatusCodes}
import akka.http.scaladsl.server.Directives.{onComplete, onSuccess}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.pattern.StatusReply
import cats.implicits.toTraverseOps
import com.typesafe.scalalogging.Logger
import it.pagopa.pdnd.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.pdnd.interop.commons.utils.AkkaUtils
import it.pagopa.pdnd.interop.commons.utils.TypeConversions.{EitherOps, OptionOps}
import it.pagopa.pdnd.interop.commons.utils.service.UUIDSupplier
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.EServiceApiService
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common.system._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.error.CatalogManagementErrors._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.{CatalogFileManager, VersionGenerator}
import org.slf4j.LoggerFactory

import java.io.File
import java.nio.file.Paths
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class EServiceApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]],
  uuidSupplier: UUIDSupplier,
  catalogFileManager: CatalogFileManager
)(implicit ec: ExecutionContext)
    extends EServiceApiService {

  val logger = Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

  private lazy val INTERFACE = "INTERFACE"
  private lazy val DOCUMENT  = "DOCUMENT"

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
  }

  @inline private def getShard(id: String): String = AkkaUtils.getShard(id, settings.numberOfShards)

  /** Code: 200, Message: EService created, DataType: EService
    * Code: 400, Message: Invalid input, DataType: Problem
    */
  override def createEService(eServiceSeed: EServiceSeed)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info("Creating e-service {} for producer {}", eServiceSeed.name, eServiceSeed.producerId)

    val result: Future[StatusReply[CatalogItem]] =
      for {
        catalogItem <- CatalogItem.create(eServiceSeed, uuidSupplier)
        shard = getShard(catalogItem.id.toString)
        added <- getCommander(shard).ask(ref => AddCatalogItem(catalogItem, ref))
      } yield added

    onComplete(result) {
      case Success(statusReply) if statusReply.isSuccess =>
        createEService200(statusReply.getValue.toApi)
      case Success(statusReply) =>
        logger.error(
          "Error while creating e-service {} for producer {}",
          eServiceSeed.name,
          eServiceSeed.producerId,
          statusReply.getError
        )
        createEService400(problemOf(StatusCodes.Conflict, EServiceAlreadyExistingError(eServiceSeed.name)))
      case Failure(ex) =>
        logger.error(
          "Error while creating e-service {} for producer {}",
          eServiceSeed.name,
          eServiceSeed.producerId,
          ex
        )
        createEService400(problemOf(StatusCodes.BadRequest, EServiceError))
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
    logger.info(
      "Creating e-service document of kind {} for e-service {} and descriptor {}",
      kind,
      eServiceId,
      descriptorId
    )

    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    val isInterface: Boolean = kind match {
      case INTERFACE => true
      case DOCUMENT  => false
    }

    val result: Future[Option[CatalogItem]] = for {
      current  <- retrieveCatalogItem(commander, eServiceId)
      verified <- CatalogFileManager.verify(doc, current, descriptorId, isInterface)
      document <- catalogFileManager.store(id = uuidSupplier.get, description = description, fileParts = doc)
      _        <- commander.ask(ref => AddCatalogItemDocument(verified.id.toString, descriptorId, document, isInterface, ref))
      updated  <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
    } yield updated

    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold({
          logger.error(
            "Failure in creation of e-service document of kind {} for e-service {} and descriptor {} - not found",
            kind,
            eServiceId,
            descriptorId
          )
          createEServiceDocument404(
            problemOf(StatusCodes.NotFound, DocumentCreationNotFound(kind, eServiceId, descriptorId))
          )
        })(ci => createEServiceDocument200(ci.toApi))
      case Failure(exception) =>
        logger.error(
          "Failure in creation of e-service document of kind {} for e-service {} and descriptor {}",
          kind,
          eServiceId,
          descriptorId,
          exception
        )
        createEServiceDocument400(
          problemOf(StatusCodes.BadRequest, DocumentCreationNotFound(kind, eServiceId, descriptorId))
        )
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
    logger.info("Getting e-service {}", eServiceId)

    val shard: String                       = getShard(eServiceId)
    val result: Future[Option[CatalogItem]] = getCommander(shard).ask(ref => GetCatalogItem(eServiceId, ref))

    onSuccess(result) {
      case Some(catalogItem) => getEService200(catalogItem.toApi)
      case None => {
        logger.error("E-service {} not found", eServiceId)
        getEService404(problemOf(StatusCodes.NotFound, EServiceNotFoundError))
      }
    }
  }

  /** Code: 200, Message: A list of EService, DataType: Seq[EService]
    */
  override def getEServices(producerId: Option[String], state: Option[String])(implicit
    toEntityMarshallerEServicearray: ToEntityMarshaller[Seq[EService]],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info("Getting e-service for producer {} and state {}", producerId.toString, state.toString)
    val sliceSize = 100

    val commanders: Seq[EntityRef[Command]] =
      (0 until settings.numberOfShards).map(shard => getCommander(shard.toString))

    def getSlice(
      commander: EntityRef[Command],
      from: Int,
      to: Int,
      producerId: Option[String],
      state: Option[CatalogDescriptorState]
    ): LazyList[CatalogItem] = {
      val slice: Seq[CatalogItem] = Await
        .result(commander.ask(ref => ListCatalogItem(from, to, producerId, state, ref)), Duration.Inf)

      if (slice.isEmpty)
        LazyList.empty[CatalogItem]
      else
        getSlice(commander, to, to + sliceSize, producerId, state) #::: slice.to(LazyList)
    }

    val stringToState: String => Either[Throwable, CatalogDescriptorState] =
      EServiceDescriptorState.fromValue(_).map(CatalogDescriptorState.fromApi)

    val stateEnum = state.traverse(stringToState)

    val result = stateEnum.map { state =>
      commanders.flatMap(ref => getSlice(ref, 0, sliceSize, producerId, state))
    }

    result match {
      case Right(items) => getEServices200(items.map(_.toApi))
      case Left(error) =>
        logger.error(
          "Error while getting e-service for producer {} and state {}",
          producerId.toString,
          state.toString,
          error
        )
        getEServices400(problemOf(StatusCodes.BadRequest, EServiceRetrievalError))
    }

  }

  /** Code: 200, Message: EService document retrieved, DataType: EServiceDoc
    * Code: 404, Message: EService not found, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */
  override def getEServiceDocument(eServiceId: String, descriptorId: String, documentId: String)(implicit
    toEntityMarshallerEServiceDoc: ToEntityMarshaller[EServiceDoc],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info(
      "Getting e-service document {} for e-service {} and descriptor {}",
      documentId,
      eServiceId,
      descriptorId
    )

    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    val result: Future[EServiceDoc] =
      for {
        found       <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
        catalogItem <- found.toFuture(EServiceNotFoundError(eServiceId))
        document    <- extractDocument(catalogItem, descriptorId, documentId)
      } yield document.toApi

    onComplete(result) {
      case Success(doc) => getEServiceDocument200(doc)
      case Failure(exception) =>
        logger.error(
          "Error while getting e-service document {} for e-service {} and descriptor {}",
          documentId,
          eServiceId,
          descriptorId,
          exception
        )
        exception match {
          case ex: EServiceNotFoundError =>
            getEService404(problemOf(StatusCodes.NotFound, ex))
          case _ => getEService400(problemOf(StatusCodes.BadRequest, DocumentRetrievalBadRequest(documentId)))
        }

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

    logger.info("Deleting draft version of descriptor {} for e-service {}", descriptorId, eServiceId)

    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    val result: Future[StatusReply[Done]] = for {
      retrieved <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
      current   <- retrieved.toFuture(new RuntimeException("EService non found"))
      _         <- descriptorDeletable(current, descriptorId)
      _ <- current
        .getInterfacePath(descriptorId)
        .fold(Future.successful(true))(path => catalogFileManager.delete(path))
      _ <- current
        .getDocumentPaths(descriptorId)
        .fold(Future.successful(Seq.empty[Boolean]))(path => Future.traverse(path)(catalogFileManager.delete))
      deleted <- commander.ask(ref => DeleteCatalogItemWithDescriptor(current, descriptorId, ref))
    } yield deleted

    onComplete(result) {
      case Success(statusReply) =>
        if (statusReply.isSuccess) deleteDraft204
        else {
          logger.error(
            "Error while deleting draft version of descriptor {} for e-service {}",
            descriptorId,
            eServiceId,
            statusReply.getError
          )
          deleteDraft400(problemOf(StatusCodes.BadRequest, DescriptorDeleteDraftBadRequest(eServiceId, descriptorId)))
        }
      case Failure(exception) =>
        logger.error(
          "Error while deleting draft version of descriptor {} for e-service {}",
          descriptorId,
          eServiceId,
          exception
        )
        deleteDraft400(problemOf(StatusCodes.BadRequest, DescriptorDeleteDraftBadRequest(eServiceId, descriptorId)))
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

    logger.info("Updating descriptor {} for e-service {}", descriptorId, eServiceId)
    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    def mergeChanges(
      descriptor: CatalogDescriptor,
      eServiceDescriptorSeed: UpdateEServiceDescriptorSeed
    ): CatalogDescriptor =
      descriptor
        .copy(
          description = eServiceDescriptorSeed.description,
          audience = eServiceDescriptorSeed.audience,
          voucherLifespan = eServiceDescriptorSeed.voucherLifespan,
          dailyCallsMaxNumber = eServiceDescriptorSeed.dailyCallsMaxNumber,
          state = CatalogDescriptorState.fromApi(eServiceDescriptorSeed.state)
        )

    val result: Future[Option[CatalogItem]] = for {
      retrieved <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
      current   <- retrieved.toFuture(EServiceNotFoundError(eServiceId))
      toUpdateDescriptor <- current.descriptors
        .find(_.id.toString == descriptorId)
        .toFuture(EServiceDescriptorNotFoundError(eServiceId, descriptorId))
      updatedDescriptor = mergeChanges(toUpdateDescriptor, eServiceDescriptorSeed)
      updatedItem = current.copy(descriptors =
        current.descriptors.filter(_.id.toString != descriptorId) :+ updatedDescriptor
      )
      updated <- commander.ask(ref => UpdateCatalogItem(updatedItem, ref))
    } yield updated

    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold({
          logger
            .error("Error while updating descriptor {} for e-service {} - {} - bad request", descriptorId, eServiceId)
          updateDescriptor400(problemOf(StatusCodes.BadRequest, DescriptorUpdateBadRequest(eServiceId, descriptorId)))
        })(ci => updateDescriptor200(ci.toApi))
      case Failure(exception) =>
        logger.error("Error while updating descriptor {} for e-service {}", descriptorId, eServiceId, exception)
        exception match {
          case ex: EServiceNotFoundError =>
            updateDescriptor404(problemOf(StatusCodes.NotFound, ex))
          case ex: EServiceDescriptorNotFoundError =>
            updateDescriptor404(problemOf(StatusCodes.NotFound, ex))
          case _ =>
            updateDescriptor400(problemOf(StatusCodes.BadRequest, DescriptorUpdateBadRequest(eServiceId, descriptorId)))
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
    logger.info("Updating e-service by id {}", eServiceId)
    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    val result: Future[Option[CatalogItem]] = for {
      current         <- retrieveCatalogItem(commander, eServiceId)
      updated         <- current.mergeWithSeed(updateEServiceSeed)
      updatedResponse <- commander.ask(ref => UpdateCatalogItem(updated, ref))
    } yield updatedResponse

    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold({
          logger.error("Error while updating e-service by id {} - not found", eServiceId)
          updateEServiceById404(problemOf(StatusCodes.NotFound, EServiceNotFoundError(eServiceId)))
        })(ci => updateEServiceById200(ci.toApi))
      case Failure(exception) =>
        logger.error("Error while updating e-service by id {}", eServiceId, exception)
        updateEServiceById400(problemOf(StatusCodes.BadRequest, EServiceUpdateError(eServiceId)))
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
    logger.info("Creating descriptor for e-service {}", eServiceId)
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
        state = Draft,
        voucherLifespan = eServiceDescriptorSeed.voucherLifespan,
        audience = eServiceDescriptorSeed.audience,
        dailyCallsMaxNumber = eServiceDescriptorSeed.dailyCallsMaxNumber
      )
      _ <- commander.ask(ref => AddCatalogItemDescriptor(current.id.toString, createdCatalogDescriptor, ref))
    } yield createdCatalogDescriptor

    onComplete(result) {
      case Success(descriptor) => createDescriptor200(descriptor.toApi)
      case Failure(exception) =>
        logger.error("Error while creating descriptor for e-service {}", eServiceId, exception)
        createDescriptor400(problemOf(StatusCodes.BadRequest, DescriptorCreationBadRequest(eServiceId)))
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
    logger.info("Delete document {} of descriptor {} for e-service {}", documentId, descriptorId, eServiceId)
    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    val result: Future[StatusReply[Done]] = for {
      found         <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
      catalogItem   <- found.toFuture(EServiceNotFoundError(eServiceId))
      document      <- extractDocument(catalogItem, descriptorId, documentId)
      extractedFile <- extractFile(document)
      _             <- catalogFileManager.delete(extractedFile.path.toString)
      updated       <- commander.ask(ref => DeleteCatalogItemDocument(catalogItem.id.toString, descriptorId, documentId, ref))
    } yield updated

    onComplete(result) {
      case Success(statusReply) =>
        if (statusReply.isSuccess) deleteEServiceDocument204
        else
          deleteEServiceDocument400({
            logger.error(
              "Error while deleting document {} of descriptor {} for e-service {} - bad request",
              documentId,
              descriptorId,
              eServiceId
            )
            problemOf(
              StatusCodes.BadRequest,
              DeleteEServiceDocumentErrorBadRequest(documentId, descriptorId, eServiceId)
            )
          })

      case Failure(exception) =>
        logger.error(
          "Error while deleting document {} of descriptor {} for e-service {}",
          documentId,
          descriptorId,
          eServiceId,
          exception
        )
        exception match {
          case ex: EServiceNotFoundError =>
            deleteEServiceDocument404(problemOf(StatusCodes.NotFound, ex))
          case ex: EServiceDescriptorNotFoundError =>
            deleteEServiceDocument404(problemOf(StatusCodes.NotFound, ex))
          case _ =>
            deleteEServiceDocument400(
              problemOf(
                StatusCodes.BadRequest,
                DeleteEServiceDocumentErrorBadRequest(documentId, descriptorId, eServiceId)
              )
            )
        }
    }
  }

  /** Code: 204, Message: EService Descriptor state archived
    * Code: 400, Message: Invalid input, DataType: Problem
    * Code: 404, Message: Not found, DataType: Problem
    */
  override def archiveDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info("Archiviation of descriptor {} of e-service {}", descriptorId, eServiceId)
    val result = updateDescriptorState(eServiceId, descriptorId, Archived)
    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold({
          logger
            .error("Error during archiviation of descriptor {} of e-service {} - bad request", descriptorId, eServiceId)
          archiveDescriptor400(problemOf(StatusCodes.BadRequest, DescriptorArchiveBadRequest(eServiceId, descriptorId)))
        })(_ => archiveDescriptor204)
      case Failure(ex) =>
        logger.error("Error during archiviation of descriptor {} of e-service {}", descriptorId, eServiceId, ex)
        archiveDescriptor400(problemOf(StatusCodes.BadRequest, DescriptorArchiveBadRequest(eServiceId, descriptorId)))
    }
  }

  /** Code: 204, Message: EService Descriptor state deprecated
    * Code: 400, Message: Invalid input, DataType: Problem
    * Code: 404, Message: Not found, DataType: Problem
    */
  override def deprecateDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info("Deprecating descriptor {} of e-service {}", descriptorId, eServiceId)
    val result = updateDescriptorState(eServiceId, descriptorId, Deprecated)
    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold({
          logger
            .error("Error during deprecation of descriptor {} of e-service {} - bad request", descriptorId, eServiceId)
          deprecateDescriptor400(
            problemOf(StatusCodes.BadRequest, DescriptorDeprecationBadRequest(eServiceId, descriptorId))
          )
        })(_ => deprecateDescriptor204)
      case Failure(ex) =>
        logger.error("Error during deprecation of descriptor {} of e-service {}", descriptorId, eServiceId, ex)
        deprecateDescriptor400(problemOf(StatusCodes.BadRequest, DescriptorDeprecationError(eServiceId, descriptorId)))
    }
  }

  /** Code: 204, Message: EService Descriptor state suspended
    * Code: 400, Message: Invalid input, DataType: Problem
    * Code: 404, Message: Not found, DataType: Problem
    */
  override def suspendDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info("Suspending descriptor {} of e-service {}", descriptorId, eServiceId)
    val result = updateDescriptorState(eServiceId, descriptorId, Suspended)
    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold({
          logger
            .error("Error during suspension of descriptor {} of e-service {} - bad request", descriptorId, eServiceId)
          suspendDescriptor400(
            problemOf(StatusCodes.BadRequest, DescriptorSuspensionBadRequest(eServiceId, descriptorId))
          )
        })(_ => suspendDescriptor204)
      case Failure(ex) =>
        logger.error("Error during suspension of descriptor {} of e-service {}", descriptorId, eServiceId, ex)
        suspendDescriptor400(problemOf(StatusCodes.BadRequest, DescriptorSuspensionError(eServiceId, descriptorId)))
    }
  }

  /** Code: 204, Message: EService Descriptor state changed in draft
    * Code: 400, Message: Invalid input, DataType: Problem
    * Code: 404, Message: Not found, DataType: Problem
    */
  override def draftDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info("Moving descriptor {} of e-service {} to draft state", descriptorId, eServiceId)
    val result = updateDescriptorState(eServiceId, descriptorId, Draft)
    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold({
          logger
            .error("Error while making descriptor {} of e-service {} as draft - bad request", descriptorId, eServiceId)
          draftDescriptor400(problemOf(StatusCodes.BadRequest, DescriptorDraftBadRequest(eServiceId, descriptorId)))
        })(_ => draftDescriptor204)
      case Failure(ex) =>
        logger.error("Error while making descriptor {} of e-service {} as draft", descriptorId, eServiceId, ex)
        draftDescriptor400(problemOf(StatusCodes.BadRequest, DescriptorDraftError(eServiceId, descriptorId)))
    }
  }

  /** Code: 204, Message: EService Descriptor state published.
    * Code: 400, Message: Invalid input, DataType: Problem
    * Code: 404, Message: Not found, DataType: Problem
    */
  override def publishDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info("Publishing descriptor {} of e-service {}", descriptorId, eServiceId)
    val result = updateDescriptorState(eServiceId, descriptorId, Published)

    onComplete(result) {
      case Success(catalogItem) =>
        catalogItem.fold({
          logger.error("Error while publishing descriptor {} of e-service {} - bad request", descriptorId, eServiceId)
          publishDescriptor400(problemOf(StatusCodes.BadRequest, DescriptorPublishBadRequest(eServiceId, descriptorId)))
        })(_ => publishDescriptor204)
      case Failure(ex) =>
        logger.error("Error while publishing descriptor {} of e-service {}", descriptorId, eServiceId, ex)
        publishDescriptor400(problemOf(StatusCodes.BadRequest, DescriptorPublishError(eServiceId, descriptorId)))
    }
  }

  /* utility method for descriptor state updates
   */
  private def updateDescriptorState(
    eServiceId: String,
    descriptorId: String,
    state: CatalogDescriptorState
  ): Future[Option[CatalogDescriptor]] = {

    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    def mergeChanges(
      descriptor: CatalogDescriptor,
      updateEServiceDescriptorState: CatalogDescriptorState
    ): Either[ValidationError, CatalogDescriptor] = {
      Right(descriptor.copy(state = updateEServiceDescriptorState))
    }

    for {
      retrieved <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
      current   <- retrieved.toFuture(EServiceNotFoundError(eServiceId))
      toUpdateDescriptor <- current.descriptors
        .find(_.id.toString == descriptorId)
        .toFuture(EServiceDescriptorNotFoundError(eServiceId, descriptorId))
      updatedDescriptor <- mergeChanges(toUpdateDescriptor, state).toFuture
      updated           <- commander.ask(ref => UpdateCatalogItemDescriptor(current.id.toString, updatedDescriptor, ref))
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
    logger.info("Updating document {} of descriptor {} of e-service {}", documentId, descriptorId, eServiceId)
    val shard: String = getShard(eServiceId)

    val commander: EntityRef[Command] = getCommander(shard)

    val result: Future[Option[CatalogDocument]] = for {
      found       <- commander.ask(ref => GetCatalogItem(eServiceId, ref))
      catalogItem <- found.toFuture(EServiceNotFoundError(eServiceId))
      document    <- extractDocument(catalogItem, descriptorId, documentId)
      updatedDocument = document.copy(description = updateEServiceDescriptorDocumentSeed.description)
      updated <- commander.ask(ref =>
        UpdateCatalogItemDocument(
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
        document.fold({
          logger.error(
            "Error while updating document {} of descriptor {} of e-service {} - not found",
            documentId,
            descriptorId,
            eServiceId
          )
          updateEServiceDocument404(
            problemOf(StatusCodes.NotFound, DocumentUpdateNotFound(documentId, descriptorId, eServiceId))
          )
        })(catalogDocument => updateEServiceDocument200(catalogDocument.toApi))
      case Failure(exception) =>
        logger.error(
          "Error while updating document {} of descriptor {} of e-service {}",
          documentId,
          descriptorId,
          eServiceId,
          exception
        )
        exception match {
          case ex: EServiceNotFoundError =>
            updateEServiceDocument404(problemOf(StatusCodes.NotFound, ex))
          case ex: EServiceDescriptorNotFoundError =>
            updateEServiceDocument404(problemOf(StatusCodes.NotFound, ex))
          case _ =>
            updateEServiceDocument400(
              problemOf(
                StatusCodes.BadRequest,
                DocumentUpdateError(documentId: String, descriptorId: String, eServiceId: String)
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
    logger.info("Cloning descriptor {} of e-service {}", descriptorId, eServiceId)
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
            interface.cloneDocument(catalogFileManager)(uuidSupplier.get).map(d => Some(d))
          )
        clonedDocuments <- Future.traverse(descriptorToClone.docs)(
          _.cloneDocument(catalogFileManager)(uuidSupplier.get)
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
            logger.error(
              "Error while cloning descriptor {} of e-service {}",
              descriptorId,
              eServiceId,
              statusReply.getError
            )
            cloneEServiceByDescriptor400(
              problemOf(StatusCodes.BadRequest, CloningEServiceBadRequest(eServiceId, descriptorId))
            )
        }

      case Failure(exception) =>
        logger.error("Error while cloning descriptor {} of e-service {}", descriptorId, eServiceId, exception)
        exception match {
          case ex: EServiceNotFoundError =>
            cloneEServiceByDescriptor404(problemOf(StatusCodes.NotFound, ex))
          case ex: EServiceDescriptorNotFoundError =>
            cloneEServiceByDescriptor404(problemOf(StatusCodes.NotFound, ex))
          case _ =>
            cloneEServiceByDescriptor400(
              problemOf(StatusCodes.BadRequest, CloningEServiceError(eServiceId, descriptorId))
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
        state = Draft,
        audience = descriptorToClone.audience,
        voucherLifespan = descriptorToClone.voucherLifespan,
        dailyCallsMaxNumber = descriptorToClone.dailyCallsMaxNumber
      )
    } yield CatalogItem(
      id = uuidSupplier.get,
      producerId = serviceToClone.producerId,
      name = s"${serviceToClone.name} - clone",
      description = serviceToClone.description,
      technology = serviceToClone.technology,
      attributes = serviceToClone.attributes,
      descriptors = Seq(descriptor),
      kind = serviceToClone.kind
    )
  }

  /** Code: 204, Message: EService deleted
    * Code: 400, Message: Invalid input, DataType: Problem
    * Code: 404, Message: Not found, DataType: Problem
    */
  override def deleteEService(
    eServiceId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {
    logger.info("Deleting e-service {}", eServiceId)
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
        else {
          logger.error("Error while deleting e-service {}", eServiceId, statusReply.getError)
          deleteEService400(problemOf(StatusCodes.BadRequest, DeleteEServiceBadRequest(eServiceId)))
        }
      case Failure(exception) =>
        logger.error("Error while deleting e-service {}", eServiceId, exception)
        deleteEService400(problemOf(StatusCodes.BadRequest, DeleteEServiceBadRequest(eServiceId)))
    }
  }

  private def canBeDeleted(catalogItem: CatalogItem): Future[Boolean] = {
    catalogItem.descriptors match {
      case Nil => Future.successful(true)
      case _ =>
        Future.failed(
          new RuntimeException(
            s"E-Service ${catalogItem.id.toString} cannot be deleted because it contains descriptors"
          )
        )
    }
  }

}
