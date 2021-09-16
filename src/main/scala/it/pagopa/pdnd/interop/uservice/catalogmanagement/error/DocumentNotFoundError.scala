package it.pagopa.pdnd.interop.uservice.catalogmanagement.error

final case class DocumentNotFoundError(catalogItemId: String, descriptorId: String, documentId: String)
    extends Throwable(s"Document with id $documentId not found in item $catalogItemId / descriptor $descriptorId")
