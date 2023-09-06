package it.pagopa.interop.catalogmanagement.model.persistence.serializer
import cats.implicits._
import com.softwaremill.diffx.Diff
import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx.munit.DiffxAssertions
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.catalogmanagement.model.persistence._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.PersistentSerializationSpec._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.catalog_item.AgreementApprovalPolicyV1.{
  AUTOMATIC,
  MANUAL
}
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.catalog_item.CatalogDescriptorStateV1._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.catalog_item._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.events._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.state._
import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

import java.time.format.DateTimeFormatter
import java.time.{OffsetDateTime, ZoneOffset}
import scala.reflect.runtime.universe.{TypeTag, typeOf}

class PersistentDeserializationSpec extends ScalaCheckSuite with DiffxAssertions {

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

  // TODO move me in commons
  def deserCheck[A, B: TypeTag](
    gen: Gen[(A, B)]
  )(implicit e: PersistEventDeserializer[B, A], loc: munit.Location, d: => Diff[Either[Throwable, A]]): Unit =
    property(s"${typeOf[B].typeSymbol.name.toString} is correctly deserialized") {
      forAll(gen) { case (state, stateV1) =>
        // * This is declared lazy in the signature to avoid a MethodTooBigException
        implicit val diffX: Diff[Either[Throwable, A]] = d
        assertEqual(PersistEventDeserializer.from[B, A](stateV1), Right(state))
      }
    }
}

object PersistentDeserializationSpec {

  val stringGen: Gen[String] = for {
    n <- Gen.chooseNum(4, 100)
    s <- Gen.containerOfN[List, Char](n, Gen.alphaNumChar)
  } yield s.foldLeft("")(_ + _)

