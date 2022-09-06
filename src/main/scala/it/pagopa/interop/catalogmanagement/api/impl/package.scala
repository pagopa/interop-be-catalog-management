package it.pagopa.interop.catalogmanagement.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCode
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.commons.utils.SprayCommonFormats._
import it.pagopa.interop.commons.utils.errors.ComponentError
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val eServiceDocFormat: RootJsonFormat[EServiceDoc]                       = jsonFormat7(EServiceDoc)
  implicit val eServiceDescriptorSeedFormat: RootJsonFormat[EServiceDescriptorSeed] = jsonFormat5(
    EServiceDescriptorSeed
  )
  implicit val updateEServiceDescriptorSeedFormat: RootJsonFormat[UpdateEServiceDescriptorSeed] = jsonFormat6(
    UpdateEServiceDescriptorSeed
  )
  implicit val eServiceDescriptorFormat: RootJsonFormat[EServiceDescriptor] = jsonFormat10(EServiceDescriptor)
  implicit val attributeValueFormat: RootJsonFormat[AttributeValue]         = jsonFormat2(AttributeValue)
  implicit val attributeFormat: RootJsonFormat[Attribute]                   = jsonFormat2(Attribute)
  implicit val attributesFormat: RootJsonFormat[Attributes]                 = jsonFormat3(Attributes)
  implicit val eServiceSeedFormat: RootJsonFormat[EServiceSeed]             = jsonFormat5(EServiceSeed)
  implicit val updateEServiceSeedFormat: RootJsonFormat[UpdateEServiceSeed] = jsonFormat4(UpdateEServiceSeed)
  implicit val eServiceFormat: RootJsonFormat[EService]                     = jsonFormat7(EService)
  implicit val problemErrorFormat: RootJsonFormat[ProblemError]             = jsonFormat2(ProblemError)
  implicit val problemFormat: RootJsonFormat[Problem]                       = jsonFormat5(Problem)
  implicit val updateEserviceDescriptorDocumentSeed: RootJsonFormat[UpdateEServiceDescriptorDocumentSeed] = jsonFormat1(
    UpdateEServiceDescriptorDocumentSeed
  )

  final val entityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  final val serviceErrorCodePrefix: String = "008"
  final val defaultProblemType: String     = "about:blank"
  final val defaultErrorMessage: String    = "Unknown error"

  def problemOf(httpError: StatusCode, error: ComponentError): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      errors = Seq(
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultErrorMessage)
        )
      )
    )

  def problemOf(httpError: StatusCode, errors: List[ComponentError]): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      errors = errors.map(error =>
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultErrorMessage)
        )
      )
    )
}
