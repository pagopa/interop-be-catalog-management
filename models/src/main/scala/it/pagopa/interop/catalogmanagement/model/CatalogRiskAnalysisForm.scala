package it.pagopa.interop.catalogmanagement.model

final case class CatalogRiskAnalysisForm(
  version: String,
  singleAnswers: Seq[CatalogRiskAnalysisSingleAnswer],
  multiAnswers: Seq[CatalogRiskAnalysisMultiAnswer]
)
