package it.pagopa.interop.catalogmanagement.service

import akka.http.scaladsl.server.directives.FileInfo
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.catalogmanagement.error.CatalogManagementErrors.{
  DocumentAlreadyUploaded,
  InvalidInterfaceFileDetected
}
import it.pagopa.interop.catalogmanagement.model.{CatalogDocument, CatalogItem, Rest, Soap}
import it.pagopa.interop.commons.files.service.{FileManager, StorageFilePath}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.Digester
import org.apache.tika.Tika
import org.slf4j.LoggerFactory

import java.io.File
import java.nio.file.Files
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

  def delete(filePath: StorageFilePath): Future[Boolean]
}

object CatalogFileManager {

  private val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

  private final val tika: Tika = new Tika()

  def verify(fileParts: (FileInfo, File), catalogItem: CatalogItem, descriptorId: String, isInterface: Boolean)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): Future[CatalogItem] = for {
    checksumVerified   <- verifyChecksum(fileParts, catalogItem, descriptorId)
    technologyVerified <-
      if (isInterface) verifyTechnology(fileParts, checksumVerified) else Future.successful(checksumVerified)
  } yield technologyVerified

  private def verifyChecksum(
    fileParts: (FileInfo, File),
    catalogItem: CatalogItem,
    descriptorId: String
  ): Future[CatalogItem] = {
    val checksum: String         = Digester.createMD5Hash(fileParts._2)
    val alreadyUploaded: Boolean = catalogItem.descriptors
      .exists(descriptor =>
        descriptor.id == UUID
          .fromString(descriptorId) && (descriptor.docs.exists(_.checksum == checksum) || descriptor.interface
          .exists(_.checksum == checksum))
      )

    if (alreadyUploaded)
      Future.failed[CatalogItem](DocumentAlreadyUploaded(catalogItem.id.toString, fileParts._1.fileName))
    else Future.successful(catalogItem)
  }

  private def verifyTechnology(fileParts: (FileInfo, File), catalogItem: CatalogItem)(implicit
    contexts: Seq[(String, String)]
  ): Future[CatalogItem] = {
    val restContentTypes: Set[String] = Set("text/x-yaml", "application/x-yaml")
    val soapContentTypes: Set[String] = Set("application/soap+xml", "application/wsdl+xml")

    val detectedContentTypes: String = tika.detect(Files.readAllBytes(fileParts._2.toPath), fileParts._1.fileName)

    logger.debug(s"Detected $detectedContentTypes interface content type for eservice: ${catalogItem.id}")

    val isValidTechnology = catalogItem.technology match {
      case Rest => restContentTypes.contains(detectedContentTypes)
      case Soap => soapContentTypes.contains(detectedContentTypes)
    }

    if (isValidTechnology)
      Future.successful(catalogItem)
    else
      Future.failed[CatalogItem](
        InvalidInterfaceFileDetected(catalogItem.id.toString, detectedContentTypes, catalogItem.technology.toString)
      )
  }
}
