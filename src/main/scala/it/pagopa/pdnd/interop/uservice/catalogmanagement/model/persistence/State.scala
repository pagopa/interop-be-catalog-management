package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.CatalogItem

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

}

object State {
  val empty: State = State(items = Map.empty[String, CatalogItem])
}
