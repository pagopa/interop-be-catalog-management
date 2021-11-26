package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import it.pagopa.pdnd.interop.commons.utils.service.UUIDSupplier
import java.util.UUID
import scala.concurrent.Future

final case class CatalogItem(
  id: UUID,
  producerId: UUID,
  name: String,
  description: String,
  technology: CatalogItemTechnology,
  attributes: CatalogAttributes,
  descriptors: Seq[CatalogDescriptor]
) extends Convertable[EService] {

  def toApi: EService = {
    EService(
      id = id,
      producerId = producerId,
      name = name,
      description = description,
      technology = technology.toApi,
      attributes = attributes.toApi,
      descriptors = descriptors.map(_.toApi)
    )
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
        technology = CatalogItemTechnology.fromApi(updateEServiceSeed.technology),
        attributes = attributes
      )
    }
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
        technology = CatalogItemTechnology.fromApi(seed.technology),
        attributes = attributes,
        descriptors = Seq.empty[CatalogDescriptor]
      )
    }
  }

}
