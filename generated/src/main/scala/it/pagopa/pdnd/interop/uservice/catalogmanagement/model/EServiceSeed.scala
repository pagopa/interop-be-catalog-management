package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import java.util.UUID

/**
 * @param producerId  for example: ''null''
 * @param name  for example: ''null''
 * @param description  for example: ''null''
 * @param technology  for example: ''null''
 * @param attributes  for example: ''null''
*/
final case class EServiceSeed (
  producerId: UUID,
  name: String,
  description: String,
  technology: EServiceTechnologyEnum,
  attributes: Attributes
)


