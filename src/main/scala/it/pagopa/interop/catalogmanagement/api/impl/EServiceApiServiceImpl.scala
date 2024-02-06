package it.pagopa.interop.catalogmanagement.api.impl

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.catalogmanagement.api.EServiceApiService
import it.pagopa.interop.catalogmanagement.api.impl.ResponseHandlers._
import it.pagopa.interop.catalogmanagement.common.system._
import it.pagopa.interop.catalogmanagement.error.CatalogManagementErrors._
import it.pagopa.interop.catalogmanagement.model.CatalogAdapters._
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.catalogmanagement.model.persistence._
import it.pagopa.interop.catalogmanagement.service.{CatalogFileManager, VersionGenerator}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils.getShard
import it.pagopa.interop.commons.utils.TypeConversions.{EitherOps, OptionOps}
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}

import java.time.format.DateTimeFormatter
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import akka.Done

class EServiceApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]],
  uuidSupplier: UUIDSupplier,
  offsetDateTimeSupplier: OffsetDateTimeSupplier,
  catalogFileManager: CatalogFileManager
)(implicit ec: ExecutionContext)
    extends EServiceApiService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  private val settings: ClusterShardingSettings = entity.settings.getOrElse(ClusterShardingSettings(system))

  override def createEService(eServiceSeed: EServiceSeed)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Creating EService ${eServiceSeed.name} for producer ${eServiceSeed.producerId}"
    logger.info(operationLabel)

    val catalogItem = CatalogItem.create(eServiceSeed, uuidSupplier, offsetDateTimeSupplier)

    val result: Future[EService] = commander(catalogItem.id.toString)
      .askWithStatus(ref => AddCatalogItem(catalogItem, ref))
      .map(_.toApi)

    onComplete(result) { createEServiceResponse[EService](operationLabel)(createEService200) }
  }

  override def createEServiceDocument(
    eServiceId: String,
    descriptorId: String,
    documentSeed: CreateEServiceDescriptorDocumentSeed
  )(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel =
      s"Creating Document ${documentSeed.documentId.toString} of kind ${documentSeed.kind} ,name ${documentSeed.fileName}, path ${documentSeed.filePath} for EService $eServiceId and Descriptor $descriptorId"
    logger.info(operationLabel)

    val isInterface: Boolean = documentSeed.kind match {
      case EServiceDocumentKind.INTERFACE => true
      case _                              => false
    }

    val result: Future[EService] = for {
      eService <- retrieveCatalogItem(eServiceId)
      _        <- getDescriptor(eService, descriptorId).toFuture
      _        <- commander(eServiceId).ask(ref =>
        AddCatalogItemDocument(
          eService.id.toString,
          descriptorId,
          CatalogDocument(
            id = documentSeed.documentId,
            name = documentSeed.fileName,
            contentType = documentSeed.contentType,
            prettyName = documentSeed.prettyName,
            path = documentSeed.filePath,
            checksum = documentSeed.checksum,
            uploadDate = offsetDateTimeSupplier.get()
          ),
          isInterface,
          documentSeed.serverUrls.toList,
          ref
        )
      )
      updated  <- askWithResult(eServiceId, ref => GetCatalogItem(eServiceId, ref))
    } yield updated.toApi

    onComplete(result) { createEServiceDocumentResponse[EService](operationLabel)(createEServiceDocument200) }
  }

  override def getEService(eServiceId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Retrieving EService $eServiceId"
    logger.info(operationLabel)

    val result: Future[EService] = retrieveCatalogItem(eServiceId).map(_.toApi)

    onComplete(result) { getEServiceResponse[EService](operationLabel)(getEService200) }
  }

  override def getEServices(producerId: Option[String], attributeId: Option[String], state: Option[String])(implicit
    toEntityMarshallerEServicearray: ToEntityMarshaller[Seq[EService]],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Retrieving EServices for Producer $producerId, Attribute $attributeId and State $state"
    logger.info(operationLabel)

    val sliceSize = 100

    val commanders: Seq[EntityRef[Command]] = (0 until settings.numberOfShards).map(shard =>
      sharding.entityRefFor(CatalogPersistentBehavior.TypeKey, shard.toString)
    )

    def getSlice(
      commander: EntityRef[Command],
      from: Int,
      to: Int,
      producerId: Option[String],
      attributeId: Option[String],
      state: Option[CatalogDescriptorState]
    ): LazyList[CatalogItem] = {
      val slice: Seq[CatalogItem] = Await
        .result(commander.ask(ref => ListCatalogItem(from, to, producerId, attributeId, state, ref)), Duration.Inf)

      if (slice.isEmpty)
        LazyList.empty[CatalogItem]
      else
        getSlice(commander, to, to + sliceSize, producerId, attributeId, state) #::: slice.to(LazyList)
    }

    val stringToState: String => Either[Throwable, CatalogDescriptorState] =
      EServiceDescriptorState.fromValue(_).map(CatalogDescriptorState.fromApi)

    val stateEnum = state.traverse(stringToState)

    val result: Either[Throwable, Seq[EService]] =
      stateEnum
        .map(state => commanders.flatMap(ref => getSlice(ref, 0, sliceSize, producerId, attributeId, state)))
        .map(_.map(_.toApi))

    getEServicesResponse[Seq[EService]](operationLabel)(getEServices200)(result.toTry)
  }

  override def getEServiceDocument(eServiceId: String, descriptorId: String, documentId: String)(implicit
    toEntityMarshallerEServiceDoc: ToEntityMarshaller[EServiceDoc],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Retrieving Document $documentId for EService $eServiceId and Descriptor $descriptorId"
    logger.info(operationLabel)

    val result: Future[EServiceDoc] = for {
      catalogItem <- retrieveCatalogItem(eServiceId)
      document    <- extractDocument(catalogItem, descriptorId, documentId).toFuture
    } yield document.toApi

    onComplete(result) { getEServiceDocumentResponse[EServiceDoc](operationLabel)(getEServiceDocument200) }
  }

  private def extractDocument(
    catalogItem: CatalogItem,
    descriptorId: String,
    documentId: String
  ): Either[ComponentError, CatalogDocument] = {

    def lookupDocument(catalogDescriptor: CatalogDescriptor): Option[CatalogDocument] = {
      val interface = catalogDescriptor.interface.fold(Seq.empty[CatalogDocument])(doc => Seq(doc))
      (interface ++: catalogDescriptor.docs).find(_.id.toString == documentId)
    }

    getDescriptor(catalogItem, descriptorId)
      .flatMap(lookupDocument(_).toRight(DocumentNotFound(catalogItem.id.toString, descriptorId, documentId)))
  }

  override def deleteDraft(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Deleting draft Descriptor $descriptorId of EService $eServiceId"
    logger.info(operationLabel)

    val result: Future[Unit] = for {
      eService <- retrieveCatalogItem(eServiceId)
      _        <- descriptorDeletable(eService, descriptorId).toFuture
      _        <- eService
        .getInterfacePath(descriptorId)
        .fold(Future.successful(true))(path => catalogFileManager.delete(path))
      _        <- eService
        .getDocumentPaths(descriptorId)
        .fold(Future.successful(Seq.empty[Boolean]))(path => Future.traverse(path)(catalogFileManager.delete))
      _        <- commander(eServiceId).askWithStatus(ref => DeleteCatalogItemDescriptor(eService, descriptorId, ref))
    } yield ()

    onComplete(result) { deleteDraftResponse[Unit](operationLabel)(_ => deleteDraft204) }
  }

  override def updateDescriptor(
    eServiceId: String,
    descriptorId: String,
    eServiceDescriptorSeed: UpdateEServiceDescriptorSeed
  )(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Updating Descriptor $descriptorId of EService $eServiceId"
    logger.info(operationLabel)

    def mergeChanges(
      descriptor: CatalogDescriptor,
      eServiceDescriptorSeed: UpdateEServiceDescriptorSeed,
      attributes: CatalogAttributes
    ): CatalogDescriptor = descriptor.copy(
      description = eServiceDescriptorSeed.description,
      audience = eServiceDescriptorSeed.audience,
      voucherLifespan = eServiceDescriptorSeed.voucherLifespan,
      dailyCallsPerConsumer = eServiceDescriptorSeed.dailyCallsPerConsumer,
      state = CatalogDescriptorState.fromApi(eServiceDescriptorSeed.state),
      dailyCallsTotal = eServiceDescriptorSeed.dailyCallsTotal,
      agreementApprovalPolicy =
        PersistentAgreementApprovalPolicy.fromApi(eServiceDescriptorSeed.agreementApprovalPolicy).some,
      attributes = attributes
    )

    val result: Future[EService] = for {
      eService           <- retrieveCatalogItem(eServiceId)
      toUpdateDescriptor <- getDescriptor(eService, descriptorId).toFuture
      updatedDescriptor = mergeChanges(
        toUpdateDescriptor,
        eServiceDescriptorSeed,
        eServiceDescriptorSeed.attributes.fromApi
      )
      updatedItem       = eService.copy(descriptors =
        eService.descriptors.filter(_.id.toString != descriptorId) :+ updatedDescriptor
      )
      updated <- askWithResult(eServiceId, ref => UpdateCatalogItem(updatedItem, ref))
    } yield updated.toApi

    onComplete(result) { updateDescriptorResponse[EService](operationLabel)(updateDescriptor200) }
  }

  override def updateEServiceById(eServiceId: String, updateEServiceSeed: UpdateEServiceSeed)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Updating EService $eServiceId"
    logger.info(operationLabel)

    val result: Future[EService] = for {
      eService <- retrieveCatalogItem(eServiceId)
      updated = eService.update(updateEServiceSeed)
      result <- askWithResult(eServiceId, ref => UpdateCatalogItem(updated, ref))
    } yield result.toApi

    onComplete(result) { updateEServiceByIdResponse[EService](operationLabel)(updateEServiceById200) }
  }

  private def descriptorDeletable(catalogItem: CatalogItem, descriptorId: String): Either[ComponentError, Unit] =
    getDescriptor(catalogItem, descriptorId).flatMap(descriptor =>
      Left(DescriptorNotInDraft(catalogItem.id.toString, descriptorId)).withRight[Unit].unlessA(descriptor.isDraft)
    )

  private def commander(id: String): EntityRef[Command] =
    sharding.entityRefFor(CatalogPersistentBehavior.TypeKey, getShard(id, settings.numberOfShards))

  private def retrieveCatalogItem(eServiceId: String): Future[CatalogItem] = for {
    retrieved <- commander(eServiceId).askWithStatus(ref => GetCatalogItem(eServiceId, ref))
    current   <- retrieved.toFuture(EServiceNotFound(eServiceId))
  } yield current

  override def createDescriptor(eServiceId: String, eServiceDescriptorSeed: EServiceDescriptorSeed)(implicit
    toEntityMarshallerEServiceDescriptor: ToEntityMarshaller[EServiceDescriptor],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Creating Descriptor for EService $eServiceId"
    logger.info(operationLabel)

    val result: Future[EServiceDescriptor] = for {
      current     <- retrieveCatalogItem(eServiceId)
      nextVersion <- VersionGenerator.next(current.currentVersion).toFuture
      createdCatalogDescriptor = CatalogDescriptor(
        id = uuidSupplier.get(),
        description = eServiceDescriptorSeed.description,
        version = nextVersion,
        interface = None,
        docs = Seq.empty[CatalogDocument],
        state = Draft,
        voucherLifespan = eServiceDescriptorSeed.voucherLifespan,
        audience = eServiceDescriptorSeed.audience,
        dailyCallsPerConsumer = eServiceDescriptorSeed.dailyCallsPerConsumer,
        dailyCallsTotal = eServiceDescriptorSeed.dailyCallsTotal,
        agreementApprovalPolicy =
          PersistentAgreementApprovalPolicy.fromApi(eServiceDescriptorSeed.agreementApprovalPolicy).some,
        createdAt = offsetDateTimeSupplier.get(),
        serverUrls = List.empty,
        publishedAt = None,
        suspendedAt = None,
        deprecatedAt = None,
        archivedAt = None,
        attributes = eServiceDescriptorSeed.attributes.fromApi
      )
      _ <- commander(eServiceId).askWithStatus(ref =>
        AddCatalogItemDescriptor(current.id.toString, createdCatalogDescriptor, ref)
      )
    } yield createdCatalogDescriptor.toApi

    onComplete(result) { createDescriptorResponse[EServiceDescriptor](operationLabel)(createDescriptor200) }
  }

  override def deleteEServiceDocument(eServiceId: String, descriptorId: String, documentId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Deleting Document $documentId of Descriptor $descriptorId for EService $eServiceId"
    logger.info(operationLabel)

    val result: Future[Unit] = for {
      eService <- retrieveCatalogItem(eServiceId)
      document <- extractDocument(eService, descriptorId, documentId).toFuture
      _        <- catalogFileManager.delete(document.path)
      _        <- commander(eServiceId).askWithStatus(ref =>
        DeleteCatalogItemDocument(eService.id.toString, descriptorId, documentId, ref)
      )
    } yield ()

    onComplete(result) { deleteEServiceDocumentResponse[Unit](operationLabel)(_ => deleteEServiceDocument204) }
  }

  override def archiveDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Archiving Descriptor $descriptorId of EService $eServiceId"
    logger.info(operationLabel)

    val result: Future[CatalogDescriptor] = updateDescriptorState(eServiceId, descriptorId, Archived)

    onComplete(result) { archiveDescriptorResponse[CatalogDescriptor](operationLabel)(_ => archiveDescriptor204) }
  }

  override def deprecateDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Deprecating Descriptor $descriptorId of EService $eServiceId"
    logger.info(operationLabel)

    val result: Future[CatalogDescriptor] = updateDescriptorState(eServiceId, descriptorId, Deprecated)

    onComplete(result) { deprecateDescriptorResponse[CatalogDescriptor](operationLabel)(_ => archiveDescriptor204) }
  }

  override def suspendDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Suspending Descriptor $descriptorId of EService $eServiceId"
    logger.info(operationLabel)

    val result: Future[CatalogDescriptor] = updateDescriptorState(eServiceId, descriptorId, Suspended)

    onComplete(result) { suspendDescriptorResponse[CatalogDescriptor](operationLabel)(_ => archiveDescriptor204) }
  }

  override def draftDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Moving to draft state Descriptor $descriptorId of EService $eServiceId"
    logger.info(operationLabel)

    val result = updateDescriptorState(eServiceId, descriptorId, Draft)

    onComplete(result) { draftDescriptorResponse[CatalogDescriptor](operationLabel)(_ => archiveDescriptor204) }
  }

  override def publishDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Publishing Descriptor $descriptorId of EService $eServiceId"
    logger.info(operationLabel)

    val result = updateDescriptorState(eServiceId, descriptorId, Published)

    onComplete(result) { publishDescriptorResponse[CatalogDescriptor](operationLabel)(_ => archiveDescriptor204) }
  }

  private def updateDescriptorState(
    eServiceId: String,
    descriptorId: String,
    state: CatalogDescriptorState
  ): Future[CatalogDescriptor] = {

    def mergeChanges(
      descriptor: CatalogDescriptor,
      updateEServiceDescriptorState: CatalogDescriptorState
    ): CatalogDescriptor = {
      (descriptor.state, updateEServiceDescriptorState) match {
        case (Draft, Published)      =>
          descriptor.copy(state = updateEServiceDescriptorState, publishedAt = offsetDateTimeSupplier.get().some)
        case (Published, Suspended)  =>
          descriptor.copy(state = updateEServiceDescriptorState, suspendedAt = offsetDateTimeSupplier.get().some)
        case (Suspended, Published)  =>
          descriptor.copy(state = updateEServiceDescriptorState, suspendedAt = None)
        case (Suspended, Deprecated) =>
          descriptor.copy(
            state = updateEServiceDescriptorState,
            suspendedAt = None,
            deprecatedAt = offsetDateTimeSupplier.get().some
          )
        case (Suspended, Archived)   =>
          descriptor.copy(
            state = updateEServiceDescriptorState,
            suspendedAt = None,
            archivedAt = offsetDateTimeSupplier.get().some
          )
        case (Published, Archived)   =>
          descriptor.copy(state = updateEServiceDescriptorState, archivedAt = offsetDateTimeSupplier.get().some)
        case (Published, Deprecated) =>
          descriptor.copy(state = updateEServiceDescriptorState, deprecatedAt = offsetDateTimeSupplier.get().some)
        case _                       => descriptor.copy(state = updateEServiceDescriptorState)
      }
    }

    for {
      eService           <- retrieveCatalogItem(eServiceId)
      toUpdateDescriptor <- getDescriptor(eService, descriptorId).toFuture
      updatedDescriptor = mergeChanges(toUpdateDescriptor, state)
      updated <- askWithResult(
        eServiceId,
        ref => UpdateCatalogItemDescriptor(eService.id.toString, updatedDescriptor, ref)
      )
    } yield updated
  }

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
    val operationLabel = s"Updating Document $documentId of Descriptor $descriptorId of EService $eServiceId"
    logger.info(operationLabel)

    val result: Future[EServiceDoc] = for {
      eService   <- retrieveCatalogItem(eServiceId)
      descriptor <- getDescriptor(eService, descriptorId).toFuture
      document   <- extractDocument(eService, descriptorId, documentId).toFuture
      updatedDocument = document.copy(prettyName = updateEServiceDescriptorDocumentSeed.prettyName)
      result <- askWithResult(
        eServiceId,
        ref =>
          UpdateCatalogItemDocument(
            eServiceId = eServiceId,
            descriptorId = descriptorId,
            documentId = documentId,
            updatedDocument,
            descriptor.serverUrls,
            ref
          )
      )
    } yield result.toApi

    onComplete(result) { updateEServiceDocumentResponse[EServiceDoc](operationLabel)(updateEServiceDocument200) }
  }

  override def cloneEServiceByDescriptor(eServiceId: String, descriptorId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Cloning Descriptor $descriptorId of EService $eServiceId"
    logger.info(operationLabel)

    val result: Future[EService] =
      for {
        eService          <- retrieveCatalogItem(eServiceId)
        descriptorToClone <- getDescriptor(eService, descriptorId).toFuture
        clonedInterface   <-
          descriptorToClone.interface.fold(Future.successful[Option[CatalogDocument]](None))(interface =>
            interface.cloneDocument(catalogFileManager)(uuidSupplier.get()).map(d => Some(d))
          )
        clonedDocuments   <- Future.traverse(descriptorToClone.docs)(
          _.cloneDocument(catalogFileManager)(uuidSupplier.get())
        )
        clonedService     <- cloneItemAsNewDraft(eService, descriptorToClone, clonedInterface, clonedDocuments)
        cloned <- commander(clonedService.id.toString).askWithStatus(ref => AddClonedCatalogItem(clonedService, ref))
      } yield cloned.toApi

    onComplete(result) { cloneEServiceByDescriptorResponse[EService](operationLabel)(cloneEServiceByDescriptor200) }
  }

  override def moveAttributesToDescriptors(
    eServiceId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {
    val operationLabel = s"Moving attributes from EService $eServiceId to its descriptors"
    logger.info(operationLabel)

    val result: Future[Done] =
      commander(eServiceId).askWithStatus(MoveAttributesFromEserviceToDescriptors(eServiceId, _))

    onComplete(result) { moveAttributesToDescriptorsResponse[Done](operationLabel)(moveAttributesToDescriptors204) }
  }

  private def cloneItemAsNewDraft(
    serviceToClone: CatalogItem,
    descriptorToClone: CatalogDescriptor,
    clonedInterface: Option[CatalogDocument],
    clonedDocuments: Seq[CatalogDocument]
  ): Future[CatalogItem] = {
    val now = offsetDateTimeSupplier.get()

    for {
      version <- VersionGenerator.next(None).toFuture
      descriptor = CatalogDescriptor(
        id = uuidSupplier.get(),
        version = version,
        description = descriptorToClone.description,
        interface = clonedInterface,
        docs = clonedDocuments,
        state = Draft,
        audience = descriptorToClone.audience,
        voucherLifespan = descriptorToClone.voucherLifespan,
        dailyCallsPerConsumer = descriptorToClone.dailyCallsPerConsumer,
        dailyCallsTotal = descriptorToClone.dailyCallsTotal,
        agreementApprovalPolicy = descriptorToClone.agreementApprovalPolicy,
        createdAt = now,
        serverUrls = descriptorToClone.serverUrls,
        publishedAt = None,
        suspendedAt = None,
        deprecatedAt = None,
        archivedAt = None,
        attributes = descriptorToClone.attributes
      )
    } yield CatalogItem(
      id = uuidSupplier.get(),
      producerId = serviceToClone.producerId,
      name = s"${serviceToClone.name} - clone - ${now.format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss"))}",
      description = serviceToClone.description,
      technology = serviceToClone.technology,
      attributes = None,
      descriptors = Seq(descriptor),
      createdAt = now,
      riskAnalysis = serviceToClone.riskAnalysis,
      mode = serviceToClone.mode
    )
  }

  override def deleteEService(
    eServiceId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {
    val operationLabel = s"Deleting EService $eServiceId"
    logger.info(operationLabel)

    val result: Future[Unit] = for {
      eService <- retrieveCatalogItem(eServiceId)
      _        <- canBeDeleted(eService)
      _        <- commander(eServiceId).askWithStatus(ref => DeleteCatalogItem(eService.id.toString, ref))
    } yield ()

    onComplete(result) { deleteEServiceResponse[Unit](operationLabel)(_ => deleteEService204) }
  }

  override def createRiskAnalysis(eServiceId: String, seed: RiskAnalysisSeed)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Creating Risk Analysis for EService $eServiceId"
    logger.info(operationLabel)

    val result: Future[Unit] = for {
      eService <- retrieveCatalogItem(eServiceId)
      newCatalogRiskAnalysis = CatalogRiskAnalysis(
        id = uuidSupplier.get(),
        name = seed.name,
        riskAnalysisForm = CatalogRiskAnalysisForm(
          id = uuidSupplier.get(),
          version = seed.riskAnalysisForm.version,
          singleAnswers = seed.riskAnalysisForm.singleAnswers.map(answer =>
            CatalogRiskAnalysisSingleAnswer(id = uuidSupplier.get(), key = answer.key, value = answer.value)
          ),
          multiAnswers = seed.riskAnalysisForm.multiAnswers.map(answer =>
            CatalogRiskAnalysisMultiAnswer(id = uuidSupplier.get(), key = answer.key, values = answer.values)
          )
        ),
        createdAt = offsetDateTimeSupplier.get()
      )
      _ <- commander(eServiceId).askWithStatus(ref =>
        AddCatalogItemRiskAnalysis(eService.id.toString, newCatalogRiskAnalysis, ref)
      )
    } yield ()

    onComplete(result) { createdRiskAnalysisResponse[Unit](operationLabel)(_ => createRiskAnalysis204) }
  }

  override def updateRiskAnalysis(eServiceId: String, riskAnalysisId: String, seed: RiskAnalysisSeed)(implicit
    toEntityMarshallerEServiceRiskAnalysis: ToEntityMarshaller[EServiceRiskAnalysis],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Updating Risk Analysis $riskAnalysisId for EService $eServiceId"
    logger.info(operationLabel)

    val result: Future[EServiceRiskAnalysis] = for {
      eService     <- retrieveCatalogItem(eServiceId)
      riskAnalysis <- getRiskAnalysis(eService, riskAnalysisId).toFuture
      updateRiskAnalysis = riskAnalysis.copy(
        name = seed.name,
        riskAnalysisForm = CatalogRiskAnalysisForm(
          id = uuidSupplier.get(),
          version = seed.riskAnalysisForm.version,
          singleAnswers = seed.riskAnalysisForm.singleAnswers.map(answer =>
            CatalogRiskAnalysisSingleAnswer(id = uuidSupplier.get(), key = answer.key, value = answer.value)
          ),
          multiAnswers = seed.riskAnalysisForm.multiAnswers.map(answer =>
            CatalogRiskAnalysisMultiAnswer(id = uuidSupplier.get(), key = answer.key, values = answer.values)
          )
        )
      )
      _ <- commander(eServiceId).askWithStatus(ref =>
        UpdateCatalogItemRiskAnalysis(eService.id.toString, updateRiskAnalysis, ref)
      )
    } yield updateRiskAnalysis.toApi

    onComplete(result) {
      updateCatalogRiskAnalysisResponse[EServiceRiskAnalysis](operationLabel)(updateRiskAnalysis200)
    }
  }

  override def deleteRiskAnalysis(eServiceId: String, riskAnalysisId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Deleting Risk Analysis $riskAnalysisId for EService $eServiceId"
    logger.info(operationLabel)

    val result: Future[Unit] = commander(eServiceId)
      .askWithStatus(ref => DeleteCatalogItemRiskAnalysis(eServiceId, riskAnalysisId, ref))
      .map(_ => ())

    onComplete(result) { deleteRiskAnalysisResponse[Unit](operationLabel)(_ => deleteRiskAnalysis204) }
  }

  private def getRiskAnalysis(
    eService: CatalogItem,
    riskAnalysisId: String
  ): Either[EServiceRiskAnalysisNotFound, CatalogRiskAnalysis] =
    eService.riskAnalysis
      .find(_.id.toString == riskAnalysisId)
      .toRight(EServiceRiskAnalysisNotFound(eService.id.toString, riskAnalysisId))

  private def askWithResult[T](eServiceId: String, command: ActorRef[StatusReply[Option[T]]] => Command): Future[T] =
    for {
      maybeResult <- commander(eServiceId).askWithStatus(command)
      result      <- maybeResult.toFuture(ElementNotFoundAfterUpdate)
    } yield result

  private def getDescriptor(
    eService: CatalogItem,
    descriptorId: String
  ): Either[EServiceDescriptorNotFound, CatalogDescriptor] =
    eService.descriptors
      .find(_.id.toString == descriptorId)
      .toRight(EServiceDescriptorNotFound(eService.id.toString, descriptorId))

  private def canBeDeleted(catalogItem: CatalogItem): Future[Boolean] = {
    catalogItem.descriptors match {
      case Nil => Future.successful(true)
      case _   => Future.failed(EServiceWithDescriptorsNotDeletable(catalogItem.id.toString))
    }
  }
}
