package it.pagopa.pdnd.interop.uservice.catalogmanagement.error

final case class ValidationError(messages: List[String])
    extends Throwable(s"Validation errors: ${messages.mkString(",")}")
