package it.pagopa.interop.catalogmanagement.model

import java.time.OffsetDateTime
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
  agreementApprovalPolicy: Option[PersistentAgreementApprovalPolicy],
  createdAt: OffsetDateTime,
  serverUrls: List[String],
  publishedAt: Option[OffsetDateTime],
  suspendedAt: Option[OffsetDateTime],
  deprecatedAt: Option[OffsetDateTime],
  archivedAt: Option[OffsetDateTime]
)
