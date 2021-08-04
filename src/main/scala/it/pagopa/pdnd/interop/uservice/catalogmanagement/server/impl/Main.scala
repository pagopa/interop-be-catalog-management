package it.pagopa.pdnd.interop.uservice.catalogmanagement.server.impl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, ShardedDaemonProcess}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.cluster.typed.{Cluster, Subscribe}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.persistence.typed.PersistenceId
import akka.projection.ProjectionBehavior
import akka.{actor => classic}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.EServiceApi
import it.pagopa.pdnd.interop.uservice.catalogmanagement.api.impl.{EServiceApiMarshallerImpl, EServiceApiServiceImpl, _}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common.system.{ApplicationConfiguration, Authenticator, s3Client}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.Problem
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.{CatalogPersistentBehavior, CatalogPersistentProjection, Command}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.server.Controller
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.impl.{S3ManagerImpl, UUIDSupplierImpl}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.{FileManager, UUIDSupplier}
import kamon.Kamon
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.jdk.CollectionConverters._

@SuppressWarnings(
  Array("org.wartremover.warts.StringPlusAny", "org.wartremover.warts.Nothing", "org.wartremover.warts.Throw")
)
object Main extends App {

  def buildPersistentEntity(): Entity[Command, ShardingEnvelope[Command]] =
    Entity(typeKey = CatalogPersistentBehavior.TypeKey) { entityContext =>
      CatalogPersistentBehavior(
        entityContext.shard,
        PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)
      )
    }

  Kamon.init()


  locally {
    val _ = ActorSystem[Nothing](
      Behaviors.setup[Nothing] { context =>
        import akka.actor.typed.scaladsl.adapter._
        implicit val classicSystem: classic.ActorSystem         = context.system.toClassic
        implicit val executionContext: ExecutionContextExecutor = context.system.executionContext
        val cluster: Cluster                                    = Cluster(context.system)

        context.log.error("Started [" + context.system + "], cluster.selfAddress = " + cluster.selfMember.address + ")")

        val sharding: ClusterSharding = ClusterSharding(context.system)

        val catalogPersistentEntity = buildPersistentEntity()

        val _ = sharding.init(catalogPersistentEntity)

        val settings: ClusterShardingSettings = catalogPersistentEntity.settings match {
          case None    => ClusterShardingSettings(context.system)
          case Some(s) => s
        }

        val persistence =
          classicSystem.classicSystem.settings.config.getString("uservice-catalog-management.persistence")
        if (persistence == "cassandra") {
          val catalogPersistentProjection = new CatalogPersistentProjection(context.system, catalogPersistentEntity)

          ShardedDaemonProcess(context.system).init[ProjectionBehavior.Command](
            name = "catalog-projections",
            numberOfInstances = settings.numberOfShards,
            behaviorFactory = (i: Int) => ProjectionBehavior(catalogPersistentProjection.projections(i)),
            stopMessage = ProjectionBehavior.Stop
          )
        }

        val uuidSupplier: UUIDSupplier = new UUIDSupplierImpl
        val fileManager: FileManager   = new S3ManagerImpl(s3Client)
        val eServiceApiMarshallerImpl  = new EServiceApiMarshallerImpl()

        val eServiceApi = new EServiceApi(
          new EServiceApiServiceImpl(context.system, sharding, catalogPersistentEntity, uuidSupplier, fileManager),
          eServiceApiMarshallerImpl,
          SecurityDirectives.authenticateOAuth2("SecurityRealm", Authenticator)
        )

        val _ = AkkaManagement.get(classicSystem).start()

        val controller = new Controller(
          eServiceApi,
          validationExceptionToRoute = Some(e => {
            val results = e.results()
            results.crumbs().asScala.foreach { crumb =>
              println(crumb.crumb())
            }
            results.items().asScala.foreach { item =>
              println(item.dataCrumbs())
              println(item.dataJsonPointer())
              println(item.schemaCrumbs())
              println(item.message())
              println(item.severity())
            }
            val message =
              Problem(Some(e.results().items().asScala.map(_.message()).mkString("\n")), 400, "some error").toJson
            complete((400, message))
          })
        )

        val _ = Http().newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(controller.routes)

        val listener = context.spawn(
          Behaviors.receive[ClusterEvent.MemberEvent]((ctx, event) => {
            ctx.log.error("MemberEvent: {}", event)
            Behaviors.same
          }),
          "listener"
        )

        cluster.subscriptions ! Subscribe(listener, classOf[ClusterEvent.MemberEvent])

        val _ = AkkaManagement(classicSystem).start()
        ClusterBootstrap.get(classicSystem).start()
        Behaviors.empty
      },
      "pdnd-interop-uservice-catalog-management"
    )

  }
}
