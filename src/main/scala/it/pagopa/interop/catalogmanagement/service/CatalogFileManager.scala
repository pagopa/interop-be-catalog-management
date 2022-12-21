package it.pagopa.interop.catalogmanagement.service

import akka.http.scaladsl.server.directives.FileInfo
import cats.syntax.all._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.catalogmanagement.error.CatalogManagementErrors.{
  DocumentAlreadyUploaded,
  InvalidInterfaceFileDetected
}
import it.pagopa.interop.catalogmanagement.model.{CatalogDocument, CatalogItem, Rest, Soap}
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.Digester
import org.apache.tika.Tika

import java.io.File
import java.nio.file.Files
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

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

object CatalogFileManager {

  private val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  private final val tika: Tika = new Tika()

  def verify(fileParts: (FileInfo, File), catalogItem: CatalogItem, descriptorId: String, isInterface: Boolean)(implicit
    contexts: Seq[(String, String)]
  ): Either[Throwable, CatalogItem] = for {
    _ <- verifyChecksum(fileParts, catalogItem, descriptorId)
    _ <- verifyTechnology(fileParts, catalogItem).whenA(isInterface)
  } yield catalogItem

  private def verifyChecksum(
    fileParts: (FileInfo, File),
    catalogItem: CatalogItem,
    descriptorId: String
  ): Either[Throwable, Unit] = {
    val checksum: String         = Digester.toMD5(fileParts._2)
    val alreadyUploaded: Boolean = catalogItem.descriptors
      .exists(descriptor =>
        descriptor.id == UUID
          .fromString(descriptorId) && (descriptor.docs.exists(_.checksum == checksum) || descriptor.interface
          .exists(_.checksum == checksum))
      )

    Left(DocumentAlreadyUploaded(catalogItem.id.toString, fileParts._1.fileName))
      .withRight[Unit]
      .whenA(alreadyUploaded)
  }

  private def verifyTechnology(fileParts: (FileInfo, File), catalogItem: CatalogItem)(implicit
    contexts: Seq[(String, String)]
  ): Either[Throwable, Unit] = {
    val restContentTypes: Set[String] = Set("text/x-yaml", "application/x-yaml", "application/json")
    val soapContentTypes: Set[String] = Set("application/xml", "application/soap+xml", "application/wsdl+xml")

    for {
      detectedContentTypes <- Try(tika.detect(Files.readAllBytes(fileParts._2.toPath), fileParts._1.fileName)).toEither
      _ = logger.debug(s"Detected $detectedContentTypes interface content type for eservice: ${catalogItem.id}")
      isValidTechnology = catalogItem.technology match {
        case Rest => restContentTypes.contains(detectedContentTypes)
        case Soap => soapContentTypes.contains(detectedContentTypes)
      }
      _ <- Left(
        InvalidInterfaceFileDetected(catalogItem.id.toString, detectedContentTypes, catalogItem.technology.toString)
      )
        .withRight[Unit]
        .unlessA(isValidTechnology)
    } yield ()
  }
}
