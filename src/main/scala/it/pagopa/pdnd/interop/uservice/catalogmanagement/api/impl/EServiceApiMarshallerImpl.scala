package it.pagopa.pdnd.interop.uservice.catalogmanagement.api.impl

import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.EServiceApiMarshaller
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.{EService, Problem}
import spray.json._

import scala.concurrent.Future

class EServiceApiMarshallerImpl extends EServiceApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def fromStringUnmarshallerStringList: FromStringUnmarshaller[Seq[String]] =
    Unmarshaller(_ => s => Future.successful(s.split(",").toSeq))

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  override implicit def toEntityMarshallerEServicearray: ToEntityMarshaller[Seq[EService]] =
    sprayJsonMarshaller[Seq[EService]]

  override implicit def toEntityMarshallerEService: ToEntityMarshaller[EService] = sprayJsonMarshaller[EService]
}
