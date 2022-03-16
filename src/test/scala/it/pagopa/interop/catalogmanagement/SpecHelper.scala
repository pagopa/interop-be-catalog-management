package it.pagopa.interop.catalogmanagement

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import it.pagopa.interop.catalogmanagement.api.impl._
import it.pagopa.interop.catalogmanagement.model.{EService, EServiceDescriptor}
import it.pagopa.interop.catalogmanagement.provider.CatalogManagementServiceSpec.mockUUIDSupplier
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait SpecHelper extends SpecConfiguration with AnyWordSpecLike with MockFactory {

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
        |	"throughput": 2099,
        |	"description": "string"
        |	}
        |""".stripMargin

    val response = Await.result(
      Http().singleRequest(
        HttpRequest(
          uri = s"$serviceURL/eservices/${eserviceId}/descriptors",
          method = HttpMethods.POST,
          entity = HttpEntity(ContentTypes.`application/json`, data),
          headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
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
          headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
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
        HttpRequest(
          uri = s"$serviceURL/eservices/$uuid",
          method = HttpMethods.GET,
          headers = Seq(headers.Authorization(OAuth2BearerToken("1234")))
        )
      ),
      Duration.Inf
    )

    response.status shouldBe StatusCodes.OK

    Await.result(Unmarshal(response).to[EService], Duration.Inf)
  }
}
