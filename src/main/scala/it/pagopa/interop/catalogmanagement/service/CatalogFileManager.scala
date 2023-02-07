package it.pagopa.interop.catalogmanagement.service

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.interop.catalogmanagement.model.CatalogDocument
import it.pagopa.interop.commons.files.service.FileManager
import java.io.File
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait CatalogFileManager {
  val fileManager: FileManager

  def store(id: UUID, prettyName: String, fileParts: (FileInfo, File))(implicit
    ec: ExecutionContext
  ): Future[CatalogDocument]

  def copy(
    filePathToCopy: String
  )(documentId: UUID, prettyName: String, checksum: String, contentType: String, fileName: String)(implicit
    ec: ExecutionContext
  ): Future[CatalogDocument]

  def delete(filePath: String): Future[Boolean]
}
