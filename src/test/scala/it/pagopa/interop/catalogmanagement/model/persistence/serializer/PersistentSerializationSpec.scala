package it.pagopa.interop.catalogmanagement.model.persistence.serializer
import cats.implicits._
import org.scalacheck.Prop.forAll
import org.scalacheck.Gen
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import munit.ScalaCheckSuite
// import cats.kernel.Eq
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.catalog_item.CatalogDescriptorStateV1._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.catalog_item._
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.catalogmanagement.model.persistence._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.state._
import PersistentSerializationSpec._
import cats.kernel.Eq
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.events._

class PersistentSerializationSpec extends ScalaCheckSuite {

  property("State is correctly deserialized") {
    forAll(stateGen) { case (state, stateV1) =>
      assertEquals(PersistEventDeserializer.from[StateV1, State](stateV1), Either.right[Throwable, State](state))
    }
  }

  property("CatalogItemAdded is correctly deserialized") {
    forAll(catalogItemAddedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventDeserializer.from[CatalogItemV1AddedV1, CatalogItemAdded](stateV1),
        Either.right[Throwable, CatalogItemAdded](state)
      )
    }
  }

  property("ClonedCatalogItemAdded is correctly deserialized") {
    forAll(clonedCatalogItemAddedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventDeserializer.from[ClonedCatalogItemV1AddedV1, ClonedCatalogItemAdded](stateV1),
        Either.right[Throwable, ClonedCatalogItemAdded](state)
      )
    }
  }

  property("CatalogItemUpdated is correctly deserialized") {
    forAll(catalogItemUpdatedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventDeserializer.from[CatalogItemV1UpdatedV1, CatalogItemUpdated](stateV1),
        Either.right[Throwable, CatalogItemUpdated](state)
      )
    }
  }

  property("CatalogItemWithDescriptorsDeleted is correctly deserialized") {
    forAll(catalogItemWithDescriptorsDeletedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventDeserializer.from[CatalogItemWithDescriptorsDeletedV1, CatalogItemWithDescriptorsDeleted](stateV1),
        Either.right[Throwable, CatalogItemWithDescriptorsDeleted](state)
      )
    }
  }
  property("CatalogItemDeleted is correctly deserialized") {
    forAll(catalogItemDeletedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventDeserializer.from[CatalogItemDeletedV1, CatalogItemDeleted](stateV1),
        Either.right[Throwable, CatalogItemDeleted](state)
      )
    }
  }
  property("CatalogItemDocumentDeleted is correctly deserialized") {
    forAll(catalogItemDocumentDeletedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventDeserializer.from[CatalogItemDocumentDeletedV1, CatalogItemDocumentDeleted](stateV1),
        Either.right[Throwable, CatalogItemDocumentDeleted](state)
      )
    }
  }
  property("CatalogItemDescriptorAdded is correctly deserialized") {
    forAll(catalogItemDescriptorAddedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventDeserializer.from[CatalogItemDescriptorAddedV1, CatalogItemDescriptorAdded](stateV1),
        Either.right[Throwable, CatalogItemDescriptorAdded](state)
      )
    }
  }
  property("CatalogItemDescriptorUpdated is correctly deserialized") {
    forAll(catalogItemDescriptorUpdatedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventDeserializer.from[CatalogItemDescriptorUpdatedV1, CatalogItemDescriptorUpdated](stateV1),
        Either.right[Throwable, CatalogItemDescriptorUpdated](state)
      )
    }
  }
  property("CatalogItemDocumentAdded is correctly deserialized") {
    forAll(catalogItemDocumentAddedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventDeserializer.from[CatalogItemDocumentAddedV1, CatalogItemDocumentAdded](stateV1),
        Either.right[Throwable, CatalogItemDocumentAdded](state)
      )
    }
  }
  property("CatalogItemDocumentUpdated is correctly deserialized") {
    forAll(catalogItemDocumentUpdatedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventDeserializer.from[CatalogItemDocumentUpdatedV1, CatalogItemDocumentUpdated](stateV1),
        Either.right[Throwable, CatalogItemDocumentUpdated](state)
      )
    }
  }

  // * Equality has been customized to do not get affected
  // * by different collection kind and order
  property("State is correctly serialized") {
    forAll(stateGen) { case (state, stateV1) =>
      Either.right[Throwable, StateV1](stateV1) === PersistEventSerializer.to[State, StateV1](state)
    }
  }

  property("CatalogItemAdded is correctly serialized") {
    forAll(catalogItemAddedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventSerializer.to[CatalogItemAdded, CatalogItemV1AddedV1](state),
        Either.right[Throwable, CatalogItemV1AddedV1](stateV1)
      )
    }
  }

  property("ClonedCatalogItemAdded is correctly serialized") {
    forAll(clonedCatalogItemAddedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventSerializer.to[ClonedCatalogItemAdded, ClonedCatalogItemV1AddedV1](state),
        Either.right[Throwable, ClonedCatalogItemV1AddedV1](stateV1)
      )
    }
  }

  property("CatalogItemUpdated is correctly serialized") {
    forAll(catalogItemUpdatedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventSerializer.to[CatalogItemUpdated, CatalogItemV1UpdatedV1](state),
        Either.right[Throwable, CatalogItemV1UpdatedV1](stateV1)
      )
    }
  }

  property("CatalogItemWithDescriptorsDeleted is correctly serialized") {
    forAll(catalogItemWithDescriptorsDeletedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventSerializer.to[CatalogItemWithDescriptorsDeleted, CatalogItemWithDescriptorsDeletedV1](state),
        Either.right[Throwable, CatalogItemWithDescriptorsDeletedV1](stateV1)
      )
    }
  }
  property("CatalogItemDeleted is correctly serialized") {
    forAll(catalogItemDeletedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventSerializer.to[CatalogItemDeleted, CatalogItemDeletedV1](state),
        Either.right[Throwable, CatalogItemDeletedV1](stateV1)
      )
    }
  }
  property("CatalogItemDocumentDeleted is correctly serialized") {
    forAll(catalogItemDocumentDeletedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventSerializer.to[CatalogItemDocumentDeleted, CatalogItemDocumentDeletedV1](state),
        Either.right[Throwable, CatalogItemDocumentDeletedV1](stateV1)
      )
    }
  }
  property("CatalogItemDescriptorAdded is correctly serialized") {
    forAll(catalogItemDescriptorAddedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventSerializer.to[CatalogItemDescriptorAdded, CatalogItemDescriptorAddedV1](state),
        Either.right[Throwable, CatalogItemDescriptorAddedV1](stateV1)
      )
    }
  }
  property("CatalogItemDescriptorUpdated is correctly serialized") {
    forAll(catalogItemDescriptorUpdatedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventSerializer.to[CatalogItemDescriptorUpdated, CatalogItemDescriptorUpdatedV1](state),
        Either.right[Throwable, CatalogItemDescriptorUpdatedV1](stateV1)
      )
    }
  }
  property("CatalogItemDocumentAdded is correctly serialized") {
    forAll(catalogItemDocumentAddedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventSerializer.to[CatalogItemDocumentAdded, CatalogItemDocumentAddedV1](state),
        Either.right[Throwable, CatalogItemDocumentAddedV1](stateV1)
      )
    }
  }
  property("CatalogItemDocumentUpdated is correctly serialized") {
    forAll(catalogItemDocumentUpdatedGen) { case (state, stateV1) =>
      assertEquals(
        PersistEventSerializer.to[CatalogItemDocumentUpdated, CatalogItemDocumentUpdatedV1](state),
        Either.right[Throwable, CatalogItemDocumentUpdatedV1](stateV1)
      )
    }
  }
}

