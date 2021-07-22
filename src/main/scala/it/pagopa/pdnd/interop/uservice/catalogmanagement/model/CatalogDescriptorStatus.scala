package it.pagopa.pdnd.interop.uservice.catalogmanagement.model

sealed trait CatalogDescriptorStatus {
  def stringify: String = this match {
    case Draft      => "draft"
    case Published  => "published"
    case Deprecated => "deprecated"
    case Archived   => "archived"
  }
}

case object Draft      extends CatalogDescriptorStatus
case object Published  extends CatalogDescriptorStatus
case object Deprecated extends CatalogDescriptorStatus
case object Archived   extends CatalogDescriptorStatus

object CatalogDescriptorStatus {

  def fromText(str: String): Either[Throwable, CatalogDescriptorStatus] = str match {
    case "draft"      => Right[Throwable, CatalogDescriptorStatus](Draft)
    case "published"  => Right[Throwable, CatalogDescriptorStatus](Published)
    case "deprecated" => Right[Throwable, CatalogDescriptorStatus](Deprecated)
    case "archived"   => Right[Throwable, CatalogDescriptorStatus](Archived)
    case _ =>
      Left[Throwable, CatalogDescriptorStatus](
        new RuntimeException(s"Invalid descriptor status: $str")
      ) //TODO meaningful error
  }
}
