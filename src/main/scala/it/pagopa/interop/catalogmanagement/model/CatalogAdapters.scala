package it.pagopa.interop.catalogmanagement.model

import it.pagopa.interop.catalogmanagement.error.CatalogManagementErrors.InvalidAttribute
import it.pagopa.interop.catalogmanagement.service.CatalogFileManager
import scala.concurrent.{ExecutionContext, Future}
import it.pagopa.interop.commons.utils.service.UUIDSupplier
import cats.implicits._
import scala.util._
import java.util.UUID

object CatalogAdapters {

  implicit class CatalogAttributeWrapper(private val p: CatalogAttribute) extends AnyVal {
    def toApi: Attribute = p match {
      case SingleAttribute(id) => Attribute(single = Some(id.toApi), group = None)
      case GroupAttribute(ids) => Attribute(single = None, group = Some(ids.map(_.toApi)))
    }
  }

  implicit class CatalogAttributeObjectWrapper(private val p: CatalogAttribute.type) extends AnyVal {
    def fromApi(attribute: Attribute): Try[CatalogAttribute] = {

      val single: Option[CatalogAttributeValue]     = attribute.single.map(CatalogAttributeValue.fromApi)
      val group: Option[Seq[CatalogAttributeValue]] =
        attribute.group.map(_.map(CatalogAttributeValue.fromApi)).filter(_.nonEmpty)

      (single, group) match {
        case (Some(attr), None)  => Success[CatalogAttribute](SingleAttribute(attr))
        case (None, Some(attrs)) => Success[CatalogAttribute](GroupAttribute(attrs))
        case _                   => Failure[CatalogAttribute](InvalidAttribute(attribute))
      }
    }
  }

  implicit class CatalogAttributesWrapper(private val p: CatalogAttributes) extends AnyVal {
    def toApi: Attributes =
      Attributes(
        certified = p.certified.map(_.toApi),
        declared = p.declared.map(_.toApi),
        verified = p.verified.map(_.toApi)
      )
  }

  implicit class CatalogAttributesObjectWrapper(private val p: CatalogAttributes.type) extends AnyVal {
    def fromApi(attributes: Attributes): Try[CatalogAttributes] = for {
      certified <- attributes.certified.toList.traverse(CatalogAttribute.fromApi)
      declared  <- attributes.declared.toList.traverse(CatalogAttribute.fromApi)
      verified  <- attributes.verified.toList.traverse(CatalogAttribute.fromApi)
    } yield CatalogAttributes(certified = certified, declared = declared, verified = verified)
  }

  implicit class CatalogAttributeValueWrapper(private val p: CatalogAttributeValue) extends AnyVal {
    def toApi: AttributeValue =
      AttributeValue(id = p.id, explicitAttributeVerification = p.explicitAttributeVerification)
  }

  implicit class CatalogAttributeValueObjectWrapper(private val p: CatalogAttributeValue.type) extends AnyVal {
    def fromApi(attributeValue: AttributeValue): CatalogAttributeValue = CatalogAttributeValue(
      id = attributeValue.id,
      explicitAttributeVerification = attributeValue.explicitAttributeVerification
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
      dailyCallsTotal = p.dailyCallsTotal
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

  implicit class CatalogDocumentWrapper(private val p: CatalogDocument) extends AnyVal {
    def toApi: EServiceDoc =
      EServiceDoc(id = p.id, name = p.name, contentType = p.contentType, prettyName = p.prettyName, path = p.path)

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
      attributes = p.attributes.toApi,
      descriptors = p.descriptors.map(_.toApi)
    )

    def getInterfacePath(descriptorId: String): Option[String] = for {
      doc       <- p.descriptors.find(_.id.toString == descriptorId)
      interface <- doc.interface
    } yield interface.path

    def getDocumentPaths(descriptorId: String): Option[Seq[String]] = for {
      documents <- p.descriptors.find(_.id == UUID.fromString(descriptorId))
    } yield documents.docs.map(_.path)

    def mergeWithSeed(updateEServiceSeed: UpdateEServiceSeed): Future[CatalogItem] = Future.fromTry {
      for {
        attributes <- CatalogAttributes.fromApi(updateEServiceSeed.attributes)
      } yield p.copy(
        name = updateEServiceSeed.name,
        description = updateEServiceSeed.description,
        technology = CatalogItemTechnology.fromApi(updateEServiceSeed.technology),
        attributes = attributes
      )
    }

    def currentVersion: Option[String] = p.descriptors.flatMap(_.version.toLongOption).maxOption.map(_.toString)
  }

  implicit class CatalogItemObjectWrapper(private val p: CatalogItem.type) extends AnyVal {
    def create(seed: EServiceSeed, uuidSupplier: UUIDSupplier): Future[CatalogItem] = Future.fromTry {
      for {
        attributes <- CatalogAttributes.fromApi(seed.attributes)
      } yield CatalogItem(
        id = uuidSupplier.get,
        producerId = seed.producerId,
        name = seed.name,
        description = seed.description,
        technology = CatalogItemTechnology.fromApi(seed.technology),
        attributes = attributes,
        descriptors = Seq.empty[CatalogDescriptor]
      )
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
