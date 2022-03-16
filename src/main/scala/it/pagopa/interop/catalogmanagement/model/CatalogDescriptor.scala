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
  throughput: Int
) extends Convertable[EServiceDescriptor] {
  def toApi: EServiceDescriptor = {
    EServiceDescriptor(
      id = id,
      version = version,
      description = description,
      interface = interface.map(_.toApi),
      docs = docs.map(_.toApi),
      state = state.toApi,
      audience = audience,
      voucherLifespan = voucherLifespan,
      dailyCallsPerConsumer = dailyCallsPerConsumer,
      throughput = throughput
    )
  }

  def isDraft: Boolean = {
    state == Draft
  }
}
