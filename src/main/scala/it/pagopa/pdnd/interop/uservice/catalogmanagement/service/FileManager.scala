package it.pagopa.pdnd.interop.uservice.catalogmanagement.service

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.CatalogItemDocument

import java.io.File
import java.util.UUID
import scala.util.Try

trait FileManager {

  def store(id: UUID, producerId: String, version: String, fileParts: (FileInfo, File)): Try[CatalogItemDocument]

  def get(id: UUID, producerId: String): File

}
