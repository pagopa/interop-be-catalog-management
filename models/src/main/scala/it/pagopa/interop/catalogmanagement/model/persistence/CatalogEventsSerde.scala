package it.pagopa.interop.catalogmanagement.model.persistence

import it.pagopa.interop.catalogmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import spray.json._

object CatalogEventsSerde {

  val projectableCatalogToJson: PartialFunction[ProjectableEvent, JsValue] = { case event: Event =>
    catalogToJson(event)
  }

  def catalogToJson(event: Event): JsValue = event match {
    case x: CatalogItemAdded                  => x.toJson
    case x: ClonedCatalogItemAdded            => x.toJson
    case x: CatalogItemUpdated                => x.toJson
    case x: CatalogItemWithDescriptorsDeleted => x.toJson
    case x: CatalogItemDocumentUpdated        => x.toJson
    case x: CatalogItemDeleted                => x.toJson
    case x: CatalogItemDocumentAdded          => x.toJson
    case x: CatalogItemDocumentDeleted        => x.toJson
    case x: CatalogItemDescriptorAdded        => x.toJson
    case x: CatalogItemDescriptorUpdated      => x.toJson
  }

  val jsonToCatalog: PartialFunction[String, JsValue => ProjectableEvent] = {
    case `catalogItemAdded`                  => _.convertTo[CatalogItemAdded]
    case `clonedCatalogItemAdded`            => _.convertTo[ClonedCatalogItemAdded]
    case `catalogItemUpdated`                => _.convertTo[CatalogItemUpdated]
    case `catalogItemWithDescriptorsDeleted` => _.convertTo[CatalogItemWithDescriptorsDeleted]
    case `catalogItemDocumentUpdated`        => _.convertTo[CatalogItemDocumentUpdated]
    case `catalogItemDeleted`                => _.convertTo[CatalogItemDeleted]
    case `catalogItemDocumentAdded`          => _.convertTo[CatalogItemDocumentAdded]
    case `catalogItemDocumentDeleted`        => _.convertTo[CatalogItemDocumentDeleted]
    case `catalogItemDescriptorAdded`        => _.convertTo[CatalogItemDescriptorAdded]
    case `catalogItemDescriptorUpdated`      => _.convertTo[CatalogItemDescriptorUpdated]
  }

  def getKind(e: Event): String = e match {
    case CatalogItemAdded(_)                     => catalogItemAdded
    case ClonedCatalogItemAdded(_)               => clonedCatalogItemAdded
    case CatalogItemUpdated(_)                   => catalogItemUpdated
    case CatalogItemWithDescriptorsDeleted(_, _) => catalogItemWithDescriptorsDeleted
    case CatalogItemDocumentUpdated(_, _, _, _)  => catalogItemDocumentUpdated
    case CatalogItemDeleted(_)                   => catalogItemDeleted
    case CatalogItemDocumentAdded(_, _, _, _)    => catalogItemDocumentAdded
    case CatalogItemDocumentDeleted(_, _, _)     => catalogItemDocumentDeleted
    case CatalogItemDescriptorAdded(_, _)        => catalogItemDescriptorAdded
    case CatalogItemDescriptorUpdated(_, _)      => catalogItemDescriptorUpdated
  }

  private val catalogItemAdded                  = "catalog_item_added"
  private val clonedCatalogItemAdded            = "cloned_catalog_item_added"
  private val catalogItemUpdated                = "catalog_item_updated"
  private val catalogItemWithDescriptorsDeleted = "catalog_item_with_descriptors_deleted"
  private val catalogItemDocumentUpdated        = "catalog_item_document_updated"
  private val catalogItemDeleted                = "catalog_item_deleted"
  private val catalogItemDocumentAdded          = "catalog_item_document_added"
  private val catalogItemDocumentDeleted        = "catalog_item_document_deleted"
  private val catalogItemDescriptorAdded        = "catalog_item_descriptor_added"
  private val catalogItemDescriptorUpdated      = "catalog_item_descriptor_updated"

}