  val offsetDatetimeStringGen: Gen[(OffsetDateTime, String)] = for {
    n <- Gen.chooseNum(0, 10000L)
    now = OffsetDateTime.now()
    time <- Gen.oneOf(now.minusSeconds(n), now.plusSeconds(n))
  } yield (time, DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time))

  val offsetDatetimeLongGen: Gen[(OffsetDateTime, Long)] = for {
    n <- Gen.chooseNum(0, 10000L)
    now      = OffsetDateTime.now(ZoneOffset.UTC)
    // Truncate to millis precision
    nowMills = now.withNano(now.getNano - (now.getNano % 1000000))
    time <- Gen.oneOf(nowMills.minusSeconds(n), nowMills.plusSeconds(n))
  } yield (time, time.toInstant.toEpochMilli)

  def listOf[T](gen: => Gen[T]): Gen[List[T]] = for {
    n    <- Gen.chooseNum(0, 10)
    list <- Gen.listOfN(n, gen)
  } yield list

  val catalogItemTechnologyGen: Gen[(CatalogItemTechnology, CatalogItemTechnologyV1)] =
    Gen.oneOf((Rest, CatalogItemTechnologyV1.REST), (Soap, CatalogItemTechnologyV1.SOAP))

  val catalogAttributeValueGen: Gen[(CatalogAttribute, CatalogAttributeValueV1)] = for {
    id                   <- Gen.uuid
    explicitVerification <- Gen.oneOf(true, false)
  } yield (CatalogAttribute(id, explicitVerification), CatalogAttributeValueV1(id.toString, explicitVerification))

  val singleCatalogAttributeGen: Gen[(Seq[CatalogAttribute], CatalogAttributeV1)] =
    catalogAttributeValueGen.map { case (av, avv1) =>
      (List(av), CatalogAttributeV1(single = Option(avv1), group = Nil))
    }

  val groupCatalogAttributeGen: Gen[(Seq[CatalogAttribute], CatalogAttributeV1)] =
    Gen.nonEmptyListOf(catalogAttributeValueGen).map { list =>
      val (a, b) = list.separate
      (a, CatalogAttributeV1(single = None, group = b))
    }

  val catalogAttributeGen: Gen[(Seq[CatalogAttribute], CatalogAttributeV1)] =
    Gen.oneOf(singleCatalogAttributeGen, groupCatalogAttributeGen)

  val catalogAttributesGen: Gen[(CatalogAttributes, CatalogAttributesV1)] = for {
    certified <- listOf(catalogAttributeGen)
    declared  <- listOf(catalogAttributeGen)
    verified  <- listOf(catalogAttributeGen)
  } yield {
    val (a, b) = certified.separate
    val (c, d) = declared.separate
    val (e, f) = verified.separate
    (CatalogAttributes(a, c, e), CatalogAttributesV1(b, d, f))
  }

  val catalogDocumentGen: Gen[(CatalogDocument, CatalogDocumentV1)] = for {
    id                        <- Gen.uuid
    name                      <- stringGen
    contentType               <- stringGen
    prettyName                <- stringGen
    path                      <- stringGen
    checksum                  <- stringGen
    (uploadDate, uploadDateS) <- offsetDatetimeStringGen
  } yield (
    CatalogDocument(
      id = id,
      name = name,
      contentType = contentType,
      prettyName = prettyName,
      path = path,
      checksum = checksum,
      uploadDate = uploadDate
    ),
    CatalogDocumentV1(
      id = id.toString,
      name = name,
      contentType = contentType,
      path = path,
      checksum = checksum,
      uploadDate = uploadDateS,
      prettyName = prettyName
    )
  )

  val catalogDescriptorStateGen: Gen[(CatalogDescriptorState, CatalogDescriptorStateV1)] =
    Gen.oneOf(
      (Draft, DRAFT),
      (Published, PUBLISHED),
      (Deprecated, DEPRECATED),
      (Suspended, SUSPENDED),
      (Archived, ARCHIVED)
    )

  val agreementApprovalPolicyGen: Gen[(PersistentAgreementApprovalPolicy, AgreementApprovalPolicyV1)] =
    Gen.oneOf((Automatic, AUTOMATIC), (Manual, MANUAL))

  val catalogDescriptorGen: Gen[(CatalogDescriptor, CatalogDescriptorV1)] = for {
    id                             <- Gen.uuid
    version                        <- stringGen
    description                    <- Gen.alphaNumStr.map(Option(_).filter(_.nonEmpty))
    (interface, interfaceV1)       <- catalogDocumentGen.map { case (a, b) => (Option(a), Option(b)) }
    (docs, docsV1)                 <- listOf(catalogDocumentGen).map(_.separate)
    (state, stateV1)               <- catalogDescriptorStateGen
    audience                       <- listOf(stringGen)
    voucherLifespan                <- Gen.posNum[Int]
    dailyCallsPerConsumer          <- Gen.posNum[Int]
    dailyCallsTotal                <- Gen.posNum[Int]
    (policy, policyV1)             <- agreementApprovalPolicyGen
    (createdAt, createdAtV1)       <- offsetDatetimeLongGen
    serverUrls                     <- listOf(stringGen)
    (publishedAt, publishedAtV1)   <- offsetDatetimeLongGen
    (suspendedAt, suspendedAtV1)   <- offsetDatetimeLongGen
    (deprecatedAt, deprecatedAtV1) <- offsetDatetimeLongGen
    (archivedAt, archivedAtV1)     <- offsetDatetimeLongGen
    (attributes, attributesV1)     <- catalogAttributesGen
  } yield (
    CatalogDescriptor(
      id = id,
      version = version,
      description = description,
      interface = interface,
      docs = docs,
      state = state,
      audience = audience,
      voucherLifespan = voucherLifespan,
      dailyCallsPerConsumer = dailyCallsPerConsumer,
      dailyCallsTotal = dailyCallsTotal,
      agreementApprovalPolicy = policy.some,
      createdAt = createdAt,
      serverUrls = serverUrls,
      publishedAt = if (state == Draft) None else publishedAt.some,
      suspendedAt = suspendedAt.some,
      deprecatedAt = deprecatedAt.some,
      archivedAt = archivedAt.some,
      attributes = attributes
    ),
    CatalogDescriptorV1(
      id = id.toString,
      version = version,
      description = description,
      docs = docsV1,
      state = stateV1,
      interface = interfaceV1,
      audience = audience,
      voucherLifespan = voucherLifespan,
      dailyCallsPerConsumer = dailyCallsPerConsumer,
      dailyCallsTotal = dailyCallsTotal,
      agreementApprovalPolicy = policyV1.some,
      createdAt = createdAtV1.some,
      serverUrls = serverUrls,
      publishedAt = if (stateV1.isDraft) None else publishedAtV1.some,
      suspendedAt = suspendedAtV1.some,
      deprecatedAt = deprecatedAtV1.some,
      archivedAt = archivedAtV1.some,
      attributes = attributesV1.some
    )
  )

  val catalogItemGen: Gen[(CatalogItem, CatalogItemV1)] = for {
    id                       <- Gen.uuid
    producerId               <- Gen.uuid
    name                     <- stringGen
    description              <- stringGen
    (tech, techV1)           <- catalogItemTechnologyGen
    (desc, descV1)           <- listOf(catalogDescriptorGen).map(_.separate)
    (createdAt, createdAtV1) <- offsetDatetimeLongGen
  } yield (
    CatalogItem(id, producerId, name, description, tech, None, desc, createdAt),
    CatalogItemV1(id.toString, producerId.toString, name, description, techV1, None, descV1, createdAtV1.some)
  )

  val stateGen: Gen[(State, StateV1)] =
    listOf(catalogItemGen).map(_.separate).map { case (items, itemsV1) =>
      val stateMap: Map[String, CatalogItem] = items.foldLeft(Map.empty[String, CatalogItem]) { case (map, item) =>
        map + (item.id.toString -> item)
      }
      val state: State                       = State(stateMap)
      val stateV1: StateV1                   = StateV1(itemsV1.map(i => CatalogItemsV1(i.id, i)))
      (state, stateV1)
    }

  implicit class PimpedStateV1(val stateV1: StateV1) extends AnyVal {
    def sorted: StateV1 = stateV1.copy(items = stateV1.items.sortBy(_.key))
  }

  val catalogItemAddedGen: Gen[(CatalogItemAdded, CatalogItemV1AddedV1)] = catalogItemGen.map { case (a, b) =>
    (CatalogItemAdded(a), CatalogItemV1AddedV1(b))
  }

  val clonedCatalogItemAddedGen: Gen[(ClonedCatalogItemAdded, ClonedCatalogItemV1AddedV1)] = catalogItemGen.map {
    case (a, b) => (ClonedCatalogItemAdded(a), ClonedCatalogItemV1AddedV1(b))
  }

  val catalogItemUpdatedGen: Gen[(CatalogItemUpdated, CatalogItemV1UpdatedV1)] = catalogItemGen.map { case (a, b) =>
    (CatalogItemUpdated(a), CatalogItemV1UpdatedV1(b))
  }

  val catalogItemWithDescriptorsDeletedGen
    : Gen[(CatalogItemWithDescriptorsDeleted, CatalogItemWithDescriptorsDeletedV1)] =
    for {
      (a, b) <- catalogItemGen
      id     <- stringGen
    } yield (CatalogItemWithDescriptorsDeleted(a, id), CatalogItemWithDescriptorsDeletedV1(b, id))

  val catalogItemDeletedGen: Gen[(CatalogItemDeleted, CatalogItemDeletedV1)] =
    stringGen.map(s => (CatalogItemDeleted(s), CatalogItemDeletedV1(s)))

  val catalogItemDocumentDeletedGen: Gen[(CatalogItemDocumentDeleted, CatalogItemDocumentDeletedV1)] = for {
    eServiceId   <- stringGen
    descriptorId <- stringGen
    documentId   <- stringGen
  } yield (
    CatalogItemDocumentDeleted(eServiceId, descriptorId, documentId),
    CatalogItemDocumentDeletedV1(eServiceId, descriptorId, documentId)
  )

  val catalogItemDescriptorAddedGen: Gen[(CatalogItemDescriptorAdded, CatalogItemDescriptorAddedV1)] = for {
    eServiceId <- stringGen
    (a, b)     <- catalogDescriptorGen
  } yield (CatalogItemDescriptorAdded(eServiceId, a), CatalogItemDescriptorAddedV1(eServiceId, b))

  val catalogItemDescriptorUpdatedGen: Gen[(CatalogItemDescriptorUpdated, CatalogItemDescriptorUpdatedV1)] = for {
    eServiceId <- stringGen
    (a, b)     <- catalogDescriptorGen
  } yield (CatalogItemDescriptorUpdated(eServiceId, a), CatalogItemDescriptorUpdatedV1(eServiceId, b))

  val catalogItemDocumentAddedGen: Gen[(CatalogItemDocumentAdded, CatalogItemDocumentAddedV1)] = for {
    eServiceId   <- stringGen
    descriptorId <- stringGen
    (doc, docV1) <- catalogDocumentGen
    isInterface  <- Gen.oneOf(true, false)
    serverUrls   <- listOf(stringGen)
  } yield (
    CatalogItemDocumentAdded(eServiceId, descriptorId, doc, isInterface, serverUrls),
    CatalogItemDocumentAddedV1(eServiceId, descriptorId, docV1, isInterface, serverUrls)
  )

  val catalogItemDocumentUpdatedGen: Gen[(CatalogItemDocumentUpdated, CatalogItemDocumentUpdatedV1)] = for {
    eServiceId   <- stringGen
    descriptorId <- stringGen
    documentId   <- stringGen
    (doc, docV1) <- catalogDocumentGen
    serverUrls   <- listOf(stringGen)
  } yield (
    CatalogItemDocumentUpdated(eServiceId, descriptorId, documentId, doc, serverUrls),
    CatalogItemDocumentUpdatedV1(eServiceId, descriptorId, documentId, docV1, serverUrls)
  )

  val movedGen: Gen[(MovedAttributesFromEserviceToDescriptors, MovedAttributesFromEserviceToDescriptorsV1)] =
    catalogItemGen.map { case (item, itemV1) =>
      (MovedAttributesFromEserviceToDescriptors(item), MovedAttributesFromEserviceToDescriptorsV1(itemV1))
    }

}
