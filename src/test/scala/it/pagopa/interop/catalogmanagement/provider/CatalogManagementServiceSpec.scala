package it.pagopa.interop.catalogmanagement.provider

import akka.actor
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.cluster.typed.{Cluster, Join}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.directives.{AuthenticationDirective, SecurityDirectives}
import it.pagopa.interop.catalogmanagement.api.impl.{EServiceApiMarshallerImpl, EServiceApiServiceImpl}
import it.pagopa.interop.catalogmanagement.api.{EServiceApi, EServiceApiMarshaller}
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.catalogmanagement.model.persistence.{CatalogPersistentBehavior, Command}
import it.pagopa.interop.catalogmanagement.server.Controller
import it.pagopa.interop.catalogmanagement.server.impl.Main.behaviorFactory
import it.pagopa.interop.catalogmanagement.service.CatalogFileManager
import it.pagopa.interop.catalogmanagement.{AdminMockAuthenticator, SpecConfiguration, SpecHelper}
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import akka.http.scaladsl.unmarshalling.Unmarshal
import it.pagopa.interop.catalogmanagement.api.impl._

object CatalogManagementServiceSpec extends MockFactory {

  val mockUUIDSupplier: UUIDSupplier                     = mock[UUIDSupplier]
  val mockOffsetDateTimeSupplier: OffsetDateTimeSupplier = mock[OffsetDateTimeSupplier]
  val mockFileManager: CatalogFileManager                = mock[CatalogFileManager]
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

  val payloadMarshaller: EServiceApiMarshaller = EServiceApiMarshallerImpl

  var controller: Option[Controller]                                    = None
  var bindServer: Option[Future[Http.ServerBinding]]                    = None
  val wrappingDirective: AuthenticationDirective[Seq[(String, String)]] =
    SecurityDirectives.authenticateOAuth2("SecurityRealm", AdminMockAuthenticator)

  val sharding: ClusterSharding = ClusterSharding(system)

  val httpSystem: ActorSystem[Any]                        =
    ActorSystem(Behaviors.ignore[Any], name = system.name, config = system.settings.config)
  implicit val executionContext: ExecutionContextExecutor = httpSystem.executionContext
  implicit val classicSystem: actor.ActorSystem           = httpSystem.classicSystem

  import CatalogManagementServiceSpec._

  override def beforeAll(): Unit = {

    val persistentEntity: Entity[Command, ShardingEnvelope[Command]] =
      Entity(CatalogPersistentBehavior.TypeKey)(behaviorFactory)

    Cluster(system).manager ! Join(Cluster(system).selfMember.address)
    sharding.init(persistentEntity)

    val partyApi = new EServiceApi(
      new EServiceApiServiceImpl(
        system,
        sharding,
        persistentEntity,
        mockUUIDSupplier,
        mockOffsetDateTimeSupplier,
        mockFileManager
      ),
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
    bindServer.foreach(_.foreach(_.unbind()))
    ActorTestKit.shutdown(httpSystem, 5.seconds)
    super.afterAll()
  }

  "Create a descriptor with attributes" should {
    "succeed" in {
      val eServiceUuid = "24772a3d-e6f2-47f2-96e5-4cbd1e4e8c45"
      val eService     = createEService(eServiceUuid)

      val attributeUUID = UUID.randomUUID()

      (() => mockUUIDSupplier.get()).expects().returning(UUID.randomUUID()).once()
      (() => mockOffsetDateTimeSupplier.get()).expects().returning(OffsetDateTime.now()).once()

      val data = s"""{
                    |     "description": "string"
                    |   , "audience": ["audience"]
                    |   , "voucherLifespan": 1984
                    |   , "dailyCallsPerConsumer": 2022
                    |   , "dailyCallsTotal": 2099
                    |   , "agreementApprovalPolicy": "AUTOMATIC"
                    |   , "attributes": {
                    |       "certified": [[
                    |         { "id": "${attributeUUID}", "explicitAttributeVerification": true}
                    |       ]],
                    |       "verified": [],
                    |       "declared": []
                    |     }
                    |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}/descriptors",
            method = HttpMethods.POST,
            entity = HttpEntity(ContentType(MediaTypes.`application/json`), data),
            headers = requestHeaders
          )
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.OK
      val descriptor = Await.result(Unmarshal(response).to[EServiceDescriptor], Duration.Inf)
      descriptor.attributes shouldBe Attributes(
        certified = Seq(Seq(Attribute(attributeUUID, true))),
        declared = Nil,
        verified = Nil
      )
    }

    "fail if there are no attributes" in {
      val eServiceUuid = "24772a2d-e6f2-47f2-96e5-4cbd1e4e8c45"
      val eService     = createEService(eServiceUuid)

      val data =
        s"""{
           |     "description": "string"
           |   , "audience": ["audience"]
           |   , "voucherLifespan": 1984
           |   , "dailyCallsPerConsumer": 2022
           |   , "dailyCallsTotal": 2099
           |   , "agreementApprovalPolicy": "AUTOMATIC"
           |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}/descriptors",
            method = HttpMethods.POST,
            entity = HttpEntity(ContentType(MediaTypes.`application/json`), data),
            headers = requestHeaders
          )
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.BadRequest
    }
  }

