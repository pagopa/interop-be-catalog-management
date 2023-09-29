package it.pagopa.interop.catalogmanagement.model.persistence.serializer

import cats.syntax.all._
import it.pagopa.interop.commons.utils.TypeConversions.{OffsetDateTimeOps, StringOps, LongOps}
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.catalogmanagement.model.persistence._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.catalog_item._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.events._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.state._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.utils._
import java.time.{OffsetDateTime, ZoneOffset}

package object v1 {

  final val defaultCreatedAt: OffsetDateTime   = OffsetDateTime.of(2022, 10, 21, 12, 0, 0, 0, ZoneOffset.UTC)
  final val defaultPublishedAt: OffsetDateTime = OffsetDateTime.of(2022, 12, 15, 12, 0, 0, 0, ZoneOffset.UTC)

  implicit def stateV1PersistEventDeserializer: PersistEventDeserializer[StateV1, State] = state =>
    state.items
      .traverse { itemsV1 => convertCatalogItemsFromV1(itemsV1.value).tupleLeft(itemsV1.key) }
      .map(its => State(its.toMap))

  implicit def stateV1PersistEventSerializer: PersistEventSerializer[State, StateV1] = state =>
    state.items.toSeq
      .traverse { case (key, catalogItem) => convertCatalogItemsToV1(catalogItem).map(CatalogItemsV1(key, _)) }
      .map(StateV1(_))

  implicit def catalogItemV1AddedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemV1AddedV1, CatalogItemAdded] = event =>
    for {
      attributes   <- event.catalogItem.attributes.traverse(convertAttributesFromV1)
      descriptors  <- convertDescriptorsFromV1(event.catalogItem.descriptors)
      technology   <- convertItemTechnologyFromV1(event.catalogItem.technology)
      uuid         <- event.catalogItem.id.toUUID.toEither
      producerId   <- event.catalogItem.producerId.toUUID.toEither
      createdAt    <- event.catalogItem.createdAt.traverse(_.toOffsetDateTime).toEither
      mode         <- event.catalogItem.mode.traverse(convertItemModeFromV1)
      riskAnalysis <- event.catalogItem.riskAnalysis.traverse(convertRiskAnalysisFromV1)
    } yield CatalogItemAdded(catalogItem =
      CatalogItem(
        id = uuid,
        producerId = producerId,
        name = event.catalogItem.name,
        description = event.catalogItem.description,
        technology = technology,
        descriptors = descriptors,
        createdAt = createdAt.getOrElse(defaultCreatedAt),
        attributes = attributes,
        mode = mode.getOrElse(CatalogItemMode.default),
        riskAnalysis = riskAnalysis
      )
    )

  implicit def catalogItemV1AddedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemAdded, CatalogItemV1AddedV1] = event =>
    for {
      descriptors  <- convertDescriptorsToV1(event.catalogItem.descriptors)
      riskAnalysis <- convertRiskAnalysisToV1(event.catalogItem.riskAnalysis)
    } yield CatalogItemV1AddedV1
      .of(
        CatalogItemV1(
          id = event.catalogItem.id.toString,
          producerId = event.catalogItem.producerId.toString,
          name = event.catalogItem.name,
          description = event.catalogItem.description,
          technology = convertItemTechnologyToV1(event.catalogItem.technology),
          attributes = event.catalogItem.attributes.map(convertAttributesToV1),
          descriptors = descriptors,
          createdAt = event.catalogItem.createdAt.toMillis.some,
          mode = convertItemModeToV1(event.catalogItem.mode).some,
          riskAnalysis = riskAnalysis
        )
      )

  implicit def clonedCatalogItemV1AddedV1PersistEventDeserializer
    : PersistEventDeserializer[ClonedCatalogItemV1AddedV1, ClonedCatalogItemAdded] = event =>
    for {
      descriptors  <- convertDescriptorsFromV1(event.catalogItem.descriptors)
      technology   <- convertItemTechnologyFromV1(event.catalogItem.technology)
      uuid         <- event.catalogItem.id.toUUID.toEither
      producerId   <- event.catalogItem.producerId.toUUID.toEither
      createdAt    <- event.catalogItem.createdAt.traverse(_.toOffsetDateTime).toEither
      attributes   <- event.catalogItem.attributes.traverse(convertAttributesFromV1)
      mode         <- event.catalogItem.mode.traverse(convertItemModeFromV1)
      riskAnalysis <- event.catalogItem.riskAnalysis.traverse(convertRiskAnalysisFromV1)
    } yield ClonedCatalogItemAdded(catalogItem =
      CatalogItem(
        id = uuid,
        producerId = producerId,
        name = event.catalogItem.name,
        description = event.catalogItem.description,
        technology = technology,
        descriptors = descriptors,
        attributes = attributes,
        createdAt = createdAt.getOrElse(defaultCreatedAt),
        mode = mode.getOrElse(CatalogItemMode.default),
        riskAnalysis = riskAnalysis
      )
    )

  implicit def clonedCatalogItemV1AddedV1PersistEventSerializer
    : PersistEventSerializer[ClonedCatalogItemAdded, ClonedCatalogItemV1AddedV1] = event =>
    for {
      descriptors  <- convertDescriptorsToV1(event.catalogItem.descriptors)
      riskAnalysis <- convertRiskAnalysisToV1(event.catalogItem.riskAnalysis)
    } yield ClonedCatalogItemV1AddedV1.of(
      CatalogItemV1(
        id = event.catalogItem.id.toString,
        producerId = event.catalogItem.producerId.toString,
        name = event.catalogItem.name,
        description = event.catalogItem.description,
        technology = convertItemTechnologyToV1(event.catalogItem.technology),
        attributes = event.catalogItem.attributes.map(convertAttributesToV1),
        descriptors = descriptors,
        createdAt = event.catalogItem.createdAt.toMillis.some,
        mode = convertItemModeToV1(event.catalogItem.mode).some,
        riskAnalysis = riskAnalysis
      )
    )

  implicit def CatalogItemWithDescriptorsDeletedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemWithDescriptorsDeletedV1, CatalogItemWithDescriptorsDeleted] = event =>
    for {
      descriptors  <- convertDescriptorsFromV1(event.catalogItem.descriptors)
      technology   <- convertItemTechnologyFromV1(event.catalogItem.technology)
      uuid         <- event.catalogItem.id.toUUID.toEither
      producerId   <- event.catalogItem.producerId.toUUID.toEither
      createdAt    <- event.catalogItem.createdAt.traverse(_.toOffsetDateTime).toEither
      attributes   <- event.catalogItem.attributes.traverse(convertAttributesFromV1)
      mode         <- event.catalogItem.mode.traverse(convertItemModeFromV1)
      riskAnalysis <- event.catalogItem.riskAnalysis.traverse(convertRiskAnalysisFromV1)
    } yield CatalogItemWithDescriptorsDeleted(
      catalogItem = CatalogItem(
        id = uuid,
        producerId = producerId,
        name = event.catalogItem.name,
        description = event.catalogItem.description,
        technology = technology,
        descriptors = descriptors,
        attributes = attributes,
        createdAt = createdAt.getOrElse(defaultCreatedAt),
        mode = mode.getOrElse(CatalogItemMode.default),
        riskAnalysis = riskAnalysis
      ),
      descriptorId = event.descriptorId
    )

  implicit def CatalogItemWithDescriptorsDeletedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemWithDescriptorsDeleted, CatalogItemWithDescriptorsDeletedV1] = event =>
    for {
      descriptors  <- convertDescriptorsToV1(event.catalogItem.descriptors)
      riskAnalysis <- convertRiskAnalysisToV1(event.catalogItem.riskAnalysis)
    } yield CatalogItemWithDescriptorsDeletedV1
      .of(
        CatalogItemV1(
          id = event.catalogItem.id.toString,
          producerId = event.catalogItem.producerId.toString,
          name = event.catalogItem.name,
          description = event.catalogItem.description,
          technology = convertItemTechnologyToV1(event.catalogItem.technology),
          attributes = event.catalogItem.attributes.map(convertAttributesToV1),
          descriptors = descriptors,
          createdAt = event.catalogItem.createdAt.toMillis.some,
          mode = convertItemModeToV1(event.catalogItem.mode).some,
          riskAnalysis = riskAnalysis
        ),
        descriptorId = event.descriptorId
      )

  implicit def catalogItemDeletedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemDeletedV1, CatalogItemDeleted] =
    event => Right[Throwable, CatalogItemDeleted](CatalogItemDeleted(catalogItemId = event.catalogItemId))

  implicit def catalogItemDeletedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemDeleted, CatalogItemDeletedV1] =
    event => Right[Throwable, CatalogItemDeletedV1](CatalogItemDeletedV1.of(catalogItemId = event.catalogItemId))

  implicit def catalogItemV1UpdatedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemV1UpdatedV1, CatalogItemUpdated] = event =>
    for {
      descriptors  <- convertDescriptorsFromV1(event.catalogItem.descriptors)
      technology   <- convertItemTechnologyFromV1(event.catalogItem.technology)
      uuid         <- event.catalogItem.id.toUUID.toEither
      producerId   <- event.catalogItem.producerId.toUUID.toEither
      createdAt    <- event.catalogItem.createdAt.traverse(_.toOffsetDateTime).toEither
      attributes   <- event.catalogItem.attributes.traverse(convertAttributesFromV1)
      mode         <- event.catalogItem.mode.traverse(convertItemModeFromV1)
      riskAnalysis <- event.catalogItem.riskAnalysis.traverse(convertRiskAnalysisFromV1)
    } yield CatalogItemUpdated(catalogItem =
      CatalogItem(
        id = uuid,
        producerId = producerId,
        name = event.catalogItem.name,
        description = event.catalogItem.description,
        technology = technology,
        descriptors = descriptors,
        attributes = attributes,
        createdAt = createdAt.getOrElse(defaultCreatedAt),
        mode = mode.getOrElse(CatalogItemMode.default),
        riskAnalysis = riskAnalysis
      )
    )

  implicit def catalogItemV1UpdatedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemUpdated, CatalogItemV1UpdatedV1] = event =>
    for {
      descriptors  <- convertDescriptorsToV1(event.catalogItem.descriptors)
      riskAnalysis <- convertRiskAnalysisToV1(event.catalogItem.riskAnalysis)
    } yield CatalogItemV1UpdatedV1
      .of(
        CatalogItemV1(
          id = event.catalogItem.id.toString,
          producerId = event.catalogItem.producerId.toString,
          name = event.catalogItem.name,
          description = event.catalogItem.description,
          technology = convertItemTechnologyToV1(event.catalogItem.technology),
          attributes = event.catalogItem.attributes.map(convertAttributesToV1),
          descriptors = descriptors,
          createdAt = event.catalogItem.createdAt.toMillis.some,
          mode = convertItemModeToV1(event.catalogItem.mode).some,
          riskAnalysis = riskAnalysis
        )
      )

  implicit def documentUpdatedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemDocumentUpdatedV1, CatalogItemDocumentUpdated] = event =>
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

  implicit def documentUpdatedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemDocumentUpdated, CatalogItemDocumentUpdatedV1] = event =>
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

  implicit def catalogItemDocumentAddedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemDocumentAdded, CatalogItemDocumentAddedV1] = event =>
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

  implicit def catalogItemDocumentAddedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemDocumentAddedV1, CatalogItemDocumentAdded] = event =>
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

  implicit def catalogItemDocumentDeletedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemDocumentDeleted, CatalogItemDocumentDeletedV1] = event =>
    Right[Throwable, CatalogItemDocumentDeletedV1](
      CatalogItemDocumentDeletedV1(
        eServiceId = event.eServiceId,
        descriptorId = event.descriptorId,
        documentId = event.documentId
      )
    )

  implicit def catalogItemDocumentDeletedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemDocumentDeletedV1, CatalogItemDocumentDeleted] = event =>
    Right[Throwable, CatalogItemDocumentDeleted](
      CatalogItemDocumentDeleted(
        eServiceId = event.eServiceId,
        descriptorId = event.descriptorId,
        documentId = event.documentId
      )
    )

  implicit def catalogItemDescriptorUpdatedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemDescriptorUpdated, CatalogItemDescriptorUpdatedV1] = event =>
    for {
      descriptor <- convertDescriptorToV1(event.catalogDescriptor)
    } yield CatalogItemDescriptorUpdatedV1(eServiceId = event.eServiceId, catalogDescriptor = descriptor)

  implicit def catalogItemDescriptorUpdatedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemDescriptorUpdatedV1, CatalogItemDescriptorUpdated] = event =>
    for {
      descriptor <- convertDescriptorFromV1(event.catalogDescriptor)
    } yield CatalogItemDescriptorUpdated(eServiceId = event.eServiceId, catalogDescriptor = descriptor)

  implicit def catalogItemDescriptorAddedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemDescriptorAdded, CatalogItemDescriptorAddedV1] = event =>
    for {
      descriptor <- convertDescriptorToV1(event.catalogDescriptor)
    } yield CatalogItemDescriptorAddedV1(eServiceId = event.eServiceId, catalogDescriptor = descriptor)

  implicit def catalogItemDescriptorAddedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemDescriptorAddedV1, CatalogItemDescriptorAdded] = event =>
    for {
      descriptor <- convertDescriptorFromV1(event.catalogDescriptor)
    } yield CatalogItemDescriptorAdded(eServiceId = event.eServiceId, catalogDescriptor = descriptor)

  implicit def movedAttributesFromEserviceToDescriptorsV1PersistEventSerializer
    : PersistEventSerializer[MovedAttributesFromEserviceToDescriptors, MovedAttributesFromEserviceToDescriptorsV1] =
    event => convertCatalogItemsToV1(event.catalogItem).map(MovedAttributesFromEserviceToDescriptorsV1(_))

  implicit def movedAttributesFromEserviceToDescriptorsV1PersistEventDeserializer
    : PersistEventDeserializer[MovedAttributesFromEserviceToDescriptorsV1, MovedAttributesFromEserviceToDescriptors] =
    event => convertCatalogItemsFromV1(event.catalogItem).map(MovedAttributesFromEserviceToDescriptors)

  implicit def catalogItemRiskAnalysisAddedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemRiskAnalysisAdded, CatalogItemRiskAnalysisAddedV1] = event =>
    convertCatalogItemsToV1(event.catalogItem).map(catalogItem =>
      CatalogItemRiskAnalysisAddedV1(catalogItem = catalogItem, catalogRiskAnalysisId = event.catalogRiskAnalysisId)
    )

  implicit def catalogItemRiskAnalysisAddedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemRiskAnalysisAddedV1, CatalogItemRiskAnalysisAdded] = event =>
    convertCatalogItemsFromV1(event.catalogItem).map(catalogItem =>
      CatalogItemRiskAnalysisAdded(catalogItem = catalogItem, catalogRiskAnalysisId = event.catalogRiskAnalysisId)
    )

  implicit def catalogItemRiskAnalysisUpdatedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemRiskAnalysisUpdated, CatalogItemRiskAnalysisUpdatedV1] = event =>
    convertCatalogItemsToV1(event.catalogItem).map(catalogItem =>
      CatalogItemRiskAnalysisUpdatedV1(catalogItem = catalogItem, catalogRiskAnalysisId = event.catalogRiskAnalysisId)
    )

  implicit def catalogItemRiskAnalysisUpdatedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemRiskAnalysisUpdatedV1, CatalogItemRiskAnalysisUpdated] = event =>
    convertCatalogItemsFromV1(event.catalogItem).map(catalogItem =>
      CatalogItemRiskAnalysisUpdated(catalogItem = catalogItem, catalogRiskAnalysisId = event.catalogRiskAnalysisId)
    )

  implicit def catalogItemRiskAnalysisDeletedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemRiskAnalysisDeleted, CatalogItemRiskAnalysisDeletedV1] = event =>
    for {
      catalogItem <- convertCatalogItemsToV1(event.catalogItem)
    } yield CatalogItemRiskAnalysisDeletedV1(
      catalogItem = catalogItem,
      catalogRiskAnalysisId = event.catalogRiskAnalysisId
    )

  implicit def catalogItemRiskAnalysisDeletedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemRiskAnalysisDeletedV1, CatalogItemRiskAnalysisDeleted] = event =>
    for {
      catalogItem <- convertCatalogItemsFromV1(event.catalogItem)
    } yield CatalogItemRiskAnalysisDeleted(
      catalogItem = catalogItem,
      catalogRiskAnalysisId = event.catalogRiskAnalysisId
    )
}
