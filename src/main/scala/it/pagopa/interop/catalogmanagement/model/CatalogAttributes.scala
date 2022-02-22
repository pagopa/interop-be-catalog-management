package it.pagopa.interop.catalogmanagement.model

import cats.implicits.toTraverseOps

import scala.util.Try

final case class CatalogAttributes(
  certified: Seq[CatalogAttribute],
  declared: Seq[CatalogAttribute],
  verified: Seq[CatalogAttribute]
) extends Convertable[Attributes] {
  def toApi: Attributes = {
    Attributes(certified = certified.map(_.toApi), declared = declared.map(_.toApi), verified = verified.map(_.toApi))
  }

}

object CatalogAttributes {
  def fromApi(attributes: Attributes): Try[CatalogAttributes] = {
    for {
      certified <- attributes.certified.traverse(CatalogAttribute.fromApi)
      declared  <- attributes.declared.traverse(CatalogAttribute.fromApi)
      verified  <- attributes.verified.traverse(CatalogAttribute.fromApi)
    } yield CatalogAttributes(certified = certified, declared = declared, verified = verified)
  }
}
