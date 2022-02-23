package it.pagopa.interop.catalogmanagement.model

import it.pagopa.interop.catalogmanagement.model.persistence.State
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import java.util.UUID

class CatalogItemSpec extends AnyWordSpecLike with Matchers {

  private[this] def catalogItemGen(descriptors: Seq[CatalogDescriptor]) = CatalogItem(
    id = UUID.randomUUID(),
    producerId = UUID.randomUUID(),
    name = "String",
    description = "String",
    technology = Rest,
    attributes = CatalogAttributes(certified = Seq.empty, declared = Seq.empty, verified = Seq.empty),
    descriptors = descriptors
  )

  private[this] def descriptorGen(
    version: String,
    interfaceFile: Option[CatalogDocument] = None,
    documents: Seq[CatalogDocument] = Seq.empty,
    descriptorId: UUID = UUID.randomUUID()
  ) =
    CatalogDescriptor(
      id = descriptorId,
      version = version,
      description = None,
      interface = interfaceFile,
      docs = documents,
      state = Draft,
      audience = Seq("a"),
      voucherLifespan = 0,
      dailyCallsMaxNumber = 10
    )

  private[this] def descriptorDocumentGen(id: UUID = UUID.randomUUID()) =
    CatalogDocument(
      id = id,
      name = "",
      contentType = "yaml",
      path = "",
      checksum = "",
      uploadDate = OffsetDateTime.now(),
      description = "fake"
    )

