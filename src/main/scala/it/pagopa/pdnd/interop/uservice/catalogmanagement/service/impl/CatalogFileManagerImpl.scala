package it.pagopa.pdnd.interop.uservice.catalogmanagement.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.pdnd.interop.commons.files.service.{FileManager, StorageFilePath}
import it.pagopa.pdnd.interop.commons.utils.Digester
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common.system.ApplicationConfiguration
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.CatalogDocument
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.CatalogFileManager

import java.io.File
import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

/** Decorates the common fileManager adding some modeling features as needed by Catalog Management
  * @param fileManager
  */
final class CatalogFileManagerImpl(val fileManager: FileManager) extends CatalogFileManager {

  override def store(id: UUID, description: String, fileParts: (FileInfo, File))(implicit
    ec: ExecutionContext
  ): Future[CatalogDocument] = {
    fileManager
      .store(ApplicationConfiguration.storageContainer)(id, fileParts)
      .map(filePath =>
        CatalogDocument(
          id = id,
          name = fileParts._1.getFileName,
          contentType = fileParts._1.getContentType.toString(),
          description = description,
          path = filePath,
          checksum = Digester.createMD5Hash(fileParts._2),
          uploadDate = OffsetDateTime.now()
        )
      )
  }

  override def copy(
    filePathToCopy: String
  )(documentId: UUID, description: String, checksum: String, contentType: String, fileName: String)(implicit
    ec: ExecutionContext
  ): Future[CatalogDocument] = {
    fileManager
      .copy(ApplicationConfiguration.storageContainer)(filePathToCopy, documentId, contentType, fileName)
      .map(copiedPath =>
        CatalogDocument(
          id = documentId,
          name = fileName,
          contentType = contentType,
          description = description,
          path = copiedPath,
          checksum = checksum,
          uploadDate = OffsetDateTime.now()
        )
      )
  }

  override def delete(filePath: StorageFilePath): Future[Boolean] =
    fileManager.delete(ApplicationConfiguration.storageContainer)(filePath)
}
