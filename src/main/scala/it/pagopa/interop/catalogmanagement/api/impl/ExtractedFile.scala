package it.pagopa.interop.catalogmanagement.api.impl

import akka.http.scaladsl.model.ContentType

import java.nio.file.Path

final case class ExtractedFile(contentType: ContentType, path: Path)
