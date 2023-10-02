package it.pagopa.interop.catalogmanagement.model

import java.util.UUID

final case class CatalogRiskAnalysisForm(
  id: UUID,
  version: String,
  singleAnswers: Seq[CatalogRiskAnalysisSingleAnswer],
  multiAnswers: Seq[CatalogRiskAnalysisMultiAnswer]
)
