package it.pagopa.interop.catalogmanagement

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.Authenticator
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.Credentials.{Missing, Provided}
import akka.http.scaladsl.unmarshalling.Unmarshal
import it.pagopa.interop.catalogmanagement.api.impl._
import it.pagopa.interop.catalogmanagement.model.{EService, EServiceDescriptor}
import it.pagopa.interop.catalogmanagement.provider.CatalogManagementServiceSpec.mockUUIDSupplier
import it.pagopa.interop.commons.utils.{BEARER, USER_ROLES}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.net.InetAddress
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
    (() => mockUUIDSupplier.get).expects().returning(descriptorId).once()
    val data =
      """
        |{
        |	"audience": ["audience"],
        |	"voucherLifespan": 1984,
        |	"dailyCallsPerConsumer": 2022,
        |	"dailyCallsTotal": 2099,
        |	"description": "string"
        |	}
        |""".stripMargin

    val response = Await.result(
      Http().singleRequest(
        HttpRequest(
          uri = s"$serviceURL/eservices/${eserviceId}/descriptors",
          method = HttpMethods.POST,
          entity = HttpEntity(ContentTypes.`application/json`, data),
          headers = requestHeaders
        )
      ),
      Duration.Inf
    )

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EServiceDescriptor], Duration.Inf)
  }

  def createEService(uuid: String)(implicit actorSystem: ActorSystem[_]): EService = {
    (() => mockUUIDSupplier.get).expects().returning(UUID.fromString(uuid)).once()

    val data =
      """
        |{
        |	"producerId": "24772a3d-e6f2-47f2-96e5-4cbd1e4e9999",
        |	"name": "string",
        |	"description": "string",
        |	"audience": [
        |		"pippo"
        |	],
        |	"technology": "REST",
        |	"voucherLifespan": 124,
        |	"attributes": {
        |		"certified": [{
        |			"single": {
        |				"id": "1234",
        |				"explicitAttributeVerification" : false
        |			}
        |		}],
        |		"declared": [{
        |			"single": {
        |				"id": "1234",
        |				"explicitAttributeVerification": false
        |			}
        |		}],
        |		"verified": [{
        |			"group": [{
        |					"id": "1234",
        |					"explicitAttributeVerification": false
        |				},
        |				{
        |					"id": "5555",
        |					"explicitAttributeVerification": false
        |				}
        |			]
        |		}]
        |	}
        |}
        |""".stripMargin

    val response = Await.result(
      Http().singleRequest(
        HttpRequest(
          uri = s"$serviceURL/eservices",
          method = HttpMethods.POST,
          entity = HttpEntity(ContentTypes.`application/json`, data),
          headers = requestHeaders
        )
      ),
      Duration.Inf
    )

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EService], Duration.Inf)
  }

  def retrieveEService(uuid: String)(implicit actorSystem: ActorSystem[_]): EService = {

    val response = Await.result(
      Http().singleRequest(
        HttpRequest(uri = s"$serviceURL/eservices/$uuid", method = HttpMethods.GET, headers = requestHeaders)
      ),
      Duration.Inf
    )

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EService], Duration.Inf)
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
