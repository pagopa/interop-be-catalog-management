package it.pagopa.pdnd.interop.uservice.catalogmanagement.model


/**
 * @param name  for example: ''null''
 * @param description  for example: ''null''
 * @param technology  for example: ''null''
 * @param attributes  for example: ''null''
*/
final case class UpdateEServiceSeed (
  name: String,
  description: String,
  technology: String,
  attributes: Attributes
)