  "a CatalogItem" should {

    "properly delete a file if found in the documents collection" in {
      val uuid1           = UUID.randomUUID()
      val uuid2           = UUID.randomUUID()
      val uuid3           = UUID.randomUUID()
      val uuid4           = UUID.randomUUID()
      val toBeRemovedUUID = UUID.randomUUID()
      val uuid6           = UUID.randomUUID()

      val documents: Seq[CatalogDocument] = Seq(
        descriptorDocumentGen(uuid1),
        descriptorDocumentGen(uuid2),
        descriptorDocumentGen(uuid3),
        descriptorDocumentGen(uuid4),
        descriptorDocumentGen(toBeRemovedUUID),
        descriptorDocumentGen(uuid6)
      )

      val descriptorId = UUID.randomUUID()
      val descriptors: Seq[CatalogDescriptor] = Seq(
        descriptorGen(
          "1",
          documents = documents,
          descriptorId = descriptorId,
          interfaceFile = Some(descriptorDocumentGen())
        )
      )

      //given a catalog item with 6 documents and an interface
      val catalogItem = catalogItemGen(descriptors)

      val state = State(items = Map(catalogItem.id.toString -> catalogItem))

      //when we remove one of the documents
      val descriptor =
        state
          .deleteDocument(catalogItem.id.toString, descriptorId.toString, toBeRemovedUUID.toString)
          .items
          .get(catalogItem.id.toString)
          .flatMap(_.descriptors.find(_.id == descriptorId))
          .get

      //then
      descriptor.docs.map(_.id) should contain only (uuid1, uuid2, uuid3, uuid4, uuid6)
      descriptor.interface shouldBe a[Some[_]]
    }

    "properly delete the interface found in the documents collection" in {
      val uuid1 = UUID.randomUUID()
      val uuid2 = UUID.randomUUID()
      val uuid3 = UUID.randomUUID()
      val uuid4 = UUID.randomUUID()
      val uuid5 = UUID.randomUUID()
      val uuid6 = UUID.randomUUID()

      val interfaceId = UUID.randomUUID()

      val documents: Seq[CatalogDocument] = Seq(
        descriptorDocumentGen(uuid1),
        descriptorDocumentGen(uuid2),
        descriptorDocumentGen(uuid3),
        descriptorDocumentGen(uuid4),
        descriptorDocumentGen(uuid5),
        descriptorDocumentGen(uuid6)
      )

      val descriptorId = UUID.randomUUID()
      val descriptors: Seq[CatalogDescriptor] = Seq(
        descriptorGen(
          "1",
          documents = documents,
          descriptorId = descriptorId,
          interfaceFile = Some(descriptorDocumentGen(interfaceId))
        )
      )

      //given a catalog item with 6 documents and an interface
      val catalogItem = catalogItemGen(descriptors)
      val state       = State(items = Map(catalogItem.id.toString -> catalogItem))

      //when we remove one of the documents
      val descriptor = state
        .deleteDocument(catalogItem.id.toString, descriptorId.toString, interfaceId.toString)
        .items
        .get(catalogItem.id.toString)
        .flatMap(_.descriptors.find(_.id == descriptorId))
        .get

      //then
      descriptor.docs.map(_.id) should contain only (uuid1, uuid2, uuid3, uuid4, uuid5, uuid6)
      descriptor.interface shouldBe None
    }

    "return the same set of documents when the interface or the document to be deleted is not found in the documents collection" in {
      val uuid1 = UUID.randomUUID()
      val uuid2 = UUID.randomUUID()
      val uuid3 = UUID.randomUUID()
      val uuid4 = UUID.randomUUID()
      val uuid5 = UUID.randomUUID()
      val uuid6 = UUID.randomUUID()

      val interfaceId = UUID.randomUUID()

      val documents: Seq[CatalogDocument] = Seq(
        descriptorDocumentGen(uuid1),
        descriptorDocumentGen(uuid2),
        descriptorDocumentGen(uuid3),
        descriptorDocumentGen(uuid4),
        descriptorDocumentGen(uuid5),
        descriptorDocumentGen(uuid6)
      )

      val descriptorId = UUID.randomUUID()
      val descriptors: Seq[CatalogDescriptor] = Seq(
        descriptorGen(
          "1",
          documents = documents,
          descriptorId = descriptorId,
          interfaceFile = Some(descriptorDocumentGen(interfaceId))
        )
      )

      //given a catalog item with 6 documents and an interface
      val catalogItem = catalogItemGen(descriptors)
      val state       = State(items = Map(catalogItem.id.toString -> catalogItem))

      //when we remove a not existing document
      val descriptor = state
        .deleteDocument(catalogItem.id.toString, descriptorId.toString, UUID.randomUUID().toString)
        .items
        .get(catalogItem.id.toString)
        .flatMap(_.descriptors.find(_.id == descriptorId))
        .get

      descriptor.docs
        .map(_.id) should contain only (uuid1, uuid2, uuid3, uuid4, uuid5, uuid6)
      descriptor.interface shouldBe a[Some[_]]
    }

    "return the same set of documents when the descriptor is not found in the e-service" in {
      val uuid1 = UUID.randomUUID()
      val uuid2 = UUID.randomUUID()
      val uuid3 = UUID.randomUUID()
      val uuid4 = UUID.randomUUID()
      val uuid5 = UUID.randomUUID()
      val uuid6 = UUID.randomUUID()

      val interfaceId = UUID.randomUUID()

      val documents: Seq[CatalogDocument] = Seq(
        descriptorDocumentGen(uuid1),
        descriptorDocumentGen(uuid2),
        descriptorDocumentGen(uuid3),
        descriptorDocumentGen(uuid4),
        descriptorDocumentGen(uuid5),
        descriptorDocumentGen(uuid6)
      )

      val descriptorId = UUID.randomUUID()
      val descriptors: Seq[CatalogDescriptor] = Seq(
        descriptorGen(
          "1",
          documents = documents,
          descriptorId = descriptorId,
          interfaceFile = Some(descriptorDocumentGen(interfaceId))
        )
      )

      //given a catalog item with 6 documents and an interface
      val catalogItem = catalogItemGen(descriptors)
      val state       = State(items = Map(catalogItem.id.toString -> catalogItem))

      //when we remove one of the documents on a not existing descriptor
      val descriptor =
        state
          .deleteDocument(catalogItem.id.toString, UUID.randomUUID().toString, interfaceId.toString)
          .items
          .get(catalogItem.id.toString)
          .flatMap(_.descriptors.find(_.id == descriptorId))
          .get

      descriptor.docs.map(_.id) should contain allOf (uuid1, uuid2, uuid3, uuid4, uuid5, uuid6)
      descriptor.interface shouldBe a[Some[_]]
    }

    "return an empty version number since no descriptors has been defined" in {
      val descriptors: Seq[CatalogDescriptor] = Seq.empty
      val catalogItem                         = catalogItemGen(descriptors)
      catalogItem.currentVersion shouldBe None
    }

    "return version '1' since version '1' descriptor has been defined" in {
      val descriptors: Seq[CatalogDescriptor] = Seq(descriptorGen("1"))
      val catalogItem                         = catalogItemGen(descriptors)
      catalogItem.currentVersion shouldBe Some("1")
    }

    "return version '10' since version '10' descriptor is the biggest one" in {
      val descriptors: Seq[CatalogDescriptor] = Seq(
        descriptorGen("1"),
        descriptorGen("2"),
        descriptorGen("3"),
        descriptorGen("4"),
        descriptorGen("5"),
        descriptorGen("6"),
        descriptorGen("7"),
        descriptorGen("8"),
        descriptorGen("9"),
        descriptorGen("10")
      )
      val catalogItem = catalogItemGen(descriptors)
      catalogItem.currentVersion shouldBe Some("10")
    }

    "return version '8' since version '8' is the biggest one parsable as long" in {
      val descriptors: Seq[CatalogDescriptor] = Seq(
        descriptorGen("1"),
        descriptorGen("2"),
        descriptorGen("3zzz"),
        descriptorGen("4.1.1"),
        descriptorGen("xx5"),
        descriptorGen("6"),
        descriptorGen("xxx7"),
        descriptorGen("8"),
        descriptorGen("9xxx"),
        descriptorGen("10xxx")
      )
      val catalogItem = catalogItemGen(descriptors)
      catalogItem.currentVersion shouldBe Some("8")
    }
  }

}
