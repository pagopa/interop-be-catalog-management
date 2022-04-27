package it.pagopa.interop.catalogmanagement.common.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {

  lazy val config: Config = ConfigFactory.load()

  lazy val serverPort: Int = config.getInt("catalog-management.port")

  lazy val storageContainer: String = config.getString("catalog-management.storage.container")

  lazy val eserviceDocsPath: String = config.getString("catalog-management.storage.eservice-docs-path")

  lazy val jwtAudience: Set[String] =
    config.getString("catalog-management.jwt.audience").split(",").toSet.filter(_.nonEmpty)

  lazy val numberOfProjectionTags: Int = config.getInt("akka.cluster.sharding.number-of-shards")

  def projectionTag(index: Int)        = s"interop-be-catalog-management-persistence|$index"
  lazy val projectionsEnabled: Boolean = config.getBoolean("akka.projection.enabled")

  require(jwtAudience.nonEmpty, "Audience cannot be empty")

}
