package it.pagopa.pdnd.interop.uservice.catalogmanagement.error

final case class EServiceNotFoundError(eServiceId: String) extends Throwable(s"EService with id $eServiceId not found")
