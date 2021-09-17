package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.UUIDSupplier

import java.util.UUID
import scala.concurrent.Future

@SuppressWarnings(Array("org.wartremover.warts.Equals"))
final case class CatalogItem(
  id: UUID,
  producerId: UUID,
  name: String,
  description: String,
  technology: String,
  attributes: CatalogAttributes,
  descriptors: Seq[CatalogDescriptor]
) extends Convertable[EService] {

  def toApi: EService = {
    EService(
      id = id,
      producerId = producerId,
      name = name,
      description = description,
      technology = technology,
      attributes = attributes.toApi,
      descriptors = descriptors.map(_.toApi)
    )
  }

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

  def removeDocument(descriptorId: String, documentId: String): CatalogItem = {

    def updateDescriptor(descriptor: CatalogDescriptor): CatalogDescriptor = {
      val interface: Option[CatalogDocument] = descriptor.interface.filter(_.id.toString == documentId)
      val document: Option[CatalogDocument]  = descriptor.docs.find(_.id.toString == documentId)
      (interface, document) match {
        case (Some(_), None) => descriptor.copy(interface = None)
        case (None, Some(_)) => descriptor.copy(docs = descriptor.docs.filterNot(_.id.toString == documentId))
        case (None, None)    => descriptor
        case (Some(_), Some(_)) =>
          descriptor.copy(interface = None, docs = descriptor.docs.filterNot(_.id.toString == documentId))
      }
    }

    this.descriptors.find(_.id.toString == descriptorId) match {
      case Some(descriptor) =>
        this.copy(descriptors =
          descriptors.filterNot(_.id.toString == descriptorId).appended(updateDescriptor(descriptor))
        )
      case None => this
    }

  }

  def getInterfacePath(descriptorId: String): Option[String] = {
    for {
      doc       <- descriptors.find(_.id.toString == descriptorId)
      interface <- doc.interface
    } yield interface.path
  }

  def getDocumentPaths(descriptorId: String): Option[Seq[String]] = {
    for {
      documents <- descriptors.find(_.id == UUID.fromString(descriptorId))
    } yield documents.docs.map(_.path)
  }

  def mergeWithSeed(updateEServiceSeed: UpdateEServiceSeed): Future[CatalogItem] = {
    Future.fromTry {
      for {
        attributes <- CatalogAttributes.fromApi(updateEServiceSeed.attributes)
      } yield copy(
        name = updateEServiceSeed.name,
        description = updateEServiceSeed.description,
        technology = updateEServiceSeed.technology,
        attributes = attributes
      )
    }
  }

  def addDescriptor(descriptor: CatalogDescriptor): CatalogItem = {
    copy(descriptors = descriptor +: descriptors)
  }

  def currentVersion: Option[String] = descriptors.flatMap(_.version.toLongOption).maxOption.map(_.toString)
}

object CatalogItem {
  def create(seed: EServiceSeed, uuidSupplier: UUIDSupplier): Future[CatalogItem] = {

    val id: UUID = uuidSupplier.get

    Future.fromTry {
      for {
        attributes <- CatalogAttributes.fromApi(seed.attributes)
      } yield CatalogItem(
        id = id,
        producerId = seed.producerId,
        name = seed.name,
        description = seed.description,
        technology = seed.technology,
        attributes = attributes,
        descriptors = Seq.empty[CatalogDescriptor]
      )
    }
  }

}
