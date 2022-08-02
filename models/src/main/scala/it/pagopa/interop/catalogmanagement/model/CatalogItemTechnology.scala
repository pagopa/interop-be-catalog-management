package it.pagopa.interop.catalogmanagement.model

sealed trait CatalogItemTechnology
case object Rest extends CatalogItemTechnology
case object Soap extends CatalogItemTechnology
object CatalogItemTechnology
