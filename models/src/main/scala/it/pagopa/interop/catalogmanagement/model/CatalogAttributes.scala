package it.pagopa.interop.catalogmanagement.model

final case class CatalogAttributes(
  certified: Seq[CatalogAttribute],
  declared: Seq[CatalogAttribute],
  verified: Seq[CatalogAttribute]
)

object CatalogAttributes
