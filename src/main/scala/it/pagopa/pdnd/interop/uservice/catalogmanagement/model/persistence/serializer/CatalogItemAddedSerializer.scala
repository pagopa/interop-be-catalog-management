package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.CatalogItemAdded
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1._

import java.io.NotSerializableException

class CatalogItemAddedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 100000

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val CatalogItemAddedManifest: String = classOf[CatalogItemAdded].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event: CatalogItemAdded =>
      serialize(event, CatalogItemAddedManifest, currentVersion)
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest.split('|').toList match {
    case CatalogItemAddedManifest :: `version1` :: Nil =>
      deserialize(v1.events.CatalogItemV1AddedV1, bytes, manifest, currentVersion)
    case _ =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
      )
  }

}
