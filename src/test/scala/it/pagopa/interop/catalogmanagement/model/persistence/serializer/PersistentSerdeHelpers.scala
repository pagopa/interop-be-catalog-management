package it.pagopa.interop.catalogmanagement.model.persistence.serializer

import cats.syntax.all._
import com.softwaremill.diffx.Diff
import com.softwaremill.diffx.munit.DiffxAssertions
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.catalogmanagement.model.persistence._
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

trait PersistentSerdeHelpers extends ScalaCheckSuite with DiffxAssertions {

  // TODO move me in commons
  def serdeCheck[A: TypeTag, B](gen: Gen[(A, B)], adapter: B => B = identity[B](_))(implicit
    e: PersistEventSerializer[A, B],
    loc: munit.Location,
    d: => Diff[Either[Throwable, B]]
  ): Unit = property(s"${typeOf[A].typeSymbol.name.toString} is correctly serialized") {
    forAll(gen) { case (state, stateV1) =>
      implicit val diffX: Diff[Either[Throwable, B]] = d
      assertEqual(PersistEventSerializer.to[A, B](state).map(adapter), Right(stateV1).map(adapter))
    }
  }

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

  def stringGen: Gen[String] = for {
    n <- Gen.chooseNum(4, 100)
    s <- Gen.containerOfN[List, Char](n, Gen.alphaNumChar)
  } yield s.foldLeft("")(_ + _)

