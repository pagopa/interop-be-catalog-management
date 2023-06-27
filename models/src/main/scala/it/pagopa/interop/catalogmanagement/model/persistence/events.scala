package it.pagopa.interop.catalogmanagement.model.persistence

import it.pagopa.interop.catalogmanagement.model.{CatalogDescriptor, CatalogDocument, CatalogItem}
import it.pagopa.interop.commons.queue.message.ProjectableEvent

sealed trait Event extends Persistable with ProjectableEvent

final case class CatalogItemAdded(catalogItem: CatalogItem)                                        extends Event
final case class ClonedCatalogItemAdded(catalogItem: CatalogItem)                                  extends Event
final case class CatalogItemUpdated(catalogItem: CatalogItem)                                      extends Event
// This event leads just the deletion of the descriptor. The old name has been kept to maintain data retrocompatibility
final case class CatalogItemWithDescriptorsDeleted(catalogItem: CatalogItem, descriptorId: String) extends Event
final case class CatalogItemDocumentUpdated(
  eServiceId: String,
  descriptorId: String,
  documentId: String,
  updatedDocument: CatalogDocument,
  serverUrls: List[String]
) extends Event
final case class CatalogItemDeleted(catalogItemId: String)                                         extends Event

final case class CatalogItemDocumentAdded(
  eServiceId: String,
  descriptorId: String,
  document: CatalogDocument,
  isInterface: Boolean,
  serverUrls: List[String]
) extends Event

final case class CatalogItemDocumentDeleted(eServiceId: String, descriptorId: String, documentId: String) extends Event

final case class CatalogItemDescriptorAdded(eServiceId: String, catalogDescriptor: CatalogDescriptor) extends Event

final case class CatalogItemDescriptorUpdated(eServiceId: String, catalogDescriptor: CatalogDescriptor) extends Event

final case class MovedAttributesFromEserviceToDescriptors(catalogItem: CatalogItem) extends Event
