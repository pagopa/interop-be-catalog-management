package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.CatalogItem

sealed trait Event extends Persistable

final case class CatalogItemAdded(catalogItem: CatalogItem) extends Event
