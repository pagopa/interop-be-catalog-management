package it.pagopa.interop.catalogmanagement.model.persistence.serializer

import cats.implicits._
import it.pagopa.interop.commons.utils.TypeConversions.{OffsetDateTimeOps, StringOps, LongOps}
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.catalogmanagement.model.persistence._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.catalog_item.{
  CatalogDocumentV1,
  CatalogItemV1
}
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.events._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.state.{CatalogItemsV1, StateV1}
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.utils._

import java.time.{OffsetDateTime, ZoneOffset}

package object v1 {

  final val defaultCreatedAt: OffsetDateTime   = OffsetDateTime.of(2022, 10, 21, 12, 0, 0, 0, ZoneOffset.UTC)
  final val defaultPublishedAt: OffsetDateTime = OffsetDateTime.of(2022, 12, 15, 12, 0, 0, 0, ZoneOffset.UTC)

  implicit def stateV1PersistEventDeserializer: PersistEventDeserializer[StateV1, State] =
    state => {
      val items: Either[Throwable, Seq[(String, CatalogItem)]] = state.items.traverse { itemsV1 =>
        for {
          attributes  <- convertAttributesFromV1(itemsV1.value.attributes)
          descriptors <- convertDescriptorsFromV1(itemsV1.value.descriptors)
          technology  <- convertItemTechnologyFromV1(itemsV1.value.technology)
          uuid        <- itemsV1.value.id.toUUID.toEither
          producerId  <- itemsV1.value.producerId.toUUID.toEither
          createdAt   <- itemsV1.value.createdAt.traverse(_.toOffsetDateTime.toEither)
        } yield itemsV1.key -> CatalogItem(
          id = uuid,
          producerId = producerId,
          name = itemsV1.value.name,
          description = itemsV1.value.description,
          technology = technology,
          attributes = attributes,
          descriptors = descriptors,
          createdAt = createdAt.getOrElse(defaultCreatedAt)
        )
      }
      items.map(its => State(its.toMap))
    }

  implicit def stateV1PersistEventSerializer: PersistEventSerializer[State, StateV1] =
    state => {
      val itemsV1: Either[Throwable, Seq[CatalogItemsV1]] = state.items.toSeq.traverse { case (key, catalogItem) =>
        for {
          descriptors <- convertDescriptorsToV1(catalogItem.descriptors)
        } yield CatalogItemsV1(
          key,
          CatalogItemV1(
            id = catalogItem.id.toString,
            producerId = catalogItem.producerId.toString,
            name = catalogItem.name,
            description = catalogItem.description,
            technology = convertItemTechnologyToV1(catalogItem.technology),
            attributes = convertAttributesToV1(catalogItem.attributes),
            descriptors = descriptors,
            createdAt = catalogItem.createdAt.toMillis.some
          )
        )

      }

      itemsV1.map(its => StateV1(its))
    }

  implicit def catalogItemV1AddedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemV1AddedV1, CatalogItemAdded] =
    event => {
      for {
        attributes  <- convertAttributesFromV1(event.catalogItem.attributes)
        descriptors <- convertDescriptorsFromV1(event.catalogItem.descriptors)
        technology  <- convertItemTechnologyFromV1(event.catalogItem.technology)
        uuid        <- event.catalogItem.id.toUUID.toEither
        producerId  <- event.catalogItem.producerId.toUUID.toEither
        createdAt   <- event.catalogItem.createdAt.traverse(_.toOffsetDateTime).toEither
      } yield CatalogItemAdded(catalogItem =
        CatalogItem(
          id = uuid,
          producerId = producerId,
          name = event.catalogItem.name,
          description = event.catalogItem.description,
          technology = technology,
          attributes = attributes,
          descriptors = descriptors,
          createdAt = createdAt.getOrElse(defaultCreatedAt)
        )
      )

    }

  implicit def catalogItemV1AddedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemAdded, CatalogItemV1AddedV1] =
    event => {
      for {
        descriptors <- convertDescriptorsToV1(event.catalogItem.descriptors)
      } yield CatalogItemV1AddedV1
        .of(
          CatalogItemV1(
            id = event.catalogItem.id.toString,
            producerId = event.catalogItem.producerId.toString,
            name = event.catalogItem.name,
            description = event.catalogItem.description,
            technology = convertItemTechnologyToV1(event.catalogItem.technology),
            attributes = convertAttributesToV1(event.catalogItem.attributes),
            descriptors = descriptors,
            createdAt = event.catalogItem.createdAt.toMillis.some
          )
        )
    }

  implicit def clonedCatalogItemV1AddedV1PersistEventDeserializer
    : PersistEventDeserializer[ClonedCatalogItemV1AddedV1, ClonedCatalogItemAdded] =
    event => {
      for {
        attributes  <- convertAttributesFromV1(event.catalogItem.attributes)
        descriptors <- convertDescriptorsFromV1(event.catalogItem.descriptors)
        technology  <- convertItemTechnologyFromV1(event.catalogItem.technology)
        uuid        <- event.catalogItem.id.toUUID.toEither
        producerId  <- event.catalogItem.producerId.toUUID.toEither
        createdAt   <- event.catalogItem.createdAt.traverse(_.toOffsetDateTime).toEither
      } yield ClonedCatalogItemAdded(catalogItem =
        CatalogItem(
          id = uuid,
          producerId = producerId,
          name = event.catalogItem.name,
          description = event.catalogItem.description,
          technology = technology,
          attributes = attributes,
          descriptors = descriptors,
          createdAt = createdAt.getOrElse(defaultCreatedAt)
        )
      )

    }

  implicit def clonedCatalogItemV1AddedV1PersistEventSerializer
    : PersistEventSerializer[ClonedCatalogItemAdded, ClonedCatalogItemV1AddedV1] =
    event => {
      for {
        descriptors <- convertDescriptorsToV1(event.catalogItem.descriptors)
      } yield ClonedCatalogItemV1AddedV1
        .of(
          CatalogItemV1(
            id = event.catalogItem.id.toString,
            producerId = event.catalogItem.producerId.toString,
            name = event.catalogItem.name,
            description = event.catalogItem.description,
            technology = convertItemTechnologyToV1(event.catalogItem.technology),
            attributes = convertAttributesToV1(event.catalogItem.attributes),
            descriptors = descriptors,
            createdAt = event.catalogItem.createdAt.toMillis.some
          )
        )
    }

  implicit def CatalogItemWithDescriptorsDeletedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemWithDescriptorsDeletedV1, CatalogItemWithDescriptorsDeleted] =
    event => {
      for {
        attributes  <- convertAttributesFromV1(event.catalogItem.attributes)
        descriptors <- convertDescriptorsFromV1(event.catalogItem.descriptors)
        technology  <- convertItemTechnologyFromV1(event.catalogItem.technology)
        uuid        <- event.catalogItem.id.toUUID.toEither
        producerId  <- event.catalogItem.producerId.toUUID.toEither
        createdAt   <- event.catalogItem.createdAt.traverse(_.toOffsetDateTime).toEither
      } yield CatalogItemWithDescriptorsDeleted(
        catalogItem = CatalogItem(
          id = uuid,
          producerId = producerId,
          name = event.catalogItem.name,
          description = event.catalogItem.description,
          technology = technology,
          attributes = attributes,
          descriptors = descriptors,
          createdAt = createdAt.getOrElse(defaultCreatedAt)
        ),
        descriptorId = event.descriptorId
      )

    }

  implicit def CatalogItemWithDescriptorsDeletedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemWithDescriptorsDeleted, CatalogItemWithDescriptorsDeletedV1] =
    event => {
      for {
        descriptors <- convertDescriptorsToV1(event.catalogItem.descriptors)
      } yield CatalogItemWithDescriptorsDeletedV1
        .of(
          CatalogItemV1(
            id = event.catalogItem.id.toString,
            producerId = event.catalogItem.producerId.toString,
            name = event.catalogItem.name,
            description = event.catalogItem.description,
            technology = convertItemTechnologyToV1(event.catalogItem.technology),
            attributes = convertAttributesToV1(event.catalogItem.attributes),
            descriptors = descriptors,
            createdAt = event.catalogItem.createdAt.toMillis.some
          ),
          descriptorId = event.descriptorId
        )
    }

  implicit def catalogItemDeletedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemDeletedV1, CatalogItemDeleted] =
    event => Right[Throwable, CatalogItemDeleted](CatalogItemDeleted(catalogItemId = event.catalogItemId))

  implicit def catalogItemDeletedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemDeleted, CatalogItemDeletedV1] =
    event => Right[Throwable, CatalogItemDeletedV1](CatalogItemDeletedV1.of(catalogItemId = event.catalogItemId))

  implicit def catalogItemV1UpdatedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemV1UpdatedV1, CatalogItemUpdated] =
    event => {
      for {
        attributes  <- convertAttributesFromV1(event.catalogItem.attributes)
        descriptors <- convertDescriptorsFromV1(event.catalogItem.descriptors)
        technology  <- convertItemTechnologyFromV1(event.catalogItem.technology)
        uuid        <- event.catalogItem.id.toUUID.toEither
        producerId  <- event.catalogItem.producerId.toUUID.toEither
        createdAt   <- event.catalogItem.createdAt.traverse(_.toOffsetDateTime).toEither
      } yield CatalogItemUpdated(catalogItem =
        CatalogItem(
          id = uuid,
          producerId = producerId,
          name = event.catalogItem.name,
          description = event.catalogItem.description,
          technology = technology,
          attributes = attributes,
          descriptors = descriptors,
          createdAt = createdAt.getOrElse(defaultCreatedAt)
        )
      )

    }

  implicit def catalogItemV1UpdatedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemUpdated, CatalogItemV1UpdatedV1] =
    event => {
      for {
        descriptors <- convertDescriptorsToV1(event.catalogItem.descriptors)
      } yield CatalogItemV1UpdatedV1
        .of(
          CatalogItemV1(
            id = event.catalogItem.id.toString,
            producerId = event.catalogItem.producerId.toString,
            name = event.catalogItem.name,
            description = event.catalogItem.description,
            technology = convertItemTechnologyToV1(event.catalogItem.technology),
            attributes = convertAttributesToV1(event.catalogItem.attributes),
            descriptors = descriptors,
            createdAt = event.catalogItem.createdAt.toMillis.some
          )
        )
    }

  implicit def documentUpdatedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemDocumentUpdatedV1, CatalogItemDocumentUpdated] =
    event => {
      for {
        documentId <- event.updatedDocument.id.toUUID.toEither
        uploadDate <- event.updatedDocument.uploadDate.toOffsetDateTime.toEither
      } yield CatalogItemDocumentUpdated(
        eServiceId = event.eServiceId,
        descriptorId = event.descriptorId,
        documentId = event.documentId,
        updatedDocument = CatalogDocument(
          id = documentId,
          name = event.updatedDocument.name,
          contentType = event.updatedDocument.contentType,
          prettyName = event.updatedDocument.prettyName,
          path = event.updatedDocument.path,
          checksum = event.updatedDocument.checksum,
          uploadDate = uploadDate
        ),
        serverUrls = event.serverUrls.toList
      )
    }

  implicit def documentUpdatedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemDocumentUpdated, CatalogItemDocumentUpdatedV1] =
    event => {
      for {
        uploadDate <- event.updatedDocument.uploadDate.asFormattedString.toEither
      } yield CatalogItemDocumentUpdatedV1(
        eServiceId = event.eServiceId,
        descriptorId = event.descriptorId,
        documentId = event.documentId,
        updatedDocument = CatalogDocumentV1(
          id = event.updatedDocument.id.toString,
          name = event.updatedDocument.name,
          contentType = event.updatedDocument.contentType,
          prettyName = event.updatedDocument.prettyName,
          path = event.updatedDocument.path,
          checksum = event.updatedDocument.checksum,
          uploadDate = uploadDate
        ),
        serverUrls = event.serverUrls
      )
    }

  implicit def catalogItemDocumentAddedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemDocumentAdded, CatalogItemDocumentAddedV1] =
    event => {
      for {
        uploadDate <- event.document.uploadDate.asFormattedString.toEither
      } yield CatalogItemDocumentAddedV1(
        eServiceId = event.eServiceId,
        descriptorId = event.descriptorId,
        document = CatalogDocumentV1(
          id = event.document.id.toString,
          name = event.document.name,
          contentType = event.document.contentType,
          prettyName = event.document.prettyName,
          path = event.document.path,
          checksum = event.document.checksum,
          uploadDate = uploadDate
        ),
        isInterface = event.isInterface,
        serverUrls = event.serverUrls
      )
    }

  implicit def catalogItemDocumentAddedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemDocumentAddedV1, CatalogItemDocumentAdded] =
    event => {
      for {
        documentId <- event.document.id.toUUID.toEither
        uploadDate <- event.document.uploadDate.toOffsetDateTime.toEither
      } yield CatalogItemDocumentAdded(
        eServiceId = event.eServiceId,
        descriptorId = event.descriptorId,
        document = CatalogDocument(
          id = documentId,
          name = event.document.name,
          contentType = event.document.contentType,
          prettyName = event.document.prettyName,
          path = event.document.path,
          checksum = event.document.checksum,
          uploadDate = uploadDate
        ),
        isInterface = event.isInterface,
        serverUrls = event.serverUrls.toList
      )
    }

  implicit def catalogItemDocumentDeletedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemDocumentDeleted, CatalogItemDocumentDeletedV1] = event => {
    Right[Throwable, CatalogItemDocumentDeletedV1](
      CatalogItemDocumentDeletedV1(
        eServiceId = event.eServiceId,
        descriptorId = event.descriptorId,
        documentId = event.documentId
      )
    )
  }

  implicit def catalogItemDocumentDeletedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemDocumentDeletedV1, CatalogItemDocumentDeleted] = event => {
    Right[Throwable, CatalogItemDocumentDeleted](
      CatalogItemDocumentDeleted(
        eServiceId = event.eServiceId,
        descriptorId = event.descriptorId,
        documentId = event.documentId
      )
    )
  }

  implicit def catalogItemDescriptorUpdatedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemDescriptorUpdated, CatalogItemDescriptorUpdatedV1] =
    event => {
      for {
        descriptor <- convertDescriptorToV1(event.catalogDescriptor)
      } yield CatalogItemDescriptorUpdatedV1(eServiceId = event.eServiceId, catalogDescriptor = descriptor)
    }

  implicit def catalogItemDescriptorUpdatedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemDescriptorUpdatedV1, CatalogItemDescriptorUpdated] = event => {
    for {
      descriptor <- convertDescriptorFromV1(event.catalogDescriptor)
    } yield CatalogItemDescriptorUpdated(eServiceId = event.eServiceId, catalogDescriptor = descriptor)
  }

  implicit def catalogItemDescriptorAddedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemDescriptorAdded, CatalogItemDescriptorAddedV1] = event => {
    for {
      descriptor <- convertDescriptorToV1(event.catalogDescriptor)
    } yield CatalogItemDescriptorAddedV1(eServiceId = event.eServiceId, catalogDescriptor = descriptor)
  }

  implicit def catalogItemDescriptorAddedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemDescriptorAddedV1, CatalogItemDescriptorAdded] = event => {
    for {
      descriptor <- convertDescriptorFromV1(event.catalogDescriptor)
    } yield CatalogItemDescriptorAdded(eServiceId = event.eServiceId, catalogDescriptor = descriptor)
  }

}
