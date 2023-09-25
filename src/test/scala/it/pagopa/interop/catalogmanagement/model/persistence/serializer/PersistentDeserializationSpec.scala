package it.pagopa.interop.catalogmanagement.model.persistence.serializer

import cats.syntax.all._
import com.softwaremill.diffx.generic.auto._
import org.scalacheck.Gen
import it.pagopa.interop.catalogmanagement.model.persistence._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.events._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.state._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.catalog_item.CatalogAttributeV1
import it.pagopa.interop.catalogmanagement.model.CatalogAttribute

class PersistentDeserializationSpec extends PersistentSerdeHelpers {

  deserCheck[State, StateV1](stateGen)
  deserCheck[CatalogItemAdded, CatalogItemV1AddedV1](catalogItemAddedGen)
  deserCheck[ClonedCatalogItemAdded, ClonedCatalogItemV1AddedV1](clonedCatalogItemAddedGen)
  deserCheck[CatalogItemUpdated, CatalogItemV1UpdatedV1](catalogItemUpdatedGen)
  deserCheck[CatalogItemWithDescriptorsDeleted, CatalogItemWithDescriptorsDeletedV1](
    catalogItemWithDescriptorsDeletedGen
  )
  deserCheck[CatalogItemDeleted, CatalogItemDeletedV1](catalogItemDeletedGen)
  deserCheck[CatalogItemDocumentDeleted, CatalogItemDocumentDeletedV1](catalogItemDocumentDeletedGen)
  deserCheck[CatalogItemDescriptorAdded, CatalogItemDescriptorAddedV1](catalogItemDescriptorAddedGen)
  deserCheck[CatalogItemDescriptorUpdated, CatalogItemDescriptorUpdatedV1](catalogItemDescriptorUpdatedGen)
  deserCheck[CatalogItemDocumentAdded, CatalogItemDocumentAddedV1](catalogItemDocumentAddedGen)
  deserCheck[CatalogItemDocumentUpdated, CatalogItemDocumentUpdatedV1](catalogItemDocumentUpdatedGen)
  deserCheck[MovedAttributesFromEserviceToDescriptors, MovedAttributesFromEserviceToDescriptorsV1](movedGen)
  deserCheck[CatalogItemRiskAnalysisAdded, CatalogItemRiskAnalysisAddedV1](catalogItemRiskAnalysisAddedGen)

  def singleCatalogAttributeGen: Gen[(List[CatalogAttribute], CatalogAttributeV1)] =
    catalogAttributeValueGen.map { case (av, avv1) =>
      (List(av), CatalogAttributeV1(single = Option(avv1), group = Nil))
    }

  def groupCatalogAttributeGen: Gen[(List[CatalogAttribute], CatalogAttributeV1)] =
    Gen.nonEmptyListOf(catalogAttributeValueGen).map { list =>
      val (a, b) = list.separate
      (a, CatalogAttributeV1(single = None, group = b))
    }

  override def catalogAttributeGen: Gen[(List[CatalogAttribute], CatalogAttributeV1)] =
    Gen.oneOf(singleCatalogAttributeGen, groupCatalogAttributeGen)
}
