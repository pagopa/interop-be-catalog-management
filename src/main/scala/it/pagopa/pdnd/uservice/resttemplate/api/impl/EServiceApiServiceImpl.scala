package it.pagopa.pdnd.uservice.resttemplate.api.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onSuccess
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import it.pagopa.pdnd.interopuservice.agreementmanagement.api.EServiceApiService
import it.pagopa.pdnd.interopuservice.agreementmanagement.model.EService
import it.pagopa.pdnd.uservice.resttemplate.common.system._
import it.pagopa.pdnd.uservice.resttemplate.model.persistence._

import java.util.UUID
import scala.concurrent.Future

@SuppressWarnings(
  Array(
    "org.wartremover.warts.ImplicitParameter",
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.Recursion"
  )
)
class EServiceApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]]
) extends EServiceApiService {

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
  }

  @inline private def getShard(id: String): String = (id.hashCode % settings.numberOfShards).toString

  /** Code: 200, Message: EService created, DataType: Eservice
    * Code: 405, Message: Invalid input
    */
  override def addEService(seed: EService)(implicit toEntityMarshallerEservice: ToEntityMarshaller[EService]): Route = {
    val id                                  = UUID.randomUUID()
    val eService: EService                  = seed.copy(id = Some(id))
    val commander: EntityRef[Command]       = sharding.entityRefFor(EServicePersistentBehavior.TypeKey, getShard(id.toString))
    val result: Future[StatusReply[String]] = commander.ask(ref => AddEService(eService, ref))
    onSuccess(result) {
      case statusReply if statusReply.isSuccess =>
        addEService200(eService)
      case statusReply if statusReply.isError => addEService405
    }
  }

}