  def offsetDatetimeStringGen: Gen[(OffsetDateTime, String)] = for {
    n <- Gen.chooseNum(0, 10000L)
    now = OffsetDateTime.now()
    time <- Gen.oneOf(now.minusSeconds(n), now.plusSeconds(n))
  } yield (time, DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time))

  def offsetDatetimeLongGen: Gen[(OffsetDateTime, Long)] = for {
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

  def catalogItemTechnologyGen: Gen[(CatalogItemTechnology, CatalogItemTechnologyV1)] =
    Gen.oneOf((Rest, CatalogItemTechnologyV1.REST), (Soap, CatalogItemTechnologyV1.SOAP))

  def catalogAttributeValueGen: Gen[(CatalogAttribute, CatalogAttributeValueV1)] = for {
    id                   <- Gen.uuid
    explicitVerification <- Gen.oneOf(true, false)
  } yield (CatalogAttribute(id, explicitVerification), CatalogAttributeValueV1(id.toString, explicitVerification))

  def catalogAttributeGen: Gen[(List[CatalogAttribute], CatalogAttributeV1)]

  def catalogAttributesGen: Gen[(CatalogAttributes, CatalogAttributesV1)] = for {
    certified <- listOf(catalogAttributeGen)
    declared  <- listOf(catalogAttributeGen)
    verified  <- listOf(catalogAttributeGen)
  } yield {
    val (a, b) = certified.separate
    val (c, d) = declared.separate
    val (e, f) = verified.separate
    (CatalogAttributes(a, c, e), CatalogAttributesV1(b, d, f))
  }

  def catalogDocumentGen: Gen[(CatalogDocument, CatalogDocumentV1)] = for {
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

  def catalogDescriptorStateGen: Gen[(CatalogDescriptorState, CatalogDescriptorStateV1)] =
    Gen.oneOf(
      (Draft, DRAFT),
      (Published, PUBLISHED),
      (Deprecated, DEPRECATED),
      (Suspended, SUSPENDED),
      (Archived, ARCHIVED)
    )

  def agreementApprovalPolicyGen: Gen[(PersistentAgreementApprovalPolicy, AgreementApprovalPolicyV1)] =
    Gen.oneOf((Automatic, AUTOMATIC), (Manual, MANUAL))

  def catalogRiskAnalysisGen: Gen[(CatalogRiskAnalysis, CatalogItemRiskAnalysisV1)] = for {
    id                       <- Gen.uuid
    name                     <- stringGen
    (form, formV1)           <- catalogRiskAnalysisFormGen
    (createdAt, createdAtV1) <- offsetDatetimeLongGen
  } yield (
    CatalogRiskAnalysis(id = id, name = name, riskAnalysisForm = form, createdAt = createdAt),
    CatalogItemRiskAnalysisV1(id = id.toString, name = name, riskAnalysisForm = formV1, createdAt = createdAtV1)
  )

  def catalogRiskAnalysisFormGen: Gen[(CatalogRiskAnalysisForm, CatalogRiskAnalysisFormV1)] = for {
    id                               <- Gen.uuid
    version                          <- stringGen
    (singleAnswers, singleAnswersV1) <- listOf(catalogRiskAnalysisSingleAnswersGen).map(_.separate)
    (multiAnswers, multiAnswersV1)   <- listOf(catalogRiskAnalysisMultiAnswersGen).map(_.separate)
  } yield (
    CatalogRiskAnalysisForm(id = id, version = version, singleAnswers = singleAnswers, multiAnswers = multiAnswers),
    CatalogRiskAnalysisFormV1(
      id = id.toString,
      version = version,
      singleAnswers = singleAnswersV1,
      multiAnswers = multiAnswersV1
    )
  )

  def catalogRiskAnalysisSingleAnswersGen: Gen[(CatalogRiskAnalysisSingleAnswer, CatalogRiskAnalysisSingleAnswerV1)] =
    for {
      id    <- Gen.uuid
      key   <- stringGen
      value <- stringGen
    } yield (
      CatalogRiskAnalysisSingleAnswer(id = id, key = key, value = value.some),
      CatalogRiskAnalysisSingleAnswerV1(id = id.toString, key = key, value = value.some)
    )

  def catalogRiskAnalysisMultiAnswersGen: Gen[(CatalogRiskAnalysisMultiAnswer, CatalogRiskAnalysisMultiAnswerV1)] =
    for {
      id     <- Gen.uuid
      key    <- stringGen
      values <- listOf(stringGen)
    } yield (
      CatalogRiskAnalysisMultiAnswer(id = id, key = key, values = values),
      CatalogRiskAnalysisMultiAnswerV1(id = id.toString, key = key, values = values)
    )

  def catalogDescriptorGen: Gen[(CatalogDescriptor, CatalogDescriptorV1)] = for {
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

  def catalogItemModeGen: Gen[(CatalogItemMode, CatalogItemModeV1)] =
    Gen.oneOf((Receive, CatalogItemModeV1.RECEIVE), (Deliver, CatalogItemModeV1.DELIVER))

  def catalogItemGen: Gen[(CatalogItem, CatalogItemV1)] = for {
    id                       <- Gen.uuid
    producerId               <- Gen.uuid
    name                     <- stringGen
    description              <- stringGen
    (tech, techV1)           <- catalogItemTechnologyGen
    (desc, descV1)           <- listOf(catalogDescriptorGen).map(_.separate)
    (risk, riskV1)           <- listOf(catalogRiskAnalysisGen).map(_.separate)
    (createdAt, createdAtV1) <- offsetDatetimeLongGen
    (mode, modeV1)           <- catalogItemModeGen
  } yield (
    CatalogItem(id, producerId, name, description, tech, None, desc, risk, mode, createdAt),
    CatalogItemV1(
      id.toString,
      producerId.toString,
      name,
      description,
      techV1,
      None,
      descV1,
      createdAtV1.some,
      riskV1,
      modeV1.some
    )
  )

  def stateGen: Gen[(State, StateV1)] =
    listOf(catalogItemGen).map(_.separate).map { case (items, itemsV1) =>
      val stateMap: Map[String, CatalogItem] = items.foldLeft(Map.empty[String, CatalogItem]) { case (map, item) =>
        map + (item.id.toString -> item)
      }
      val state: State                       = State(stateMap)
      val stateV1: StateV1                   = StateV1(itemsV1.map(i => CatalogItemsV1(i.id, i)))
      (state, stateV1)
    }

  def catalogItemAddedGen: Gen[(CatalogItemAdded, CatalogItemV1AddedV1)] = catalogItemGen.map { case (a, b) =>
    (CatalogItemAdded(a), CatalogItemV1AddedV1(b))
  }

  def clonedCatalogItemAddedGen: Gen[(ClonedCatalogItemAdded, ClonedCatalogItemV1AddedV1)] = catalogItemGen.map {
    case (a, b) => (ClonedCatalogItemAdded(a), ClonedCatalogItemV1AddedV1(b))
  }

  def catalogItemUpdatedGen: Gen[(CatalogItemUpdated, CatalogItemV1UpdatedV1)] = catalogItemGen.map { case (a, b) =>
    (CatalogItemUpdated(a), CatalogItemV1UpdatedV1(b))
  }

  def catalogItemWithDescriptorsDeletedGen
    : Gen[(CatalogItemWithDescriptorsDeleted, CatalogItemWithDescriptorsDeletedV1)] =
    for {
      (a, b) <- catalogItemGen
      id     <- stringGen
    } yield (CatalogItemWithDescriptorsDeleted(a, id), CatalogItemWithDescriptorsDeletedV1(b, id))

  def catalogItemDeletedGen: Gen[(CatalogItemDeleted, CatalogItemDeletedV1)] =
    stringGen.map(s => (CatalogItemDeleted(s), CatalogItemDeletedV1(s)))

  def catalogItemDocumentDeletedGen: Gen[(CatalogItemDocumentDeleted, CatalogItemDocumentDeletedV1)] = for {
    eServiceId   <- stringGen
    descriptorId <- stringGen
    documentId   <- stringGen
  } yield (
    CatalogItemDocumentDeleted(eServiceId, descriptorId, documentId),
    CatalogItemDocumentDeletedV1(eServiceId, descriptorId, documentId)
  )

  def catalogItemRiskAnalysisAddedGen: Gen[(CatalogItemRiskAnalysisAdded, CatalogItemRiskAnalysisAddedV1)] = for {
    (a, b)         <- catalogItemGen
    riskAnalysisId <- Gen.uuid
  } yield (
    CatalogItemRiskAnalysisAdded(a, riskAnalysisId.toString),
    CatalogItemRiskAnalysisAddedV1(b, riskAnalysisId.toString)
  )

  def catalogItemDescriptorAddedGen: Gen[(CatalogItemDescriptorAdded, CatalogItemDescriptorAddedV1)] = for {
    eServiceId <- stringGen
    (a, b)     <- catalogDescriptorGen
  } yield (CatalogItemDescriptorAdded(eServiceId, a), CatalogItemDescriptorAddedV1(eServiceId, b))

  def catalogItemDescriptorUpdatedGen: Gen[(CatalogItemDescriptorUpdated, CatalogItemDescriptorUpdatedV1)] = for {
    eServiceId <- stringGen
    (a, b)     <- catalogDescriptorGen
  } yield (CatalogItemDescriptorUpdated(eServiceId, a), CatalogItemDescriptorUpdatedV1(eServiceId, b))

  def catalogItemDocumentAddedGen: Gen[(CatalogItemDocumentAdded, CatalogItemDocumentAddedV1)] = for {
    eServiceId   <- stringGen
    descriptorId <- stringGen
    (doc, docV1) <- catalogDocumentGen
    isInterface  <- Gen.oneOf(true, false)
    serverUrls   <- listOf(stringGen)
  } yield (
    CatalogItemDocumentAdded(eServiceId, descriptorId, doc, isInterface, serverUrls),
    CatalogItemDocumentAddedV1(eServiceId, descriptorId, docV1, isInterface, serverUrls)
  )

  def catalogItemDocumentUpdatedGen: Gen[(CatalogItemDocumentUpdated, CatalogItemDocumentUpdatedV1)] = for {
    eServiceId   <- stringGen
    descriptorId <- stringGen
    documentId   <- stringGen
    (doc, docV1) <- catalogDocumentGen
    serverUrls   <- listOf(stringGen)
  } yield (
    CatalogItemDocumentUpdated(eServiceId, descriptorId, documentId, doc, serverUrls),
    CatalogItemDocumentUpdatedV1(eServiceId, descriptorId, documentId, docV1, serverUrls)
  )

  def movedGen: Gen[(MovedAttributesFromEserviceToDescriptors, MovedAttributesFromEserviceToDescriptorsV1)] =
    catalogItemGen.map { case (item, itemV1) =>
      (MovedAttributesFromEserviceToDescriptors(item), MovedAttributesFromEserviceToDescriptorsV1(itemV1))
    }

}

object PersistentSerdeHelpers {
  implicit class PimpedStateV1(val stateV1: StateV1) extends AnyVal {
    def sorted: StateV1 = stateV1.copy(items = stateV1.items.sortBy(_.key))
  }
}
