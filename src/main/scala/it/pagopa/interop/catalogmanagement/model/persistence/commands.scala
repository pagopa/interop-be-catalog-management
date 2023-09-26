package it.pagopa.interop.catalogmanagement.model.persistence

import akka.Done
import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import it.pagopa.interop.catalogmanagement.model.{
  CatalogDescriptor,
  CatalogDescriptorState,
  CatalogDocument,
  CatalogItem
}

sealed trait Command

case object Idle extends Command

final case class AddCatalogItem(catalogItem: CatalogItem, replyTo: ActorRef[StatusReply[CatalogItem]]) extends Command
final case class AddClonedCatalogItem(catalogItem: CatalogItem, replyTo: ActorRef[StatusReply[CatalogItem]])
    extends Command

final case class UpdateCatalogItem(catalogItem: CatalogItem, replyTo: ActorRef[StatusReply[Option[CatalogItem]]])
    extends Command
final case class DeleteCatalogItem(eServiceId: String, replyTo: ActorRef[StatusReply[Done]]) extends Command

final case class DeleteCatalogItemDescriptor(
  catalogItem: CatalogItem,
  descriptorId: String,
  replyTo: ActorRef[StatusReply[Done]]
) extends Command

final case class GetCatalogItem(catalogItemId: String, replyTo: ActorRef[StatusReply[Option[CatalogItem]]])
    extends Command

final case class ListCatalogItem(
  from: Int,
  to: Int,
  producerId: Option[String],
  attributeId: Option[String],
  status: Option[CatalogDescriptorState],
  replyTo: ActorRef[Seq[CatalogItem]]
) extends Command

final case class UpdateCatalogItemDocument(
  eServiceId: String,
  descriptorId: String,
  documentId: String,
  updateEServiceDescriptorDocumentSeed: CatalogDocument,
  serverUrls: List[String],
  replyTo: ActorRef[StatusReply[Option[CatalogDocument]]]
) extends Command

final case class AddCatalogItemDocument(
  eServiceId: String,
  descriptorId: String,
  document: CatalogDocument,
  isInterface: Boolean,
  serverUrls: List[String],
  replyTo: ActorRef[Option[CatalogDocument]]
) extends Command

final case class DeleteCatalogItemDocument(
  eServiceId: String,
  descriptorId: String,
  documentId: String,
  replyTo: ActorRef[StatusReply[Done]]
) extends Command

final case class AddCatalogItemDescriptor(
  eServiceId: String,
  catalogDescriptor: CatalogDescriptor,
  replyTo: ActorRef[StatusReply[Option[CatalogDescriptor]]]
) extends Command

final case class AddCatalogItemRiskAnalysis(
  catalogItem: CatalogItem,
  riskAnalysisId: String,
  replyTo: ActorRef[StatusReply[Done]]
) extends Command

final case class UpdateCatalogItemDescriptor(
  eServiceId: String,
  catalogDescriptor: CatalogDescriptor,
  replyTo: ActorRef[StatusReply[Option[CatalogDescriptor]]]
) extends Command

final case class MoveAttributesFromEserviceToDescriptors(eServiceId: String, replyTo: ActorRef[StatusReply[Done]])
    extends Command
