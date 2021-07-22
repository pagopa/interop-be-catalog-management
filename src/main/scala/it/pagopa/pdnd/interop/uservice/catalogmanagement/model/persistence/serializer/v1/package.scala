package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer

import cats.implicits._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.catalog_item.{
  CatalogDescriptorStatusV1,
  CatalogDescriptorV1,
  CatalogDocumentV1,
  CatalogItemV1
}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.events.CatalogItemV1AddedV1
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.state.{CatalogItemsV1, StateV1}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.{
  CatalogDescriptor,
  CatalogDescriptorStatus,
  CatalogDocument,
  CatalogItem
}

import java.util.UUID
package object v1 {

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any"))
  implicit def stateV1PersistEventDeserializer: PersistEventDeserializer[StateV1, State] =
    state => {
      val items: Either[Throwable, Seq[(String, CatalogItem)]] = state.items.traverse { itemsV1 =>
        val catalogDescriptors: Either[Throwable, Seq[CatalogDescriptor]] =
          convertDescriptorsFromV1(itemsV1.value.descriptors)

        catalogDescriptors.map(descriptors =>
          itemsV1.key -> CatalogItem(
            id = UUID.fromString(itemsV1.value.id),
            producerId = UUID.fromString(itemsV1.value.producerId),
            name = itemsV1.value.name,
            audience = itemsV1.value.audience,
            technology = itemsV1.value.technology,
            descriptors = descriptors
          )
        )

      }

      items.map(its => State(its.toMap))
    }

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any"))
  implicit def stateV1PersistEventSerializer: PersistEventSerializer[State, StateV1] =
    state => {
      val itemsV1: Either[RuntimeException, Seq[CatalogItemsV1]] = state.items.toSeq.traverse {
        case (key, catalogItem) =>
          val descriptors: Either[RuntimeException, Seq[CatalogDescriptorV1]] =
            convertDescriptorsToV1(catalogItem.descriptors)

          descriptors.map { ds =>
            CatalogItemsV1(
              key,
              CatalogItemV1(
                id = catalogItem.id.toString,
                producerId = catalogItem.producerId.toString,
                name = catalogItem.name,
                audience = catalogItem.audience,
                technology = catalogItem.technology,
                descriptors = ds
              )
            )

          }

      }

      itemsV1.map(its => StateV1(its))
    }

  implicit def catalogItemV1AddedV1PersistEventDeserializer
    : PersistEventDeserializer[CatalogItemV1AddedV1, CatalogItemAdded] =
    event => {
      val descriptors: Either[Throwable, Seq[CatalogDescriptor]] =
        convertDescriptorsFromV1(event.catalogItem.descriptors)

      descriptors.map { ds =>
        CatalogItemAdded(catalogItem =
          CatalogItem(
            id = UUID.fromString(event.catalogItem.id),
            producerId = UUID.fromString(event.catalogItem.producerId),
            name = event.catalogItem.name,
            audience = event.catalogItem.audience,
            technology = event.catalogItem.technology,
            descriptors = ds
          )
        )
      }

    }

  implicit def catalogItemV1AddedV1PersistEventSerializer
    : PersistEventSerializer[CatalogItemAdded, CatalogItemV1AddedV1] =
    event => {
      val descriptors: Either[RuntimeException, Seq[CatalogDescriptorV1]] =
        convertDescriptorsToV1(event.catalogItem.descriptors)

      descriptors.map { ds =>
        CatalogItemV1AddedV1
          .of(
            CatalogItemV1(
              id = event.catalogItem.id.toString,
              producerId = event.catalogItem.producerId.toString,
              name = event.catalogItem.name,
              audience = event.catalogItem.audience,
              technology = event.catalogItem.technology,
              descriptors = ds
            )
          )

      }

    }

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any"))
  private def convertDescriptorsToV1(
    descriptors: Seq[CatalogDescriptor]
  ): Either[RuntimeException, Seq[CatalogDescriptorV1]] = {
    descriptors.traverse { ver =>
      CatalogDescriptorStatusV1
        .fromName(ver.status.stringify)
        .toRight(new RuntimeException("Invalid descriptor status"))
        .map { status =>
          CatalogDescriptorV1(
            id = ver.id.toString,
            version = ver.version,
            description = ver.description,
            voucherLifespan = ver.voucherLifespan,
            docs = ver.docs.map { doc =>
              CatalogDocumentV1(id = doc.id.toString, name = doc.name, contentType = doc.contentType, path = doc.path)
            },
            status = status
          )
        }

    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any"))
  private def convertDescriptorsFromV1(
    descriptors: Seq[CatalogDescriptorV1]
  ): Either[Throwable, Seq[CatalogDescriptor]] = {
    descriptors.traverse(ver1 =>
      CatalogDescriptorStatus.fromText(ver1.status.name).map { status =>
        CatalogDescriptor(
          id = UUID.fromString(ver1.id),
          version = ver1.version,
          description = ver1.description,
          voucherLifespan = ver1.voucherLifespan,
          docs = ver1.docs.map { doc =>
            CatalogDocument(
              id = UUID.fromString(doc.id),
              name = doc.name,
              contentType = doc.contentType,
              path = doc.path
            )
          },
          status = status
        )
      }
    )
  }

}
