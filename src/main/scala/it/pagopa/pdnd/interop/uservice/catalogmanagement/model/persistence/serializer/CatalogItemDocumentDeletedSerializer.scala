package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence. CatalogItemDocumentDeleted
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1._

import java.io.NotSerializableException

class CatalogItemDocumentDeletedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 100009

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val  CatalogItemDocumentDeletedManifest: String = classOf[ CatalogItemDocumentDeleted].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event:  CatalogItemDocumentDeleted =>
      serialize(event,  CatalogItemDocumentDeletedManifest, currentVersion)
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest.split('|').toList match {
    case  CatalogItemDocumentDeletedManifest :: `version1` :: Nil =>
      deserialize(v1.events.CatalogItemDocumentDeletedV1, bytes, manifest, currentVersion)
    case _ =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
      )
  }

}
