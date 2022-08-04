package it.pagopa.interop.catalogmanagement.projection.cqrs

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.catalogmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.catalogmanagement.utils.PersistentAdapters._
import it.pagopa.interop.catalogmanagement.{ItSpecConfiguration, ItSpecHelper}
import spray.json.JsObject

import java.util.UUID

class CqrsProjectionSpec extends ScalaTestWithActorTestKit(ItSpecConfiguration.config) with ItSpecHelper {

  "Projection" should {
    "succeed for event CatalogItemAdded" in {
      val eServiceId = UUID.randomUUID()

      val eService = createEService(eServiceId)

      val expectedData = eService.toPersistent

      val persisted = findOne[CatalogItem](eServiceId.toString).futureValue

      expectedData shouldBe persisted
    }

    "succeed for event ClonedCatalogItemAdded" in {
      val eServiceId          = UUID.randomUUID()
      val descriptorId        = UUID.randomUUID()
      val cloningDescriptorId = UUID.randomUUID()

      createEService(eServiceId)
      createEServiceDescriptor(eServiceId, descriptorId)
      createEServiceDescriptor(eServiceId, cloningDescriptorId)

      val clonedEService = cloneEService(eServiceId, cloningDescriptorId)

      val expectedData = clonedEService.toPersistent

      val persisted = findOne[CatalogItem](clonedEService.id.toString).futureValue

      expectedData shouldBe persisted
    }

    "succeed for event CatalogItemDescriptorAdded" in {
      val eServiceId   = UUID.randomUUID()
      val descriptorId = UUID.randomUUID()

      val eService   = createEService(eServiceId)
      val descriptor = createEServiceDescriptor(eServiceId, descriptorId)

      val expectedData = eService.copy(descriptors = Seq(descriptor)).toPersistent

      val persisted = findOne[CatalogItem](eServiceId.toString).futureValue

      compareCatalogItems(expectedData, persisted)
    }

    "succeed for event CatalogItemDeleted" in {
      val eServiceId = UUID.randomUUID()

      createEService(eServiceId)
      deleteEService(eServiceId)

      val persisted = find[CatalogItem](eServiceId.toString).futureValue

      persisted shouldBe empty
    }

    "succeed for event CatalogItemUpdated on EService update" in {
      val eServiceId = UUID.randomUUID()

      createEService(eServiceId)

      val updatedEService = updateEService(eServiceId)

      val expectedData = updatedEService.toPersistent

      val persisted = findOne[CatalogItem](updatedEService.id.toString).futureValue

      expectedData shouldBe persisted
    }

    "succeed for event CatalogItemUpdated on Descriptor update" in {
      val eServiceId           = UUID.randomUUID()
      val descriptorId         = UUID.randomUUID()
      val updatingDescriptorId = UUID.randomUUID()

      createEService(eServiceId)
      createEServiceDescriptor(eServiceId, descriptorId)
      createEServiceDescriptor(eServiceId, updatingDescriptorId)

      val updatedEService = updateDescriptor(eServiceId, updatingDescriptorId)

      val expectedData = updatedEService.toPersistent

      val persisted = findOne[CatalogItem](updatedEService.id.toString).futureValue

      compareCatalogItems(expectedData, persisted)
    }

    "succeed for event CatalogItemWithDescriptorsDeleted" in {
      val eServiceId           = UUID.randomUUID()
      val descriptorId         = UUID.randomUUID()
      val deletingDescriptorId = UUID.randomUUID()

      val eService   = createEService(eServiceId)
      val descriptor = createEServiceDescriptor(eServiceId, descriptorId)
      createEServiceDescriptor(eServiceId, deletingDescriptorId)

      deleteDescriptor(eServiceId, deletingDescriptorId)

      val expectedData = eService.copy(descriptors = Seq(descriptor)).toPersistent

      val persisted = findOne[CatalogItem](eServiceId.toString).futureValue

      compareCatalogItems(expectedData, persisted)
    }

    "succeed for event CatalogItemDocumentAdded with interface document" in {
      val eServiceId    = UUID.randomUUID()
      val descriptorId  = UUID.randomUUID()
      val descriptorId2 = UUID.randomUUID()

      createEService(eServiceId)
      createEServiceDescriptor(eServiceId, descriptorId)
      createEServiceDescriptor(eServiceId, descriptorId2)

      createDescriptorDocument(eServiceId, descriptorId2, "INTERFACE")

      createDescriptorDocument(eServiceId, descriptorId, "INTERFACE")
      val eService = createDescriptorDocument(eServiceId, descriptorId, "DOCUMENT")

      val expectedData = eService.toPersistent

      val persisted = findOne[CatalogItem](eServiceId.toString).futureValue

      compareCatalogItems(expectedData, persisted)
    }

    "succeed for event CatalogItemDocumentAdded with generic document" in {
      val eServiceId    = UUID.randomUUID()
      val descriptorId  = UUID.randomUUID()
      val descriptorId2 = UUID.randomUUID()

      createEService(eServiceId)
      createEServiceDescriptor(eServiceId, descriptorId)
      createEServiceDescriptor(eServiceId, descriptorId2)

      createDescriptorDocument(eServiceId, descriptorId2, "DOCUMENT")

      createDescriptorDocument(eServiceId, descriptorId, "INTERFACE")

      createDescriptorDocument(eServiceId, descriptorId, "DOCUMENT")
      val eService = createDescriptorDocument(eServiceId, descriptorId, "DOCUMENT")

      val expectedData = eService.toPersistent

      val persisted = findOne[CatalogItem](eServiceId.toString).futureValue

      sortEServiceArrayFields(expectedData) shouldBe sortEServiceArrayFields(persisted)
    }

    "succeed for event CatalogItemDocumentUpdated with interface document" in {
      val eServiceId    = UUID.randomUUID()
      val descriptorId  = UUID.randomUUID()
      val descriptorId2 = UUID.randomUUID()
      val documentId    = UUID.randomUUID()

      createEService(eServiceId)
      createEServiceDescriptor(eServiceId, descriptorId)
      createEServiceDescriptor(eServiceId, descriptorId2)

      createDescriptorDocument(eServiceId, descriptorId, "INTERFACE", documentId)

      createDescriptorDocument(eServiceId, descriptorId, "DOCUMENT")
      createDescriptorDocument(eServiceId, descriptorId2, "INTERFACE")
      val eService = createDescriptorDocument(eServiceId, descriptorId2, "DOCUMENT")

      val doc = updateDescriptorDocument(eServiceId, descriptorId, documentId)

      val descriptorWithDocs  = eService.descriptors.find(_.id == descriptorId).get.copy(interface = Some(doc))
      val descriptor2WithDocs = eService.descriptors.find(_.id == descriptorId2).get

      val expectedData = eService.copy(descriptors = Seq(descriptorWithDocs, descriptor2WithDocs)).toPersistent

      val persisted = findOne[CatalogItem](eServiceId.toString).futureValue

      compareCatalogItems(expectedData, persisted)
    }

    "succeed for event CatalogItemDocumentUpdated with generic document" in {
      val eServiceId    = UUID.randomUUID()
      val descriptorId  = UUID.randomUUID()
      val descriptorId2 = UUID.randomUUID()
      val documentId    = UUID.randomUUID()

      createEService(eServiceId)
      createEServiceDescriptor(eServiceId, descriptorId)
      createEServiceDescriptor(eServiceId, descriptorId2)

      createDescriptorDocument(eServiceId, descriptorId, "DOCUMENT", documentId)

      createDescriptorDocument(eServiceId, descriptorId, "INTERFACE")
      createDescriptorDocument(eServiceId, descriptorId, "DOCUMENT")
      createDescriptorDocument(eServiceId, descriptorId2, "INTERFACE")
      val eService = createDescriptorDocument(eServiceId, descriptorId2, "DOCUMENT")

      val doc = updateDescriptorDocument(eServiceId, descriptorId, documentId)

      val updatedDescriptor   = eService.descriptors.find(_.id == descriptorId).get
      val descriptorWithDocs  = updatedDescriptor.copy(docs = updatedDescriptor.docs.filter(_.id != documentId) :+ doc)
      val descriptor2WithDocs = eService.descriptors.find(_.id == descriptorId2).get

      val expectedData = eService.copy(descriptors = Seq(descriptorWithDocs, descriptor2WithDocs)).toPersistent

      val persisted = findOne[CatalogItem](eServiceId.toString).futureValue

      compareCatalogItems(expectedData, persisted)
    }

    "succeed for event CatalogItemDocumentDeleted with interface document" in {
      val eServiceId    = UUID.randomUUID()
      val descriptorId  = UUID.randomUUID()
      val descriptorId2 = UUID.randomUUID()
      val documentId    = UUID.randomUUID()

      createEService(eServiceId)
      createEServiceDescriptor(eServiceId, descriptorId)
      createEServiceDescriptor(eServiceId, descriptorId2)

      createDescriptorDocument(eServiceId, descriptorId, "INTERFACE", documentId)

      createDescriptorDocument(eServiceId, descriptorId, "DOCUMENT")
      createDescriptorDocument(eServiceId, descriptorId2, "INTERFACE")
      val eService = createDescriptorDocument(eServiceId, descriptorId2, "DOCUMENT")

      deleteDescriptorDocument(eServiceId, descriptorId, documentId)

      val descriptorWithDocs  = eService.descriptors.find(_.id == descriptorId).get.copy(interface = None)
      val descriptor2WithDocs = eService.descriptors.find(_.id == descriptorId2).get

      val expectedData = eService.copy(descriptors = Seq(descriptorWithDocs, descriptor2WithDocs)).toPersistent

      val persisted = findOne[CatalogItem](eServiceId.toString).futureValue

      compareCatalogItems(expectedData, persisted)
    }

    "succeed for event CatalogItemDocumentDeleted with generic document" in {
      val eServiceId    = UUID.randomUUID()
      val descriptorId  = UUID.randomUUID()
      val descriptorId2 = UUID.randomUUID()
      val documentId    = UUID.randomUUID()

      createEService(eServiceId)
      createEServiceDescriptor(eServiceId, descriptorId)
      createEServiceDescriptor(eServiceId, descriptorId2)

      createDescriptorDocument(eServiceId, descriptorId, "DOCUMENT", documentId)

      createDescriptorDocument(eServiceId, descriptorId, "INTERFACE")
      createDescriptorDocument(eServiceId, descriptorId, "DOCUMENT")
      createDescriptorDocument(eServiceId, descriptorId2, "INTERFACE")
      val eService = createDescriptorDocument(eServiceId, descriptorId2, "DOCUMENT")

      deleteDescriptorDocument(eServiceId, descriptorId, documentId)

      val updatedDescriptor   = eService.descriptors.find(_.id == descriptorId).get
      val descriptorWithDocs  = updatedDescriptor.copy(docs = updatedDescriptor.docs.filter(_.id != documentId))
      val descriptor2WithDocs = eService.descriptors.find(_.id == descriptorId2).get

      val expectedData = eService.copy(descriptors = Seq(descriptorWithDocs, descriptor2WithDocs)).toPersistent

      val persisted = findOne[CatalogItem](eServiceId.toString).futureValue

      compareCatalogItems(expectedData, persisted)
    }

  }

}
