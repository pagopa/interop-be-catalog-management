package it.pagopa.pdnd.interop.uservice.catalogmanagement.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model._
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}

import java.util.UUID
import scala.util.{Failure, Success, Try}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val uuidFormat: JsonFormat[UUID] =
    new JsonFormat[UUID] {
      override def write(obj: UUID): JsValue = JsString(obj.toString)

      override def read(json: JsValue): UUID = json match {
        case JsString(s) =>
          Try(UUID.fromString(s)) match {
            case Success(result) => result
            case Failure(exception) =>
              deserializationError(s"could not parse $s as UUID", exception)
          }
        case notAJsString =>
          deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
      }
    }

  implicit val eServiceDocFormat: RootJsonFormat[EServiceDoc] = jsonFormat5(EServiceDoc)
  implicit val eServiceDescriptorSeedFormat: RootJsonFormat[EServiceDescriptorSeed] = jsonFormat3(
    EServiceDescriptorSeed
  )
  implicit val updateEServiceDescriptorSeedFormat: RootJsonFormat[UpdateEServiceDescriptorSeed] = jsonFormat4(
    UpdateEServiceDescriptorSeed
  )
  implicit val eServiceDescriptorFormat: RootJsonFormat[EServiceDescriptor] = jsonFormat8(EServiceDescriptor)
  implicit val attributeValueFormat: RootJsonFormat[AttributeValue]         = jsonFormat2(AttributeValue)
  implicit val attributeFormat: RootJsonFormat[Attribute]                   = jsonFormat2(Attribute)
  implicit val attributesFormat: RootJsonFormat[Attributes]                 = jsonFormat3(Attributes)
  implicit val eServiceSeedFormat: RootJsonFormat[EServiceSeed]             = jsonFormat5(EServiceSeed)
  implicit val updateEServiceSeedFormat: RootJsonFormat[UpdateEServiceSeed] = jsonFormat4(UpdateEServiceSeed)
  implicit val eServiceFormat: RootJsonFormat[EService]                     = jsonFormat7(EService)
  implicit val problemFormat: RootJsonFormat[Problem]                       = jsonFormat3(Problem)
  implicit val updateEserviceDescriptorDocumentSeed: RootJsonFormat[UpdateEServiceDescriptorDocumentSeed] = jsonFormat1(
    UpdateEServiceDescriptorDocumentSeed
  )

}
