package it.pagopa.pdnd.interop.uservice.catalogmanagement.service

import it.pagopa.pdnd.interop.uservice.catalogmanagement.error.VersionError

trait VersionGenerator {
  @SuppressWarnings(Array("org.wartremover.warts.StringPlusAny"))
  def next(optVersionSeed: Option[String]): Either[VersionError, String] = {
    val currentVersion = optVersionSeed.getOrElse("0")
    currentVersion.toLongOption match {
      case Some(version) => Right[VersionError, String]((version + 1).toString)
      case None          => Left[VersionError, String](VersionError(currentVersion))
    }
  }
}

/** Selfless trait implementation
  */
object VersionGenerator extends VersionGenerator
