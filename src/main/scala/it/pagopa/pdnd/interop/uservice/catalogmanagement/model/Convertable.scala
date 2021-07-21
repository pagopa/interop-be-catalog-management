package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

trait Convertable[A] {
  def toApi: A
}
