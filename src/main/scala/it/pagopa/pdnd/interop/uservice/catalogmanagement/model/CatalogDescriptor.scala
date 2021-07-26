package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import java.util.UUID

@SuppressWarnings(Array("org.wartremover.warts.Equals"))
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

  def isPublishable: Boolean = {
    docs.exists(doc => doc.interface) && status == Draft
  }

  def publish: CatalogDescriptor = {
    copy(status = Published)
  }

}
