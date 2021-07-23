package it.pagopa.pdnd.interop.uservice.catalogmanagement.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common.Digester
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.CatalogDocument
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.FileManager

import java.io.File
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

@SuppressWarnings(Array("org.wartremover.warts.ToString"))
class FileManagerImpl extends FileManager {

  val currentPath: Path = Paths.get(System.getProperty("user.dir"))

  def store(id: UUID, eServiceId: String, descriptorId: String, fileParts: (FileInfo, File)): Future[CatalogDocument] =
    Future.fromTry {
      Try {
        val destPath = createPath(eServiceId, descriptorId, id.toString, fileParts._1)

        val path = moveRenameFile(fileParts._2.getPath, destPath).toString
        CatalogDocument(
          id = id,
          name = fileParts._1.getFileName,
          contentType = fileParts._1.getContentType.toString(),
          path = path,
          checksum = Digester.createHash(fileParts._2),
          uploadDate = OffsetDateTime.now()
        )

      }
    }

  def get(id: UUID, producerId: String): File = ???

  private def createPath(producerId: String, descriptorId: String, id: String, fileInfo: FileInfo): String = {

    val docsPath: Path = Paths.get(
      currentPath.toString,
      s"target/pdnd-interop/docs/$producerId/$descriptorId/$id${fileInfo.getFieldName}/${fileInfo.getContentType.toString}"
    )
    val pathCreated: Path = Files.createDirectories(docsPath)

    Paths.get(pathCreated.toString, s"${fileInfo.getFileName}").toString

  }

  private def moveRenameFile(source: String, destination: String): Path = {
    Files.move(Paths.get(source), Paths.get(destination), StandardCopyOption.REPLACE_EXISTING)

  }
}
