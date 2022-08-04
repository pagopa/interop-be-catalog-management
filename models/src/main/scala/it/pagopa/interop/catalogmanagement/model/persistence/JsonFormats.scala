package it.pagopa.interop.catalogmanagement.model.persistence

import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.commons.utils.SprayCommonFormats._
import spray.json.DefaultJsonProtocol._
import spray.json._

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

  implicit val cdocFormat: RootJsonFormat[CatalogDocument] = jsonFormat7(CatalogDocument.apply)

  implicit val cavFormat: RootJsonFormat[CatalogAttributeValue] = jsonFormat2(CatalogAttributeValue.apply)
  implicit val saFormat: RootJsonFormat[SingleAttribute]        = jsonFormat1(SingleAttribute.apply)
  implicit val gaFormat: RootJsonFormat[GroupAttribute]         = jsonFormat1(GroupAttribute.apply)

  implicit val caFormat: RootJsonFormat[CatalogAttribute] =
    new RootJsonFormat[CatalogAttribute] {
      override def read(json: JsValue): CatalogAttribute = json match {
        // TODO Is there a better way?
        case JsObject(fields) =>
          fields
            .get("ids")
            .map(_ => gaFormat.read(json))
            .orElse(fields.get("id").map(_ => saFormat.read(json)))
            .getOrElse(deserializationError(s"Unable to deserialize json as a CatalogAttribute: $json"))
        case other            => deserializationError(s"Unable to deserialize json as a CatalogAttribute: $other")
      }

      override def write(obj: CatalogAttribute): JsValue = obj match {
        case ca: SingleAttribute => ca.toJson
        case ca: GroupAttribute  => ca.toJson
      }
    }

  implicit val cdFormat: RootJsonFormat[CatalogDescriptor]  = jsonFormat10(CatalogDescriptor.apply)
  implicit val casFormat: RootJsonFormat[CatalogAttributes] = jsonFormat3(CatalogAttributes.apply)
  implicit val ciFormat: RootJsonFormat[CatalogItem]        = jsonFormat7(CatalogItem.apply)
}
