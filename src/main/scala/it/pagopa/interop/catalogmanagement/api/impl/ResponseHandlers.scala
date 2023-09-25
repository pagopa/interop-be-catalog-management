package it.pagopa.interop.catalogmanagement.api.impl

import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.catalogmanagement.error.CatalogManagementErrors._
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.errors.{AkkaResponses, ServiceCode}

import scala.util.{Failure, Success, Try}

object ResponseHandlers extends AkkaResponses {

  implicit val serviceCode: ServiceCode = ServiceCode("008")

  def createEServiceResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)  => success(s)
      case Failure(ex) => internalServerError(ex, logMessage)
    }

  def createEServiceDocumentResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                => success(s)
      case Failure(ex: InvalidInterfaceFileDetected) => badRequest(ex, logMessage)
      case Failure(ex: EServiceNotFound)             => notFound(ex, logMessage)
      case Failure(ex: EServiceDescriptorNotFound)   => notFound(ex, logMessage)
      case Failure(ex)                               => internalServerError(ex, logMessage)
    }

  def getEServiceResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                    => success(s)
      case Failure(ex: EServiceNotFound) => notFound(ex, logMessage)
      case Failure(ex)                   => internalServerError(ex, logMessage)
    }

  def getEServicesResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)  => success(s)
      case Failure(ex) => internalServerError(ex, logMessage)
    }

  def getEServiceDocumentResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                              => success(s)
      case Failure(ex: EServiceNotFound)           => notFound(ex, logMessage)
      case Failure(ex: EServiceDescriptorNotFound) => notFound(ex, logMessage)
      case Failure(ex: DocumentNotFound)           => notFound(ex, logMessage)
      case Failure(ex)                             => internalServerError(ex, logMessage)
    }

  def deleteDraftResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                              => success(s)
      case Failure(ex: EServiceNotFound)           => notFound(ex, logMessage)
      case Failure(ex: EServiceDescriptorNotFound) => notFound(ex, logMessage)
      case Failure(ex: DescriptorNotInDraft)       => conflict(ex, logMessage)
      case Failure(ex)                             => internalServerError(ex, logMessage)
    }

  def updateDescriptorResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                              => success(s)
      case Failure(ex: EServiceNotFound)           => notFound(ex, logMessage)
      case Failure(ex: EServiceDescriptorNotFound) => notFound(ex, logMessage)
      case Failure(ex)                             => internalServerError(ex, logMessage)
    }

  def updateEServiceByIdResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                    => success(s)
      case Failure(ex: EServiceNotFound) => notFound(ex, logMessage)
      case Failure(ex)                   => internalServerError(ex, logMessage)
    }

  def createdRiskAnalysisResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                    => success(s)
      case Failure(ex: EServiceNotFound) => notFound(ex, logMessage)
      case Failure(ex)                   => internalServerError(ex, logMessage)
    }

  def updateCatalogRiskAnalysisResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                => success(s)
      case Failure(ex: EServiceNotFound)             => notFound(ex, logMessage)
      case Failure(ex: EServiceRiskAnalysisNotFound) => notFound(ex, logMessage)
      case Failure(ex)                               => internalServerError(ex, logMessage)
    }

  def createDescriptorResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                    => success(s)
      case Failure(ex: EServiceNotFound) => notFound(ex, logMessage)
      case Failure(ex)                   => internalServerError(ex, logMessage)
    }

  def deleteEServiceDocumentResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                              => success(s)
      case Failure(ex: EServiceNotFound)           => notFound(ex, logMessage)
      case Failure(ex: EServiceDescriptorNotFound) => notFound(ex, logMessage)
      case Failure(ex: DocumentNotFound)           => notFound(ex, logMessage)
      case Failure(ex)                             => internalServerError(ex, logMessage)
    }

  def archiveDescriptorResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                              => success(s)
      case Failure(ex: EServiceNotFound)           => notFound(ex, logMessage)
      case Failure(ex: EServiceDescriptorNotFound) => notFound(ex, logMessage)
      case Failure(ex)                             => internalServerError(ex, logMessage)
    }

  def deprecateDescriptorResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                              => success(s)
      case Failure(ex: EServiceNotFound)           => notFound(ex, logMessage)
      case Failure(ex: EServiceDescriptorNotFound) => notFound(ex, logMessage)
      case Failure(ex)                             => internalServerError(ex, logMessage)
    }

  def suspendDescriptorResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                              => success(s)
      case Failure(ex: EServiceNotFound)           => notFound(ex, logMessage)
      case Failure(ex: EServiceDescriptorNotFound) => notFound(ex, logMessage)
      case Failure(ex)                             => internalServerError(ex, logMessage)
    }

  def draftDescriptorResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                              => success(s)
      case Failure(ex: EServiceNotFound)           => notFound(ex, logMessage)
      case Failure(ex: EServiceDescriptorNotFound) => notFound(ex, logMessage)
      case Failure(ex)                             => internalServerError(ex, logMessage)
    }

  def publishDescriptorResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                              => success(s)
      case Failure(ex: EServiceNotFound)           => notFound(ex, logMessage)
      case Failure(ex: EServiceDescriptorNotFound) => notFound(ex, logMessage)
      case Failure(ex)                             => internalServerError(ex, logMessage)
    }

  def updateEServiceDocumentResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                              => success(s)
      case Failure(ex: EServiceNotFound)           => notFound(ex, logMessage)
      case Failure(ex: EServiceDescriptorNotFound) => notFound(ex, logMessage)
      case Failure(ex: DocumentNotFound)           => notFound(ex, logMessage)
      case Failure(ex)                             => internalServerError(ex, logMessage)
    }

  def cloneEServiceByDescriptorResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                              => success(s)
      case Failure(ex: EServiceNotFound)           => notFound(ex, logMessage)
      case Failure(ex: EServiceDescriptorNotFound) => notFound(ex, logMessage)
      case Failure(ex)                             => internalServerError(ex, logMessage)
    }

  def deleteEServiceResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                       => success(s)
      case Failure(ex: EServiceNotFound)                    => notFound(ex, logMessage)
      case Failure(ex: EServiceWithDescriptorsNotDeletable) => conflict(ex, logMessage)
      case Failure(ex)                                      => internalServerError(ex, logMessage)
    }

  def moveAttributesToDescriptorsResponse[T](logMessage: String)(
    success: => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(_)                    => success
      case Failure(ex: EServiceNotFound) => notFound(ex, logMessage)
      case Failure(ex)                   => internalServerError(ex, logMessage)
    }
}
