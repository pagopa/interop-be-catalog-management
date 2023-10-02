package it.pagopa.interop.catalogmanagement.model

import java.time.OffsetDateTime
import java.util.UUID

final case class CatalogItem(
  id: UUID,
  producerId: UUID,
  name: String,
  description: String,
  technology: CatalogItemTechnology,
  attributes: Option[CatalogAttributes],
  descriptors: Seq[CatalogDescriptor],
  riskAnalysis: Seq[CatalogRiskAnalysis],
  mode: CatalogItemMode,
  createdAt: OffsetDateTime
)

object CatalogItem
