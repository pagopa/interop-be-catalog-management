package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.EService

sealed trait Command

case object Idle                                                                                   extends Command
final case class AddEService(eService: EService, replyTo: ActorRef[StatusReply[String]])           extends Command
final case class GetEService(eServiceId: String, replyTo: ActorRef[StatusReply[Option[EService]]]) extends Command
final case class ListServices(
  from: Int,
  to: Int,
  producerId: Option[String],
  consumerId: Option[String],
  status: Option[String],
  replyTo: ActorRef[LazyList[EService]]
) extends Command
