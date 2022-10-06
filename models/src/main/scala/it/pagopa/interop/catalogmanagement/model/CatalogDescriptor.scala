package it.pagopa.interop.catalogmanagement.model

import java.util.UUID

final case class CatalogDescriptor(
  id: UUID,
  version: String,
  description: Option[String],
  interface: Option[CatalogDocument],
  docs: Seq[CatalogDocument],
  state: CatalogDescriptorState,
  audience: Seq[String],
  voucherLifespan: Int,
  dailyCallsPerConsumer: Int,
  dailyCallsTotal: Int,
  requireAgreementManualApproval: Option[Boolean]
)
