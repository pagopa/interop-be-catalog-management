package it.pagopa.interop.catalogmanagement.projection.cqrs

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.catalogmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.catalogmanagement.utils.PersistentAdapters._
import it.pagopa.interop.catalogmanagement.{ItSpecConfiguration, ItSpecHelper}

import java.util.UUID

class CqrsProjectionSpec extends ScalaTestWithActorTestKit(ItSpecConfiguration.config) with ItSpecHelper {

  "Projection" should {
    "succeed for event CatalogItemAdded" in {
      val eServiceId = UUID.randomUUID()

      val eService = createEService(eServiceId.toString)

      val expectedData = eService.toPersistent

      val persisted = findOne[CatalogItem](eServiceId.toString).futureValue

      expectedData shouldBe persisted
    }

    "succeed for event ClonedCatalogItemAdded" in {
      val eServiceId          = UUID.randomUUID()
      val descriptorId        = UUID.randomUUID()
      val cloningDescriptorId = UUID.randomUUID()

      createEService(eServiceId.toString)
      createEServiceDescriptor(eServiceId.toString, descriptorId)
      createEServiceDescriptor(eServiceId.toString, cloningDescriptorId)

      val clonedEService = cloneEService(eServiceId, cloningDescriptorId)

      val expectedData = clonedEService.toPersistent

      val persisted = findOne[CatalogItem](clonedEService.id.toString).futureValue

      expectedData shouldBe persisted
    }

  }

}
