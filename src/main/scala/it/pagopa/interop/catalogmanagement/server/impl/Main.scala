package it.pagopa.interop.catalogmanagement.server.impl

import cats.syntax.all._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.cluster.ClusterEvent
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{Cluster, Subscribe}
import akka.http.scaladsl.Http

import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement

import it.pagopa.interop.catalogmanagement.api.EServiceApi
import it.pagopa.interop.catalogmanagement.api.impl._
import it.pagopa.interop.catalogmanagement.common.system.ApplicationConfiguration

import it.pagopa.interop.catalogmanagement.server.Controller
import it.pagopa.interop.catalogmanagement.service.impl.CatalogFileManagerImpl
import it.pagopa.interop.commons.logging.renderBuildInfo
import kamon.Kamon
import com.typesafe.scalalogging.Logger

import buildinfo.BuildInfo
import scala.concurrent.ExecutionContext
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContextExecutor
import akka.actor.typed.DispatcherSelector

object Main extends App with Dependencies {

  Kamon.init()

  val logger: Logger = Logger(this.getClass)

  System.setProperty("kanela.show-banner", "false")

  val actorSystem = ActorSystem[Nothing](
    Behaviors.setup[Nothing] { context =>
      implicit val actorSystem: ActorSystem[_]        = context.system
      implicit val executionContext: ExecutionContext = actorSystem.executionContext

      val selector: DispatcherSelector         = DispatcherSelector.fromConfig("futures-dispatcher")
      val blockingEc: ExecutionContextExecutor = actorSystem.dispatchers.lookup(selector)

      Kamon.init()
      AkkaManagement.get(actorSystem).start()

      val sharding: ClusterSharding = ClusterSharding(context.system)
      sharding.init(catalogPersistentEntity)

      val cluster: Cluster = Cluster(context.system)
      ClusterBootstrap.get(actorSystem).start()

      val listener = context.spawn(
        Behaviors.receive[ClusterEvent.MemberEvent]((ctx, event) => {
          ctx.log.info("MemberEvent: {}", event)
          Behaviors.same
        }),
        "listener"
      )

      cluster.subscriptions ! Subscribe(listener, classOf[ClusterEvent.MemberEvent])

      if (ApplicationConfiguration.projectionsEnabled) initProjections()

      logger.info(renderBuildInfo(BuildInfo))
      logger.info(s"Started cluster at ${cluster.selfMember.address}")

      val fileManager        = getFileManager(blockingEc)
      val catalogFileManager = new CatalogFileManagerImpl(fileManager)

      val serverBinding = for {
        jwtReader <- getJwtValidator()
        eServiceApi = new EServiceApi(
          new EServiceApiServiceImpl(actorSystem, sharding, catalogPersistentEntity, uuidSupplier, catalogFileManager),
          EServiceApiMarshallerImpl,
          jwtReader.OAuth2JWTValidatorAsContexts
        )
        controller  = new Controller(eServiceApi, validationExceptionToRoute.some)(actorSystem.classicSystem)
        binding <- Http().newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(controller.routes)
      } yield binding

      serverBinding.onComplete {
        case Success(b) =>
          logger.info(s"Started server at ${b.localAddress.getHostString()}:${b.localAddress.getPort()}")
        case Failure(e) =>
          actorSystem.terminate()
          logger.error("Startup error: ", e)
      }

      Behaviors.empty
    },
    BuildInfo.name
  )

  actorSystem.whenTerminated.onComplete { case _ => Kamon.stop() }(scala.concurrent.ExecutionContext.global)

}
