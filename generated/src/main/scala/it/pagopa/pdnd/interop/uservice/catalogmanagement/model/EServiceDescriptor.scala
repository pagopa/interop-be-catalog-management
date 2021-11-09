package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import java.util.UUID

/**
 * @param id  for example: ''null''
 * @param version  for example: ''null''
 * @param description  for example: ''null''
 * @param audience  for example: ''null''
 * @param voucherLifespan  for example: ''null''
 * @param interface  for example: ''null''
 * @param docs  for example: ''null''
 * @param state  for example: ''null''
*/
final case class EServiceDescriptor (
  id: UUID,
  version: String,
  description: Option[String] = None,
  audience: Seq[String],
  voucherLifespan: Int,
  interface: Option[EServiceDoc] = None,
  docs: Seq[EServiceDoc],
  state: EServiceDescriptorState
)


