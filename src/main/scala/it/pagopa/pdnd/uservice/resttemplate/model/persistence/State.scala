package it.pagopa.pdnd.uservice.resttemplate.model.persistence

import it.pagopa.pdnd.interopuservice.agreementmanagement.model.EService

final case class State(eServices: Map[String, EService]) extends Persistable {

  def add(eService: EService): State =
    eService.id.fold(this)(id => copy(eServices = eServices + (id.toString -> eService)))

}

object State {
  val empty: State = State(eServices = Map.empty[String, EService])
}
