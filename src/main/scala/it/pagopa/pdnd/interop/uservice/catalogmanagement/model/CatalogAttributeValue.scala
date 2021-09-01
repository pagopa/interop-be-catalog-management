package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

final case class CatalogAttributeId(id: String, explicitAttributeVerification: Boolean)
    extends Convertable[AttributeId] {

  override def toApi: AttributeId = AttributeId(id = id, explicitAttributeVerification = explicitAttributeVerification)
}

object CatalogAttributeId {
  def fromApi(attributeId: AttributeId): CatalogAttributeId =
    CatalogAttributeId(id = attributeId.id, explicitAttributeVerification = attributeId.explicitAttributeVerification)
}
