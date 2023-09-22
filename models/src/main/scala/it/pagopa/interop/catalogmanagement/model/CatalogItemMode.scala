package it.pagopa.interop.catalogmanagement.model

sealed trait CatalogItemMode
case object RECEIVE extends CatalogItemMode
case object DELIVER extends CatalogItemMode

object CatalogItemMode {
  val default = DELIVER
}
