package it.pagopa.interop.catalogmanagement.server.impl

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.ClusterEvent
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext, ShardedDaemonProcess}
import akka.cluster.typed.{Cluster, Subscribe}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.persistence.typed.PersistenceId
import akka.projection.ProjectionBehavior
import akka.{actor => classic}
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.interop.catalogmanagement.api.EServiceApi
import it.pagopa.interop.catalogmanagement.api.impl.{EServiceApiMarshallerImpl, EServiceApiServiceImpl, _}
import it.pagopa.interop.catalogmanagement.common.system.ApplicationConfiguration
import it.pagopa.interop.catalogmanagement.common.system.ApplicationConfiguration.{
  numberOfProjectionTags,
  projectionTag
}
import it.pagopa.interop.catalogmanagement.model.persistence.{
  CatalogPersistentBehavior,
  CatalogPersistentProjection,
  Command
}
import it.pagopa.interop.catalogmanagement.server.Controller
import it.pagopa.interop.catalogmanagement.service.CatalogFileManager
import it.pagopa.interop.catalogmanagement.service.impl.CatalogFileManagerImpl
import it.pagopa.interop.commons.files.StorageConfiguration
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.jwt.service.JWTReader
import it.pagopa.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.{JWTConfiguration, PublicKeysHolder}
import it.pagopa.interop.commons.utils.OpenapiUtils
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.ValidationRequestError
import it.pagopa.interop.commons.utils.service.UUIDSupplier
import it.pagopa.interop.commons.utils.service.impl.UUIDSupplierImpl
import kamon.Kamon
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContextExecutor
import scala.util.Try

object Main extends App {

  lazy val behaviorFactory: EntityContext[Command] => Behavior[Command] = { entityContext =>
    val i = math.abs(entityContext.entityId.hashCode % numberOfProjectionTags)
    CatalogPersistentBehavior(
      entityContext.shard,
      PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId),
      projectionTag(i)
    )
  }

  val dependenciesLoaded: Try[(FileManager, JWTReader)] = for {
    fileManager <- FileManager.getConcreteImplementation(StorageConfiguration.runtimeFileManager)
    keyset      <- JWTConfiguration.jwtReader.loadKeyset()
    jwtValidator = new DefaultJWTReader with PublicKeysHolder {
      var publicKeyset = keyset
      override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
        getClaimsVerifier(audience = ApplicationConfiguration.jwtAudience)
    }
  } yield (fileManager, jwtValidator)

  val (runtimeFileManager, jwtValidator) =
    dependenciesLoaded.get //THIS IS THE END OF THE WORLD. Exceptions are welcomed here.

  Kamon.init()

  locally {
    val _ = ActorSystem[Nothing](
      Behaviors.setup[Nothing] { context =>
        import akka.actor.typed.scaladsl.adapter._
        implicit val classicSystem: classic.ActorSystem         = context.system.toClassic
        implicit val executionContext: ExecutionContextExecutor = context.system.executionContext
        val cluster: Cluster                                    = Cluster(context.system)

        context.log.info(
          "Started [" + context.system + "], cluster.selfAddress = " + cluster.selfMember.address + ", build info = " + buildinfo.BuildInfo.toString + ")"
        )

        val sharding: ClusterSharding = ClusterSharding(context.system)

        val catalogPersistentEntity = Entity(CatalogPersistentBehavior.TypeKey)(behaviorFactory)

        val _ = sharding.init(catalogPersistentEntity)

        val persistence = classicSystem.classicSystem.settings.config.getString("akka.persistence.journal.plugin")

        if (persistence == "jdbc-journal") {
          val dbConfig: DatabaseConfig[JdbcProfile] =
            DatabaseConfig.forConfig("akka-persistence-jdbc.shared-databases.slick")

          val catalogPersistentProjection =
            CatalogPersistentProjection(context.system, catalogPersistentEntity, dbConfig)

          ShardedDaemonProcess(context.system).init[ProjectionBehavior.Command](
            name = "catalog-projections",
            numberOfInstances = numberOfProjectionTags,
            behaviorFactory = (i: Int) => ProjectionBehavior(catalogPersistentProjection.projection(projectionTag(i))),
            stopMessage = ProjectionBehavior.Stop
          )
        }

        val uuidSupplier: UUIDSupplier      = new UUIDSupplierImpl
        val fileManager: CatalogFileManager = new CatalogFileManagerImpl(runtimeFileManager)
        val eServiceApiMarshallerImpl       = new EServiceApiMarshallerImpl()

        val eServiceApi = new EServiceApi(
          new EServiceApiServiceImpl(context.system, sharding, catalogPersistentEntity, uuidSupplier, fileManager),
          eServiceApiMarshallerImpl,
          jwtValidator.OAuth2JWTValidatorAsContexts
        )

        val _ = AkkaManagement.get(classicSystem).start()

        val controller = new Controller(
          eServiceApi,
          validationExceptionToRoute = Some(report => {
            val error =
              problemOf(
                StatusCodes.BadRequest,
                ValidationRequestError(OpenapiUtils.errorFromRequestValidationReport(report))
              )
            complete(error.status, error)(eServiceApiMarshallerImpl.toEntityMarshallerProblem)
          })
        )

        val _ = Http().newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(controller.routes)

        val listener = context.spawn(
          Behaviors.receive[ClusterEvent.MemberEvent]((ctx, event) => {
            ctx.log.info("MemberEvent: {}", event)
            Behaviors.same
          }),
          "listener"
        )

        cluster.subscriptions ! Subscribe(listener, classOf[ClusterEvent.MemberEvent])

        val _ = AkkaManagement(classicSystem).start()
        ClusterBootstrap.get(classicSystem).start()
        Behaviors.empty
      },
      "interop-be-catalog-management"
    )
  }

}
