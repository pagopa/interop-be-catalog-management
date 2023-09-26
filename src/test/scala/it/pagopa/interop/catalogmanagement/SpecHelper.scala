package it.pagopa.interop.catalogmanagement

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.Directives.Authenticator
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.Credentials.{Missing, Provided}
import akka.http.scaladsl.unmarshalling.Unmarshal
import it.pagopa.interop.catalogmanagement.api.impl._
import it.pagopa.interop.catalogmanagement.model.AgreementApprovalPolicy.AUTOMATIC
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.catalogmanagement.provider.CatalogManagementServiceSpec.{
  mockOffsetDateTimeSupplier,
  mockUUIDSupplier
}
import it.pagopa.interop.commons.utils.{BEARER, USER_ROLES}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike
import spray.json._

import java.net.InetAddress
import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait SpecHelper extends SpecConfiguration with AnyWordSpecLike with MockFactory {

  final val requestHeaders: Seq[HttpHeader] =
    Seq(
      headers.Authorization(OAuth2BearerToken("1234")),
      headers.RawHeader("X-Correlation-Id", "test-id"),
      headers.`X-Forwarded-For`(RemoteAddress(InetAddress.getByName("127.0.0.1")))
    )

  def createEServiceDescriptor(eserviceId: String, descriptorId: UUID)(implicit
    actorSystem: ActorSystem[_]
  ): EServiceDescriptor = {
    (() => mockUUIDSupplier.get()).expects().returning(descriptorId).once()
    (() => mockOffsetDateTimeSupplier.get()).expects().returning(OffsetDateTime.now()).once()
    val seed = EServiceDescriptorSeed(
      audience = Seq("audience"),
      voucherLifespan = 1984,
      dailyCallsPerConsumer = 2022,
      dailyCallsTotal = 2099,
      description = Some("string"),
      agreementApprovalPolicy = AUTOMATIC,
      attributes = Attributes(
        certified = Seq(Seq(Attribute(id = UUID.randomUUID(), explicitAttributeVerification = false))),
        declared = Seq(Seq(Attribute(id = UUID.randomUUID(), explicitAttributeVerification = false))),
        verified = Seq(
          Seq(
            Attribute(id = UUID.randomUUID(), explicitAttributeVerification = false),
            Attribute(id = UUID.randomUUID(), explicitAttributeVerification = false)
          )
        )
      )
    )

    val data = seed.toJson.compactPrint

    val response = request(s"$serviceURL/eservices/$eserviceId/descriptors", HttpMethods.POST, Some(data))

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EServiceDescriptor], Duration.Inf)
  }

  def createEServiceRiskAnalysis(eserviceId: String, riskanalysisId: UUID, now: OffsetDateTime)(implicit
    actorSystem: ActorSystem[_]
  ): Unit = {
    (() => mockUUIDSupplier.get()).expects().returning(riskanalysisId).repeat(6)
    (() => mockOffsetDateTimeSupplier.get()).expects().returning(now).once()
    val seed = RiskAnalysisSeed(
      name = "name of the new risk analysis",
      riskAnalysisForm = RiskAnalysisFormSeed(
        version = "3.0",
        singleAnswers = Seq(
          RiskAnalysisSingleAnswerSeed(key = "key1", value = None),
          RiskAnalysisSingleAnswerSeed(key = "key2", value = Some("value"))
        ),
        multiAnswers = Seq(
          RiskAnalysisMultiAnswerSeed(key = "key1", values = Seq("value1", "value2", "value3")),
          RiskAnalysisMultiAnswerSeed(key = "key2", values = Seq("value4", "value5", "value6"))
        )
      )
    )

    val response = Await.result(
      Http().singleRequest(
        HttpRequest(
          uri = s"$serviceURL/eservices/$eserviceId/riskanalysis",
          method = HttpMethods.POST,
          entity = HttpEntity(ContentType(MediaTypes.`application/json`), seed.toJson.compactPrint),
          headers = requestHeaders
        )
      ),
      Duration.Inf
    )

    response.status shouldBe StatusCodes.NoContent

    ()
  }

  def updateEServiceRiskAnalysis(eserviceId: String, riskanalysisId: UUID)(implicit
    actorSystem: ActorSystem[_]
  ): EServiceRiskAnalysis = {
    (() => mockUUIDSupplier.get()).expects().returning(riskanalysisId).repeat(5)
    val seed = RiskAnalysisSeed(
      name = "name of the updated risk analysis",
      riskAnalysisForm = RiskAnalysisFormSeed(
        version = "3.0U",
        singleAnswers = Seq(
          RiskAnalysisSingleAnswerSeed(key = "key1U", value = None),
          RiskAnalysisSingleAnswerSeed(key = "key2U", value = Some("valueU"))
        ),
        multiAnswers = Seq(
          RiskAnalysisMultiAnswerSeed(key = "key1U", values = Seq("value1U", "value2U", "value3U")),
          RiskAnalysisMultiAnswerSeed(key = "key2U", values = Seq("value4U", "value5U", "value6U"))
        )
      )
    )

    val response = request(
      s"$serviceURL/eservices/$eserviceId/riskanalysis/${riskanalysisId.toString}",
      HttpMethods.POST,
      Some(seed.toJson.compactPrint)
    )

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EServiceRiskAnalysis], Duration.Inf)
  }

  def createEService(uuid: String)(implicit actorSystem: ActorSystem[_]): EService = {
    (() => mockUUIDSupplier.get()).expects().returning(UUID.fromString(uuid)).once()
    (() => mockOffsetDateTimeSupplier.get()).expects().returning(OffsetDateTime.now()).once()
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

  def retrieveEService(uuid: String)(implicit actorSystem: ActorSystem[_]): EService = {

    val response = request(s"$serviceURL/eservices/$uuid", HttpMethods.GET)

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EService], Duration.Inf)

  }

  def request(uri: String, method: HttpMethod, data: Option[String] = None)(implicit
    actorSystem: ActorSystem[_]
  ): HttpResponse = {
    val httpRequest: HttpRequest = HttpRequest(uri = uri, method = method, headers = requestHeaders)

    val requestWithEntity: HttpRequest =
      data.fold(httpRequest)(d => httpRequest.withEntity(HttpEntity(ContentTypes.`application/json`, d)))

    Await.result(Http().singleRequest(requestWithEntity), Duration.Inf)
  }
}

//mocks admin user role rights for every call
object AdminMockAuthenticator extends Authenticator[Seq[(String, String)]] {
  override def apply(credentials: Credentials): Option[Seq[(String, String)]] = {
    credentials match {
      case Provided(identifier) => Some(Seq(BEARER -> identifier, USER_ROLES -> "admin"))
      case Missing              => None
    }
  }
}
