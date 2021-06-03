package it.pagopa.pdnd.uservice.resttemplate.model.persistence

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import it.pagopa.pdnd.interopuservice.agreementmanagement.model.EService

sealed trait Command

final case class AddEService(eService: EService, replyTo: ActorRef[StatusReply[String]]) extends Command
case object Idle                                                                         extends Command
