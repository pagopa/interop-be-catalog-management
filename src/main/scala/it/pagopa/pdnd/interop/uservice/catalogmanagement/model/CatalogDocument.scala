package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import java.util.UUID

final case class CatalogDocument(id: UUID, name: String, contentType: String, path: String)
    extends Convertable[EServiceDoc] {
  override def toApi: EServiceDoc = EServiceDoc(id = id, name = name, contentType = contentType)
}
