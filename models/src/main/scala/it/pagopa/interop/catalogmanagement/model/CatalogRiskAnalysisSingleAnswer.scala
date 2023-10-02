package it.pagopa.interop.catalogmanagement.model

import java.util.UUID

final case class CatalogRiskAnalysisSingleAnswer(id: UUID, key: String, value: Option[String] = None)
