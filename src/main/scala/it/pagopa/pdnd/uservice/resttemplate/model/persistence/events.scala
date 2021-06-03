package it.pagopa.pdnd.uservice.resttemplate.model.persistence

import it.pagopa.pdnd.interopuservice.agreementmanagement.model.EService

sealed trait Event extends Persistable

final case class EServiceAdded(eService: EService) extends Event
