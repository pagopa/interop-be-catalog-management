package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.EService

sealed trait Event extends Persistable

final case class EServiceAdded(eService: EService) extends Event