object PersistentSerializationSpec {

  val stringGen: Gen[String] = for {
    n <- Gen.chooseNum(4, 100)
    s <- Gen.containerOfN[List, Char](n, Gen.alphaNumChar)
  } yield s.foldLeft("")(_ + _)

  val offsetDatetimeGen: Gen[(OffsetDateTime, String)] = for {
    n <- Gen.chooseNum(0, 10000L)
    now = OffsetDateTime.now()
    time <- Gen.oneOf(now.minusSeconds(n), now.plusSeconds(n))
  } yield (time, DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time))

  def listOf[T](gen: => Gen[T]): Gen[List[T]] = for {
    n    <- Gen.chooseNum(0, 10)
    list <- Gen.listOfN(n, gen)
  } yield list

  val catalogItemTechnologyGen: Gen[(CatalogItemTechnology, CatalogItemTechnologyV1)] =
    Gen.oneOf((Rest, CatalogItemTechnologyV1.REST), (Soap, CatalogItemTechnologyV1.SOAP))

  val catalogAttributeValueGen: Gen[(CatalogAttributeValue, CatalogAttributeValueV1)] = for {
    id                   <- stringGen
    explicitVerification <- Gen.oneOf(true, false)
  } yield (CatalogAttributeValue(id, explicitVerification), CatalogAttributeValueV1(id, explicitVerification))

  val singleCatalogAttributeGen: Gen[(SingleAttribute, CatalogAttributeV1)] =
    catalogAttributeValueGen.map { case (av, avv1) =>
      (SingleAttribute(av), CatalogAttributeV1(single = Option(avv1), group = Nil))
    }

  val groupCatalogAttributeGen: Gen[(GroupAttribute, CatalogAttributeV1)] =
    Gen.nonEmptyListOf(catalogAttributeValueGen).map { list =>
      val (a, b) = list.separate
      (GroupAttribute(a), CatalogAttributeV1(single = None, group = b))
    }

  val catalogAttributeGen: Gen[(CatalogAttribute, CatalogAttributeV1)] =
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
    (uploadDate, uploadDateS) <- offsetDatetimeGen
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
      id = id.toString(),
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

  val catalogDescriptorGen: Gen[(CatalogDescriptor, CatalogDescriptorV1)] = for {
    id                       <- Gen.uuid
    version                  <- stringGen
    description              <- Gen.alphaNumStr.map(Option(_).filter(_.nonEmpty))
    (interface, interfaceV1) <- catalogDocumentGen.map { case (a, b) => (Option(a), Option(b)) }
    (docs, docsV1)           <- listOf(catalogDocumentGen).map(_.separate)
    (state, stateV1)         <- catalogDescriptorStateGen
    audience                 <- listOf(stringGen)
    voucherLifespan          <- Gen.posNum[Int]
    dailyCallsPerConsumer    <- Gen.posNum[Int]
    dailyCallsTotal          <- Gen.posNum[Int]
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
      dailyCallsTotal = dailyCallsTotal
    ),
    CatalogDescriptorV1(
      id = id.toString(),
      version = version,
      description = description,
      docs = docsV1,
      state = stateV1,
      interface = interfaceV1,
      audience = audience,
      voucherLifespan = voucherLifespan,
      dailyCallsPerConsumer = dailyCallsPerConsumer,
      dailyCallsTotal = dailyCallsTotal
    )
  )

  val catalogItemGen: Gen[(CatalogItem, CatalogItemV1)] = for {
    id             <- Gen.uuid
    producerId     <- Gen.uuid
    name           <- stringGen
    description    <- stringGen
    (tech, techV1) <- catalogItemTechnologyGen
    (attr, attrV1) <- catalogAttributesGen
    (desc, descV1) <- listOf(catalogDescriptorGen).map(_.separate)
  } yield (
    CatalogItem(id, producerId, name, description, tech, attr, desc),
    CatalogItemV1(id.toString(), producerId.toString(), name, description, techV1, attrV1, descV1)
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

  implicit val throwableEq: Eq[Throwable] = Eq.fromUniversalEquals

  implicit val stateEq: Eq[StateV1] = Eq.instance { case (stateA, stateB) =>
    stateA.items.sortBy(_.key) == stateB.items.sortBy(_.key)
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
  } yield (
    CatalogItemDocumentAdded(eServiceId, descriptorId, doc, isInterface),
    CatalogItemDocumentAddedV1(eServiceId, descriptorId, docV1, isInterface)
  )

  val catalogItemDocumentUpdatedGen: Gen[(CatalogItemDocumentUpdated, CatalogItemDocumentUpdatedV1)] = for {
    eServiceId   <- stringGen
    descriptorId <- stringGen
    documentId   <- stringGen
    (doc, docV1) <- catalogDocumentGen
  } yield (
    CatalogItemDocumentUpdated(eServiceId, descriptorId, documentId, doc),
    CatalogItemDocumentUpdatedV1(eServiceId, descriptorId, documentId, docV1)
  )

}
