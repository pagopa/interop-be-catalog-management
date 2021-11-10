package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

sealed trait CatalogDescriptorState {
  def toApi: EServiceDescriptorState = this match {
    case Draft      => EServiceDescriptorState.DRAFT
    case Published  => EServiceDescriptorState.PUBLISHED
    case Deprecated => EServiceDescriptorState.DEPRECATED
    case Suspended  => EServiceDescriptorState.SUSPENDED
    case Archived   => EServiceDescriptorState.ARCHIVED
  }
}

case object Draft      extends CatalogDescriptorState
case object Published  extends CatalogDescriptorState
case object Deprecated extends CatalogDescriptorState
case object Suspended  extends CatalogDescriptorState
case object Archived   extends CatalogDescriptorState

object CatalogDescriptorState {

  def fromApi(status: EServiceDescriptorState): CatalogDescriptorState = status match {
    case EServiceDescriptorState.DRAFT      => Draft
    case EServiceDescriptorState.PUBLISHED  => Published
    case EServiceDescriptorState.DEPRECATED => Deprecated
    case EServiceDescriptorState.SUSPENDED  => Suspended
    case EServiceDescriptorState.ARCHIVED   => Archived
  }
}
