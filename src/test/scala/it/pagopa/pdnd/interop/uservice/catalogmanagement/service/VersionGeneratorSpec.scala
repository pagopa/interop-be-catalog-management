package it.pagopa.pdnd.interop.uservice.catalogmanagement.service

import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class VersionGeneratorSpec extends AnyWordSpec with Matchers with EitherValues {

  "a VersionGenerator.next " should {
    "increment a version properly" in {
      VersionGenerator.next(Some("10")).value shouldBe "11"
    }

    "return version '1' when no version has been provided" in {
      VersionGenerator.next(None).value shouldBe "1"
    }

    "return an error when an invalid version is provided" in {
      VersionGenerator.next(Some("xxx")).left.value.getMessage shouldBe "xxx is not a valid descriptor version"
    }
  }

}
