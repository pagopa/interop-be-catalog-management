package it.pagopa.interop.catalogmanagement.model

import java.time.OffsetDateTime
import java.util.UUID

final case class CatalogRiskAnalysis(
  id: UUID,
  name: String,
  riskAnalysisForm: CatalogRiskAnalysisForm,
  createdAt: OffsetDateTime
)
