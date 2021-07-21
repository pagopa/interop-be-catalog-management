package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.CatalogItem

final case class State(items: Map[String, CatalogItem]) extends Persistable {

  def add(catalogItem: CatalogItem): State =
    copy(items = items + (catalogItem.id.toString -> catalogItem))

}

object State {
  val empty: State = State(items = Map.empty[String, CatalogItem])
}
