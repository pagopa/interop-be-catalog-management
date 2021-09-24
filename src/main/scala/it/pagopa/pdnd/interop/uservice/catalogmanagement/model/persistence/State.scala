package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.{CatalogDescriptor, CatalogDocument, CatalogItem}

import java.util.UUID

final case class State(items: Map[String, CatalogItem]) extends Persistable {
  def delete(catalogItem: CatalogItem, descriptorId: String): State = {
    val updated = catalogItem.copy(descriptors = catalogItem.descriptors.filter(_.id != UUID.fromString(descriptorId)))
    if (updated.descriptors.isEmpty) {
      copy(items = items - (catalogItem.id.toString))
    } else {
      this.update(updated)
    }
  }

  def deleteEService(catalogItemId: String): State =
    copy(items = items - (catalogItemId))

  def add(catalogItem: CatalogItem): State =
    copy(items = items + (catalogItem.id.toString -> catalogItem))

  def update(catalogItem: CatalogItem): State =
    copy(items = items + (catalogItem.id.toString -> catalogItem))

  def addDescriptor(eServiceId: String, catalogDescriptor: CatalogDescriptor): State = {
    items.get(eServiceId) match {
      case Some(item) =>
        val updatedItem = item.copy(descriptors = catalogDescriptor +: item.descriptors)
        copy(items = items + (item.id.toString -> updatedItem))
      case None => this
    }
  }

  def updateDescriptor(eServiceId: String, catalogDescriptor: CatalogDescriptor): State = {
    updateDescriptorLens(eServiceId, catalogDescriptor.id.toString, _ => catalogDescriptor)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  def updateDocument(
    eServiceId: String,
    descriptorId: String,
    documentId: String,
    modifiedDocument: CatalogDocument
  ): State = {

    def updateDocument(docId: String)(descriptor: CatalogDescriptor): CatalogDescriptor = {
      val interface: Option[CatalogDocument] = descriptor.interface.filter(_.id.toString == docId)
      val document: Option[CatalogDocument]  = descriptor.docs.find(_.id.toString == docId)
      (interface, document) match {
        case (Some(_), None) => descriptor.copy(interface = Some(modifiedDocument))
        case (None, Some(_)) =>
          descriptor.copy(docs = modifiedDocument +: descriptor.docs.filterNot(_.id.toString == docId))
        case (None, None) => descriptor
        case (Some(_), Some(_)) =>
          descriptor.copy(
            interface = Some(modifiedDocument),
            docs = modifiedDocument +: descriptor.docs.filterNot(_.id.toString == docId)
          )
      }
    }

    updateDescriptorLens(eServiceId, descriptorId, updateDocument(documentId))
  }

  def addItemDocument(
    eServiceId: String,
    descriptorId: String,
    openapiDoc: CatalogDocument,
    isInterface: Boolean
  ): State = {
    def addDocument(doc: CatalogDocument, isInterface: Boolean)(descriptor: CatalogDescriptor) = {
      if (isInterface) {
        descriptor.copy(interface = Some(doc))
      } else {
        descriptor.copy(docs = doc +: descriptor.docs)
      }
    }
    updateDescriptorLens(eServiceId, descriptorId, addDocument(openapiDoc, isInterface))
  }

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  def deleteDocument(eServiceId: String, descriptorId: String, documentId: String): State = {

    def deleteDescriptorDocument(docId: String)(descriptor: CatalogDescriptor): CatalogDescriptor = {
      val interface: Option[CatalogDocument] = descriptor.interface.filter(_.id.toString == docId)
      val document: Option[CatalogDocument]  = descriptor.docs.find(_.id.toString == docId)
      (interface, document) match {
        case (Some(_), None) => descriptor.copy(interface = None)
        case (None, Some(_)) => descriptor.copy(docs = descriptor.docs.filterNot(_.id.toString == docId))
        case (None, None)    => descriptor
        case (Some(_), Some(_)) =>
          descriptor.copy(interface = None, docs = descriptor.docs.filterNot(_.id.toString == docId))
      }
    }

    updateDescriptorLens(eServiceId, descriptorId, deleteDescriptorDocument(documentId))
  }

  /*
    Inspect the state data in order to find the corresponding descriptor to update with a custom descriptor operation.
    Even though quick and dirty, it behaves like a lens.
    //TODO for updates of complex states as this, evaluate the introduction of an optic library (e.g.: Monocle)
   */
  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  private def updateDescriptorLens(
                                    eServiceId: String,
                                    descriptorId: String,
                                    descriptorOperation: CatalogDescriptor => CatalogDescriptor
  ): State = {

    val newState = for {
      item       <- items.get(eServiceId)
      descriptor <- item.descriptors.find(_.id.toString == descriptorId)
      updatedItem = item.copy(descriptors =
        item.descriptors.filterNot(_.id.toString == descriptorId).prepended(descriptorOperation(descriptor))
      )
      updatedState = copy(items = items + (item.id.toString -> updatedItem))
    } yield updatedState

    newState.getOrElse(this)
  }
}

object State {
  val empty: State = State(items = Map.empty[String, CatalogItem])
}
