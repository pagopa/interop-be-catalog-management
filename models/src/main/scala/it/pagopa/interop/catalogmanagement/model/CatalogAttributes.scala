package it.pagopa.interop.catalogmanagement.model

final case class CatalogAttributes(
  certified: Seq[Seq[CatalogAttribute]],
  declared: Seq[Seq[CatalogAttribute]],
  verified: Seq[Seq[CatalogAttribute]]
) {
  def combine(x: CatalogAttributes): CatalogAttributes = CatalogAttributes(
    certified = (certified ++ x.certified).toSet.toList,
    declared = (declared ++ x.declared).toSet.toList,
    verified = (verified ++ x.verified).toSet.toList
  )
}

object CatalogAttributes {
  val empty: CatalogAttributes = CatalogAttributes(Nil, Nil, Nil)
}
