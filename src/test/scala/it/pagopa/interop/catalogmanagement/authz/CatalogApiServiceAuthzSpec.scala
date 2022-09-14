package it.pagopa.interop.catalogmanagement.authz

import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.interop.catalogmanagement.api.impl.EServiceApiMarshallerImpl._
import it.pagopa.interop.catalogmanagement.api.impl.EServiceApiServiceImpl
import it.pagopa.interop.catalogmanagement.model.{
  Attributes,
  EServiceDescriptorSeed,
  EServiceDescriptorState,
  EServiceSeed,
  EServiceTechnology,
  UpdateEServiceDescriptorDocumentSeed,
  UpdateEServiceDescriptorSeed,
  UpdateEServiceSeed
}
import it.pagopa.interop.catalogmanagement.model.persistence.Command
import it.pagopa.interop.catalogmanagement.server.impl.Main.catalogPersistentEntity
import it.pagopa.interop.catalogmanagement.service.impl.CatalogFileManagerImpl
import it.pagopa.interop.catalogmanagement.util.{AuthorizedRoutes, ClusteredScalatestRouteTest}
import it.pagopa.interop.commons.utils.service.UUIDSupplier
import org.scalatest.wordspec.AnyWordSpecLike

import java.io.File
import java.util.UUID
import it.pagopa.interop.commons.files.service.FileManager
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.ExecutionContextExecutor
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import java.util.concurrent.ExecutorService

class CatalogApiServiceAuthzSpec extends AnyWordSpecLike with BeforeAndAfterAll with ClusteredScalatestRouteTest {

  private val threadPool: ExecutorService          = Executors.newSingleThreadExecutor()
  private val blockingEc: ExecutionContextExecutor = ExecutionContext.fromExecutorService(threadPool)

  override val testPersistentEntity: Entity[Command, ShardingEnvelope[Command]] = catalogPersistentEntity

  val service: EServiceApiServiceImpl =
    new EServiceApiServiceImpl(
      testTypedSystem,
      testAkkaSharding,
      testPersistentEntity,
      new UUIDSupplier {
        override def get: UUID = UUID.randomUUID()
      },
      new CatalogFileManagerImpl(FileManager.get(FileManager.File)(blockingEc))
    )

  override def afterAll(): Unit = { threadPool.shutdown() }

  "E-Service api operation authorization spec" should {

    "accept authorized roles for createEService" in {
      val endpoint = AuthorizedRoutes.endpoints("createEService")
      val fakeSeed =
        EServiceSeed(
          producerId = UUID.randomUUID(),
          "test",
          "test",
          EServiceTechnology.REST,
          Attributes(Seq.empty, Seq.empty, Seq.empty)
        )
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.createEService(fakeSeed) })
    }

    "accept authorized roles for createEServiceDocument" in {
      val endpoint = AuthorizedRoutes.endpoints("createEServiceDocument")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] =>
          service.createEServiceDocument(
            kind = "fake",
            prettyName = "fake",
            doc = (FileInfo("test", "test", ContentTypes.NoContentType), File.createTempFile("fake", "fake")),
            eServiceId = "fake",
            descriptorId = "fake"
          )
        }
      )
    }

    "accept authorized roles for getEService" in {
      val endpoint = AuthorizedRoutes.endpoints("getEService")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.getEService("fake") })
    }

    "accept authorized roles for getEServices" in {
      val endpoint = AuthorizedRoutes.endpoints("getEServices")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.getEServices(None, None, None) })
    }

    "accept authorized roles for getEServiceDocument" in {
      val endpoint = AuthorizedRoutes.endpoints("getEServiceDocument")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.getEServiceDocument("fake", "fake", "fake") }
      )
    }

    "accept authorized roles for deleteDraft" in {
      val endpoint = AuthorizedRoutes.endpoints("deleteDraft")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.deleteDraft("fake", "fake") })
    }

    "accept authorized roles for updateDescriptor" in {
      val endpoint = AuthorizedRoutes.endpoints("updateDescriptor")
      val fakeSeed = UpdateEServiceDescriptorSeed(None, EServiceDescriptorState.DRAFT, Seq.empty, 0, 0, 0)
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.updateDescriptor("fake", "fake", fakeSeed) }
      )
    }

    "accept authorized roles for updateEServiceById" in {
      val endpoint = AuthorizedRoutes.endpoints("updateEServiceById")
      val fakeSeed =
        UpdateEServiceSeed("test", "test", EServiceTechnology.REST, Attributes(Seq.empty, Seq.empty, Seq.empty))
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.updateEServiceById("fake", fakeSeed) }
      )
    }

    "accept authorized roles for createDescriptor" in {
      val endpoint = AuthorizedRoutes.endpoints("createDescriptor")
      val fakeSeed = EServiceDescriptorSeed(None, Seq.empty, 0, 0, 0)
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.createDescriptor("fake", fakeSeed) }
      )
    }

    "accept authorized roles for deleteEServiceDocument" in {
      val endpoint = AuthorizedRoutes.endpoints("deleteEServiceDocument")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.deleteEServiceDocument("fake", "fake", "fake") }
      )
    }

    "accept authorized roles for archiveDescriptor" in {
      val endpoint = AuthorizedRoutes.endpoints("archiveDescriptor")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.archiveDescriptor("fake", "fake") }
      )
    }

    "accept authorized roles for deprecateDescriptor" in {
      val endpoint = AuthorizedRoutes.endpoints("deprecateDescriptor")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.deprecateDescriptor("fake", "fake") }
      )
    }

    "accept authorized roles for suspendDescriptor" in {
      val endpoint = AuthorizedRoutes.endpoints("suspendDescriptor")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.suspendDescriptor("fake", "fake") }
      )
    }

    "accept authorized roles for draftDescriptor" in {
      val endpoint = AuthorizedRoutes.endpoints("draftDescriptor")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.draftDescriptor("fake", "fake") })
    }

    "accept authorized roles for publishDescriptor" in {
      val endpoint = AuthorizedRoutes.endpoints("publishDescriptor")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.publishDescriptor("fake", "fake") }
      )
    }

    "accept authorized roles for updateEServiceDocument" in {
      val endpoint = AuthorizedRoutes.endpoints("updateEServiceDocument")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] =>
          service.updateEServiceDocument(
            eServiceId = "fake",
            descriptorId = "fake",
            documentId = "fake",
            updateEServiceDescriptorDocumentSeed = UpdateEServiceDescriptorDocumentSeed("fake")
          )
        }
      )
    }

    "accept authorized roles for cloneEServiceByDescriptor" in {
      val endpoint = AuthorizedRoutes.endpoints("cloneEServiceByDescriptor")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.cloneEServiceByDescriptor("fake", "fake") }
      )
    }

    "accept authorized roles for deleteEService" in {
      val endpoint = AuthorizedRoutes.endpoints("deleteEService")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.deleteEService("fake") })
    }
  }
}
