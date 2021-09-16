package it.pagopa.pdnd.interop.uservice.catalogmanagement.service

trait VersionGenerator {
  @SuppressWarnings(Array("org.wartremover.warts.StringPlusAny"))
  def generate(optVersion: Option[String]): String = {
    optVersion.flatMap(currentVersion => currentVersion.toLongOption).fold("1") { version =>
      (version + 1).toString
    }
  }
}

/** Selfless trait implementation
  */
object VersionGenerator extends VersionGenerator
