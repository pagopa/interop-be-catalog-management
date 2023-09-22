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
