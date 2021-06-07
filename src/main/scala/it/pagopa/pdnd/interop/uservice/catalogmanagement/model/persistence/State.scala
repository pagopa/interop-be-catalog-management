package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.EService

final case class State(eServices: Map[String, EService]) extends Persistable {

  def add(eService: EService): State = {
    val out = eService.id.fold(this)(id => this.copy(eServices = eServices + (id.toString -> eService)))
    println(out.toString)
    out
  }

}

object State {
  val empty: State = State(eServices = Map.empty[String, EService])
}
