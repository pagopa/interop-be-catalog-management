package it.pagopa.pdnd.interop.uservice.catalogmanagement.common.system

import com.typesafe.config.{Config, ConfigFactory}
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials

object ApplicationConfiguration {

  lazy val config: Config = ConfigFactory.load()

  def serverPort: Int = config.getInt("uservice-catalog-management.port")

  def bucketName: String =
    config.getString("pdnd-uservice-uservice-catalog-management.aws.s3-bucket-name")

  def awsCredentials: AwsBasicCredentials =
    AwsBasicCredentials.create(
      config.getString("pdnd-uservice-uservice-catalog-management.aws.access-key-id"),
      config.getString("pdnd-uservice-uservice-catalog-management.aws.secret-access-key")
    )

}
