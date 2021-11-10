package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1

import cats.implicits.toTraverseOps
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.catalog_item.CatalogItemTechnologyV1.{
  Unrecognized => UnrecognizedTechnology
}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.catalog_item._

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object utils {

  def convertAttributeValueToV1(catalogAttributeValue: CatalogAttributeValue): CatalogAttributeValueV1 =
    CatalogAttributeValueV1(catalogAttributeValue.id, catalogAttributeValue.explicitAttributeVerification)

  def convertAttributeToV1(catalogAttribute: CatalogAttribute): CatalogAttributeV1 =
    catalogAttribute match {
      case s: SingleAttribute =>
        CatalogAttributeV1(Some(convertAttributeValueToV1(s.id)), Seq.empty[CatalogAttributeValueV1])
      case g: GroupAttribute => CatalogAttributeV1(None, g.ids.map(convertAttributeValueToV1))
    }

  def convertAttributesToV1(attributes: CatalogAttributes): CatalogAttributesV1 = {
    CatalogAttributesV1(
      certified = attributes.certified.map(convertAttributeToV1),
      declared = attributes.declared.map(convertAttributeToV1),
      verified = attributes.verified.map(convertAttributeToV1)
    )

  }

  def convertAttributeFromV1(catalogAttributeV1: CatalogAttributeV1): Either[RuntimeException, CatalogAttribute] = {
    val singleAttribute: Option[SingleAttribute] = catalogAttributeV1.single.map(attr =>
      SingleAttribute(CatalogAttributeValue(attr.id, attr.explicitAttributeVerification))
    )

    val groupAttribute: Option[GroupAttribute] = {
      val attributes: Seq[CatalogAttributeValue] =
        catalogAttributeV1.group.map(attr => CatalogAttributeValue(attr.id, attr.explicitAttributeVerification))

      Option(attributes).filter(_.nonEmpty).map(GroupAttribute)
    }

    (singleAttribute, groupAttribute) match {
      case (Some(attr), None) => Right(attr)
      case (None, Some(attr)) => Right(attr)
      case _                  => Left(new RuntimeException("Deserialization from protobuf failed"))
    }

  }

  def convertAttributesFromV1(attributes: CatalogAttributesV1): Either[RuntimeException, CatalogAttributes] = {
    for {
      certified <- attributes.certified.traverse(convertAttributeFromV1)
      declared  <- attributes.declared.traverse(convertAttributeFromV1)
      verified  <- attributes.verified.traverse(convertAttributeFromV1)
    } yield CatalogAttributes(certified = certified, declared = declared, verified = verified)

  }

  def convertDescriptorToV1(descriptor: CatalogDescriptor): Either[RuntimeException, CatalogDescriptorV1] = {
    Right(
      CatalogDescriptorV1(
        id = descriptor.id.toString,
        version = descriptor.version,
        description = descriptor.description,
        docs = descriptor.docs.map { doc =>
          CatalogDocumentV1(
            id = doc.id.toString,
            name = doc.name,
            contentType = doc.contentType,
            description = doc.description,
            path = doc.path,
            checksum = doc.checksum,
            uploadDate = doc.uploadDate.format(DateTimeFormatter.ISO_DATE_TIME)
          )
        },
        interface = descriptor.interface.map { doc =>
          CatalogDocumentV1(
            id = doc.id.toString,
            name = doc.name,
            contentType = doc.contentType,
            description = doc.description,
            path = doc.path,
            checksum = doc.checksum,
            uploadDate = doc.uploadDate.format(DateTimeFormatter.ISO_DATE_TIME)
          )
        },
        state = convertDescriptorStateToV1(descriptor.state),
        audience = descriptor.audience,
        voucherLifespan = descriptor.voucherLifespan
      )
    )
  }

  def convertDescriptorsToV1(
    descriptors: Seq[CatalogDescriptor]
  ): Either[RuntimeException, Seq[CatalogDescriptorV1]] = {
    descriptors.traverse { convertDescriptorToV1 }
  }

  def convertDescriptorFromV1(ver1: CatalogDescriptorV1): Either[Throwable, CatalogDescriptor] = {
    convertDescriptorStateFromV1(ver1.state).map { state =>
      CatalogDescriptor(
        id = UUID.fromString(ver1.id),
        version = ver1.version,
        description = ver1.description,
        interface = ver1.interface.map { doc =>
          CatalogDocument(
            id = UUID.fromString(doc.id),
            name = doc.name,
            contentType = doc.contentType,
            description = doc.description,
            path = doc.path,
            checksum = doc.checksum,
            uploadDate = OffsetDateTime.parse(doc.uploadDate, DateTimeFormatter.ISO_DATE_TIME)
          )
        },
        docs = ver1.docs.map { doc =>
          CatalogDocument(
            id = UUID.fromString(doc.id),
            name = doc.name,
            contentType = doc.contentType,
            description = doc.description,
            path = doc.path,
            checksum = doc.checksum,
            uploadDate = OffsetDateTime.parse(doc.uploadDate, DateTimeFormatter.ISO_DATE_TIME)
          )
        },
        state = state,
        audience = ver1.audience,
        voucherLifespan = ver1.voucherLifespan
      )
    }
  }

  def convertDescriptorsFromV1(descriptors: Seq[CatalogDescriptorV1]): Either[Throwable, Seq[CatalogDescriptor]] = {
    descriptors.traverse(convertDescriptorFromV1)
  }

  def convertDescriptorStateFromV1(state: CatalogDescriptorStateV1): Either[Throwable, CatalogDescriptorState] =
    state match {
      case CatalogDescriptorStateV1.DRAFT      => Right(Draft)
      case CatalogDescriptorStateV1.PUBLISHED  => Right(Published)
      case CatalogDescriptorStateV1.DEPRECATED => Right(Deprecated)
      case CatalogDescriptorStateV1.SUSPENDED  => Right(Suspended)
      case CatalogDescriptorStateV1.ARCHIVED   => Right(Archived)
      case CatalogDescriptorStateV1.Unrecognized(value) =>
        Left(new RuntimeException(s"Unable to deserialize catalog descriptor state value $value"))
    }

  def convertDescriptorStateToV1(state: CatalogDescriptorState): CatalogDescriptorStateV1 =
    state match {
      case Draft      => CatalogDescriptorStateV1.DRAFT
      case Published  => CatalogDescriptorStateV1.PUBLISHED
      case Deprecated => CatalogDescriptorStateV1.DEPRECATED
      case Suspended  => CatalogDescriptorStateV1.SUSPENDED
      case Archived   => CatalogDescriptorStateV1.ARCHIVED
    }

  def convertItemTechnologyFromV1(technology: CatalogItemTechnologyV1): Either[Throwable, CatalogItemTechnology] =
    technology match {
      case CatalogItemTechnologyV1.REST => Right(Rest)
      case CatalogItemTechnologyV1.SOAP => Right(Soap)
      case UnrecognizedTechnology(value) =>
        Left(new RuntimeException(s"Unable to deserialize catalog item technology value $value"))
    }

  def convertItemTechnologyToV1(technology: CatalogItemTechnology): CatalogItemTechnologyV1 =
    technology match {
      case Rest => CatalogItemTechnologyV1.REST
      case Soap => CatalogItemTechnologyV1.SOAP
    }

}
