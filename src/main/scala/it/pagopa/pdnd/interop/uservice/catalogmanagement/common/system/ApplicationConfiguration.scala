package it.pagopa.pdnd.interop.uservice.catalogmanagement.common.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {

  lazy val config: Config = ConfigFactory.load()

  def serverPort: Int = config.getInt("uservice-catalog-management.port")

  def storageContainer: String = config.getString("pdnd-interop-commons.storage.container")

}
