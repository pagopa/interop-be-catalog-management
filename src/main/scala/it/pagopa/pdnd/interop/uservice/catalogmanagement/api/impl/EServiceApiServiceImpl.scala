package it.pagopa.pdnd.interop.uservice.catalogmanagement.api.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onSuccess
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.EServiceApiService
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.{
  AddEService,
  Command,
  EServicePersistentBehavior,
  GetEService,
  ListServices
}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.{EService, Problem}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common.system._

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

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

  /** Code: 200, Message: EService created, DataType: EService
    * Code: 405, Message: Invalid input, DataType: Problem
    */
  override def addEService(eService: EService)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService]
  ): Route = {
    val id = UUID.randomUUID()
    val newEService: EService = eService.copy(
      id = Some(id),
      status = Some("active"),
      versions = eService.versions.map(_.map(_.copy(id = Some(UUID.randomUUID()))))
    )
    val commander: EntityRef[Command]       = sharding.entityRefFor(EServicePersistentBehavior.TypeKey, getShard(id.toString))
    val result: Future[StatusReply[String]] = commander.ask(ref => AddEService(newEService, ref))
    onSuccess(result) {
      case statusReply if statusReply.isSuccess => addEService200(newEService)
      case statusReply if statusReply.isError =>
        addEService405(Problem(Option(statusReply.getError.getMessage), status = 405, "some error"))
    }
  }

  /** Code: 200, Message: EService retrieved, DataType: EService
    * Code: 404, Message: EService not found, DataType: Problem
    */
  override def getEService(eServiceId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerEService: ToEntityMarshaller[EService]
  ): Route = {
    val commander: EntityRef[Command]                 = sharding.entityRefFor(EServicePersistentBehavior.TypeKey, getShard(eServiceId))
    val result: Future[StatusReply[Option[EService]]] = commander.ask(ref => GetEService(eServiceId, ref))
    onSuccess(result) {
      case statusReply if statusReply.isSuccess =>
        statusReply.getValue.fold(getEService404(Problem(None, status = 404, "some error")))(eService =>
          getEService200(eService)
        )
      case statusReply if statusReply.isError =>
        getEService400(Problem(Option(statusReply.getError.getMessage), status = 400, "some error"))
    }
  }

  /** Code: 200, Message: A list of EService, DataType: Seq[EService]
    */
//  override def getEServices(
//    producerId: Option[String],
//    consumerId: Option[String],
//    status: Option[String],
//    from: Option[Int],
//    to: Option[Int]
//  )(implicit toEntityMarshallerEServicearray: ToEntityMarshaller[Seq[EService]]): Route = {
//    val sliceSize = to.getOrElse(100)
//
//    @annotation.tailrec
//    def getSlice(commander: EntityRef[Command], from: Int, to: Int, acc: LazyList[EService]): LazyList[EService] = {
//      val slice: Seq[EService] = Await
//        .result(commander.ask(ref => ListServices(from, to, producerId, consumerId, status, ref)), Duration.Inf)
//
//      if (slice.isEmpty)
//        LazyList.empty[EService]
//      else
//        getSlice(commander, to, to + sliceSize, acc #::: slice.to(LazyList))
//    }
//
//    val commanders: Seq[EntityRef[Command]] = (0 until settings.numberOfShards).map(shard =>
//      sharding.entityRefFor(EServicePersistentBehavior.TypeKey, shard.toString)
//    )
//
//    val eServices: Seq[EService] =
//      commanders.flatMap(ref => getSlice(ref, 0, sliceSize, LazyList.empty))
//    getEServices200(eServices)
//
//  }
  override def getEServices(
    producerId: Option[String],
    consumerId: Option[String],
    status: Option[String],
    from: Option[Int],
    to: Option[Int]
  )(implicit toEntityMarshallerEServicearray: ToEntityMarshaller[Seq[EService]]): Route = {
    val sliceSize = 100
    def getSlice(commander: EntityRef[Command], from: Int, to: Int): LazyList[EService] = {
      val slice: Seq[EService] = Await
        .result(commander.ask(ref => ListServices(from, to, producerId, consumerId, status, ref)), Duration.Inf)

      if (slice.isEmpty)
        LazyList.empty[EService]
      else
        getSlice(commander, to, to + sliceSize) #::: slice.to(LazyList)
    }
    val commanders: Seq[EntityRef[Command]] = (0 until settings.numberOfShards).map(shard =>
      sharding.entityRefFor(EServicePersistentBehavior.TypeKey, getShard(shard.toString))
    )
    val eServices = commanders.flatMap(ref => getSlice(ref, 0, sliceSize))
    getEServices200(eServices)

  }
}
