package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import akka.http.scaladsl.model.ContentType
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.UUIDSupplier

import java.nio.file.{Path, Paths}
import java.util.UUID
import scala.concurrent.Future

@SuppressWarnings(Array("org.wartremover.warts.Equals"))
final case class CatalogItem(
  id: UUID,
  producerId: UUID,
  name: String,
  description: String,
  audience: Seq[String],
  technology: Option[String],
  voucherLifespan: Int,
  attributes: CatalogAttributes,
  descriptors: Seq[CatalogDescriptor]
) extends Convertable[EService] {
  def toApi: EService = {
    EService(
      id = id,
      producerId = producerId,
      name = name,
      description = description,
      audience = audience,
      technology = technology,
      voucherLifespan = voucherLifespan,
      attributes = attributes.toApi,
      descriptors = descriptors.map(_.toApi)
    )
  }

  def extractFile(descriptorId: String, documentId: String): Option[(ContentType, Path)] = for {
    doc <- descriptors
      .find(_.id == UUID.fromString(descriptorId))
      .flatMap(_.docs.find(_.id == UUID.fromString(documentId)))
    contentType <- ContentType.parse(doc.contentType).toOption //TODO: improve
  } yield (contentType, Paths.get(doc.path))

  def updateFile(descriptorId: String, document: CatalogDocument): CatalogItem = {
    val updated: Seq[CatalogDescriptor] = descriptors.map {
      case descriptor if descriptor.id == UUID.fromString(descriptorId) =>
        descriptor.copy(docs = descriptor.docs.appended(document))
      case descriptor => descriptor
    }
    copy(descriptors = updated)
  }

  def publish(descriptorId: String): CatalogItem = {
    val updated: Seq[CatalogDescriptor] = descriptors.map {
      case descriptor if descriptor.id == UUID.fromString(descriptorId) => descriptor.publish
      case descriptor                                                   => descriptor
    }
    copy(descriptors = updated)
  }

}

object CatalogItem {
  def create(seed: EServiceSeed, uuidSupplier: UUIDSupplier): Future[CatalogItem] = {

    val firstVersion: String = "1"

    val id: UUID   = uuidSupplier.get
    val producerId = uuidSupplier.get //TODO from jwt

    Future.fromTry {
      for {
        attributes <- CatalogAttributes.fromApi(seed.attributes)
      } yield CatalogItem(
        id = id,
        producerId = producerId,
        name = seed.name,
        description = seed.description,
        audience = seed.audience,
        technology = None,
        voucherLifespan = seed.voucherLifespan,
        attributes = attributes,
        descriptors = Seq(
          CatalogDescriptor(
            id = uuidSupplier.get,
            description = None,
            version = firstVersion,
            docs = Seq.empty[CatalogDocument],
            status = Draft
          )
        )
      )
    }
  }

}
