package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import akka.Done
import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.{CatalogDocument, CatalogItem}

sealed trait Command

case object Idle extends Command

final case class AddCatalogItem(catalogItem: CatalogItem, replyTo: ActorRef[StatusReply[CatalogItem]]) extends Command

final case class UpdateCatalogItem(catalogItem: CatalogItem, replyTo: ActorRef[Option[CatalogItem]]) extends Command

final case class DeleteCatalogItemWithDescriptor(catalogItem: CatalogItem, descriptorId: String, replyTo: ActorRef[StatusReply[Done]]) extends Command

final case class GetCatalogItem(catalogItemId: String, replyTo: ActorRef[Option[CatalogItem]]) extends Command

final case class ListCatalogItem(
  from: Int,
  to: Int,
  producerId: Option[String],
  consumerId: Option[String],
  status: Option[String],
  replyTo: ActorRef[Seq[CatalogItem]]
) extends Command

final case class UpdateDocument(eServiceId: String, descriptorId: String, documentId: String, updateEServiceDescriptorDocumentSeed: CatalogDocument, replyTo: ActorRef[Option[CatalogDocument]]) extends Command