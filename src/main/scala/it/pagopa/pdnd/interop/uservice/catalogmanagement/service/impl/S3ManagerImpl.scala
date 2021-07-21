package it.pagopa.pdnd.interop.uservice.catalogmanagement.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.pdnd.interop.uservice.catalogmanagement.common.system.ApplicationConfiguration.bucketName
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.CatalogDocument
import it.pagopa.pdnd.interop.uservice.catalogmanagement.service.FileManager
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

import java.io.File
import java.nio.file.Paths
import java.util.UUID
import scala.util.Try

class S3ManagerImpl(s3Client: S3Client) extends FileManager {

  override def store(
    id: UUID,
    producerId: String,
    version: String,
    fileParts: (FileInfo, File)
  ): Try[CatalogDocument] = {

    Try {
      val s3Key = createS3Key(producerId, version, id.toString, fileInfo = fileParts._1)
      val objectRequest =
        PutObjectRequest.builder
          .bucket(bucketName)
          .key(s3Key)
          .build

      val _ =
        s3Client.putObject(objectRequest, RequestBody.fromFile(Paths.get(fileParts._2.getPath)))
      CatalogDocument(id, fileParts._1.getFileName, fileParts._1.getContentType.toString(), s3Key)
    }

  }

  private def createS3Key(producerId: String, version: String, id: String, fileInfo: FileInfo): String =
    s"e-services/docs/${producerId}/${version}/${id}/${fileInfo.getFieldName}/${fileInfo.getContentType.toString}/${fileInfo.getFileName}"

  override def get(id: UUID, producerId: String): File = ???
}
