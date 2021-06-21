package it.pagopa.pdnd.interop.uservice.catalogmanagement.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.CatalogItemDocument
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.FileManager

import java.io.File
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.util.UUID
import scala.util.Try

@SuppressWarnings(Array("org.wartremover.warts.ToString"))
class FileManagerImpl extends FileManager {

  val currentPath: Path = Paths.get(System.getProperty("user.dir"))

  def store(id: UUID, producerId: String, version: String, fileParts: (FileInfo, File)): Try[CatalogItemDocument] =
    Try {
      val destPath = createPath(producerId, version, id.toString, fileParts._1)

      val path = moveRenameFile(fileParts._2.getPath, destPath).toString
      CatalogItemDocument(id, fileParts._1.getFileName, fileParts._1.getContentType.toString(), path)
    }

  def get(id: UUID, producerId: String): File = ???

  private def createPath(producerId: String, version: String, id: String, fileInfo: FileInfo): String = {

    val docsPath: Path = Paths.get(
      currentPath.toString,
      s"target/pdnd-interop/docs/$producerId/$version/$id${fileInfo.getFieldName}/${fileInfo.getContentType.toString}"
    )
    val pathCreated: Path = Files.createDirectories(docsPath)

    Paths.get(pathCreated.toString, s"${fileInfo.getFileName}").toString

  }

  private def moveRenameFile(source: String, destination: String): Path = {
    Files.move(Paths.get(source), Paths.get(destination), StandardCopyOption.REPLACE_EXISTING)

  }
}
