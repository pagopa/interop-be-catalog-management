package it.pagopa.interop.catalogmanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.interop.catalogmanagement.model.persistence.CatalogItemDeleted
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1._

import java.io.NotSerializableException

class CatalogItemDeletedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 100005

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val CatalogItemDeletedManifest: String = classOf[CatalogItemDeleted].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event: CatalogItemDeleted =>
      serialize(event, CatalogItemDeletedManifest, currentVersion)
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest.split('|').toList match {
    case CatalogItemDeletedManifest :: `version1` :: Nil =>
      deserialize(v1.events.CatalogItemDeletedV1, bytes, manifest, currentVersion)
    case _ =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
      )
  }

}