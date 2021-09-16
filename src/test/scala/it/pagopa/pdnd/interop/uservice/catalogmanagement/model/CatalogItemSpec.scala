package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class CatalogItemSpec extends AnyWordSpecLike with Matchers {

  private[this] def catalogItemGen(descriptors: Seq[CatalogDescriptor]) = CatalogItem(
    id = UUID.randomUUID(),
    producerId = UUID.randomUUID(),
    name = "String",
    description = "String",
    technology = "String",
    attributes = CatalogAttributes(
      certified = Seq.empty,
      declared = Seq.empty,
      verified = Seq.empty
    ),
    descriptors = descriptors
  )

  private[this] def descriptorGen(version: String) =
    CatalogDescriptor(id = UUID.randomUUID(),
    version = version,
    description = None,
    interface = None,
    docs = Seq.empty,
    status = Draft, audience = Seq("a"), voucherLifespan = 0)


  "a CatalogItem" should {

    "return an empty version number since no descriptors has been defined" in {
      val descriptors: Seq[CatalogDescriptor] = Seq.empty
      val catalogItem = catalogItemGen(descriptors)
      catalogItem.currentVersion shouldBe None
    }

    "return version '1' since version '1' descriptor has been defined" in {
      val descriptors: Seq[CatalogDescriptor] = Seq(
        descriptorGen("1")
      )
      val catalogItem = catalogItemGen(descriptors)
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
