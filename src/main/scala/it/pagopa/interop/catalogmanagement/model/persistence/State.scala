package it.pagopa.interop.catalogmanagement.model.persistence

import it.pagopa.interop.catalogmanagement.model.{CatalogDescriptor, CatalogDocument, CatalogItem}
import it.pagopa.interop.catalogmanagement.model.CatalogRiskAnalysis

final case class State(items: Map[String, CatalogItem]) extends Persistable {

  def deleteDescriptor(catalogItem: CatalogItem, descriptorId: String): State = {
    val updated = catalogItem.copy(descriptors = catalogItem.descriptors.filter(_.id.toString != descriptorId))
    update(updated)
  }

  def deleteEService(catalogItemId: String): State = copy(items = items - catalogItemId)

  def add(catalogItem: CatalogItem): State = copy(items = items + (catalogItem.id.toString -> catalogItem))

  def update(catalogItem: CatalogItem): State = copy(items = items + (catalogItem.id.toString -> catalogItem))

  def addRiskAnalysis(eServiceId: String, catalogRiskAnalysis: CatalogRiskAnalysis): State =
    items.get(eServiceId) match {
      case Some(item) =>
        val updatedItem = item.copy(riskAnalysis = catalogRiskAnalysis +: item.riskAnalysis)
        copy(items = items + (item.id.toString -> updatedItem))
      case None       => this
    }

  def addDescriptor(eServiceId: String, catalogDescriptor: CatalogDescriptor): State = items.get(eServiceId) match {
    case Some(item) =>
      val updatedItem = item.copy(descriptors = catalogDescriptor +: item.descriptors)
      copy(items = items + (item.id.toString -> updatedItem))
    case None       => this
  }

  def updateDescriptor(eServiceId: String, catalogDescriptor: CatalogDescriptor): State =
    updateDescriptorLens(eServiceId, catalogDescriptor.id.toString, _ => catalogDescriptor)

  def updateDocument(
    eServiceId: String,
    descriptorId: String,
    documentId: String,
    modifiedDocument: CatalogDocument,
    serverUrls: List[String]
  ): State = {

    def updateDocument(docId: String)(descriptor: CatalogDescriptor): CatalogDescriptor = {
      val interface: Option[CatalogDocument] = descriptor.interface.filter(_.id.toString == docId)
      val document: Option[CatalogDocument]  = descriptor.docs.find(_.id.toString == docId)
      (interface, document) match {
        case (Some(_), None)    => descriptor.copy(interface = Some(modifiedDocument), serverUrls = serverUrls)
        case (None, Some(_))    =>
          descriptor.copy(docs = modifiedDocument +: descriptor.docs.filterNot(_.id.toString == docId))
        case (None, None)       => descriptor
        case (Some(_), Some(_)) =>
          descriptor.copy(
            interface = Some(modifiedDocument),
            docs = modifiedDocument +: descriptor.docs.filterNot(_.id.toString == docId),
            serverUrls = serverUrls
          )
      }
    }

    updateDescriptorLens(eServiceId, descriptorId, updateDocument(documentId))
  }

  def addItemDocument(
    eServiceId: String,
    descriptorId: String,
    openapiDoc: CatalogDocument,
    isInterface: Boolean,
    serverUrls: List[String]
  ): State = {
    def addDocument(doc: CatalogDocument, isInterface: Boolean)(descriptor: CatalogDescriptor) =
      if (isInterface) descriptor.copy(interface = Some(doc), serverUrls = serverUrls)
      else descriptor.copy(docs = doc +: descriptor.docs)

    updateDescriptorLens(eServiceId, descriptorId, addDocument(openapiDoc, isInterface))
  }

  def deleteDocument(eServiceId: String, descriptorId: String, documentId: String): State = {

    def deleteDescriptorDocument(docId: String)(descriptor: CatalogDescriptor): CatalogDescriptor = {
      val interface: Option[CatalogDocument] = descriptor.interface.filter(_.id.toString == docId)
      val document: Option[CatalogDocument]  = descriptor.docs.find(_.id.toString == docId)
      (interface, document) match {
        case (Some(_), None)    => descriptor.copy(interface = None, serverUrls = List.empty)
        case (None, Some(_))    => descriptor.copy(docs = descriptor.docs.filterNot(_.id.toString == docId))
        case (None, None)       => descriptor
        case (Some(_), Some(_)) =>
          descriptor.copy(
            interface = None,
            docs = descriptor.docs.filterNot(_.id.toString == docId),
            serverUrls = List.empty
          )
      }
    }

    updateDescriptorLens(eServiceId, descriptorId, deleteDescriptorDocument(documentId))
  }

  /*
    Inspect the state data in order to find the corresponding descriptor to update with a custom descriptor operation.
    Even though quick and dirty, it behaves like a lens.
    TODO for updates of complex states as this, evaluate the introduction of an optic library (e.g.: Monocle)
   */
  private def updateDescriptorLens(
    eServiceId: String,
    descriptorId: String,
    descriptorOperation: CatalogDescriptor => CatalogDescriptor
  ): State = {

    val newState = for {
      item       <- items.get(eServiceId)
      descriptor <- item.descriptors.find(_.id.toString == descriptorId)
      updatedItem  = item.copy(descriptors =
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
