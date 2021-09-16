package it.pagopa.pdnd.interop.uservice.catalogmanagement.error

import akka.http.scaladsl.model.ErrorInfo
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.CatalogDocument

final case class ContentTypeParsingError(catalogDocument: CatalogDocument, errors: List[ErrorInfo]) extends Throwable {
  override def getMessage: String = {
    val errorTxt: String = errors.map(_.formatPretty).mkString("\n")
    s"Error trying to parse content type ${catalogDocument.contentType} for document ${catalogDocument.path},reason:\n$errorTxt"
  }
}
