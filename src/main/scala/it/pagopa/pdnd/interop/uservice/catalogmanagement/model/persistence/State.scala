package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.{CatalogDescriptor, CatalogDocument, CatalogItem}

import java.util.UUID

final case class State(items: Map[String, CatalogItem]) extends Persistable {
  def delete(catalogItem: CatalogItem, descriptorId: String): State = {
    val updated = catalogItem.copy(descriptors = catalogItem.descriptors.filter(_.id != UUID.fromString(descriptorId)))
    if(updated.descriptors.isEmpty) {
      copy(items = items - (catalogItem.id.toString))
    } else {
      this.update(updated)
    }
  }

  def add(catalogItem: CatalogItem): State =
    copy(items = items + (catalogItem.id.toString -> catalogItem))

  def update(catalogItem: CatalogItem): State =
    copy(items = items + (catalogItem.id.toString -> catalogItem))

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  def updateDocument(eServiceId: String, descriptorId: String, documentId: String, modifiedDocument: CatalogDocument): State = {

    def updateDescriptor(descriptor: CatalogDescriptor): CatalogDescriptor = {
      val interface: Option[CatalogDocument] = descriptor.interface.filter(_.id.toString == documentId)
      val document: Option[CatalogDocument]  = descriptor.docs.find(_.id.toString == documentId)
      (interface, document) match {
        case (Some(_), None) => descriptor.copy(interface = Some(modifiedDocument))
        case (None, Some(_)) => descriptor.copy(docs = modifiedDocument +: descriptor.docs.filterNot(_.id.toString == documentId))
        case (None, None)    => descriptor
        case (Some(_), Some(_)) =>
          descriptor.copy(interface = Some(modifiedDocument), docs = modifiedDocument +: descriptor.docs.filterNot(_.id.toString == documentId))
      }
    }

    items.get(eServiceId).fold(this) {
      item => item.descriptors.find(_.id.toString == descriptorId) match {
        case Some(descriptor) =>
          val updatedItem = item.copy(descriptors = item.descriptors.filterNot(_.id.toString == descriptorId).appended(updateDescriptor(descriptor)))
          copy(items = items + (item.id.toString -> updatedItem))
        case None => this
      }
    }
  }

}

object State {
  val empty: State = State(items = Map.empty[String, CatalogItem])
}
