package it.pagopa.interop.catalogmanagement.model.persistence

import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.commons.utils.SprayCommonFormats._
import spray.json.DefaultJsonProtocol._
import spray.json._
import it.pagopa.interop.catalogmanagement.model.{Deliver, Receive}

object JsonFormats {

  implicit val citFormat: RootJsonFormat[CatalogItemTechnology] =
    new RootJsonFormat[CatalogItemTechnology] {
      override def read(json: JsValue): CatalogItemTechnology = json match {
        case JsString("Rest") => Rest
        case JsString("Soap") => Soap
        case other            => deserializationError(s"Unable to deserialize json as a CatalogItemTechnology: $other")
      }

      override def write(obj: CatalogItemTechnology): JsValue = obj match {
        case Rest => JsString("Rest")
        case Soap => JsString("Soap")
      }
    }

  implicit val cdsFormat: RootJsonFormat[CatalogDescriptorState] =
    new RootJsonFormat[CatalogDescriptorState] {
      override def read(json: JsValue): CatalogDescriptorState = json match {
        case JsString("Draft")      => Draft
        case JsString("Published")  => Published
        case JsString("Deprecated") => Deprecated
        case JsString("Suspended")  => Suspended
        case JsString("Archived")   => Archived
        case other => deserializationError(s"Unable to deserialize json as a CatalogDescriptorState: $other")
      }

      override def write(obj: CatalogDescriptorState): JsValue = obj match {
        case Draft      => JsString("Draft")
        case Published  => JsString("Published")
        case Deprecated => JsString("Deprecated")
        case Suspended  => JsString("Suspended")
        case Archived   => JsString("Archived")
      }
    }

  implicit val appFormat: RootJsonFormat[PersistentAgreementApprovalPolicy] =
    new RootJsonFormat[PersistentAgreementApprovalPolicy] {
      override def read(json: JsValue): PersistentAgreementApprovalPolicy = json match {
        case JsString("Automatic") => Automatic
        case JsString("Manual")    => Manual
        case other => deserializationError(s"Unable to deserialize json as a AgreementApprovalPolicy: $other")
      }

      override def write(obj: PersistentAgreementApprovalPolicy): JsValue = obj match {
        case Automatic => JsString("Automatic")
        case Manual    => JsString("Manual")
      }
    }

  implicit val modeFormat: RootJsonFormat[CatalogItemMode] =
    new RootJsonFormat[CatalogItemMode] {
      override def read(json: JsValue): CatalogItemMode = json match {
        case JsString("Receive") => Receive
        case JsString("Deliver") => Deliver
        case other               => deserializationError(s"Unable to deserialize json as a CatalogMode: $other")
      }

      override def write(obj: CatalogItemMode): JsValue = obj match {
        case Receive => JsString("Receive")
        case Deliver => JsString("Deliver")
      }
    }

  implicit val cdocFormat: RootJsonFormat[CatalogDocument] = jsonFormat7(CatalogDocument.apply)

  implicit val saFormat: RootJsonFormat[CatalogAttribute] = jsonFormat2(CatalogAttribute.apply)

  implicit val casFormat: RootJsonFormat[CatalogAttributes]                = jsonFormat3(CatalogAttributes.apply)
  implicit val cdFormat: RootJsonFormat[CatalogDescriptor]                 = jsonFormat18(CatalogDescriptor.apply)
  implicit val ramaFormat: RootJsonFormat[CatalogRiskAnalysisMultiAnswer]  = jsonFormat3(
    CatalogRiskAnalysisMultiAnswer.apply
  )
  implicit val rasaFormat: RootJsonFormat[CatalogRiskAnalysisSingleAnswer] = jsonFormat3(
    CatalogRiskAnalysisSingleAnswer.apply
  )
  implicit val rafFormat: RootJsonFormat[CatalogRiskAnalysisForm]          = jsonFormat4(CatalogRiskAnalysisForm.apply)
  implicit val raFormat: RootJsonFormat[CatalogRiskAnalysis]               = jsonFormat4(CatalogRiskAnalysis.apply)
  implicit val ciFormat: RootJsonFormat[CatalogItem]                       = jsonFormat10(CatalogItem.apply)

  implicit val ciaFormat: RootJsonFormat[CatalogItemAdded]        = jsonFormat1(CatalogItemAdded.apply)
  implicit val cciaFormat: RootJsonFormat[ClonedCatalogItemAdded] = jsonFormat1(ClonedCatalogItemAdded.apply)
  implicit val ciuFormat: RootJsonFormat[CatalogItemUpdated]      = jsonFormat1(CatalogItemUpdated.apply)
  implicit val ciwddFormat: RootJsonFormat[CatalogItemWithDescriptorsDeleted] = jsonFormat2(
    CatalogItemWithDescriptorsDeleted.apply
  )
  implicit val ciduFormat: RootJsonFormat[CatalogItemDocumentUpdated]    = jsonFormat5(CatalogItemDocumentUpdated.apply)
  implicit val cidFormat: RootJsonFormat[CatalogItemDeleted]             = jsonFormat1(CatalogItemDeleted.apply)
  implicit val cidaFormat: RootJsonFormat[CatalogItemDocumentAdded]      = jsonFormat5(CatalogItemDocumentAdded.apply)
  implicit val ciddFormat: RootJsonFormat[CatalogItemDocumentDeleted]    = jsonFormat3(CatalogItemDocumentDeleted.apply)
  implicit val cideaFormat: RootJsonFormat[CatalogItemDescriptorAdded]   = jsonFormat2(CatalogItemDescriptorAdded.apply)
  implicit val cideuFormat: RootJsonFormat[CatalogItemDescriptorUpdated] = jsonFormat2(
    CatalogItemDescriptorUpdated.apply
  )
  implicit val mafetdFormat: RootJsonFormat[MovedAttributesFromEserviceToDescriptors] = jsonFormat1(
    MovedAttributesFromEserviceToDescriptors.apply
  )
  implicit val ciraFormat: RootJsonFormat[CatalogItemRiskAnalysisAdded]               = jsonFormat2(
    CatalogItemRiskAnalysisAdded.apply
  )
  implicit val cirauFormat: RootJsonFormat[CatalogItemRiskAnalysisUpdated]            = jsonFormat2(
    CatalogItemRiskAnalysisUpdated.apply
  )
  implicit val ciradFormat: RootJsonFormat[CatalogItemRiskAnalysisDeleted]            = jsonFormat2(
    CatalogItemRiskAnalysisDeleted.apply
  )

}
