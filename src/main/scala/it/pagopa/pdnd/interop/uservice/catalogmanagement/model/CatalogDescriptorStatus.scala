package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

sealed trait CatalogDescriptorStatus {
  def toApi: EServiceDescriptorStatusEnum = this match {
    case Draft      => DRAFT
    case Published  => PUBLISHED
    case Deprecated => DEPRECATED
    case Suspended  => SUSPENDED
    case Archived   => ARCHIVED
  }
}

case object Draft      extends CatalogDescriptorStatus
case object Published  extends CatalogDescriptorStatus
case object Deprecated extends CatalogDescriptorStatus
case object Suspended  extends CatalogDescriptorStatus
case object Archived   extends CatalogDescriptorStatus

object CatalogDescriptorStatus {

  def fromApi(status: EServiceDescriptorStatusEnum): CatalogDescriptorStatus = status match {
    case DRAFT      => Draft
    case PUBLISHED  => Published
    case DEPRECATED => Deprecated
    case SUSPENDED  => Suspended
    case ARCHIVED   => Archived
  }
}
