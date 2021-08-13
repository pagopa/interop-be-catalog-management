package it.pagopa.pdnd.interop.uservice.catalogmanagement.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common.Digester
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common.system.ApplicationConfiguration.bucketName
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.CatalogDocument
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.FileManager
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{DeleteObjectRequest, PutObjectRequest}

import java.io.File
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

class S3ManagerImpl(s3Client: S3Client) extends FileManager {

  override def store(
    id: UUID,
    eServiceId: String,
    descriptorId: String,
    description: String,
    interface: Boolean,
    fileParts: (FileInfo, File)
  ): Future[CatalogDocument] = Future.fromTry {

    Try {
      val s3Key = createS3Key(eServiceId, descriptorId, id.toString, fileInfo = fileParts._1)
      val objectRequest =
        PutObjectRequest.builder
          .bucket(bucketName)
          .key(s3Key)
          .build

      val _ = s3Client.putObject(objectRequest, RequestBody.fromFile(Paths.get(fileParts._2.getPath)))

      CatalogDocument(
        id = id,
        name = fileParts._1.getFileName,
        contentType = fileParts._1.getContentType.toString(),
        description = description,
        path = s3Key,
        checksum = Digester.createHash(fileParts._2),
        uploadDate = OffsetDateTime.now()
      )
    }
  }

  private def createS3Key(eServiceId: String, descriptorId: String, id: String, fileInfo: FileInfo): String =
    s"e-services/docs/${eServiceId}/${descriptorId}/${id}/${fileInfo.getFieldName}/${fileInfo.getContentType.toString}/${fileInfo.getFileName}"

  override def get(id: UUID, producerId: String): File = ???

  override def delete(path: String) = {
    Try {
      s3Client.deleteObject(
        DeleteObjectRequest.builder
          .bucket(bucketName)
          .key(path)
          .build()
      )
    }.fold(error => Future.failed[Boolean](error), _ => Future.successful(true))
  }

}
