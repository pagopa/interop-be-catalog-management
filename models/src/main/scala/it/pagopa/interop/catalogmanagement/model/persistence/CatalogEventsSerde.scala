package it.pagopa.interop.catalogmanagement.model.persistence

import it.pagopa.interop.catalogmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import spray.json._

object CatalogEventsSerde {

  val projectableCatalogToJson: PartialFunction[ProjectableEvent, JsValue] = { case event: Event =>
    catalogToJson(event)
  }

  def catalogToJson(event: Event): JsValue = event match {
    case x: CatalogItemAdded                         => x.toJson
    case x: ClonedCatalogItemAdded                   => x.toJson
    case x: CatalogItemUpdated                       => x.toJson
    case x: CatalogItemWithDescriptorsDeleted        => x.toJson
    case x: CatalogItemDocumentUpdated               => x.toJson
    case x: CatalogItemDeleted                       => x.toJson
    case x: CatalogItemDocumentAdded                 => x.toJson
    case x: CatalogItemDocumentDeleted               => x.toJson
    case x: CatalogItemDescriptorAdded               => x.toJson
    case x: CatalogItemDescriptorUpdated             => x.toJson
    case x: MovedAttributesFromEserviceToDescriptors => x.toJson
    case x: CatalogItemRiskAnalysisAdded             => x.toJson
    case x: CatalogItemRiskAnalysisUpdated           => x.toJson
    case x: CatalogItemRiskAnalysisDeleted           => x.toJson
  }

  val jsonToCatalog: PartialFunction[String, JsValue => ProjectableEvent] = {
    case `catalogItemAdded`                        => _.convertTo[CatalogItemAdded]
    case `clonedCatalogItemAdded`                  => _.convertTo[ClonedCatalogItemAdded]
    case `catalogItemUpdated`                      => _.convertTo[CatalogItemUpdated]
    case `catalogItemWithDescriptorsDeleted`       => _.convertTo[CatalogItemWithDescriptorsDeleted]
    case `catalogItemDocumentUpdated`              => _.convertTo[CatalogItemDocumentUpdated]
    case `catalogItemDeleted`                      => _.convertTo[CatalogItemDeleted]
    case `catalogItemDocumentAdded`                => _.convertTo[CatalogItemDocumentAdded]
    case `catalogItemDocumentDeleted`              => _.convertTo[CatalogItemDocumentDeleted]
    case `catalogItemDescriptorAdded`              => _.convertTo[CatalogItemDescriptorAdded]
    case `catalogItemDescriptorUpdated`            => _.convertTo[CatalogItemDescriptorUpdated]
    case `moveAttributesFromEserviceToDescriptors` => _.convertTo[MovedAttributesFromEserviceToDescriptors]
    case `catalogItemRiskAnalysisAdded`            => _.convertTo[CatalogItemRiskAnalysisAdded]
    case `catalogItemRiskAnalysisUpdated`          => _.convertTo[CatalogItemRiskAnalysisUpdated]
    case `catalogItemRiskAnalysisDeleted`          => _.convertTo[CatalogItemRiskAnalysisDeleted]
  }

  def getKind(e: Event): String = e match {
    case _: CatalogItemAdded                         => catalogItemAdded
    case _: ClonedCatalogItemAdded                   => clonedCatalogItemAdded
    case _: CatalogItemUpdated                       => catalogItemUpdated
    case _: CatalogItemWithDescriptorsDeleted        => catalogItemWithDescriptorsDeleted
    case _: CatalogItemDocumentUpdated               => catalogItemDocumentUpdated
    case _: CatalogItemDeleted                       => catalogItemDeleted
    case _: CatalogItemDocumentAdded                 => catalogItemDocumentAdded
    case _: CatalogItemDocumentDeleted               => catalogItemDocumentDeleted
    case _: CatalogItemDescriptorAdded               => catalogItemDescriptorAdded
    case _: CatalogItemDescriptorUpdated             => catalogItemDescriptorUpdated
    case _: MovedAttributesFromEserviceToDescriptors => moveAttributesFromEserviceToDescriptors
    case _: CatalogItemRiskAnalysisAdded             => catalogItemRiskAnalysisAdded
    case _: CatalogItemRiskAnalysisUpdated           => catalogItemRiskAnalysisUpdated
    case _: CatalogItemRiskAnalysisDeleted           => catalogItemRiskAnalysisDeleted
  }

  private val catalogItemAdded                        = "catalog_item_added"
  private val clonedCatalogItemAdded                  = "cloned_catalog_item_added"
  private val catalogItemUpdated                      = "catalog_item_updated"
  private val catalogItemWithDescriptorsDeleted       = "catalog_item_with_descriptors_deleted"
  private val catalogItemDocumentUpdated              = "catalog_item_document_updated"
  private val catalogItemDeleted                      = "catalog_item_deleted"
  private val catalogItemDocumentAdded                = "catalog_item_document_added"
  private val catalogItemDocumentDeleted              = "catalog_item_document_deleted"
  private val catalogItemDescriptorAdded              = "catalog_item_descriptor_added"
  private val catalogItemDescriptorUpdated            = "catalog_item_descriptor_updated"
  private val moveAttributesFromEserviceToDescriptors = "moved_attributes_from_eservice_to_descriptors"
  private val catalogItemRiskAnalysisAdded            = "catalog_item_risk_analysis_added"
  private val catalogItemRiskAnalysisUpdated          = "catalog_item_risk_analysis_updated"
  private val catalogItemRiskAnalysisDeleted          = "catalog_item_risk_analysis_deleted"
}
