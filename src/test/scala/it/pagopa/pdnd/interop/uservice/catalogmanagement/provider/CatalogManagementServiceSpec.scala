package it.pagopa.pdnd.interop.uservice.catalogmanagement.provider

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{Cluster, Join}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.directives.{AuthenticationDirective, SecurityDirectives}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.itv.scalapact.ScalaPactVerify._
import com.itv.scalapact.shared.ProviderStateResult
import com.typesafe.config.ConfigFactory
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.impl.{EServiceApiMarshallerImpl, EServiceApiServiceImpl}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.{EServiceApi, EServiceApiMarshaller}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common.system.Authenticator
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.EService
import it.pagopa.pdnd.interop.uservice.catalogmanagement.server.Controller
import it.pagopa.pdnd.interop.uservice.catalogmanagement.server.impl.Main
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.{FileManager, UUIDSupplier}
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpecLike
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.impl._

import java.util.UUID
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object CatalogManagementServiceSpec extends MockFactory {

  System.setProperty("AWS_ACCESS_KEY_ID", "foo")
  System.setProperty("AWS_SECRET_ACCESS_KEY", "bar")

  val url = "http://localhost:18088/pdnd-interop-uservice-catalog-management/0.0.1"

  val testData = ConfigFactory.parseString(s"""
      akka.actor.provider = cluster

      akka.remote.classic.netty.tcp.port = 0
      akka.remote.artery.canonical.port = 0
      akka.remote.artery.canonical.hostname = 127.0.0.1

      akka.cluster.jmx.multi-mbeans-in-same-jvm = on

      akka.cluster.sharding.number-of-shards = 10

      akka.coordinated-shutdown.terminate-actor-system = off
      akka.coordinated-shutdown.run-by-actor-system-terminate = off
      akka.coordinated-shutdown.run-by-jvm-shutdown-hook = off
      akka.cluster.run-coordinated-shutdown-when-down = off
    """)

  val config = ConfigFactory.parseResourcesAnySyntax("test")
    .withFallback(testData)


  val mockUUIDSupplier: UUIDSupplier = mock[UUIDSupplier]
  val mockFileManager: FileManager = mock[FileManager]
}

/**
  * Local integration test.
  *
  * Starts a local cluster sharding and invokes REST operations on the eventsourcing entity
  */
class CatalogManagementServiceSpec
    extends ScalaTestWithActorTestKit(CatalogManagementServiceSpec.config)
    with AnyWordSpecLike {

  val payloadMarshaller: EServiceApiMarshaller = new EServiceApiMarshallerImpl

  var controller: Option[Controller]        = None
  var bindServer: Option[Future[Http.ServerBinding]] = None
  val wrappingDirective: AuthenticationDirective[Seq[(String, String)]] = SecurityDirectives.authenticateOAuth2("SecurityRealm", Authenticator)

  val sharding = ClusterSharding(system)

  val httpSystem = ActorSystem(Behaviors.ignore[Any], name = system.name, config = system.settings.config)
  implicit val executionContext: ExecutionContextExecutor = httpSystem.executionContext
  implicit val classicSystem = httpSystem.classicSystem

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

  "Processing a request payload" should {

    import com.itv.scalapact.http._
    import com.itv.scalapact.json._

    "be able to verify it's contracts" in {
      verifyPact
        .withPactSource(loadFromLocal("src/test/resources/pacts"))
        .setupProviderState("given") {
          case "e-service id" =>
            createEService("24772a3d-e6f2-47f2-96e5-4cbd1e4e8c84")
            val newHeader = "Content-Type" -> "application/json"
            ProviderStateResult(true, req => req.copy(headers = Option(req.headers.fold(Map(newHeader))(_ + newHeader))))
        }
        .runVerificationAgainst("localhost", 18088, 10.seconds)
    }

    "update existing descriptor" in {

      val eServiceUuid = "24772a3d-e6f2-47f2-96e5-4cbd1e4e8c85"
      val eService = createEService(eServiceUuid)
      val descriptor = eService.descriptors.head

      val data =
        """{
          |  "description": "NewDescription",
          |  "status": "archived"
          |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/eservices/${eService.id.toString}/descriptors/${descriptor.id.toString}",
            method = HttpMethods.PATCH,
            entity = HttpEntity(ContentTypes.`application/json`, data),
            headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
          )),
        Duration.Inf)

      response.status shouldBe StatusCodes.OK

      val updatedEService = retrieveEService(eServiceUuid)

      updatedEService.descriptors.size shouldBe 1
      val updatedDescriptor = updatedEService.descriptors.head
      updatedDescriptor.description shouldBe Some("NewDescription")
      updatedDescriptor.status shouldBe "archived"
    }

    "fail with 404 code when updating a non-existing descriptor of existing eservice" in {

      val newEService = createEService("24772a3d-e6f2-47f2-96e5-4cbd1e4e8c84")

      val data =
        """{
          |  "description": "NewDescription",
          |  "status": "draft"
          |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/eservices/${newEService.id.toString}/descriptors/2",
            method = HttpMethods.PATCH,
            entity = HttpEntity(ContentTypes.`application/json`, data),
            headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
          )),
        Duration.Inf)

      response.status shouldBe StatusCodes.NotFound
    }

    "fail with 404 code when updating a descriptor of non-existing eservice" in {

      val data =
        """{
          |  "description": "NewDescription",
          |  "status": "draft"
          |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/eservices/1/descriptors/2",
            method = HttpMethods.PATCH,
            entity = HttpEntity(ContentTypes.`application/json`, data),
            headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
          )),
        Duration.Inf)

      response.status shouldBe StatusCodes.NotFound

    }
  }

  private def createEService(uuid: String): EService = {
    //mocking id twice since UUID supplier is invoked two times in the flow
    (() => mockUUIDSupplier.get).expects().returning(UUID.fromString(uuid)).twice()

    val data =
      """
        |{
        |          "producerId" : "24772a3d-e6f2-47f2-96e5-4cbd1e4e9999",
        |          "name" : "string",
        |          "description" : "string",
        |          "audience" : [
        |            "pippo"
        |          ],
        |          "technology" : "REST",
        |          "voucherLifespan" : 124,
        |          "attributes" : {
        |            "certified" : [
        |              {
        |                "simple" : "1234"
        |              }
        |            ],
        |            "declared" : [
        |              {
        |                "simple" : "1234"
        |              }
        |            ],
        |            "verified" : [
        |              {
        |                "group" : [
        |                  "1234",
        |                  "5555"
        |                ]
        |              }
        |            ]
        |          }
        |        }
        |""".stripMargin

    val response = Await.result(
      Http().singleRequest(
        HttpRequest(uri = s"$url/eservices",
          method = HttpMethods.POST,
          entity = HttpEntity(ContentTypes.`application/json`, data),
          headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
        )),
      Duration.Inf)

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EService], Duration.Inf)
  }

  private def retrieveEService(uuid: String): EService = {

    val response = Await.result(
      Http().singleRequest(
        HttpRequest(uri = s"$url/eservices/$uuid",
          method = HttpMethods.GET,
          headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
        )),
      Duration.Inf)

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EService], Duration.Inf)
  }

}