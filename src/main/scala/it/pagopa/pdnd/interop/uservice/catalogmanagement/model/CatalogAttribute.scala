package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import scala.util.{Failure, Success, Try}

sealed trait CatalogAttribute extends Convertable[Attribute] {
  override def toApi: Attribute = this match {
    case SimpleAttribute(id) => Attribute(simple = Some(id), group = None)
    case GroupAttribute(ids) => Attribute(simple = None, group = Some(ids))
  }
}

final case class SimpleAttribute(id: String)      extends CatalogAttribute
final case class GroupAttribute(ids: Seq[String]) extends CatalogAttribute

object CatalogAttribute {
  def fromApi(attribute: Attribute): Try[CatalogAttribute] = {
    val simple = attribute.simple.map(SimpleAttribute)
    val group  = attribute.group.map(GroupAttribute)

    (simple, group) match {
      case (Some(attr), None) => Success[CatalogAttribute](attr)
      case (None, Some(attr)) => Success[CatalogAttribute](attr)
      case _ =>
        Failure[CatalogAttribute] {
          new RuntimeException(s"""
                                  |Invalid attribute: 
                                  |- simple:${attribute.simple.getOrElse("None")} 
                                  |- group:${attribute.group.map(_.mkString(", ")).getOrElse("None")}
                                  |""".stripMargin)
        }
    }

  }
}
