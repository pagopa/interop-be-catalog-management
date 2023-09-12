package it.pagopa.interop.catalogmanagement.error

import it.pagopa.interop.commons.utils.errors.ComponentError

object CatalogManagementErrors {
  final case class DocumentNotFound(catalogItemId: String, descriptorId: String, documentId: String)
      extends ComponentError(
        "0001",
        s"Document with id $documentId not found in EService $catalogItemId / Descriptor $descriptorId"
      )

  final case class EServiceDescriptorNotFound(eServiceId: String, descriptorId: String)
      extends ComponentError("0002", s"Descriptor with id $descriptorId of EService $eServiceId not found")

  final case class EServiceNotFound(eServiceId: String)
      extends ComponentError("0003", s"EService $eServiceId not found")

  final case class InvalidDescriptorVersion(version: String)
      extends ComponentError("0004", s"$version is not a valid descriptor version")

  final case class InvalidInterfaceFileDetected(eServiceId: String, contentType: String, technology: String)
      extends ComponentError(
        "0005",
        s"The interface file for EService $eServiceId has a contentType $contentType not admitted for $technology technology"
      )

  final case class DocumentAlreadyUploaded(eServiceId: String, fileName: String)
      extends ComponentError("0006", s"File $fileName already uploaded for EService $eServiceId")

  final case class DescriptorNotInDraft(eServiceId: String, descriptorId: String)
      extends ComponentError("0008", s"Descriptor $descriptorId of EService $eServiceId is not Draft")

  final case class EServiceWithDescriptorsNotDeletable(eServiceId: String)
      extends ComponentError("0009", s"EService $eServiceId contains descriptors and cannot be deleted")

  case object ElementNotFoundAfterUpdate extends ComponentError("0010", s"Element cannot be found after update")

}
