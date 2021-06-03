package it.pagopa.pdnd.uservice.resttemplate.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.pdnd.interopuservice.agreementmanagement.api.EServiceApiMarshaller
import it.pagopa.pdnd.interopuservice.agreementmanagement.model.EService
import spray.json._

class EServiceApiMarshallerImpl extends EServiceApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {
  override implicit def fromEntityUnmarshallerEService: FromEntityUnmarshaller[EService] =
    sprayJsonUnmarshaller[EService]

  override implicit def toEntityMarshallerEService: ToEntityMarshaller[EService] = sprayJsonMarshaller[EService]
}
