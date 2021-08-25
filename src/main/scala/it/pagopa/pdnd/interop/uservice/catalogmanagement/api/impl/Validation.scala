package it.pagopa.pdnd.interop.uservice.catalogmanagement.api.impl

import cats.data.ValidatedNel
import cats.implicits.catsSyntaxValidatedId
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.CatalogDescriptorStatus

trait Validation {

  def validateDescriptorStatus(status: String): ValidatedNel[String, CatalogDescriptorStatus] = {
    CatalogDescriptorStatus.fromText(status) match {
      case Right(s) => s.validNel[String]
      case Left(ex) => ex.getMessage.invalidNel[CatalogDescriptorStatus]
    }
  }
}
