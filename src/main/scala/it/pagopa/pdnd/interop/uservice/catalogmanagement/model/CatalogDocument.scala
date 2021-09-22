package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.FileManager

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Future

final case class CatalogDocument(
  id: UUID,
  name: String,
  contentType: String,
  description: String,
  path: String,
  checksum: String,
  uploadDate: OffsetDateTime
) extends Convertable[EServiceDoc] {

  override def toApi: EServiceDoc =
    EServiceDoc(id = id, name = name, contentType = contentType, description = description)

  /**
   * clones the current document instance as a new one.
   * @param fileManager - DI of the fileManager in place
   * @param clonedDocumentId - new cloned document identifier
   * @param eServiceId - identifier of the eservice bound to this new cloned document
   * @param descriptorId - identifier of the descriptor bound to this new cloned document
   * @return
   */
  def cloneDocument(
    fileManager: FileManager
  )(clonedDocumentId: UUID, eServiceId: String, descriptorId: String): Future[CatalogDocument] = {
    fileManager.copy(path)(
      documentId = clonedDocumentId,
      eServiceId = eServiceId,
      descriptorId = descriptorId,
      description = this.description,
      checksum = this.checksum,
      contentType = this.contentType,
      fileName = this.name
    )
  }

}
