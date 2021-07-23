package it.pagopa.pdnd.interop.uservice.catalogmanagement.service

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.CatalogDocument

import java.io.File
import java.util.UUID
import scala.util.Try

trait FileManager {

  def store(id: UUID, eServiceId: String, descriptorId: String, fileParts: (FileInfo, File)): Try[CatalogDocument]

  def get(id: UUID, producerId: String): File

}
