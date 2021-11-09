package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import java.util.UUID

/**
 * @param id  for example: ''null''
 * @param producerId  for example: ''null''
 * @param name  for example: ''null''
 * @param description  for example: ''null''
 * @param technology  for example: ''null''
 * @param attributes  for example: ''null''
 * @param descriptors  for example: ''null''
*/
final case class EService (
  id: UUID,
  producerId: UUID,
  name: String,
  description: String,
  technology: EServiceTechnology,
  attributes: Attributes,
  descriptors: Seq[EServiceDescriptor]
)


