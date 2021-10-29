package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import java.util.UUID

/**
 * @param id  for example: ''null''
 * @param name  for example: ''null''
 * @param contentType  for example: ''null''
 * @param description  for example: ''null''
 * @param path  for example: ''null''
*/
final case class EServiceDoc (
  id: UUID,
  name: String,
  contentType: String,
  description: String,
  path: String
)


