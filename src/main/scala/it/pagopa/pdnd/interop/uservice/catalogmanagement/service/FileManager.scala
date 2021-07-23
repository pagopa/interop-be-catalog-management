package it.pagopa.pdnd.interop.uservice.catalogmanagement.service

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common.Digester
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.{CatalogDocument, CatalogItem}

import java.io.File
import java.util.UUID
import scala.concurrent.Future

@SuppressWarnings(Array("org.wartremover.warts.Equals"))
trait FileManager {

  def store(id: UUID, eServiceId: String, descriptorId: String, fileParts: (FileInfo, File)): Future[CatalogDocument]

  def get(id: UUID, producerId: String): File

  def verifyFile(fileParts: (FileInfo, File), catalogItem: CatalogItem): Future[CatalogItem] = {
    val checksum: String = Digester.createHash(fileParts._2)
    val alreadyUploaded  = catalogItem.descriptors.flatMap(_.docs).exists(_.checksum == checksum)

    if (alreadyUploaded)
      Future.failed[CatalogItem](new RuntimeException(s"File ${fileParts._1.getFileName} already uploaded"))
    else Future.successful(catalogItem)
  }

}
