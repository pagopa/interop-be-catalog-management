package it.pagopa.pdnd.interop.uservice.catalogmanagement.model


/**
 * EService Descriptor Status Enum
 *
*/

sealed trait EServiceDescriptorStatusEnum

case object DRAFT extends EServiceDescriptorStatusEnum
case object PUBLISHED extends EServiceDescriptorStatusEnum
case object DEPRECATED extends EServiceDescriptorStatusEnum
case object SUSPENDED extends EServiceDescriptorStatusEnum
case object ARCHIVED extends EServiceDescriptorStatusEnum

object EServiceDescriptorStatusEnum {
    import spray.json._

    implicit object EServiceDescriptorStatusEnumFormat extends RootJsonFormat[EServiceDescriptorStatusEnum] {
        def write(obj: EServiceDescriptorStatusEnum): JsValue =
          obj match {
             case DRAFT => JsString("DRAFT")
             case PUBLISHED => JsString("PUBLISHED")
             case DEPRECATED => JsString("DEPRECATED")
             case SUSPENDED => JsString("SUSPENDED")
             case ARCHIVED => JsString("ARCHIVED")
          }

        def read(json: JsValue): EServiceDescriptorStatusEnum =
          json match {
             case JsString("DRAFT") => DRAFT
             case JsString("PUBLISHED") => PUBLISHED
             case JsString("DEPRECATED") => DEPRECATED
             case JsString("SUSPENDED") => SUSPENDED
             case JsString("ARCHIVED") => ARCHIVED
            case unrecognized     => deserializationError(s"EServiceDescriptorStatusEnum serialization error ${unrecognized.toString}")
          }
    }

    def fromValue(value: String): Either[Throwable, EServiceDescriptorStatusEnum] =
      value match {
         case "DRAFT" => Right(DRAFT)
         case "PUBLISHED" => Right(PUBLISHED)
         case "DEPRECATED" => Right(DEPRECATED)
         case "SUSPENDED" => Right(SUSPENDED)
         case "ARCHIVED" => Right(ARCHIVED)
         case other => Left(new RuntimeException(s"Unable to decode value $other"))
      }
}
