package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1

import cats.implicits.toTraverseOps
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.{
  CatalogAttribute,
  CatalogAttributes,
  CatalogDescriptor,
  CatalogDescriptorStatus,
  CatalogDocument,
  GroupAttribute,
  SimpleAttribute
}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.catalog_item.CatalogAttributeV1.Empty
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.catalog_item.{
  CatalogAttributeV1,
  CatalogAttributesV1,
  CatalogDescriptorStatusV1,
  CatalogDescriptorV1,
  CatalogDocumentV1,
  GroupAttributeV1,
  SimpleAttributeV1
}

import java.util.UUID

@SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any"))
object utils {

  def convertAttributeToV1(catalogAttribute: CatalogAttribute): Either[RuntimeException, CatalogAttributeV1] =
    catalogAttribute match {
      case s: SimpleAttribute => Right(SimpleAttributeV1(s.id))
      case g: GroupAttribute  => Right(GroupAttributeV1(g.ids))
    }

  def convertAttributesToV1(attributes: CatalogAttributes): Either[RuntimeException, CatalogAttributesV1] = {
    for {
      certified <- attributes.certified.traverse(convertAttributeToV1)
      declared  <- attributes.declared.traverse(convertAttributeToV1)
      verified  <- attributes.verified.traverse(convertAttributeToV1)
    } yield CatalogAttributesV1(certified = certified, declared = declared, verified = verified)

  }

  def convertAttributeFromV1(catalogAttributeV1: CatalogAttributeV1): Either[RuntimeException, CatalogAttribute] =
    catalogAttributeV1 match {
      case s: SimpleAttributeV1 => Right(SimpleAttribute(s.id))
      case g: GroupAttributeV1  => Right(GroupAttribute(g.ids))
      case Empty                => Left(new RuntimeException("Deserialization from protobuf failed"))
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
              CatalogDocumentV1(id = doc.id.toString, name = doc.name, contentType = doc.contentType, path = doc.path)
            },
            status = status
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
          docs = ver1.docs.map { doc =>
            CatalogDocument(
              id = UUID.fromString(doc.id),
              name = doc.name,
              contentType = doc.contentType,
              path = doc.path
            )
          },
          status = status
        )
      }
    )
  }

}
