package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import java.util.UUID

final case class CatalogDescriptor(
  id: UUID,
  version: String,
  description: Option[String],
  docs: Seq[CatalogDocument],
  status: CatalogDescriptorStatus
) extends Convertable[EServiceDescriptor] {
  def toApi: EServiceDescriptor = {
    EServiceDescriptor(
      id = id,
      version = version,
      description = description,
      docs = docs.map(_.toApi),
      status = status.stringify
    )
  }

  def publish: CatalogDescriptor = {
    copy(status = Published)
  }

}
