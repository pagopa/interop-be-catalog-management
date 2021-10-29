package it.pagopa.pdnd.interop.uservice.catalogmanagement.model


/**
 * API Technology Enum
 *
*/

sealed trait EServiceTechnologyEnum

case object REST extends EServiceTechnologyEnum
case object SOAP extends EServiceTechnologyEnum

object EServiceTechnologyEnum {
    import spray.json._

    implicit object EServiceTechnologyEnumFormat extends RootJsonFormat[EServiceTechnologyEnum] {
        def write(obj: EServiceTechnologyEnum): JsValue =
          obj match {
             case REST => JsString("REST")
             case SOAP => JsString("SOAP")
          }

        def read(json: JsValue): EServiceTechnologyEnum =
          json match {
             case JsString("REST") => REST
             case JsString("SOAP") => SOAP
            case unrecognized     => deserializationError(s"EServiceTechnologyEnum serialization error ${unrecognized.toString}")
          }
    }

    def fromValue(value: String): Either[Throwable, EServiceTechnologyEnum] =
      value match {
         case "REST" => Right(REST)
         case "SOAP" => Right(SOAP)
         case other => Left(new RuntimeException(s"Unable to decode value $other"))
      }
}
