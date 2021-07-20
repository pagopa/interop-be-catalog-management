package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer

import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.catalog_item.{
  CatalogItemDocumentV1,
  CatalogItemV1,
  CatalogItemVersionV1
}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.events.CatalogItemV1AddedV1
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.state.{CatalogItemsV1, StateV1}

import java.util.UUID

package object v1 {

  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  implicit def stateV1PersistEventDeserializer: PersistEventDeserializer[StateV1, State] =
    state => {
      val items = state.items
        .map(itemsV1 =>
          (
            itemsV1.key,
            CatalogItem(
              id = UUID.fromString(itemsV1.value.id),
              producerId = UUID.fromString(itemsV1.value.producerId),
              name = itemsV1.value.name,
              audience = itemsV1.value.audience,
              versions = itemsV1.value.versions.map(ver1 =>
                CatalogItemVersion(
                  id = UUID.fromString(ver1.id),
                  version = ver1.version,
                  description = ver1.description,
                  docs = ver1.docs.map { doc =>
                    CatalogItemDocument(
                      id = UUID.fromString(doc.id),
                      name = doc.name,
                      contentType = doc.contentType,
                      path = doc.path
                    )
                  },
                  status = ver1.status
                )
              ),
              status = itemsV1.value.status
            )
          )
        )
        .toMap
      Right(State(items))
    }

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.OptionPartial"))
  implicit def stateV1PersistEventSerializer: PersistEventSerializer[State, StateV1] =
    state => {
      val itemsV1 = state.items.map { case (key, catalogItem) =>
        CatalogItemsV1(
          key,
          CatalogItemV1(
            id = catalogItem.id.toString,
            producerId = catalogItem.producerId.toString,
            name = catalogItem.name,
            audience = catalogItem.audience,
            versions = catalogItem.versions.map(ver =>
              CatalogItemVersionV1(
                id = ver.id.toString,
                version = ver.version,
                description = ver.description,
                docs = ver.docs.map { doc =>
                  CatalogItemDocumentV1(
                    id = doc.id.toString,
                    name = doc.name,
                    contentType = doc.contentType,
                    path = doc.path
                  )
                },
                status = ver.status
              )
            ),
            status = catalogItem.status
          )
        )
      }.toSeq
      Right(StateV1(itemsV1))
    }

  implicit def catalogItemV1AddedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemV1AddedV1, CatalogItemAdded] =
    event =>
      Right[Throwable, CatalogItemAdded](
        CatalogItemAdded(catalogItem =
          CatalogItem(
            id = UUID.fromString(event.catalogItem.id),
            producerId = UUID.fromString(event.catalogItem.producerId),
            name = event.catalogItem.name,
            audience = event.catalogItem.audience,
            versions = event.catalogItem.versions.map(ver1 =>
              CatalogItemVersion(
                id = UUID.fromString(ver1.id),
                version = ver1.version,
                description = ver1.description,
                docs = ver1.docs.map { doc =>
                  CatalogItemDocument(
                    id = UUID.fromString(doc.id),
                    name = doc.name,
                    contentType = doc.contentType,
                    path = doc.path
                  )
                },
                status = ver1.status
              )
            ),
            status = event.catalogItem.status
          )
        )
      )

  implicit def catalogItemV1AddedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemAdded, CatalogItemV1AddedV1] =
    event =>
      Right[Throwable, CatalogItemV1AddedV1] {
        CatalogItemV1AddedV1
          .of(
            CatalogItemV1(
              id = event.catalogItem.id.toString,
              producerId = event.catalogItem.producerId.toString,
              name = event.catalogItem.name,
              audience = event.catalogItem.audience,
              versions = event.catalogItem.versions.map(ver =>
                CatalogItemVersionV1(
                  id = ver.id.toString,
                  version = ver.version,
                  description = ver.description,
                  docs = ver.docs.map { doc =>
                    CatalogItemDocumentV1(
                      id = doc.id.toString,
                      name = doc.name,
                      contentType = doc.contentType,
                      path = doc.path
                    )
                  },
                  status = ver.status
                )
              ),
              status = event.catalogItem.status
            )
          )
      }

}
