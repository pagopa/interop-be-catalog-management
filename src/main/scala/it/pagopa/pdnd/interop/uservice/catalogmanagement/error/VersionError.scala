package it.pagopa.pdnd.interop.uservice.catalogmanagement.error

final case class VersionError(version: String) extends Throwable(s"$version is not a valid descriptor version")
