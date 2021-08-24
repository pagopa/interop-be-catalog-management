package it.pagopa.pdnd.interop.uservice.catalogmanagement.error

final case class EServiceDescriptorNotFoundError(eServiceId: String, descriptorId: String)
    extends Throwable(s"Descriptor with id $descriptorId of E-Service $eServiceId not found")
