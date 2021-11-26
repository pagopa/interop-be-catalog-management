package it.pagopa.pdnd.interop.uservice.catalogmanagement.provider

import akka.actor
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{Cluster, Join}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.directives.{AuthenticationDirective, SecurityDirectives}
import com.itv.scalapact.ScalaPactVerify._
import com.itv.scalapact.shared.ProviderStateResult
import it.pagopa.pdnd.interop.commons.utils.AkkaUtils
import it.pagopa.pdnd.interop.commons.utils.service.UUIDSupplier
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.impl.{EServiceApiMarshallerImpl, EServiceApiServiceImpl}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.{EServiceApi, EServiceApiMarshaller}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.server.Controller
import it.pagopa.pdnd.interop.uservice.catalogmanagement.server.impl.Main
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.CatalogFileManager
import it.pagopa.pdnd.interop.uservice.catalogmanagement.{SpecConfiguration, SpecHelper}
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object CatalogManagementServiceSpec extends MockFactory {

  val mockUUIDSupplier: UUIDSupplier      = mock[UUIDSupplier]
  val mockFileManager: CatalogFileManager = mock[CatalogFileManager]
}

/** Local integration test.
  *
  * Starts a local cluster sharding and invokes REST operations on the eventsourcing entity
  */
class CatalogManagementServiceSpec
    extends ScalaTestWithActorTestKit(SpecConfiguration.config)
    with AnyWordSpecLike
    with SpecConfiguration
    with SpecHelper {

  val payloadMarshaller: EServiceApiMarshaller = new EServiceApiMarshallerImpl

  var controller: Option[Controller]                 = None
  var bindServer: Option[Future[Http.ServerBinding]] = None
  val wrappingDirective: AuthenticationDirective[Seq[(String, String)]] =
    SecurityDirectives.authenticateOAuth2("SecurityRealm", AkkaUtils.Authenticator)

  val sharding: ClusterSharding = ClusterSharding(system)

  val httpSystem: ActorSystem[Any] =
    ActorSystem(Behaviors.ignore[Any], name = system.name, config = system.settings.config)
  implicit val executionContext: ExecutionContextExecutor = httpSystem.executionContext
  implicit val classicSystem: actor.ActorSystem           = httpSystem.classicSystem

  import CatalogManagementServiceSpec._

  override def beforeAll(): Unit = {

    val persistentEntity = Main.buildPersistentEntity()

    Cluster(system).manager ! Join(Cluster(system).selfMember.address)
    sharding.init(persistentEntity)

    val partyApi = new EServiceApi(
      new EServiceApiServiceImpl(system, sharding, persistentEntity, mockUUIDSupplier, mockFileManager),
      payloadMarshaller,
      wrappingDirective
    )

    controller = Some(new Controller(partyApi)(classicSystem))

    controller foreach { controller =>
      bindServer = Some(
        Http()
          .newServerAt("0.0.0.0", 18088)
          .bind(controller.routes)
      )

      Await.result(bindServer.get, 100.seconds)
    }
  }

  override def afterAll(): Unit = {
    println("****** Cleaning resources ********")
    bindServer.foreach(_.foreach(_.unbind()))
    ActorTestKit.shutdown(httpSystem, 5.seconds)
    super.afterAll()
    println("Resources cleaned")
  }

  "Contract verification" should {
    import com.itv.scalapact.http._
    import com.itv.scalapact.json._
    "succeed" in {

      val pactJson: String =
        getPact("src/test/resources/pacts/agreement-process-consumer_catalog-management-provider.json")

      verifyPact
        .withPactSource(pactAsJsonString(pactJson))
        .setupProviderState("given") { case "e-service id" =>
          createEService("24772a3d-e6f2-47f2-96e5-4cbd1e4e8c84")
          createEServiceDescriptor(
            "24772a3d-e6f2-47f2-96e5-4cbd1e4e8c84",
            UUID.fromString("24772a3d-e6f2-47f2-96e5-4cbd1e4e9999")
          )
          val newHeader = "Content-Type" -> "application/json"
          ProviderStateResult(
            result = true,
            req => req.copy(headers = Option(req.headers.fold(Map(newHeader))(_ + newHeader)))
          )
        }
        .runVerificationAgainst("localhost", 18088, 10.seconds)

    }
  }

  "Update descriptor" should {

    "succeed" in {

      val eServiceUuid = "24772a3d-e6f2-47f2-96e5-4cbd1e4e8c85"
      val eService     = createEService(eServiceUuid)
      val descriptorId = UUID.randomUUID()
      val descriptor   = createEServiceDescriptor(eServiceUuid, descriptorId)

      val data =
        """{
          |     "description": "NewDescription"
          |   , "voucherLifespan": 30
          |   , "audience": ["a", "b", "c"]
          |   , "state": "ARCHIVED"
          |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}/descriptors/${descriptor.id.toString}",
            method = HttpMethods.PUT,
            entity = HttpEntity(ContentType(MediaTypes.`application/json`), data),
            headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
          )
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.OK

      val updatedEService = retrieveEService(eServiceUuid)

      updatedEService.descriptors.size shouldBe 1
      val updatedDescriptor = updatedEService.descriptors.head
      updatedDescriptor.description shouldBe Some("NewDescription")
      updatedDescriptor.voucherLifespan shouldBe 30
      updatedDescriptor.audience shouldBe Seq("a", "b", "c")
      updatedDescriptor.state shouldBe EServiceDescriptorState.ARCHIVED
    }

    "fail with 404 code when updating a non-existing descriptor of existing eservice" in {

      val newEService  = createEService("24772a3d-e6f2-47f2-96e5-4cbd1e4e8c00")
      val descriptorId = UUID.randomUUID()
      val _            = createEServiceDescriptor(newEService.id.toString, descriptorId)

      val data =
        """{
          |  "description": "NewDescription",
          |  "audience": ["1"],
          |  "voucherLifespan": 20,
          |  "state": "DRAFT"
          |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${newEService.id.toString}/descriptors/2",
            method = HttpMethods.PUT,
            entity = HttpEntity(ContentType(MediaTypes.`application/json`), data),
            headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
          )
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.NotFound
    }

    "fail with 404 code when updating a descriptor of non-existing eservice" in {

      val data =
        """{
          |  "description": "NewDescription",
          |  "audience": ["1"],
          |  "voucherLifespan": 20,
          |  "state": "DRAFT"
          |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/1/descriptors/2",
            method = HttpMethods.PUT,
            entity = HttpEntity(ContentType(MediaTypes.`application/json`), data),
            headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
          )
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.NotFound
    }

    "fail with 400 code on wrong status value" in {

      val eServiceUuid = "24772a3d-e6f2-47f2-96e5-4cbd1e4e8c01"
      val eService     = createEService(eServiceUuid)
      val descriptorId = UUID.randomUUID()
      val _            = createEServiceDescriptor(eServiceUuid, descriptorId)

      val data =
        """{
          |  "description": "NewDescription",
          |  "audience": ["1"],
          |  "voucherLifespan": 20,
          |  "state": "not_existing_state"
          |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}/descriptors/${descriptorId.toString}",
            method = HttpMethods.PUT,
            entity = HttpEntity(ContentType(MediaTypes.`application/json`), data),
            headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
          )
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.BadRequest

    }
  }

  "Update descriptor state" should {
    "succeed on publish" in {
      val eServiceUuid = UUID.randomUUID()
      val eService     = createEService(eServiceUuid.toString)
      val descriptorId = UUID.randomUUID()
      val descriptor   = createEServiceDescriptor(eServiceUuid.toString, descriptorId)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}/descriptors/${descriptor.id.toString}/publish",
            method = HttpMethods.POST,
            headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
          )
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.NoContent
      val updatedEService = retrieveEService(eServiceUuid.toString)
      updatedEService.descriptors.size shouldBe 1
      val updatedDescriptor = updatedEService.descriptors.head
      updatedDescriptor.state shouldBe EServiceDescriptorState.PUBLISHED
    }

    "succeed on deprecate" in {
      val eServiceUuid = UUID.randomUUID()
      val eService     = createEService(eServiceUuid.toString)
      val descriptorId = UUID.randomUUID()
      val descriptor   = createEServiceDescriptor(eServiceUuid.toString, descriptorId)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}/descriptors/${descriptor.id.toString}/deprecate",
            method = HttpMethods.POST,
            headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
          )
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.NoContent
      val updatedEService = retrieveEService(eServiceUuid.toString)
      updatedEService.descriptors.size shouldBe 1
      val updatedDescriptor = updatedEService.descriptors.head
      updatedDescriptor.state shouldBe EServiceDescriptorState.DEPRECATED
    }

    "succeed on suspend" in {
      val eServiceUuid = UUID.randomUUID()
      val eService     = createEService(eServiceUuid.toString)
      val descriptorId = UUID.randomUUID()
      val descriptor   = createEServiceDescriptor(eServiceUuid.toString, descriptorId)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}/descriptors/${descriptor.id.toString}/suspend",
            method = HttpMethods.POST,
            headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
          )
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.NoContent
      val updatedEService = retrieveEService(eServiceUuid.toString)
      updatedEService.descriptors.size shouldBe 1
      val updatedDescriptor = updatedEService.descriptors.head
      updatedDescriptor.state shouldBe EServiceDescriptorState.SUSPENDED
    }
  }

  "Update an e-service" should {
    "return a modified set of e-service information" in {
      //given an e-service
      val eServiceUuid = UUID.randomUUID().toString
      val eService     = createEService(eServiceUuid)

      //when updated with the following data
      val data =
        """{
          |     "name": "TestName"
          |   , "description": "howdy!"
          |   , "technology": "SOAP"
          |   , "attributes": {"verified": [], "certified": [], "declared": []}
          |}""".stripMargin
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}",
            method = HttpMethods.PUT,
            entity = HttpEntity(ContentType(MediaTypes.`application/json`), data),
            headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
          )
        ),
        Duration.Inf
      )

      //then
      response.status shouldBe StatusCodes.OK
      val updatedEService = retrieveEService(eServiceUuid)

      updatedEService.name shouldBe "TestName"
      updatedEService.description shouldBe "howdy!"
      updatedEService.technology shouldBe EServiceTechnology.SOAP
      updatedEService.attributes.certified.size shouldBe 0
      updatedEService.attributes.declared.size shouldBe 0
      updatedEService.attributes.verified.size shouldBe 0
    }

    "delete an e-service when it has no descriptors" in {
      //given an e-service
      val eServiceUuid = UUID.randomUUID().toString
      val eService     = createEService(eServiceUuid)

      //when deleted
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}",
            method = HttpMethods.DELETE,
            headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
          )
        ),
        Duration.Inf
      )

      //then
      response.status shouldBe StatusCodes.NoContent
    }

    "not delete an e-service when it has at least one descriptor" in {
      //given an e-service
      val eServiceUuid = UUID.randomUUID().toString
      val eService     = createEService(eServiceUuid)
      val _            = createEServiceDescriptor(eServiceUuid, UUID.randomUUID())

      //when deleted
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}",
            method = HttpMethods.DELETE,
            headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
          )
        ),
        Duration.Inf
      )

      //then
      response.status shouldBe StatusCodes.BadRequest
    }
  }

}
