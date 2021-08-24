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
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.impl.{EServiceApiMarshallerImpl, EServiceApiServiceImpl}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.{EServiceApi, EServiceApiMarshaller}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common.system.Authenticator
import it.pagopa.pdnd.interop.uservice.catalogmanagement.server.Controller
import it.pagopa.pdnd.interop.uservice.catalogmanagement.server.impl.Main
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.{FileManager, UUIDSupplier}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.{SpecConfiguration, SpecHelper}
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object CatalogManagementServiceSpec extends MockFactory {

  val mockUUIDSupplier: UUIDSupplier = mock[UUIDSupplier]
  val mockFileManager: FileManager   = mock[FileManager]
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
    SecurityDirectives.authenticateOAuth2("SecurityRealm", Authenticator)

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

  "Processing a request payload" should {

    import com.itv.scalapact.http._
    import com.itv.scalapact.json._

    "be able to verify it's contracts" in {
      verifyPact
        .withPactSource(loadFromLocal("src/test/resources/pacts"))
        .setupProviderState("given") { case "e-service id" =>
          createEService("24772a3d-e6f2-47f2-96e5-4cbd1e4e8c84")
          val newHeader = "Content-Type" -> "application/json"
          ProviderStateResult(
            result = true,
            req => req.copy(headers = Option(req.headers.fold(Map(newHeader))(_ + newHeader)))
          )
        }
        .runVerificationAgainst("localhost", 18088, 10.seconds)
    }

    "update existing descriptor" in {

      val eServiceUuid = "24772a3d-e6f2-47f2-96e5-4cbd1e4e8c85"
      val eService     = createEService(eServiceUuid)
      val descriptor   = eService.descriptors.head

      val data =
        """{
          |  "description": "NewDescription",
          |  "status": "archived"
          |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}/descriptors/${descriptor.id.toString}",
            method = HttpMethods.PATCH,
            entity = HttpEntity(ContentTypes.`application/json`, data),
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
      updatedDescriptor.status shouldBe "archived"
    }

    "fail with 404 code when updating a non-existing descriptor of existing eservice" in {

      val newEService = createEService("24772a3d-e6f2-47f2-96e5-4cbd1e4e8c00")

      val data =
        """{
          |  "description": "NewDescription",
          |  "status": "draft"
          |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${newEService.id.toString}/descriptors/2",
            method = HttpMethods.PATCH,
            entity = HttpEntity(ContentTypes.`application/json`, data),
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
          |  "status": "draft"
          |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/1/descriptors/2",
            method = HttpMethods.PATCH,
            entity = HttpEntity(ContentTypes.`application/json`, data),
            headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
          )
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.NotFound

    }
  }

}
