package it.pagopa.interop.catalogmanagement.model.persistence.serializer

import cats.syntax.all._
import com.softwaremill.diffx.generic.auto._
import org.scalacheck.Gen
import it.pagopa.interop.catalogmanagement.model.persistence._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.events._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.state._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.PersistentSerdeHelpers._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.catalog_item.CatalogAttributeV1
import it.pagopa.interop.catalogmanagement.model.CatalogAttribute

class PersistentSerializationSpec extends PersistentSerdeHelpers {

  serdeCheck[State, StateV1](stateGen, _.sorted)
  serdeCheck[CatalogItemAdded, CatalogItemV1AddedV1](catalogItemAddedGen)
  serdeCheck[ClonedCatalogItemAdded, ClonedCatalogItemV1AddedV1](clonedCatalogItemAddedGen)
  serdeCheck[CatalogItemUpdated, CatalogItemV1UpdatedV1](catalogItemUpdatedGen)
  serdeCheck[CatalogItemWithDescriptorsDeleted, CatalogItemWithDescriptorsDeletedV1](
    catalogItemWithDescriptorsDeletedGen
  )
  serdeCheck[CatalogItemDeleted, CatalogItemDeletedV1](catalogItemDeletedGen)
  serdeCheck[CatalogItemDocumentDeleted, CatalogItemDocumentDeletedV1](catalogItemDocumentDeletedGen)
  serdeCheck[CatalogItemDescriptorAdded, CatalogItemDescriptorAddedV1](catalogItemDescriptorAddedGen)
  serdeCheck[CatalogItemDescriptorUpdated, CatalogItemDescriptorUpdatedV1](catalogItemDescriptorUpdatedGen)
  serdeCheck[CatalogItemDocumentAdded, CatalogItemDocumentAddedV1](catalogItemDocumentAddedGen)
  serdeCheck[CatalogItemDocumentUpdated, CatalogItemDocumentUpdatedV1](catalogItemDocumentUpdatedGen)
  serdeCheck[MovedAttributesFromEserviceToDescriptors, MovedAttributesFromEserviceToDescriptorsV1](movedGen)
  serdeCheck[CatalogItemRiskAnalysisAdded, CatalogItemRiskAnalysisAddedV1](catalogItemRiskAnalysisAddedGen)

  override def catalogAttributeGen: Gen[(List[CatalogAttribute], CatalogAttributeV1)] =
    Gen.nonEmptyListOf(catalogAttributeValueGen).map { list =>
      val (a, b) = list.separate
      (a, CatalogAttributeV1(single = None, group = b))
    }
}
