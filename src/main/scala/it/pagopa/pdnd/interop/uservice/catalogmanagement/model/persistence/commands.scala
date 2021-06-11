package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply

sealed trait Command

case object Idle                                                                                       extends Command
final case class AddCatalogItem(catalogItem: CatalogItem, replyTo: ActorRef[StatusReply[CatalogItem]]) extends Command
final case class GetCatalogItem(catalogItemId: String, replyTo: ActorRef[StatusReply[Option[CatalogItem]]])
    extends Command
final case class ListCatalogItem(
  from: Int,
  to: Int,
  producerId: Option[String],
  consumerId: Option[String],
  status: Option[String],
  replyTo: ActorRef[Seq[CatalogItem]]
) extends Command
