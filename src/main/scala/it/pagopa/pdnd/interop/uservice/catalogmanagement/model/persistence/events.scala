package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.{CatalogDocument, CatalogItem}

sealed trait Event extends Persistable

final case class CatalogItemAdded(catalogItem: CatalogItem)                         extends Event
final case class ClonedCatalogItemAdded(catalogItem: CatalogItem)                   extends Event
final case class CatalogItemUpdated(catalogItem: CatalogItem)                       extends Event
final case class CatalogItemDeleted(catalogItem: CatalogItem, descriptorId: String) extends Event
final case class DocumentUpdated(
  eServiceId: String,
  descriptorId: String,
  documentId: String,
  updatedDocument: CatalogDocument
) extends Event
