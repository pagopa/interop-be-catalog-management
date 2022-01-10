package it.pagopa.pdnd.interop.uservice.catalogmanagement.error

import akka.http.scaladsl.model.ErrorInfo
import it.pagopa.pdnd.interop.commons.utils.errors.ComponentError
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.CatalogDocument

object CatalogManagementErrors {
  final case class ContentTypeParsingError(catalogDocument: CatalogDocument, errors: List[ErrorInfo])
      extends ComponentError(
        "0001",
        s"""Error trying to parse content type ${catalogDocument.contentType} for document ${catalogDocument.path},reason:\n$errors.map(_.formatPretty).mkString("\n")"""
      )

  final case class DocumentNotFoundError(catalogItemId: String, descriptorId: String, documentId: String)
      extends ComponentError(
        "0002",
        s"Document with id $documentId not found in item $catalogItemId / descriptor $descriptorId"
      )

  final case class EServiceDescriptorNotFoundError(eServiceId: String, descriptorId: String)
      extends ComponentError("0003", s"Descriptor with id $descriptorId of E-Service $eServiceId not found")

  final case class EServiceNotFoundError(eServiceId: String)
      extends ComponentError("0004", s"EService with id $eServiceId not found")

  final case class ValidationError(messages: List[String])
      extends ComponentError("0005", s"Validation errors: ${messages.mkString(",")}")

  final case class VersionError(version: String)
      extends ComponentError("0006", s"$version is not a valid descriptor version")

  final case class EServiceAlreadyExistingError(eServiceName: String)
      extends ComponentError("0007", s"Error while creating e-service $eServiceName because it already exist.")
  final case object EServiceError         extends ComponentError("0008", s"Error while creating e-service.")
  final case object EServiceNotFoundError extends ComponentError("0009", s"E-Service not found.")

  final case class DocumentCreationNotFound(kind: String, eServiceId: String, descriptor: String)
      extends ComponentError(
        "0010",
        s"Failure in creation of e-service document of kind $kind for e-service $eServiceId and descriptor $descriptor - not found"
      )

  final case class DocumentCreationBadRequest(kind: String, eServiceId: String, descriptor: String)
      extends ComponentError(
        "0011",
        s"Failure in creation of e-service document of kind $kind for e-service $eServiceId and descriptor $descriptor - bad request"
      )

  final case object EServiceRetrievalError extends ComponentError("0012", "Catalog Items retrieve error")

  final case class DocumentRetrievalBadRequest(documentId: String)
      extends ComponentError("0013", s"Failure while retrieving document $documentId - bad request")

  final case class DescriptorDeleteDraftBadRequest(eServiceId: String, descriptorId: String)
      extends ComponentError(
        "0014",
        s"Failure while deleting descriptor draft $descriptorId of e-service $eServiceId - bad request"
      )

  final case class DescriptorUpdateBadRequest(eServiceId: String, descriptorId: String)
      extends ComponentError(
        "0015",
        s"Failure while updating descriptor $descriptorId of e-service $eServiceId - bad request"
      )

  final case class DescriptorCreationBadRequest(eServiceId: String)
      extends ComponentError("0017", s"Failure while creation descriptor of e-service $eServiceId - bad request")

  final case class EServiceUpdateError(eServiceId: String)
      extends ComponentError("0016", s"Error while updating e-service $eServiceId.")

  final case class DeleteEServiceDocumentErrorBadRequest(documentId: String, descriptorId: String, eServiceId: String)
      extends ComponentError(
        "0018",
        s"Error on deletion of $documentId on descriptor $descriptorId on E-Service $eServiceId - bad request"
      )

  final case class DescriptorArchiveBadRequest(eServiceId: String, descriptorId: String)
      extends ComponentError(
        "0019",
        s"Failure while archiving descriptor $descriptorId of e-service $eServiceId - bad request"
      )

  final case class DescriptorDeprecationBadRequest(eServiceId: String, descriptorId: String)
      extends ComponentError(
        "0020",
        s"Failure while deprecating descriptor $descriptorId of e-service $eServiceId - bad request"
      )

  final case class DescriptorDeprecationError(eServiceId: String, descriptorId: String)
      extends ComponentError("0021", s"Failure while deprecating descriptor $descriptorId of e-service $eServiceId")

  final case class DescriptorSuspensionBadRequest(eServiceId: String, descriptorId: String)
      extends ComponentError(
        "0022",
        s"Failure while suspending descriptor $descriptorId of e-service $eServiceId - bad request"
      )

  final case class DescriptorSuspensionError(eServiceId: String, descriptorId: String)
      extends ComponentError("0023", s"Failure while suspending descriptor $descriptorId of e-service $eServiceId")

  final case class DescriptorDraftBadRequest(eServiceId: String, descriptorId: String)
      extends ComponentError(
        "0024",
        s"Failure while making draft descriptor $descriptorId of e-service $eServiceId - bad request"
      )

  final case class DescriptorDraftError(eServiceId: String, descriptorId: String)
      extends ComponentError("0025", s"Failure while making draft descriptor $descriptorId of e-service $eServiceId")

  final case class DescriptorPublishBadRequest(eServiceId: String, descriptorId: String)
      extends ComponentError(
        "0026",
        s"Failure while publishing descriptor $descriptorId of e-service $eServiceId - bad request"
      )

  final case class DescriptorPublishError(eServiceId: String, descriptorId: String)
      extends ComponentError("0027", s"Failure while publishing descriptor $descriptorId of e-service $eServiceId")

  final case class DocumentUpdateNotFound(documentId: String, descriptorId: String, eServiceId: String)
      extends ComponentError(
        "0028",
        s"Error on update of $documentId on descriptor $descriptorId on E-Service $eServiceId - not found"
      )

  final case class DocumentUpdateError(documentId: String, descriptorId: String, eServiceId: String)
      extends ComponentError(
        "0029",
        s"Error on update of $documentId on descriptor $descriptorId on E-Service $eServiceId"
      )

  final case class CloningEServiceBadRequest(eServiceId: String, descriptorId: String)
      extends ComponentError(
        "0030",
        s"Failure in cloning e-service $eServiceId and descriptor $descriptorId - bad request"
      )

  final case class CloningEServiceError(eServiceId: String, descriptorId: String)
      extends ComponentError("0031", s"Failure in cloning e-service $eServiceId and descriptor $descriptorId")

  final case class DeleteEServiceBadRequest(eServiceId: String)
      extends ComponentError("0032", s"Failure in deleting e-service $eServiceId - bad request")

  final case class DeleteEServiceError(eServiceId: String)
      extends ComponentError("0033", s"Failure in deleting e-service $eServiceId")
}
