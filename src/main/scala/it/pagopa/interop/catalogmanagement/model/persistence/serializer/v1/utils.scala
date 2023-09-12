package it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1

import cats.syntax.all._
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.catalog_item.CatalogItemTechnologyV1.{
  Unrecognized => UnrecognizedTechnology
}
import it.pagopa.interop.catalogmanagement.model.persistence.serializer.v1.catalog_item._
import it.pagopa.interop.commons.utils.TypeConversions._

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object utils {

  def convertAttributeValueToV1(catalogAttribute: CatalogAttribute): CatalogAttributeValueV1 =
    CatalogAttributeValueV1(catalogAttribute.id.toString, catalogAttribute.explicitAttributeVerification)

  def convertAttributeToV1(catalogAttribute: Seq[CatalogAttribute]): CatalogAttributeV1 =
    CatalogAttributeV1(single = None, group = catalogAttribute.map(convertAttributeValueToV1))

  def convertAttributesToV1(attributes: CatalogAttributes): CatalogAttributesV1 = {
    CatalogAttributesV1(
      certified = attributes.certified.map(convertAttributeToV1),
      declared = attributes.declared.map(convertAttributeToV1),
      verified = attributes.verified.map(convertAttributeToV1)
    )
  }

  def convertAttributeFromV1(catalogAttributeV1: CatalogAttributeV1): Either[Throwable, Seq[CatalogAttribute]] = {
    val singleAttribute: Option[Either[Throwable, Seq[CatalogAttribute]]] = catalogAttributeV1.single.map(attr =>
      attr.id.toUUID.toEither.map(uuid => Seq(CatalogAttribute(uuid, attr.explicitAttributeVerification)))
    )
    val groupAttribute: Option[Either[Throwable, Seq[CatalogAttribute]]]  =
      Option
        .unless(catalogAttributeV1.group.isEmpty)(catalogAttributeV1.group)
        .map(attributes =>
          attributes
            .traverse(attr =>
              attr.id.toUUID.toEither.map(id => CatalogAttribute(id, attr.explicitAttributeVerification))
            )
        )

    singleAttribute
      .orElse(groupAttribute)
      .getOrElse(new RuntimeException("Deserialization from protobuf failed").asLeft)
  }

  def convertAttributesFromV1(attributes: CatalogAttributesV1): Either[Throwable, CatalogAttributes] = for {
    certified <- attributes.certified.traverse(convertAttributeFromV1)
    declared  <- attributes.declared.traverse(convertAttributeFromV1)
    verified  <- attributes.verified.traverse(convertAttributeFromV1)
  } yield CatalogAttributes(certified = certified, declared = declared, verified = verified)

  def convertDescriptorToV1(descriptor: CatalogDescriptor): Either[Throwable, CatalogDescriptorV1] =
    Right(
      CatalogDescriptorV1(
        id = descriptor.id.toString,
        version = descriptor.version,
        description = descriptor.description,
        docs = descriptor.docs.map { doc =>
          CatalogDocumentV1(
            id = doc.id.toString,
            name = doc.name,
            contentType = doc.contentType,
            prettyName = doc.prettyName,
            path = doc.path,
            checksum = doc.checksum,
            uploadDate = doc.uploadDate.format(DateTimeFormatter.ISO_DATE_TIME)
          )
        },
        interface = descriptor.interface.map { doc =>
          CatalogDocumentV1(
            id = doc.id.toString,
            name = doc.name,
            contentType = doc.contentType,
            prettyName = doc.prettyName,
            path = doc.path,
            checksum = doc.checksum,
            uploadDate = doc.uploadDate.format(DateTimeFormatter.ISO_DATE_TIME)
          )
        },
        state = convertDescriptorStateToV1(descriptor.state),
        audience = descriptor.audience,
        voucherLifespan = descriptor.voucherLifespan,
        dailyCallsPerConsumer = descriptor.dailyCallsPerConsumer,
        dailyCallsTotal = descriptor.dailyCallsTotal,
        agreementApprovalPolicy = descriptor.agreementApprovalPolicy.map(convertAgreementApprovalPolicyToV1),
        createdAt = descriptor.createdAt.toMillis.some,
        serverUrls = descriptor.serverUrls,
        attributes = convertAttributesToV1(descriptor.attributes).some,
        publishedAt = descriptor.publishedAt.map(_.toMillis),
        suspendedAt = descriptor.suspendedAt.map(_.toMillis),
        deprecatedAt = descriptor.deprecatedAt.map(_.toMillis),
        archivedAt = descriptor.archivedAt.map(_.toMillis)
      )
    )

  def convertDescriptorsToV1(descriptors: Seq[CatalogDescriptor]): Either[Throwable, Seq[CatalogDescriptorV1]] =
    descriptors.traverse(convertDescriptorToV1)

  def convertDescriptorFromV1(ver1: CatalogDescriptorV1): Either[Throwable, CatalogDescriptor] =
    for {
      state                   <- convertDescriptorStateFromV1(ver1.state)
      agreementApprovalPolicy <- ver1.agreementApprovalPolicy.traverse(convertAgreementApprovalPolicyFromV1)
      createdAt               <- ver1.createdAt.traverse(_.toOffsetDateTime.toEither)
      publishedAt             <- ver1.publishedAt.traverse(_.toOffsetDateTime.toEither)
      suspendedAt             <- ver1.suspendedAt.traverse(_.toOffsetDateTime.toEither)
      deprecatedAt            <- ver1.deprecatedAt.traverse(_.toOffsetDateTime.toEither)
      archivedAt              <- ver1.archivedAt.traverse(_.toOffsetDateTime.toEither)
      maybeAttributes         <- ver1.attributes.traverse(convertAttributesFromV1)
    } yield CatalogDescriptor(
      id = UUID.fromString(ver1.id),
      version = ver1.version,
      description = ver1.description,
      interface = ver1.interface.map { doc =>
        CatalogDocument(
          id = UUID.fromString(doc.id),
          name = doc.name,
          contentType = doc.contentType,
          prettyName = doc.prettyName,
          path = doc.path,
          checksum = doc.checksum,
          uploadDate = OffsetDateTime.parse(doc.uploadDate, DateTimeFormatter.ISO_DATE_TIME)
        )
      },
      docs = ver1.docs.map { doc =>
        CatalogDocument(
          id = UUID.fromString(doc.id),
          name = doc.name,
          contentType = doc.contentType,
          prettyName = doc.prettyName,
          path = doc.path,
          checksum = doc.checksum,
          uploadDate = OffsetDateTime.parse(doc.uploadDate, DateTimeFormatter.ISO_DATE_TIME)
        )
      },
      state = state,
      audience = ver1.audience,
      voucherLifespan = ver1.voucherLifespan,
      dailyCallsPerConsumer = ver1.dailyCallsPerConsumer,
      dailyCallsTotal = ver1.dailyCallsTotal,
      agreementApprovalPolicy = agreementApprovalPolicy,
      createdAt = createdAt.getOrElse(defaultCreatedAt),
      serverUrls = ver1.serverUrls.toList,
      publishedAt = if (state == Draft) None else publishedAt orElse defaultPublishedAt.some,
      suspendedAt = suspendedAt,
      deprecatedAt = deprecatedAt,
      archivedAt = archivedAt,
      attributes = maybeAttributes.getOrElse(CatalogAttributes.empty)
    )

  def convertDescriptorsFromV1(descriptors: Seq[CatalogDescriptorV1]): Either[Throwable, Seq[CatalogDescriptor]] =
    descriptors.traverse(convertDescriptorFromV1)

  def convertCatalogItemsFromV1(itemV1: CatalogItemV1): Either[Throwable, CatalogItem] = for {
    attributes  <- itemV1.attributes.traverse(convertAttributesFromV1)
    descriptors <- convertDescriptorsFromV1(itemV1.descriptors)
    technology  <- convertItemTechnologyFromV1(itemV1.technology)
    uuid        <- itemV1.id.toUUID.toEither
    producerId  <- itemV1.producerId.toUUID.toEither
    createdAt   <- itemV1.createdAt.traverse(_.toOffsetDateTime.toEither)
  } yield CatalogItem(
    id = uuid,
    producerId = producerId,
    name = itemV1.name,
    description = itemV1.description,
    technology = technology,
    attributes = attributes,
    descriptors = descriptors,
    createdAt = createdAt.getOrElse(defaultCreatedAt)
  )

  def convertCatalogItemsToV1(item: CatalogItem): Either[Throwable, CatalogItemV1] = for {
    descriptors <- convertDescriptorsToV1(item.descriptors)
  } yield CatalogItemV1(
    id = item.id.toString,
    producerId = item.producerId.toString,
    name = item.name,
    description = item.description,
    technology = convertItemTechnologyToV1(item.technology),
    descriptors = descriptors,
    createdAt = item.createdAt.toMillis.some,
    attributes = item.attributes.map(convertAttributesToV1)
  )

  def convertDescriptorStateFromV1(state: CatalogDescriptorStateV1): Either[Throwable, CatalogDescriptorState] =
    state match {
      case CatalogDescriptorStateV1.DRAFT               => Right(Draft)
      case CatalogDescriptorStateV1.PUBLISHED           => Right(Published)
      case CatalogDescriptorStateV1.DEPRECATED          => Right(Deprecated)
      case CatalogDescriptorStateV1.SUSPENDED           => Right(Suspended)
      case CatalogDescriptorStateV1.ARCHIVED            => Right(Archived)
      case CatalogDescriptorStateV1.Unrecognized(value) =>
        Left(new RuntimeException(s"Unable to deserialize catalog descriptor state value $value"))
    }

  def convertDescriptorStateToV1(state: CatalogDescriptorState): CatalogDescriptorStateV1 = state match {
    case Draft      => CatalogDescriptorStateV1.DRAFT
    case Published  => CatalogDescriptorStateV1.PUBLISHED
    case Deprecated => CatalogDescriptorStateV1.DEPRECATED
    case Suspended  => CatalogDescriptorStateV1.SUSPENDED
    case Archived   => CatalogDescriptorStateV1.ARCHIVED
  }

  def convertAgreementApprovalPolicyFromV1(
    policyV1: AgreementApprovalPolicyV1
  ): Either[Throwable, PersistentAgreementApprovalPolicy] =
    policyV1 match {
      case AgreementApprovalPolicyV1.AUTOMATIC           => Right(Automatic)
      case AgreementApprovalPolicyV1.MANUAL              => Right(Manual)
      case AgreementApprovalPolicyV1.Unrecognized(value) =>
        Left(new RuntimeException(s"Unable to deserialize agreement approval policy value $value"))
    }

  def convertAgreementApprovalPolicyToV1(policy: PersistentAgreementApprovalPolicy): AgreementApprovalPolicyV1 =
    policy match {
      case Automatic => AgreementApprovalPolicyV1.AUTOMATIC
      case Manual    => AgreementApprovalPolicyV1.MANUAL
    }

  def convertItemTechnologyFromV1(technology: CatalogItemTechnologyV1): Either[Throwable, CatalogItemTechnology] =
    technology match {
      case CatalogItemTechnologyV1.REST  => Right(Rest)
      case CatalogItemTechnologyV1.SOAP  => Right(Soap)
      case UnrecognizedTechnology(value) =>
        Left(new RuntimeException(s"Unable to deserialize catalog item technology value $value"))
    }

  def convertItemTechnologyToV1(technology: CatalogItemTechnology): CatalogItemTechnologyV1 =
    technology match {
      case Rest => CatalogItemTechnologyV1.REST
      case Soap => CatalogItemTechnologyV1.SOAP
    }

}
