package it.pagopa.interop.catalogmanagement.model

sealed trait CatalogDescriptorState
case object Draft      extends CatalogDescriptorState
case object Published  extends CatalogDescriptorState
case object Deprecated extends CatalogDescriptorState
case object Suspended  extends CatalogDescriptorState
case object Archived   extends CatalogDescriptorState

object CatalogDescriptorState
