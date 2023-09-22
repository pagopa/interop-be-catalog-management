package it.pagopa.interop.catalogmanagement.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.commons.utils.SprayCommonFormats._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val eServiceDocFormat: RootJsonFormat[EServiceDoc]                       = jsonFormat7(EServiceDoc)
  implicit val attributeFormat: RootJsonFormat[Attribute]                           = jsonFormat2(Attribute)
  implicit val attributesFormat: RootJsonFormat[Attributes]                         = jsonFormat3(Attributes)
  implicit val eServiceDescriptorFormat: RootJsonFormat[EServiceDescriptor]         = jsonFormat17(EServiceDescriptor)
  implicit val eServiceDescriptorSeedFormat: RootJsonFormat[EServiceDescriptorSeed] =
    jsonFormat7(EServiceDescriptorSeed)
  implicit val updateEServiceDescriptorSeedFormat: RootJsonFormat[UpdateEServiceDescriptorSeed] =
    jsonFormat8(UpdateEServiceDescriptorSeed)
  implicit val riskAnalysisSingleAnswerFormFormat: RootJsonFormat[RiskAnalysisSingleAnswer]     = jsonFormat2(
    RiskAnalysisSingleAnswer
  )
  implicit val riskAnalysisSMultiAnswerFormFormat: RootJsonFormat[RiskAnalysisMultiAnswer]      = jsonFormat2(
    RiskAnalysisMultiAnswer
  )
  implicit val riskAnalysisFormFormat: RootJsonFormat[RiskAnalysisForm]     = jsonFormat3(RiskAnalysisForm)
  implicit val riskAnalysisFormat: RootJsonFormat[RiskAnalysis]             = jsonFormat4(RiskAnalysis)
  implicit val eServiceSeedFormat: RootJsonFormat[EServiceSeed]             = jsonFormat5(EServiceSeed)
  implicit val updateEServiceSeedFormat: RootJsonFormat[UpdateEServiceSeed] = jsonFormat3(UpdateEServiceSeed)
  implicit val eServiceFormat: RootJsonFormat[EService]                     = jsonFormat8(EService)
  implicit val problemErrorFormat: RootJsonFormat[ProblemError]             = jsonFormat2(ProblemError)
  implicit val problemFormat: RootJsonFormat[Problem]                       = jsonFormat6(Problem)
  implicit val updateEserviceDescriptorDocumentSeed: RootJsonFormat[UpdateEServiceDescriptorDocumentSeed] =
    jsonFormat1(UpdateEServiceDescriptorDocumentSeed)
  implicit val createEserviceDescriptorDocumentSeed: RootJsonFormat[CreateEServiceDescriptorDocumentSeed] =
    jsonFormat8(CreateEServiceDescriptorDocumentSeed)

  final val entityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]
}
