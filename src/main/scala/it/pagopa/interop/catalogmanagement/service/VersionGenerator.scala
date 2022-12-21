package it.pagopa.interop.catalogmanagement.service

import it.pagopa.interop.catalogmanagement.error.CatalogManagementErrors.VersionError

trait VersionGenerator {

  def next(optVersionSeed: Option[String]): Either[VersionError, String] = {
    val currentVersion = optVersionSeed.getOrElse("0")
    currentVersion.toLongOption match {
      case Some(version) => Right[VersionError, String]((version + 1).toString)
      case None          => Left[VersionError, String](VersionError(currentVersion))
    }
  }
}

object VersionGenerator extends VersionGenerator
