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

      expectedData shouldBe persisted
    }

    "succeed for event CatalogItemUpdated" in {
      val eServiceId           = UUID.randomUUID()
      val descriptorId         = UUID.randomUUID()
      val updatingDescriptorId = UUID.randomUUID()

      createEService(eServiceId)
      createEServiceDescriptor(eServiceId, descriptorId)
      createEServiceDescriptor(eServiceId, updatingDescriptorId)

      val updatedEService = updateDescriptor(eServiceId, updatingDescriptorId)

      val expectedData = updatedEService.toPersistent

      val persisted = findOne[CatalogItem](updatedEService.id.toString).futureValue

      expectedData shouldBe persisted
    }

  }

}
