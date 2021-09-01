package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import scala.util.{Failure, Success, Try}

sealed trait CatalogAttribute extends Convertable[Attribute] {
  override def toApi: Attribute = this match {
    case attr: SingleAttribute => Attribute(single = Some(attr.id.toApi), group = None)
    case attr: GroupAttribute  => Attribute(single = None, group = Some(attr.ids.map(_.toApi)))
  }
}

final case class SingleAttribute(id: CatalogAttributeId)      extends CatalogAttribute
final case class GroupAttribute(ids: Seq[CatalogAttributeId]) extends CatalogAttribute

object CatalogAttribute {

  def fromApi(attribute: Attribute): Try[CatalogAttribute] = {

    val simple: Option[CatalogAttributeId]     = attribute.single.map(CatalogAttributeId.fromApi)
    val group: Option[Seq[CatalogAttributeId]] = attribute.group.map(_.map(CatalogAttributeId.fromApi))

    (simple, group) match {
      case (Some(attr), None)  => Success[CatalogAttribute](SingleAttribute(attr))
      case (None, Some(attrs)) => Success[CatalogAttribute](GroupAttribute(attrs))
      case _ =>
        Failure[CatalogAttribute] {
          new RuntimeException(s"""
                                  |Invalid attribute:
                                  |- simple:${attribute.single.map(_.id).getOrElse("None")}
                                  |- group:${attribute.group.map(_.map(_.id).mkString(", ")).getOrElse("None")}
                                  |""".stripMargin)
        }
    }
  }
}
