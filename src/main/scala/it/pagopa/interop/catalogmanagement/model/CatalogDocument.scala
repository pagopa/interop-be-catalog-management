package it.pagopa.interop.catalogmanagement.model

import it.pagopa.interop.catalogmanagement.service.CatalogFileManager

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

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
    EServiceDoc(id = id, name = name, contentType = contentType, description = description, path = path)

  /** clones the current document instance as a new one.
    * @param fileManager - DI of the fileManager in place
    * @param clonedDocumentId - new cloned document identifier
    * @param eServiceId - identifier of the eservice bound to this new cloned document
    * @param descriptorId - identifier of the descriptor bound to this new cloned document
    * @return
    */
  def cloneDocument(
    fileManager: CatalogFileManager
  )(clonedDocumentId: UUID)(implicit ec: ExecutionContext): Future[CatalogDocument] = {
    fileManager.copy(path)(
      documentId = clonedDocumentId,
      description = this.description,
      checksum = this.checksum,
      contentType = this.contentType,
      fileName = this.name
    )
  }

}
