package it.pagopa.interop.catalogmanagement.model

import scala.util.{Failure, Success, Try}

sealed trait CatalogAttribute extends Convertable[Attribute] {
  override def toApi: Attribute = this match {
    case SingleAttribute(id) => Attribute(single = Some(id.toApi), group = None)
    case GroupAttribute(ids) => Attribute(single = None, group = Some(ids.map(_.toApi)))
  }
}

final case class SingleAttribute(id: CatalogAttributeValue)      extends CatalogAttribute
final case class GroupAttribute(ids: Seq[CatalogAttributeValue]) extends CatalogAttribute

object CatalogAttribute {

  def fromApi(attribute: Attribute): Try[CatalogAttribute] = {

    val single: Option[CatalogAttributeValue]     = attribute.single.map(CatalogAttributeValue.fromApi)
    val group: Option[Seq[CatalogAttributeValue]] = attribute.group.map(_.map(CatalogAttributeValue.fromApi))

    (single, group) match {
      case (Some(attr), None)  => Success[CatalogAttribute](SingleAttribute(attr))
      case (None, Some(attrs)) => Success[CatalogAttribute](GroupAttribute(attrs))
      case _ =>
        Failure[CatalogAttribute] {
          new RuntimeException(s"""
                                  |Invalid attribute:
                                  |- single:${attribute.single.map(_.id).getOrElse("None")}
                                  |- group:${attribute.group.map(_.map(_.id).mkString(", ")).getOrElse("None")}
                                  |""".stripMargin)
        }
    }
  }
}
