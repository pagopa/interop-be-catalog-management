package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import java.util.UUID

@SuppressWarnings(Array("org.wartremover.warts.Equals"))
final case class CatalogDescriptor(
  id: UUID,
  version: String,
  description: Option[String],
  interface: Option[CatalogDocument],
  docs: Seq[CatalogDocument],
  status: CatalogDescriptorStatus
) extends Convertable[EServiceDescriptor] {
  def toApi: EServiceDescriptor = {
    EServiceDescriptor(
      id = id,
      version = version,
      description = description,
      interface = interface.map(_.toApi),
      docs = docs.map(_.toApi),
      status = status.stringify
    )
  }

  def isPublishable: Boolean = {
    interface.isDefined && status == Draft
  }

  def isDraft: Boolean = {
    status == Draft
  }

  def publish: CatalogDescriptor = {
    copy(status = Published)
  }

}
