package it.pagopa.pdnd.interop.uservice.catalogmanagement.common.system

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.ListHasAsScala

object ApplicationConfiguration {

  lazy val config: Config = ConfigFactory.load()

  def serverPort: Int = config.getInt("uservice-catalog-management.port")

  def storageContainer: String = config.getString("uservice-catalog-management.storage.container")

  def eserviceDocsPath: String = config.getString("uservice-catalog-management.storage.eservice-docs-path")

  def jwtAudience: Set[String] = config.getStringList("uservice-catalog-management.jwt.audience").asScala.toSet

}
