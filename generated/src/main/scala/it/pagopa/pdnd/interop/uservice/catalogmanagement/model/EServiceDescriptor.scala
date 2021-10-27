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
 * @param status  for example: ''null''
*/
final case class EServiceDescriptor (
  id: UUID,
  version: String,
  description: Option[String],
  audience: Seq[String],
  voucherLifespan: Int,
  interface: Option[EServiceDoc],
  docs: Seq[EServiceDoc],
  status: String
)
