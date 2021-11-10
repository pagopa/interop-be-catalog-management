package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

sealed trait CatalogItemTechnology {
  def toApi: EServiceTechnology = this match {
    case Rest => EServiceTechnology.REST
    case Soap => EServiceTechnology.SOAP
  }
}

case object Rest extends CatalogItemTechnology
case object Soap extends CatalogItemTechnology

object CatalogItemTechnology {

  def fromApi(status: EServiceTechnology): CatalogItemTechnology = status match {
    case EServiceTechnology.REST => Rest
    case EServiceTechnology.SOAP => Soap
  }
}
