package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import java.util.UUID

final case class CatalogDescriptor(
  id: UUID,
  version: String,
  description: Option[String],
  interface: Option[CatalogDocument],
  docs: Seq[CatalogDocument],
  status: CatalogDescriptorStatus,
  audience: Seq[String],
  voucherLifespan: Int
) extends Convertable[EServiceDescriptor] {
  def toApi: EServiceDescriptor = {
    EServiceDescriptor(
      id = id,
      version = version,
      description = description,
      interface = interface.map(_.toApi),
      docs = docs.map(_.toApi),
      status = status.stringify,
      audience = audience,
      voucherLifespan = voucherLifespan
    )
  }

  def isDraft: Boolean = {
    status == Draft
  }
}
