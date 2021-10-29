package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

sealed trait CatalogDescriptorStatus {
  def toApi: EServiceDescriptorStatusEnum = this match {
    case DraftStatus      => DRAFT
    case PublishedStatus  => PUBLISHED
    case DeprecatedStatus => DEPRECATED
    case SuspendedStatus  => SUSPENDED
    case ArchivedStatus   => ARCHIVED
  }
}

case object DraftStatus      extends CatalogDescriptorStatus
case object PublishedStatus  extends CatalogDescriptorStatus
case object DeprecatedStatus extends CatalogDescriptorStatus
case object SuspendedStatus  extends CatalogDescriptorStatus
case object ArchivedStatus   extends CatalogDescriptorStatus

object CatalogDescriptorStatus {

  def fromApi(status: EServiceDescriptorStatusEnum): CatalogDescriptorStatus = status match {
    case DRAFT      => DraftStatus
    case PUBLISHED  => PublishedStatus
    case DEPRECATED => DeprecatedStatus
    case SUSPENDED  => SuspendedStatus
    case ARCHIVED   => ArchivedStatus
  }
}
