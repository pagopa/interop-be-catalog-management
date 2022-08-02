package it.pagopa.interop.catalogmanagement.model

import java.util.UUID

final case class CatalogItem(
  id: UUID,
  producerId: UUID,
  name: String,
  description: String,
  technology: CatalogItemTechnology,
  attributes: CatalogAttributes,
  descriptors: Seq[CatalogDescriptor]
)

object CatalogItem
