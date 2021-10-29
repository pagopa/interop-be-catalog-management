package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

sealed trait CatalogItemTechnology {
  def toApi: EServiceTechnologyEnum = this match {
    case RestTechnology => REST
    case SoapTechnology => SOAP
  }
}

case object RestTechnology extends CatalogItemTechnology
case object SoapTechnology extends CatalogItemTechnology

object CatalogItemTechnology {

  def fromApi(status: EServiceTechnologyEnum): CatalogItemTechnology = status match {
    case REST => RestTechnology
    case SOAP => SoapTechnology
  }
}
