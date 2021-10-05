package it.pagopa.pdnd.interop.uservice.catalogmanagement.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.CatalogDocument
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.FileManager

import java.io.{ByteArrayOutputStream, File, FileInputStream, InputStream}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

@SuppressWarnings(Array("org.wartremover.warts.ToString"))
class FileManagerImpl extends FileManager {

  val currentPath: Path = Paths.get(System.getProperty("user.dir"))

  override def store(
    id: UUID,
    eServiceId: String,
    descriptorId: String,
    description: String,
    interface: Boolean,
    fileParts: (FileInfo, File)
  ): Future[CatalogDocument] =
    Future.fromTry {
      Try {
        val destPath = createPath(
          eServiceId,
          descriptorId,
          id.toString,
          fileParts._1.getContentType.toString(),
          fileParts._1.getFileName
        )

        val path = moveRenameFile(fileParts._2.getPath, destPath).toString
        CatalogDocument(
          id = id,
          name = fileParts._1.getFileName,
          contentType = fileParts._1.getContentType.toString(),
          description = description,
          path = path,
//          checksum = Digester.createHash(fileParts._2),
          checksum = UUID.randomUUID().toString,
          uploadDate = OffsetDateTime.now()
        )

      }
    }

  def get(filePath: String): Future[ByteArrayOutputStream] = Future.fromTry {
    Try {
      val inputStream: InputStream            = new FileInputStream(filePath)
      val outputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val _                                   = inputStream.transferTo(outputStream)
      outputStream
    }
  }

  override def delete(filePath: String): Future[Boolean] = Future.fromTry {
    Try {
      val file: File = Paths.get(filePath).toFile
      file.delete()
    }
  }

  private def createPath(
    producerId: String,
    descriptorId: String,
    id: String,
    contentType: String,
    fileName: String
  ): String = {

    val docsPath: Path =
      Paths.get(currentPath.toString, s"target/pdnd-interop/docs/$producerId/$descriptorId/$id/${contentType}")
    val pathCreated: Path = Files.createDirectories(docsPath)

    Paths.get(pathCreated.toString, s"${fileName}").toString

  }

  private def moveRenameFile(source: String, destination: String): Path = {
    Files.move(Paths.get(source), Paths.get(destination), StandardCopyOption.REPLACE_EXISTING)

  }

  override def copy(filePathToCopy: String)(
    documentId: UUID,
    eServiceId: String,
    descriptorId: String,
    description: String,
    checksum: String,
    contentType: String,
    fileName: String
  ): Future[CatalogDocument] = Future.fromTry {
    Try {
      val destination = createPath(eServiceId, descriptorId, documentId.toString, contentType, fileName)
      val _ = Files.copy(Paths.get(filePathToCopy), Paths.get(destination), StandardCopyOption.REPLACE_EXISTING)

      CatalogDocument(
        id = documentId,
        name = fileName,
        contentType = contentType,
        description = description,
        path = destination,
        checksum = checksum,
        uploadDate = OffsetDateTime.now()
      )

    }
  }
}
