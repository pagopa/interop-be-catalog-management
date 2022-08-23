package it.pagopa.interop.catalogmanagement.server.impl

import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{Entity, EntityContext, ShardedDaemonProcess}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import akka.persistence.typed.PersistenceId
import akka.projection.ProjectionBehavior
import com.atlassian.oai.validator.report.ValidationReport
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.interop.catalogmanagement.api.impl._
import it.pagopa.interop.catalogmanagement.common.system.ApplicationConfiguration
import it.pagopa.interop.catalogmanagement.common.system.ApplicationConfiguration.{
  numberOfProjectionTags,
  projectionTag
}
import it.pagopa.interop.catalogmanagement.model.persistence.projection.EServiceCqrsProjection
import it.pagopa.interop.catalogmanagement.model.persistence.{CatalogPersistentBehavior, Command}
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.jwt.service.JWTReader
import it.pagopa.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.{JWTConfiguration, PublicKeysHolder}
import it.pagopa.interop.commons.utils.OpenapiUtils
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.ValidationRequestError
import it.pagopa.interop.commons.utils.service.UUIDSupplier
import it.pagopa.interop.commons.utils.service.impl.UUIDSupplierImpl
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

trait Dependencies {

  lazy val behaviorFactory: EntityContext[Command] => Behavior[Command] = { entityContext =>
    val index = math.abs(entityContext.entityId.hashCode % numberOfProjectionTags)
    CatalogPersistentBehavior(
      entityContext.shard,
      PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId),
      projectionTag(index)
    )
  }

  val catalogPersistentEntity: Entity[Command, ShardingEnvelope[Command]] =
    Entity(CatalogPersistentBehavior.TypeKey)(behaviorFactory)

  val uuidSupplier: UUIDSupplier = new UUIDSupplierImpl

  def getFileManager(blockingEc: ExecutionContextExecutor): FileManager =
    FileManager.get(ApplicationConfiguration.storageKind match {
      case "S3"   => FileManager.S3
      case "file" => FileManager.File
      case _      => throw new Exception("Incorrect File Manager")
    })(blockingEc)

  def getJwtValidator(): Future[JWTReader] = JWTConfiguration.jwtReader
    .loadKeyset()
    .map(keyset =>
      new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset                                                                 = keyset
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(audience = ApplicationConfiguration.jwtAudience)
      }
    )
    .toFuture

  def initProjections()(implicit actorSystem: ActorSystem[_], ec: ExecutionContext): Unit = {
    val dbConfig: DatabaseConfig[JdbcProfile] =
      DatabaseConfig.forConfig("akka-persistence-jdbc.shared-databases.slick")

    val mongoDbConfig = ApplicationConfiguration.mongoDb

    val projectionId   = "eservice-cqrs-projections"
    val cqrsProjection = EServiceCqrsProjection.projection(dbConfig, mongoDbConfig, projectionId)

    ShardedDaemonProcess(actorSystem).init[ProjectionBehavior.Command](
      name = projectionId,
      numberOfInstances = numberOfProjectionTags,
      behaviorFactory = (i: Int) => ProjectionBehavior(cqrsProjection.projection(projectionTag(i))),
      stopMessage = ProjectionBehavior.Stop
    )
  }

  val validationExceptionToRoute: ValidationReport => Route = report => {
    val error =
      problemOf(StatusCodes.BadRequest, ValidationRequestError(OpenapiUtils.errorFromRequestValidationReport(report)))
    complete(error.status, error)(EServiceApiMarshallerImpl.toEntityMarshallerProblem)
  }

}
