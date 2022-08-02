package it.pagopa.interop.catalogmanagement.model

import java.time.OffsetDateTime
import java.util.UUID

final case class CatalogDocument(
  id: UUID,
  name: String,
  contentType: String,
  prettyName: String,
  path: String,
  checksum: String,
  uploadDate: OffsetDateTime
)
