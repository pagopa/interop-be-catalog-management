package it.pagopa.interop.catalogmanagement

import akka.actor
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.cluster.typed.{Cluster, Join}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.directives.{AuthenticationDirective, FileInfo, SecurityDirectives}
import akka.http.scaladsl.unmarshalling.Unmarshal
import it.pagopa.interop.catalogmanagement.api._
import it.pagopa.interop.catalogmanagement.api.impl._
import it.pagopa.interop.catalogmanagement.common.system.ApplicationConfiguration
import it.pagopa.interop.catalogmanagement.model.AgreementApprovalPolicy.AUTOMATIC
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.catalogmanagement.model.persistence.{CatalogPersistentBehavior, Command}
import it.pagopa.interop.catalogmanagement.server.Controller
import it.pagopa.interop.catalogmanagement.server.impl.Dependencies
import it.pagopa.interop.catalogmanagement.service.CatalogFileManager
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import org.scalamock.scalatest.MockFactory
import org.scalatest.Assertion
import spray.json._

import java.io.File
import java.net.InetAddress
import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

trait ItSpecHelper
    extends ItSpecConfiguration
    with ItCqrsSpec
    with MockFactory
    with SprayJsonSupport
    with DefaultJsonProtocol
    with Dependencies {
  self: ScalaTestWithActorTestKit =>

  val bearerToken: String                   = "token"
  final val requestHeaders: Seq[HttpHeader] =
    Seq(
      headers.Authorization(OAuth2BearerToken("token")),
      headers.RawHeader("X-Correlation-Id", "test-id"),
      headers.`X-Forwarded-For`(RemoteAddress(InetAddress.getByName("127.0.0.1")))
    )

  val mockUUIDSupplier: UUIDSupplier               = mock[UUIDSupplier]
  val mockDateTimeSupplier: OffsetDateTimeSupplier = () => ItSpecData.timestamp
  val mockFileManager: CatalogFileManager          = mock[CatalogFileManager]

  val eServiceApiMarshaller: EServiceApiMarshaller = EServiceApiMarshallerImpl

  var controller: Option[Controller]                 = None
  var bindServer: Option[Future[Http.ServerBinding]] = None

  val wrappingDirective: AuthenticationDirective[Seq[(String, String)]] =
    SecurityDirectives.authenticateOAuth2("SecurityRealm", AdminMockAuthenticator)

  val sharding: ClusterSharding = ClusterSharding(system)

  val httpSystem: ActorSystem[Any]                        =
    ActorSystem(Behaviors.ignore[Any], name = system.name, config = system.settings.config)
  implicit val executionContext: ExecutionContextExecutor = httpSystem.executionContext
  val classicSystem: actor.ActorSystem                    = httpSystem.classicSystem

  override def startServer(): Unit = {
    val persistentEntity: Entity[Command, ShardingEnvelope[Command]] =
      Entity(CatalogPersistentBehavior.TypeKey)(behaviorFactory)

    Cluster(system).manager ! Join(Cluster(system).selfMember.address)
    sharding.init(persistentEntity)

    val eServiceApi =
      new EServiceApi(
        new EServiceApiServiceImpl(
          system,
          sharding,
          persistentEntity,
          mockUUIDSupplier,
          mockDateTimeSupplier,
          mockFileManager
        ),
        eServiceApiMarshaller,
        wrappingDirective
      )

    if (ApplicationConfiguration.projectionsEnabled) initProjections(system.executionContext)

    controller = Some(new Controller(eServiceApi)(classicSystem))

    controller foreach { controller =>
      bindServer = Some(
        Http()(classicSystem)
          .newServerAt("0.0.0.0", 18088)
          .bind(controller.routes)
      )

      Await.result(bindServer.get, 100.seconds)
    }
  }

  override def shutdownServer(): Unit = {
    bindServer.foreach(_.foreach(_.unbind()))
    ActorTestKit.shutdown(httpSystem, 5.seconds)
  }

  def compareCatalogItems(item1: CatalogItem, item2: CatalogItem): Assertion =
    sortEServiceArrayFields(item1) shouldBe sortEServiceArrayFields(item2)

  def sortEServiceArrayFields(eService: CatalogItem): CatalogItem =
    eService.copy(descriptors = eService.descriptors.sortBy(_.id).map(desc => desc.copy(docs = desc.docs.sortBy(_.id))))

  def createEServiceRiskAnalysis(eServiceId: UUID, riskAnalysisId: UUID): EServiceRiskAnalysis = {
    (() => mockUUIDSupplier.get()).expects().returning(riskAnalysisId).once()

    val seed = RiskAnalysisSeed(
      name = "name",
      riskAnalysisForm = RiskAnalysisFormSeed(version = "2.0", singleAnswers = Seq.empty, multiAnswers = Seq.empty)
    )

    val data = seed.toJson.compactPrint

    val response = request(s"$serviceURL/eservices/$eServiceId/riskAnalysis", HttpMethods.POST, Some(data))

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EServiceRiskAnalysis], Duration.Inf)
  }

  def createEServiceDescriptor(eServiceId: UUID, descriptorId: UUID): EServiceDescriptor = {
    (() => mockUUIDSupplier.get()).expects().returning(descriptorId).once()

    val seed = EServiceDescriptorSeed(
      audience = Seq("audience"),
      voucherLifespan = 1984,
      dailyCallsPerConsumer = 2022,
      dailyCallsTotal = 2099,
      description = Some("string"),
      agreementApprovalPolicy = AUTOMATIC,
      attributes = Attributes(Seq.empty, Seq.empty, Seq.empty)
    )

    val data = seed.toJson.compactPrint

    val response = request(s"$serviceURL/eservices/$eServiceId/descriptors", HttpMethods.POST, Some(data))

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EServiceDescriptor], Duration.Inf)
  }

  def createEService(uuid: UUID): EService = {
    (() => mockUUIDSupplier.get()).expects().returning(uuid).once()

    val seed = EServiceSeed(
      producerId = UUID.randomUUID(),
      name = "string",
      description = "string",
      technology = EServiceTechnology.REST,
      mode = EServiceMode.DELIVER
    )

    val data = seed.toJson.compactPrint

    val response = request(s"$serviceURL/eservices", HttpMethods.POST, Some(data))

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EService], Duration.Inf)
  }

  def createDescriptorDocument(
    eServiceId: UUID,
    descriptorId: UUID,
    kind: String,
    documentId: UUID = UUID.randomUUID()
  ): EService = {
    val doc = CatalogDocument(
      id = documentId,
      name = "name",
      contentType = "application/yaml",
      prettyName = "prettyName",
      path = "path",
      checksum = "trustme",
      uploadDate = OffsetDateTime.now()
    )

    (() => mockUUIDSupplier.get()).expects().returning(documentId).once()
    (mockFileManager
      .store(_: UUID, _: String, _: (FileInfo, File))(_: ExecutionContext))
      .expects(*, *, *, *)
      .returning(Future.successful(doc))
      .once()

    val file = new File("src/it/resources/apis.yaml")

    val formData =
      Multipart.FormData(
        Multipart.FormData.BodyPart.fromPath("doc", MediaTypes.`application/octet-stream`, file.toPath),
        Multipart.FormData.BodyPart.Strict("kind", kind),
        Multipart.FormData.BodyPart.Strict("prettyName", file.getName)
      )

    val response =
      Http()
        .singleRequest(
          HttpRequest(
            uri = s"$serviceURL/eservices/$eServiceId/descriptors/$descriptorId/documents",
            method = HttpMethods.POST,
            entity = formData.toEntity,
            headers = requestHeaders
          )
        )
        .futureValue

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EService], Duration.Inf)
  }

  def deleteDescriptorDocument(eServiceId: UUID, descriptorId: UUID, documentId: UUID): Unit = {
    (mockFileManager
      .delete(_: String))
      .expects(*)
      .returning(Future.successful(true))
      .once()

    val response =
      request(s"$serviceURL/eservices/$eServiceId/descriptors/$descriptorId/documents/$documentId", HttpMethods.DELETE)
    response.status shouldBe StatusCodes.NoContent
    ()
  }

  def updateDescriptorDocument(eServiceId: UUID, descriptorId: UUID, documentId: UUID): EServiceDoc = {
    val seed = UpdateEServiceDescriptorDocumentSeed(prettyName = "new prettyName")

    val data = seed.toJson.prettyPrint

    val response = request(
      s"$serviceURL/eservices/$eServiceId/descriptors/$descriptorId/documents/$documentId/update",
      HttpMethods.POST,
      Some(data)
    )
    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EServiceDoc], Duration.Inf)
  }

  def deleteEService(eServiceId: UUID): Unit = {
    val response = request(s"$serviceURL/eservices/$eServiceId", HttpMethods.DELETE)
    response.status shouldBe StatusCodes.NoContent
    ()
  }

  def deleteDescriptor(eServiceId: UUID, descriptorId: UUID): Unit = {
    val response = request(s"$serviceURL/eservices/$eServiceId/descriptors/$descriptorId", HttpMethods.DELETE)
    response.status shouldBe StatusCodes.NoContent
    ()
  }

  def cloneEService(eServiceId: UUID, descriptorId: UUID): EService = {
    (() => mockUUIDSupplier.get()).expects().returning(UUID.randomUUID()).once()
    (() => mockUUIDSupplier.get()).expects().returning(UUID.randomUUID()).once()

    val response = request(s"$serviceURL/eservices/$eServiceId/descriptors/$descriptorId/clone", HttpMethods.POST)

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EService], Duration.Inf)
  }

  def updateEService(eServiceId: UUID): EService = {

    val seed =
      UpdateEServiceSeed(
        name = "New name",
        description = "New description",
        technology = EServiceTechnology.SOAP,
        mode = EServiceMode.DELIVER
      )

    val data = seed.toJson.compactPrint

    val response = request(s"$serviceURL/eservices/$eServiceId", HttpMethods.PUT, Some(data))

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EService], Duration.Inf)
  }

  def updateDescriptor(eServiceId: UUID, descriptorId: UUID): EService = {

    val seed = UpdateEServiceDescriptorSeed(
      description = Some("New description"),
      state = EServiceDescriptorState.ARCHIVED,
      audience = Seq("newAud1", "newAud2"),
      voucherLifespan = 987654,
      dailyCallsPerConsumer = 556644,
      dailyCallsTotal = 884455,
      agreementApprovalPolicy = AUTOMATIC,
      attributes = Attributes(Seq.empty, Seq.empty, Seq.empty)
    )

    val data = seed.toJson.compactPrint

    val response = request(s"$serviceURL/eservices/$eServiceId/descriptors/$descriptorId", HttpMethods.PUT, Some(data))

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EService], Duration.Inf)
  }

  def publishDescriptor(eServiceId: UUID, descriptorId: UUID): Unit = {
    val response = request(s"$serviceURL/eservices/$eServiceId/descriptors/$descriptorId/publish", HttpMethods.POST)
    response.status shouldBe StatusCodes.NoContent
    ()
  }

  def retrieveEService(uuid: String): EService = {

    val response = request(s"$serviceURL/eservices/$uuid", HttpMethods.GET)

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EService], Duration.Inf)

  }

  def request(uri: String, method: HttpMethod, data: Option[String] = None): HttpResponse = {
    val httpRequest: HttpRequest = HttpRequest(uri = uri, method = method, headers = requestHeaders)

    val requestWithEntity: HttpRequest =
      data.fold(httpRequest)(d => httpRequest.withEntity(HttpEntity(ContentTypes.`application/json`, d)))

    Await.result(Http().singleRequest(requestWithEntity), Duration.Inf)
  }
}
