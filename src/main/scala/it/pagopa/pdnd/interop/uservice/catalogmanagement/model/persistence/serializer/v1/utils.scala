package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1

import cats.implicits.toTraverseOps
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.catalog_item._

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any"))
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

  def convertDescriptorsToV1(
    descriptors: Seq[CatalogDescriptor]
  ): Either[RuntimeException, Seq[CatalogDescriptorV1]] = {
    descriptors.traverse { ver =>
      CatalogDescriptorStatusV1
        .fromName(ver.status.stringify)
        .toRight(new RuntimeException("Invalid descriptor status"))
        .map { status =>
          CatalogDescriptorV1(
            id = ver.id.toString,
            version = ver.version,
            description = ver.description,
            docs = ver.docs.map { doc =>
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
            interface = ver.interface.map { doc =>
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
            status = status,
            audience = ver.audience,
            voucherLifespan = ver.voucherLifespan
          )
        }

    }
  }

  def convertDescriptorsFromV1(descriptors: Seq[CatalogDescriptorV1]): Either[Throwable, Seq[CatalogDescriptor]] = {
    descriptors.traverse(ver1 =>
      CatalogDescriptorStatus.fromText(ver1.status.name).map { status =>
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
          status = status,
          audience = ver1.audience,
          voucherLifespan = ver1.voucherLifespan
        )
      }
    )
  }

}
