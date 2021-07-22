package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import akka.http.scaladsl.model.ContentType

import java.nio.file.{Path, Paths}
import java.util.UUID

@SuppressWarnings(Array("org.wartremover.warts.Equals"))
final case class CatalogItem(
  id: UUID,
  producerId: UUID,
  name: String,
  audience: Seq[String],
  technology: String,
  attributes: CatalogAttributes,
  descriptors: Seq[CatalogDescriptor]
) extends Convertable[EService] {
  def toApi: EService = {
    EService(
      id = id,
      producerId = producerId,
      name = name,
      audience = audience,
      technology = technology,
      attributes = attributes.toApi,
      descriptors = descriptors.map(_.toApi)
    )
  }

  def extractFile(documentId: UUID): Option[(ContentType, Path)] = for {
    doc <- descriptors
      .flatMap(_.docs.find(_.id == documentId))
      .headOption
    contentType <- ContentType.parse(doc.contentType).toOption //TODO: improve
  } yield (contentType, Paths.get(doc.path))
}
