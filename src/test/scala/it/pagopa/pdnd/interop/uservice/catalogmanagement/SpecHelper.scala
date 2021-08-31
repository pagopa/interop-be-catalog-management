package it.pagopa.pdnd.interop.uservice.catalogmanagement

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.impl._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.EService
import it.pagopa.pdnd.interop.uservice.catalogmanagement.provider.CatalogManagementServiceSpec.mockUUIDSupplier
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait SpecHelper extends SpecConfiguration with AnyWordSpecLike with MockFactory {
  def createEService(uuid: String)(implicit actorSystem: ActorSystem[_]): EService = {
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
        |          },
        |          "explicitAttributesVerification" : false
        |        }
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
