package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

sealed trait Event extends Persistable

final case class CatalogItemAdded(catalogItem: CatalogItem) extends Event
