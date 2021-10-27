package it.pagopa.pdnd.interop.uservice.catalogmanagement.model


/**
 * @param certified  for example: ''null''
 * @param declared  for example: ''null''
 * @param verified  for example: ''null''
*/
final case class Attributes (
  certified: Seq[Attribute],
  declared: Seq[Attribute],
  verified: Seq[Attribute]
)

