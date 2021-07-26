package it.pagopa.pdnd.interop.uservice.catalogmanagement.service

import akka.http.scaladsl.model.{ContentType, HttpCharsets, MediaType, MediaTypes}
import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common.Digester
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.{CatalogDocument, CatalogItem}

import java.io.File
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@SuppressWarnings(Array("org.wartremover.warts.Equals", "org.wartremover.warts.ImplicitParameter"))
trait FileManager {

  def store(
    id: UUID,
    eServiceId: String,
    descriptorId: String,
    description: String,
    interface: Boolean,
    fileParts: (FileInfo, File)
  ): Future[CatalogDocument]

  def get(id: UUID, producerId: String): File

  def verify(fileParts: (FileInfo, File), catalogItem: CatalogItem, descriptorId: String, isInterface: Boolean)(implicit
    ec: ExecutionContext
  ): Future[CatalogItem] = for {
    checksumVerified <- verifyChecksum(fileParts, catalogItem, descriptorId)
    technologyVerified <-
      if (isInterface) verifyTechnology(fileParts, checksumVerified) else Future.successful(checksumVerified)
  } yield technologyVerified

  private def verifyChecksum(
    fileParts: (FileInfo, File),
    catalogItem: CatalogItem,
    descriptorId: String
  ): Future[CatalogItem] = {
    val checksum: String = Digester.createHash(fileParts._2)
    val alreadyUploaded: Boolean = catalogItem.descriptors
      .exists(descriptor =>
        descriptor.id == UUID
          .fromString(descriptorId) && (descriptor.docs.exists(_.checksum == checksum) || descriptor.interface
          .exists(_.checksum == checksum))
      )

    if (alreadyUploaded)
      Future.failed[CatalogItem](new RuntimeException(s"File ${fileParts._1.getFileName} already uploaded"))
    else Future.successful(catalogItem)
  }

  //TODO use Apache Tika
  private def verifyTechnology(fileParts: (FileInfo, File), catalogItem: CatalogItem): Future[CatalogItem] = {
    val isValidTechnology = catalogItem.technology match {
      case "REST" =>
        fileParts._1.getContentType.toString == ContentType(
          MediaType.textWithFixedCharset("yaml", HttpCharsets.`UTF-8`, "yaml", "yml")
        ).toString
      case "SOAP" =>
        fileParts._1.getContentType.toString == ContentType(
          MediaTypes.`application/soap+xml`,
          HttpCharsets.`UTF-8`
        ).toString
      case _ => false
    }

    if (isValidTechnology)
      Future.successful(catalogItem)
    else
      Future.failed[CatalogItem](
        new RuntimeException(
          s"ContentType ${fileParts._1.contentType.toString} is not valid for ${catalogItem.technology}"
        )
      )
  }

}
