package it.pagopa.interop.catalogmanagement.model

sealed trait CatalogItemMode
case object Receive extends CatalogItemMode
case object Deliver extends CatalogItemMode

object CatalogItemMode {
  val default = Deliver
}
