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
  technology: String,
  voucherLifespan: Int,
  attributes: CatalogAttributes,
  forcedVerification: Boolean,
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
      forcedVerification = forcedVerification,
      descriptors = descriptors.map(_.toApi)
    )
  }

  def extractFile(descriptorId: String, documentId: String): Option[(ContentType, Path)] = for {
    doc <- descriptors
      .find(_.id == UUID.fromString(descriptorId))
      .flatMap(_.docs.find(_.id == UUID.fromString(documentId)))
    contentType <- ContentType.parse(doc.contentType).toOption //TODO: improve
  } yield (contentType, Paths.get(doc.path))

  def updateFile(descriptorId: String, document: CatalogDocument, isInterface: Boolean): CatalogItem = {
    val uuid: UUID = UUID.fromString(descriptorId)
    val updated: Seq[CatalogDescriptor] =
      descriptors.map {
        case descriptor if descriptor.id == uuid && isInterface => descriptor.copy(interface = Some(document))
        case descriptor if descriptor.id == uuid                => descriptor.copy(docs = descriptor.docs.appended(document))
        case descriptor                                         => descriptor
      }

    copy(descriptors = updated)
  }

  def getInterfacePath(descriptorId: String): Option[String] = {
    for {
      doc       <- descriptors.find(_.id == UUID.fromString(descriptorId))
      interface <- doc.interface
    } yield interface.path
  }

  def getDocumentPaths(descriptorId: String): Option[Seq[String]] = {
    for {
      documents <- descriptors.find(_.id == UUID.fromString(descriptorId))
    } yield documents.docs.map(_.path)
  }

}

object CatalogItem {
  def create(seed: EServiceSeed, uuidSupplier: UUIDSupplier): Future[CatalogItem] = {

    val firstVersion: String = "1"

    val id: UUID = uuidSupplier.get

    Future.fromTry {
      for {
        attributes <- CatalogAttributes.fromApi(seed.attributes)
      } yield CatalogItem(
        id = id,
        producerId = seed.producerId,
        name = seed.name,
        description = seed.description,
        audience = seed.audience,
        technology = seed.technology,
        voucherLifespan = seed.voucherLifespan,
        attributes = attributes,
        forcedVerification = seed.forcedVerification,
        descriptors = Seq(
          CatalogDescriptor(
            id = uuidSupplier.get,
            description = None,
            version = firstVersion,
            interface = None,
            docs = Seq.empty[CatalogDocument],
            status = Draft
          )
        )
      )
    }
  }

}
