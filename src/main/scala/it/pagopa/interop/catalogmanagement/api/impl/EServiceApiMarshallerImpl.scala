package it.pagopa.interop.catalogmanagement.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.interop.catalogmanagement.api.EServiceApiMarshaller
import it.pagopa.interop.catalogmanagement.model._
import spray.json._
final object EServiceApiMarshallerImpl extends EServiceApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  override implicit def toEntityMarshallerEServicearray: ToEntityMarshaller[Seq[EService]] =
    sprayJsonMarshaller[Seq[EService]]

  override implicit def toEntityMarshallerEService: ToEntityMarshaller[EService] = sprayJsonMarshaller[EService]

  override implicit def fromEntityUnmarshallerEServiceSeed: FromEntityUnmarshaller[EServiceSeed] =
    sprayJsonUnmarshaller[EServiceSeed]

  override implicit def fromEntityUnmarshallerEServiceDescriptorSeed: FromEntityUnmarshaller[EServiceDescriptorSeed] =
    sprayJsonUnmarshaller[EServiceDescriptorSeed]

  override implicit def fromEntityUnmarshallerUpdateEServiceSeed: FromEntityUnmarshaller[UpdateEServiceSeed] =
    sprayJsonUnmarshaller[UpdateEServiceSeed]

  override implicit def fromEntityUnmarshallerUpdateEServiceDescriptorSeed
    : FromEntityUnmarshaller[UpdateEServiceDescriptorSeed] = sprayJsonUnmarshaller[UpdateEServiceDescriptorSeed]

  override implicit def toEntityMarshallerEServiceDescriptor: ToEntityMarshaller[EServiceDescriptor] =
    sprayJsonMarshaller[EServiceDescriptor]

  override implicit def fromEntityUnmarshallerUpdateEServiceDescriptorDocumentSeed
    : FromEntityUnmarshaller[UpdateEServiceDescriptorDocumentSeed] =
    sprayJsonUnmarshaller[UpdateEServiceDescriptorDocumentSeed]

  override implicit def toEntityMarshallerEServiceDoc: ToEntityMarshaller[EServiceDoc] =
    sprayJsonMarshaller[EServiceDoc]
}
