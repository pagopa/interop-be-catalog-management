package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer

import cats.implicits._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.catalog_item.CatalogItemV1
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.events.CatalogItemV1AddedV1
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.state.{CatalogItemsV1, StateV1}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.utils._

import java.util.UUID
package object v1 {

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any"))
  implicit def stateV1PersistEventDeserializer: PersistEventDeserializer[StateV1, State] =
    state => {
      val items: Either[Throwable, Seq[(String, CatalogItem)]] = state.items.traverse { itemsV1 =>
        for {
          attributes  <- convertAttributesFromV1(itemsV1.value.attributes)
          descriptors <- convertDescriptorsFromV1(itemsV1.value.descriptors)
        } yield itemsV1.key -> CatalogItem(
          id = UUID.fromString(itemsV1.value.id),
          producerId = UUID.fromString(itemsV1.value.producerId),
          name = itemsV1.value.name,
          description = itemsV1.value.description,
          audience = itemsV1.value.audience,
          technology = itemsV1.value.technology,
          voucherLifespan = itemsV1.value.voucherLifespan,
          attributes = attributes,
          descriptors = descriptors
        )
      }
      items.map(its => State(its.toMap))
    }

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any"))
  implicit def stateV1PersistEventSerializer: PersistEventSerializer[State, StateV1] =
    state => {
      val itemsV1: Either[RuntimeException, Seq[CatalogItemsV1]] = state.items.toSeq.traverse {
        case (key, catalogItem) =>
          for {
            attributes  <- convertAttributesToV1(catalogItem.attributes)
            descriptors <- convertDescriptorsToV1(catalogItem.descriptors)
          } yield CatalogItemsV1(
            key,
            CatalogItemV1(
              id = catalogItem.id.toString,
              producerId = catalogItem.producerId.toString,
              name = catalogItem.name,
              description = catalogItem.description,
              audience = catalogItem.audience,
              technology = catalogItem.technology,
              voucherLifespan = catalogItem.voucherLifespan,
              attributes = attributes,
              descriptors = descriptors
            )
          )

      }

      itemsV1.map(its => StateV1(its))
    }

  implicit def catalogItemV1AddedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemV1AddedV1, CatalogItemAdded] =
    event => {
      for {
        attributes  <- convertAttributesFromV1(event.catalogItem.attributes)
        descriptors <- convertDescriptorsFromV1(event.catalogItem.descriptors)
      } yield CatalogItemAdded(catalogItem =
        CatalogItem(
          id = UUID.fromString(event.catalogItem.id),
          producerId = UUID.fromString(event.catalogItem.producerId),
          name = event.catalogItem.name,
          description = event.catalogItem.description,
          audience = event.catalogItem.audience,
          technology = event.catalogItem.technology,
          voucherLifespan = event.catalogItem.voucherLifespan,
          attributes = attributes,
          descriptors = descriptors
        )
      )

    }

  implicit def catalogItemV1AddedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemAdded, CatalogItemV1AddedV1] =
    event => {
      for {
        attributes  <- convertAttributesToV1(event.catalogItem.attributes)
        descriptors <- convertDescriptorsToV1(event.catalogItem.descriptors)
      } yield CatalogItemV1AddedV1
        .of(
          CatalogItemV1(
            id = event.catalogItem.id.toString,
            producerId = event.catalogItem.producerId.toString,
            name = event.catalogItem.name,
            description = event.catalogItem.description,
            audience = event.catalogItem.audience,
            technology = event.catalogItem.technology,
            voucherLifespan = event.catalogItem.voucherLifespan,
            attributes = attributes,
            descriptors = descriptors
          )
        )
    }

}
