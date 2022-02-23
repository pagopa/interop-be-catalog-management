package it.pagopa.interop.catalogmanagement.model

final case class CatalogAttributeValue(id: String, explicitAttributeVerification: Boolean)
    extends Convertable[AttributeValue] {

  override def toApi: AttributeValue =
    AttributeValue(id = id, explicitAttributeVerification = explicitAttributeVerification)
}

object CatalogAttributeValue {
  def fromApi(attributeValue: AttributeValue): CatalogAttributeValue =
    CatalogAttributeValue(
      id = attributeValue.id,
      explicitAttributeVerification = attributeValue.explicitAttributeVerification
    )
}
