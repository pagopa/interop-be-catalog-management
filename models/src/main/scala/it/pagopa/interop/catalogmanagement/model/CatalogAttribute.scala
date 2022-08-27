package it.pagopa.interop.catalogmanagement.model

sealed trait CatalogAttribute
final case class SingleAttribute(id: CatalogAttributeValue)      extends CatalogAttribute
final case class GroupAttribute(ids: Seq[CatalogAttributeValue]) extends CatalogAttribute

object CatalogAttribute
