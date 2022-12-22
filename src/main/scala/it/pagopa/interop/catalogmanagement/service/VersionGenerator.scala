package it.pagopa.interop.catalogmanagement.service

import it.pagopa.interop.catalogmanagement.error.CatalogManagementErrors.InvalidDescriptorVersion

trait VersionGenerator {

  def next(optVersionSeed: Option[String]): Either[InvalidDescriptorVersion, String] = {
    val currentVersion = optVersionSeed.getOrElse("0")
    currentVersion.toLongOption match {
      case Some(version) => Right[InvalidDescriptorVersion, String]((version + 1).toString)
      case None          => Left[InvalidDescriptorVersion, String](InvalidDescriptorVersion(currentVersion))
    }
  }
}

object VersionGenerator extends VersionGenerator