  "Create a risk Analysis" should {
    "succeed" in {
      val eServiceUuid = UUID.randomUUID().toString()
      val eService     = createEService(eServiceUuid)

      val data =
        """{"name":"string","riskAnalysisForm":{"multiAnswers":[{"key":"1","values":["val1","val2","val3"]}],"singleAnswers":[{"key":"1","value":"value1"}],"version":"2.0"}}"""

      (() => mockUUIDSupplier.get()).expects().returning(UUID.randomUUID()).repeat(4)
      (() => mockOffsetDateTimeSupplier.get()).expects().returning(OffsetDateTime.now()).once()

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}/riskanalysis",
            method = HttpMethods.POST,
            entity = HttpEntity(ContentType(MediaTypes.`application/json`), data),
            headers = requestHeaders
          )
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.NoContent
    }
  }

  "Update Risk Analysis" should {
    "succeed" in {
      val eServiceUuid                       = UUID.randomUUID().toString()
      createEService(eServiceUuid)
      val riskAnalysisId                     = UUID.randomUUID()
      createEServiceRiskAnalysis(eServiceUuid, riskAnalysisId, OffsetDateTime.now())
      val riskAnalysis: EServiceRiskAnalysis = updateEServiceRiskAnalysis(eServiceUuid, riskAnalysisId)

      riskAnalysis.name shouldBe ("name of the updated risk analysis")
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
          |   , "dailyCallsPerConsumer": 30000
          |   , "dailyCallsTotal": 900
          |   , "agreementApprovalPolicy": "MANUAL"
          |   , "audience": ["a", "b", "c"]
          |   , "state": "ARCHIVED"
          |   , "attributes": {"verified": [], "certified": [], "declared": []}
          |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}/descriptors/${descriptor.id.toString}",
            method = HttpMethods.PUT,
            entity = HttpEntity(ContentType(MediaTypes.`application/json`), data),
            headers = requestHeaders
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
      updatedDescriptor.dailyCallsPerConsumer shouldBe 30000
      updatedDescriptor.dailyCallsTotal shouldBe 900
      updatedDescriptor.audience shouldBe Seq("a", "b", "c")
      updatedDescriptor.state shouldBe EServiceDescriptorState.ARCHIVED
      updatedDescriptor.agreementApprovalPolicy shouldBe AgreementApprovalPolicy.MANUAL
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
          |  "dailyCallsPerConsumer": 30000,
          |  "dailyCallsTotal": 30000,
          |  "agreementApprovalPolicy": "MANUAL",
          |  "state": "DRAFT",
          |   "attributes": {"verified": [], "certified": [], "declared": []}
          |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${newEService.id.toString}/descriptors/2",
            method = HttpMethods.PUT,
            entity = HttpEntity(ContentType(MediaTypes.`application/json`), data),
            headers = requestHeaders
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
          |  "dailyCallsPerConsumer": 30000,
          |  "dailyCallsTotal": 30000,
          |  "agreementApprovalPolicy": "MANUAL",
          |  "state": "DRAFT",
          |  "attributes": {"verified": [], "certified": [], "declared": []}
          |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/1/descriptors/2",
            method = HttpMethods.PUT,
            entity = HttpEntity(ContentType(MediaTypes.`application/json`), data),
            headers = requestHeaders
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
          |  "dailyCallsPerConsumer": 30000,
          |  "dailyCallsTotal": 30000,
          |  "agreementApprovalPolicy": "MANUAL",
          |  "state": "not_existing_state"
          |}""".stripMargin

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}/descriptors/${descriptorId.toString}",
            method = HttpMethods.PUT,
            entity = HttpEntity(ContentType(MediaTypes.`application/json`), data),
            headers = requestHeaders
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

      (() => mockOffsetDateTimeSupplier.get()).expects().returning(OffsetDateTime.now()).repeat(2)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}/descriptors/${descriptor.id.toString}/publish",
            method = HttpMethods.POST,
            headers = requestHeaders
          )
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.NoContent
      val updatedEService   = retrieveEService(eServiceUuid.toString)
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
            headers = requestHeaders
          )
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.NoContent
      val updatedEService   = retrieveEService(eServiceUuid.toString)
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
            headers = requestHeaders
          )
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.NoContent
      val updatedEService   = retrieveEService(eServiceUuid.toString)
      updatedEService.descriptors.size shouldBe 1
      val updatedDescriptor = updatedEService.descriptors.head
      updatedDescriptor.state shouldBe EServiceDescriptorState.SUSPENDED
    }
  }

  "Update an e-service" should {
    "return a modified set of e-service information" in {
      // given an e-service
      val eServiceUuid = UUID.randomUUID().toString
      val eService     = createEService(eServiceUuid)

      // when updated with the following data
      val data     =
        """{
          |     "name": "TestName"
          |   , "description": "howdy!"
          |   , "technology": "SOAP"
          |   , "mode": "DELIVER"
          |}""".stripMargin
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}",
            method = HttpMethods.PUT,
            entity = HttpEntity(ContentType(MediaTypes.`application/json`), data),
            headers = requestHeaders
          )
        ),
        Duration.Inf
      )

      // then
      response.status shouldBe StatusCodes.OK
      val updatedEService = retrieveEService(eServiceUuid)

      updatedEService.name shouldBe "TestName"
      updatedEService.description shouldBe "howdy!"
      updatedEService.technology shouldBe EServiceTechnology.SOAP
    }

    "delete an e-service when it has no descriptors" in {
      // given an e-service
      val eServiceUuid = UUID.randomUUID().toString
      val eService     = createEService(eServiceUuid)

      // when deleted
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}",
            method = HttpMethods.DELETE,
            headers = requestHeaders
          )
        ),
        Duration.Inf
      )

      // then
      response.status shouldBe StatusCodes.NoContent
    }

    "not delete an e-service when it has at least one descriptor" in {
      // given an e-service
      val eServiceUuid = UUID.randomUUID().toString
      val eService     = createEService(eServiceUuid)
      val _            = createEServiceDescriptor(eServiceUuid, UUID.randomUUID())

      // when deleted
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/${eService.id.toString}",
            method = HttpMethods.DELETE,
            headers = requestHeaders
          )
        ),
        Duration.Inf
      )

      // then
      response.status shouldBe StatusCodes.Conflict
    }
  }
}
