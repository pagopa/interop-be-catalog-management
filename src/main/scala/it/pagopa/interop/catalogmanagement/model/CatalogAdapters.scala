package it.pagopa.interop.catalogmanagement.model

import cats.implicits._
import it.pagopa.interop.catalogmanagement.service.CatalogFileManager
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object CatalogAdapters {

  implicit class CatalogAttributeWrapper(private val p: CatalogAttribute) extends AnyVal {
    def toApi: Attribute = Attribute(p.id, p.explicitAttributeVerification)
  }

  implicit class AttributeWrapper(private val p: Attribute) extends AnyVal {
    def fromApi: CatalogAttribute = CatalogAttribute(p.id, p.explicitAttributeVerification)
  }

  implicit class CatalogAttributesWrapper(private val p: CatalogAttributes) extends AnyVal {
    def toApi: Attributes = Attributes(
      certified = p.certified.map(_.map(_.toApi)),
      declared = p.declared.map(_.map(_.toApi)),
      verified = p.verified.map(_.map(_.toApi))
    )
  }

  implicit class CatalogAttributesObjectWrapper(private val p: Attributes) extends AnyVal {
    def fromApi: CatalogAttributes = CatalogAttributes(
      certified = p.certified.map(_.map(_.fromApi)),
      declared = p.declared.map(_.map(_.fromApi)),
      verified = p.verified.map(_.map(_.fromApi))
    )
  }

  implicit class CatalogDescriptorWrapper(private val p: CatalogDescriptor) extends AnyVal {
    def toApi: EServiceDescriptor = EServiceDescriptor(
      id = p.id,
      version = p.version,
      description = p.description,
      interface = p.interface.map(_.toApi),
      docs = p.docs.map(_.toApi),
      state = p.state.toApi,
      audience = p.audience,
      voucherLifespan = p.voucherLifespan,
      dailyCallsPerConsumer = p.dailyCallsPerConsumer,
      dailyCallsTotal = p.dailyCallsTotal,
      agreementApprovalPolicy = p.agreementApprovalPolicy.getOrElse(PersistentAgreementApprovalPolicy.default).toApi,
      serverUrls = p.serverUrls,
      publishedAt = p.publishedAt,
      suspendedAt = p.suspendedAt,
      deprecatedAt = p.deprecatedAt,
      archivedAt = p.archivedAt,
      attributes = p.attributes.toApi
    )

    def mergeWithSeed(updateEServiceDescriptorSeed: UpdateEServiceDescriptorSeed): CatalogDescriptor =
      p.copy(
        description = updateEServiceDescriptorSeed.description,
        state = CatalogDescriptorState.fromApi(updateEServiceDescriptorSeed.state),
        audience = updateEServiceDescriptorSeed.audience,
        voucherLifespan = updateEServiceDescriptorSeed.voucherLifespan,
        dailyCallsPerConsumer = updateEServiceDescriptorSeed.dailyCallsPerConsumer,
        dailyCallsTotal = updateEServiceDescriptorSeed.dailyCallsTotal,
        agreementApprovalPolicy =
          PersistentAgreementApprovalPolicy.fromApi(updateEServiceDescriptorSeed.agreementApprovalPolicy).some,
        attributes = updateEServiceDescriptorSeed.attributes.fromApi
      )

    def isDraft: Boolean = p.state == Draft
  }

  implicit class CatalogDescriptorStateWrapper(private val p: CatalogDescriptorState) extends AnyVal {
    def toApi: EServiceDescriptorState = p match {
      case Draft      => EServiceDescriptorState.DRAFT
      case Published  => EServiceDescriptorState.PUBLISHED
      case Deprecated => EServiceDescriptorState.DEPRECATED
      case Suspended  => EServiceDescriptorState.SUSPENDED
      case Archived   => EServiceDescriptorState.ARCHIVED
    }
  }

  implicit class CatalogDescriptorStateObjectWrapper(private val p: CatalogDescriptorState.type) extends AnyVal {
    def fromApi(status: EServiceDescriptorState): CatalogDescriptorState = status match {
      case EServiceDescriptorState.DRAFT      => Draft
      case EServiceDescriptorState.PUBLISHED  => Published
      case EServiceDescriptorState.DEPRECATED => Deprecated
      case EServiceDescriptorState.SUSPENDED  => Suspended
      case EServiceDescriptorState.ARCHIVED   => Archived
    }
  }

  implicit class PersistentAgreementApprovalPolicyWrapper(private val p: PersistentAgreementApprovalPolicy)
      extends AnyVal {
    def toApi: AgreementApprovalPolicy = p match {
      case Automatic => AgreementApprovalPolicy.AUTOMATIC
      case Manual    => AgreementApprovalPolicy.MANUAL
    }
  }

  implicit class PersistentAgreementApprovalPolicyObjectWrapper(private val p: PersistentAgreementApprovalPolicy.type)
      extends AnyVal {
    def fromApi(policy: AgreementApprovalPolicy): PersistentAgreementApprovalPolicy = policy match {
      case AgreementApprovalPolicy.AUTOMATIC => Automatic
      case AgreementApprovalPolicy.MANUAL    => Manual
    }
  }

  implicit class CatalogDocumentWrapper(private val p: CatalogDocument) extends AnyVal {
    def toApi: EServiceDoc =
      EServiceDoc(
        id = p.id,
        name = p.name,
        contentType = p.contentType,
        prettyName = p.prettyName,
        path = p.path,
        checksum = p.checksum,
        uploadDate = p.uploadDate
      )

    def cloneDocument(
      fileManager: CatalogFileManager
    )(clonedDocumentId: UUID)(implicit ec: ExecutionContext): Future[CatalogDocument] = fileManager.copy(p.path)(
      documentId = clonedDocumentId,
      prettyName = p.prettyName,
      checksum = p.checksum,
      contentType = p.contentType,
      fileName = p.name
    )
  }

  implicit class CatalogItemWrapper(private val p: CatalogItem) extends AnyVal {
    def toApi: EService = EService(
      id = p.id,
      producerId = p.producerId,
      name = p.name,
      description = p.description,
      technology = p.technology.toApi,
      descriptors = p.descriptors.map(_.toApi),
      riskAnalysis = p.riskAnalysis.map(_.toApi),
      mode = p.mode.toApi
    )

    def getInterfacePath(descriptorId: String): Option[String] = for {
      doc       <- p.descriptors.find(_.id.toString == descriptorId)
      interface <- doc.interface
    } yield interface.path

    def getDocumentPaths(descriptorId: String): Option[Seq[String]] = for {
      documents <- p.descriptors.find(_.id == UUID.fromString(descriptorId))
    } yield documents.docs.map(_.path)

    def update(updateEServiceSeed: UpdateEServiceSeed): CatalogItem = p.copy(
      name = updateEServiceSeed.name,
      description = updateEServiceSeed.description,
      technology = CatalogItemTechnology.fromApi(updateEServiceSeed.technology)
    )

    def currentVersion: Option[String] = p.descriptors.flatMap(_.version.toLongOption).maxOption.map(_.toString)
  }

  implicit class CatalogItemObjectWrapper(private val p: CatalogItem.type) extends AnyVal {
    def create(
      seed: EServiceSeed,
      uuidSupplier: UUIDSupplier,
      offsetDateTimeSupplier: OffsetDateTimeSupplier
    ): CatalogItem = CatalogItem(
      id = uuidSupplier.get(),
      producerId = seed.producerId,
      name = seed.name,
      description = seed.description,
      technology = CatalogItemTechnology.fromApi(seed.technology),
      descriptors = List.empty[CatalogDescriptor],
      createdAt = offsetDateTimeSupplier.get(),
      attributes = None,
      riskAnalysis = List.empty[CatalogRiskAnalysis],
      mode = CatalogItemMode.fromApi(seed.mode)
    )
  }

  implicit class CatalogRiskAnalysisObjectWrapper(private val p: CatalogRiskAnalysis) extends AnyVal {
    def toApi: EServiceRiskAnalysis =
      EServiceRiskAnalysis(
        id = p.id,
        name = p.name,
        riskAnalysisForm = p.riskAnalysisForm.toApi,
        createdAt = p.createdAt
      )
  }

  implicit class CatalogRiskAnalysisFormObjectWrapper(private val p: CatalogRiskAnalysisForm) extends AnyVal {
    def toApi: RiskAnalysisForm = RiskAnalysisForm(
      id = p.id,
      version = p.version,
      singleAnswers = p.singleAnswers.map(_.toApi),
      multiAnswers = p.multiAnswers.map(_.toApi)
    )
  }

  implicit class CatalogRiskAnalysisSingleAnswerObjectWrapper(private val p: CatalogRiskAnalysisSingleAnswer)
      extends AnyVal {
    def toApi: RiskAnalysisSingleAnswer = RiskAnalysisSingleAnswer(id = p.id, key = p.key, value = p.value)
  }

  implicit class CatalogRiskAnalysisMultiAnswerObjectWrapper(private val p: CatalogRiskAnalysisMultiAnswer)
      extends AnyVal {
    def toApi: RiskAnalysisMultiAnswer = RiskAnalysisMultiAnswer(id = p.id, key = p.key, values = p.values)
  }

  implicit class RiskAnalysisObjectWrapper(private val p: EServiceRiskAnalysis) extends AnyVal {
    def fromApi: CatalogRiskAnalysis = CatalogRiskAnalysis(
      id = p.id,
      name = p.name,
      riskAnalysisForm = p.riskAnalysisForm.fromApi,
      createdAt = p.createdAt
    )
  }

  implicit class RiskAnalysisFormObjectWrapper(private val p: RiskAnalysisForm) extends AnyVal {
    def fromApi: CatalogRiskAnalysisForm = CatalogRiskAnalysisForm(
      id = p.id,
      version = p.version,
      singleAnswers = p.singleAnswers.map(_.fromApi),
      multiAnswers = p.multiAnswers.map(_.fromApi)
    )
  }

  implicit class RiskAnalysisSingleAnswerObjectWrapper(private val p: RiskAnalysisSingleAnswer) extends AnyVal {
    def fromApi: CatalogRiskAnalysisSingleAnswer =
      CatalogRiskAnalysisSingleAnswer(id = p.id, key = p.key, value = p.value)
  }

  implicit class RiskAnalysisMultiAnswerObjectWrapper(private val p: RiskAnalysisMultiAnswer) extends AnyVal {
    def fromApi: CatalogRiskAnalysisMultiAnswer =
      CatalogRiskAnalysisMultiAnswer(id = p.id, key = p.key, values = p.values)
  }

  implicit class ModeObjectWrapper(private val p: CatalogItemMode.type) extends AnyVal {
    def fromApi(status: EServiceMode): CatalogItemMode = status match {
      case EServiceMode.RECEIVE => RECEIVE
      case EServiceMode.DELIVER => DELIVER
    }
  }

  implicit class CatalogItemModeObjectWrapper(private val p: CatalogItemMode) extends AnyVal {
    def toApi: EServiceMode = p match {
      case RECEIVE => EServiceMode.RECEIVE
      case DELIVER => EServiceMode.DELIVER
    }
  }

  implicit class CatalogItemTechnologyWrapper(private val p: CatalogItemTechnology) extends AnyVal {
    def toApi: EServiceTechnology = p match {
      case Rest => EServiceTechnology.REST
      case Soap => EServiceTechnology.SOAP
    }
  }

  implicit class CatalogItemTechnologyObjectWrapper(private val p: CatalogItemTechnology.type) extends AnyVal {
    def fromApi(status: EServiceTechnology): CatalogItemTechnology = status match {
      case EServiceTechnology.REST => Rest
      case EServiceTechnology.SOAP => Soap
    }
  }
}
