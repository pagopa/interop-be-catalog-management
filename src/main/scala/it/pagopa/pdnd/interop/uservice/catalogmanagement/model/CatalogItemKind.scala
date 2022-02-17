package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

sealed trait CatalogItemKind {
  def toApi: EServiceKind = this match {
    case Public  => EServiceKind.PUBLIC
    case Private => EServiceKind.PRIVATE
  }
}

case object Public  extends CatalogItemKind
case object Private extends CatalogItemKind

object CatalogItemKind {
  def fromApi(status: EServiceKind): CatalogItemKind = status match {
    case EServiceKind.PUBLIC  => Public
    case EServiceKind.PRIVATE => Private
  }
}
