package it.pagopa.pdnd.interop.uservice.catalogmanagement.model


/**
 * @param single  for example: ''null''
 * @param group  for example: ''null''
*/
final case class Attribute (
  single: Option[AttributeValue] = None,
  group: Option[Seq[AttributeValue]] = None
)


