package it.pagopa.pdnd.interop.uservice.catalogmanagement.service

import java.util.UUID

trait UUIDSupplier {
  def get: UUID
}
