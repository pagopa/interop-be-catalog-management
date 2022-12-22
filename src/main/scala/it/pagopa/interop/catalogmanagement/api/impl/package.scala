package it.pagopa.interop.catalogmanagement.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.commons.utils.SprayCommonFormats._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val eServiceDocFormat: RootJsonFormat[EServiceDoc]                       = jsonFormat7(EServiceDoc)
  implicit val eServiceDescriptorSeedFormat: RootJsonFormat[EServiceDescriptorSeed] =
    jsonFormat6(EServiceDescriptorSeed)
  implicit val updateEServiceDescriptorSeedFormat: RootJsonFormat[UpdateEServiceDescriptorSeed] =
    jsonFormat7(UpdateEServiceDescriptorSeed)
  implicit val eServiceDescriptorFormat: RootJsonFormat[EServiceDescriptor] = jsonFormat12(EServiceDescriptor)
  implicit val attributeValueFormat: RootJsonFormat[AttributeValue]         = jsonFormat2(AttributeValue)
  implicit val attributeFormat: RootJsonFormat[Attribute]                   = jsonFormat2(Attribute)
  implicit val attributesFormat: RootJsonFormat[Attributes]                 = jsonFormat3(Attributes)
  implicit val eServiceSeedFormat: RootJsonFormat[EServiceSeed]             = jsonFormat5(EServiceSeed)
  implicit val updateEServiceSeedFormat: RootJsonFormat[UpdateEServiceSeed] = jsonFormat4(UpdateEServiceSeed)
  implicit val eServiceFormat: RootJsonFormat[EService]                     = jsonFormat7(EService)
  implicit val problemErrorFormat: RootJsonFormat[ProblemError]             = jsonFormat2(ProblemError)
  implicit val problemFormat: RootJsonFormat[Problem]                       = jsonFormat5(Problem)
  implicit val updateEserviceDescriptorDocumentSeed: RootJsonFormat[UpdateEServiceDescriptorDocumentSeed] =
    jsonFormat1(UpdateEServiceDescriptorDocumentSeed)

  final val entityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

}
