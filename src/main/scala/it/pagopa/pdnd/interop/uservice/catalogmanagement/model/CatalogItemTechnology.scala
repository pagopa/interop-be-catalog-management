package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

sealed trait CatalogItemTechnology {
  def toApi: EServiceTechnologyEnum = this match {
    case Rest => REST
    case Soap => SOAP
  }
}

case object Rest extends CatalogItemTechnology
case object Soap extends CatalogItemTechnology

object CatalogItemTechnology {

  def fromApi(status: EServiceTechnologyEnum): CatalogItemTechnology = status match {
    case REST => Rest
    case SOAP => Soap
  }
}
