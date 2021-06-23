package it.pagopa.pdnd.interop.uservice.catalogmanagement.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.EServiceApiMarshaller
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.{EService, Problem}
import spray.json._

import java.io.File
import scala.concurrent.Future

class EServiceApiMarshallerImpl extends EServiceApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  //TODO this implicit is not really used, due usage of HttpEntity.fromFile
  override implicit def toEntityMarshallerFile: ToEntityMarshaller[File] =
    Marshaller.withFixedContentType(ContentTypes.`application/octet-stream`)(s => s.toString)

  override implicit def fromStringUnmarshallerStringList: FromStringUnmarshaller[Seq[String]] =
    Unmarshaller(_ => s => Future.successful(s.split(",").toSeq))

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  override implicit def toEntityMarshallerEServicearray: ToEntityMarshaller[Seq[EService]] =
    sprayJsonMarshaller[Seq[EService]]

  override implicit def toEntityMarshallerEService: ToEntityMarshaller[EService] = sprayJsonMarshaller[EService]
}
