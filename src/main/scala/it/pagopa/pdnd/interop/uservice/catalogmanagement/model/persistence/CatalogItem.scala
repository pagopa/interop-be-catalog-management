package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import akka.http.scaladsl.model.ContentType
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.{EService, EServiceDoc, EServiceVersion}

import java.nio.file.{Path, Paths}
import java.util.UUID

trait Convertable[A] {
  def toApi: A
}

@SuppressWarnings(Array("org.wartremover.warts.Equals"))
final case class CatalogItem(
  id: UUID,
  producerId: UUID,
  name: String,
  versions: Seq[CatalogItemVersion],
  status: String
) extends Convertable[EService] {
  def toApi: EService = {
    EService(
      id = id,
      producerId = producerId,
      name = name,
      status = "active",
      versions = versions.map(_.toApi)
    )
  }

  def extractFile(documentId: UUID): Option[(ContentType, Path)] = for {
    doc <- versions
      .flatMap(_.docs.find(_.id == documentId))
      .headOption
    contentType <- ContentType.parse(doc.contentType).toOption //TODO: improve
  } yield (contentType, Paths.get(doc.path))
}

final case class CatalogItemVersion(
  id: UUID,
  version: String,
  description: String,
  docs: Seq[CatalogItemDocument],
  status: String
) extends Convertable[EServiceVersion] {
  def toApi: EServiceVersion = {
    EServiceVersion(id = id, version = version, description = description, docs =  docs.map(_.toApi), status = status)
  }

}

final case class CatalogItemDocument(id: UUID, name: String, contentType: String, path: String)
    extends Convertable[EServiceDoc] {
  override def toApi: EServiceDoc = EServiceDoc(id = id, name = name, contentType = contentType)
}
