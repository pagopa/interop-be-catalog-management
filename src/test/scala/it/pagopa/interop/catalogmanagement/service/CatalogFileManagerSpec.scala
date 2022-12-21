package it.pagopa.interop.catalogmanagement.service

import akka.http.scaladsl.model.{ContentTypes, HttpCharsets, MediaType}
import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.interop.catalogmanagement.error.CatalogManagementErrors.InvalidInterfaceFileDetected
import it.pagopa.interop.catalogmanagement.model._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.File
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.UUID

class CatalogFileManagerSpec() extends AnyWordSpec with Matchers with ScalaFutures {

  val descriptorId: UUID       = UUID.randomUUID()
  val catalogItem: CatalogItem = CatalogItem(
    id = UUID.randomUUID(),
    producerId = UUID.randomUUID(),
    name = "name",
    description = "description",
    technology = Rest,
    attributes = CatalogAttributes(certified = Seq.empty, declared = Seq.empty, verified = Seq.empty),
    descriptors = Seq(
      CatalogDescriptor(
        id = descriptorId,
        version = "1",
        description = None,
        interface = None,
        docs = Seq.empty,
        state = Published,
        audience = Seq.empty,
        voucherLifespan = 1,
        dailyCallsPerConsumer = 1,
        dailyCallsTotal = 1,
        agreementApprovalPolicy = Some(Automatic),
        createdAt = OffsetDateTime.now(),
        activatedAt = None
      )
    ),
    createdAt = OffsetDateTime.now()
  )

  "a CatalogFileManager.verify " should {
    "succeed with a JSON file" in {

      val file = Paths.get("src/test/resources/apis.json").toFile

      val fileParts: (FileInfo, File) =
        (FileInfo(fieldName = "apis", fileName = "apis.json", contentType = ContentTypes.`application/json`), file)

      CatalogFileManager
        .verify(
          fileParts = fileParts,
          catalogItem = catalogItem,
          descriptorId = descriptorId.toString,
          isInterface = true
        )(Seq.empty) shouldBe Right(catalogItem)
    }

    "succeed with a YAML file" in {

      val file = Paths.get("src/test/resources/apis.json").toFile

      val fileParts: (FileInfo, File) =
        (
          FileInfo(
            fieldName = "apis",
            fileName = "apis.yaml",
            contentType =
              MediaType.customWithFixedCharset("application", "x-yaml", HttpCharsets.`UTF-8`, List("yaml", "yml"))
          ),
          file
        )

      CatalogFileManager
        .verify(
          fileParts = fileParts,
          catalogItem = catalogItem,
          descriptorId = descriptorId.toString,
          isInterface = true
        )(Seq.empty) shouldBe Right(catalogItem)
    }

    "succeed with a WSDL file" in {

      val file = Paths.get("src/test/resources/apis.wsdl").toFile

      val fileParts: (FileInfo, File) =
        (
          FileInfo(
            fieldName = "apis",
            fileName = "apis.wsdl",
            contentType =
              MediaType.customWithFixedCharset("application", "wsdl+xml", HttpCharsets.`UTF-8`, List("wsdl"))
          ),
          file
        )

      val soapCatalogItem: CatalogItem = catalogItem.copy(technology = Soap)

      CatalogFileManager
        .verify(
          fileParts = fileParts,
          catalogItem = soapCatalogItem,
          descriptorId = descriptorId.toString,
          isInterface = true
        )(Seq.empty) shouldBe Right(soapCatalogItem)
    }

    "succeed with a XML file" in {

      val file = Paths.get("src/test/resources/apis.xml").toFile

      val fileParts: (FileInfo, File) =
        (
          FileInfo(
            fieldName = "apis",
            fileName = "apis.xml",
            contentType = MediaType.customWithFixedCharset("application", "soap+xml", HttpCharsets.`UTF-8`, List("xml"))
          ),
          file
        )

      val soapCatalogItem: CatalogItem = catalogItem.copy(technology = Soap)

      CatalogFileManager
        .verify(
          fileParts = fileParts,
          catalogItem = soapCatalogItem,
          descriptorId = descriptorId.toString,
          isInterface = true
        )(Seq.empty) shouldBe Right(soapCatalogItem)
    }

    "fail for unexpected file format" in {

      val file = Paths.get("src/test/resources/apis.conf").toFile

      val fileParts: (FileInfo, File) =
        (
          FileInfo(
            fieldName = "apis",
            fileName = "apis.conf",
            contentType = MediaType.customWithFixedCharset("text", "plain", HttpCharsets.`UTF-8`, List("conf"))
          ),
          file
        )

      CatalogFileManager
        .verify(
          fileParts = fileParts,
          catalogItem = catalogItem,
          descriptorId = descriptorId.toString,
          isInterface = true
        )(Seq.empty) shouldBe Left(
        InvalidInterfaceFileDetected(catalogItem.id.toString, "text/x-config", catalogItem.technology.toString)
      )
    }

  }
}
