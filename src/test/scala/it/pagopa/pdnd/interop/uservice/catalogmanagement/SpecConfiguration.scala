package it.pagopa.pdnd.interop.uservice.catalogmanagement

import com.typesafe.config.{Config, ConfigFactory}

import java.nio.file.{Files, Paths}

/** Selfless trait containing base test configuration for Akka Cluster Setup
  */
trait SpecConfiguration {

  System.setProperty("AWS_ACCESS_KEY_ID", "foo")
  System.setProperty("AWS_SECRET_ACCESS_KEY", "bar")

  val testData: Config = ConfigFactory.parseString(s"""
      akka.actor.provider = cluster

      akka.remote.classic.netty.tcp.port = 0
      akka.remote.artery.canonical.port = 0
      akka.remote.artery.canonical.hostname = 127.0.0.1

      akka.cluster.jmx.multi-mbeans-in-same-jvm = on

      akka.cluster.sharding.number-of-shards = 10

      akka.coordinated-shutdown.terminate-actor-system = off
      akka.coordinated-shutdown.run-by-actor-system-terminate = off
      akka.coordinated-shutdown.run-by-jvm-shutdown-hook = off
      akka.cluster.run-coordinated-shutdown-when-down = off
    """)

  val config: Config = ConfigFactory
    .parseResourcesAnySyntax("application-test")
    .withFallback(testData)

  def serviceURL: String =
    s"${config.getString("pdnd-interop-uservice-catalog-management.url")}/${buildinfo.BuildInfo.interfaceVersion}"

  def getPact(path: String): String = {
    val provider = Files.readString(Paths.get(path))
    provider.replace(":version:", buildinfo.BuildInfo.interfaceVersion)
  }
}

object SpecConfiguration extends SpecConfiguration
