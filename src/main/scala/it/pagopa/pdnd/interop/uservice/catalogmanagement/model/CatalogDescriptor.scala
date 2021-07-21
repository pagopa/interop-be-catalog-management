package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import java.util.UUID

final case class CatalogDescriptor(
                                    id: UUID,
                                    version: String,
                                    description: String,
                                    docs: Seq[CatalogDocument],
                                    voucherLifespan: Int,
                                    technology: String,
                                    status: CatalogDescriptorStatus
) extends Convertable[EServiceDescriptor] {
  def toApi: EServiceDescriptor = {
    EServiceDescriptor(
      id = id,
      version = version,
      description = description,
      docs = docs.map(_.toApi),
      voucherLifespan = Some(voucherLifespan),
      technology = technology,
      status = status.stringify
    )
  }

}
