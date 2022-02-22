package it.pagopa.interop.catalogmanagement.model

trait Convertable[A] {
  def toApi: A
}
