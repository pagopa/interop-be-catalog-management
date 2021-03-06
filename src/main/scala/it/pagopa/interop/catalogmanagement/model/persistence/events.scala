package it.pagopa.interop.catalogmanagement.model.persistence

import it.pagopa.interop.catalogmanagement.model.{CatalogDescriptor, CatalogDocument, CatalogItem}

sealed trait Event extends Persistable

final case class CatalogItemAdded(catalogItem: CatalogItem)                                        extends Event
final case class ClonedCatalogItemAdded(catalogItem: CatalogItem)                                  extends Event
final case class CatalogItemUpdated(catalogItem: CatalogItem)                                      extends Event
final case class CatalogItemWithDescriptorsDeleted(catalogItem: CatalogItem, descriptorId: String) extends Event
final case class CatalogItemDocumentUpdated(
  eServiceId: String,
  descriptorId: String,
  documentId: String,
  updatedDocument: CatalogDocument
) extends Event
final case class CatalogItemDeleted(catalogItemId: String)                                         extends Event

final case class CatalogItemDocumentAdded(
  eServiceId: String,
  descriptorId: String,
  document: CatalogDocument,
  isInterface: Boolean
) extends Event

final case class CatalogItemDocumentDeleted(eServiceId: String, descriptorId: String, documentId: String) extends Event

final case class CatalogItemDescriptorAdded(eServiceId: String, catalogDescriptor: CatalogDescriptor) extends Event

final case class CatalogItemDescriptorUpdated(eServiceId: String, catalogDescriptor: CatalogDescriptor) extends Event
