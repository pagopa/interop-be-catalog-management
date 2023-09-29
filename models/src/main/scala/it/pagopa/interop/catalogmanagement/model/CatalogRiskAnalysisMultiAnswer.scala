package it.pagopa.interop.catalogmanagement.model

import java.util.UUID

final case class CatalogRiskAnalysisMultiAnswer(id: UUID, key: String, values: Seq[String])
