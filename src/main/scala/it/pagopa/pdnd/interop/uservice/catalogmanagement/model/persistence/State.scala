package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.EService

final case class State(eServices: Map[String, EService]) extends Persistable {

  def add(eService: EService): State =
    copy(eServices = eServices + (eService.id.toString -> eService))

}

object State {
  val empty: State = State(eServices = Map.empty[String, EService])
}
