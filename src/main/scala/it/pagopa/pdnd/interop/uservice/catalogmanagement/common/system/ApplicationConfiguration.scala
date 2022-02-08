package it.pagopa.pdnd.interop.uservice.catalogmanagement.common.system

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.ListHasAsScala

object ApplicationConfiguration {

  lazy val config: Config = ConfigFactory.load()

  lazy val serverPort: Int = config.getInt("catalog-management.port")

  lazy val storageContainer: String = config.getString("catalog-management.storage.container")

  lazy val eserviceDocsPath: String = config.getString("catalog-management.storage.eservice-docs-path")

  lazy val jwtAudience: Set[String] = config.getStringList("catalog-management.jwt.audience").asScala.toSet

}
