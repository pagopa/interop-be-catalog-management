package it.pagopa.pdnd.interop.uservice.catalogmanagement.model


/**
 * @param description  for example: ''null''
 * @param state  for example: ''null''
 * @param audience  for example: ''null''
 * @param voucherLifespan  for example: ''null''
*/
final case class UpdateEServiceDescriptorSeed (
  description: Option[String] = None,
  state: EServiceDescriptorState,
  audience: Seq[String],
  voucherLifespan: Int
)


