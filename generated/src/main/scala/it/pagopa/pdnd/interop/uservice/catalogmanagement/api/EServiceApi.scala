package it.pagopa.pdnd.interop.uservice.catalogmanagement.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
    import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
    import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller
import it.pagopa.pdnd.interop.uservice.catalogmanagement.server.AkkaHttpHelper._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.server.StringDirectives
import it.pagopa.pdnd.interop.uservice.catalogmanagement.server.MultipartDirectives
import it.pagopa.pdnd.interop.uservice.catalogmanagement.server.FileField
import it.pagopa.pdnd.interop.uservice.catalogmanagement.server.PartsAndFiles
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.EService
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.EServiceDescriptor
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.EServiceDescriptorSeed
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.EServiceDoc
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.EServiceSeed
import java.io.File
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.Problem
import java.util.UUID
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.UpdateEServiceDescriptorDocumentSeed
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.UpdateEServiceDescriptorSeed
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.UpdateEServiceSeed
import scala.util.Try
import akka.http.scaladsl.server.MalformedRequestContentRejection
import akka.http.scaladsl.server.directives.FileInfo


    class EServiceApi(
    eServiceService: EServiceApiService,
    eServiceMarshaller: EServiceApiMarshaller,
    wrappingDirective: Directive1[Seq[(String, String)]]
    )  extends MultipartDirectives with StringDirectives {
    
    import eServiceMarshaller._

    lazy val route: Route =
        path("eservices" / Segment / "descriptors" / Segment / "archive") { (eServiceId, descriptorId) => 
        post { wrappingDirective { implicit contexts =>  
            eServiceService.archiveDescriptor(eServiceId = eServiceId, descriptorId = descriptorId)
        }
        }
        } ~
        path("eservices" / Segment / "descriptors" / Segment / "clone") { (eServiceId, descriptorId) => 
        post { wrappingDirective { implicit contexts =>  
            eServiceService.cloneEServiceByDescriptor(eServiceId = eServiceId, descriptorId = descriptorId)
        }
        }
        } ~
        path("eservices" / Segment / "descriptors") { (eServiceId) => 
        post { wrappingDirective { implicit contexts =>  
            entity(as[EServiceDescriptorSeed]){ eServiceDescriptorSeed =>
              eServiceService.createDescriptor(eServiceId = eServiceId, eServiceDescriptorSeed = eServiceDescriptorSeed)
            }
        }
        }
        } ~
        path("eservices") { 
        post { wrappingDirective { implicit contexts =>  
            entity(as[EServiceSeed]){ eServiceSeed =>
              eServiceService.createEService(eServiceSeed = eServiceSeed)
            }
        }
        }
        } ~
        path("eservices" / Segment / "descriptors" / Segment / "documents") { (eServiceId, descriptorId) => 
        post { wrappingDirective { implicit contexts =>  
            
formAndFiles(FileField("doc"))  { partsAndFiles => 
    val routes : Try[Route] = for {
    doc <- optToTry(partsAndFiles.files.get("doc"), s"File doc missing")
    } yield { 
implicit val vp: StringValueProvider = partsAndFiles.form ++ contexts.toMap
    stringFields("kind".as[String], "description".as[String]) { (kind, description) =>
eServiceService.createEServiceDocument(eServiceId = eServiceId, descriptorId = descriptorId, kind = kind, description = description, doc = doc)
    }
    }
    routes.fold[Route](t => reject(MalformedRequestContentRejection("Missing file.", t)), identity)
}
        }
        }
        } ~
        path("eservices" / Segment / "descriptors" / Segment) { (eServiceId, descriptorId) => 
        delete { wrappingDirective { implicit contexts =>  
            eServiceService.deleteDraft(eServiceId = eServiceId, descriptorId = descriptorId)
        }
        }
        } ~
        path("eservices" / Segment) { (eServiceId) => 
        delete { wrappingDirective { implicit contexts =>  
            eServiceService.deleteEService(eServiceId = eServiceId)
        }
        }
        } ~
        path("eservices" / Segment / "descriptors" / Segment / "documents" / Segment) { (eServiceId, descriptorId, documentId) => 
        delete { wrappingDirective { implicit contexts =>  
            eServiceService.deleteEServiceDocument(eServiceId = eServiceId, descriptorId = descriptorId, documentId = documentId)
        }
        }
        } ~
        path("eservices" / Segment / "descriptors" / Segment / "deprecate") { (eServiceId, descriptorId) => 
        post { wrappingDirective { implicit contexts =>  
            eServiceService.deprecateDescriptor(eServiceId = eServiceId, descriptorId = descriptorId)
        }
        }
        } ~
        path("eservices" / Segment / "descriptors" / Segment / "draft") { (eServiceId, descriptorId) => 
        post { wrappingDirective { implicit contexts =>  
            eServiceService.draftDescriptor(eServiceId = eServiceId, descriptorId = descriptorId)
        }
        }
        } ~
        path("eservices" / Segment) { (eServiceId) => 
        get { wrappingDirective { implicit contexts =>  
            eServiceService.getEService(eServiceId = eServiceId)
        }
        }
        } ~
        path("eservices" / Segment / "descriptors" / Segment / "documents" / Segment) { (eServiceId, descriptorId, documentId) => 
        get { wrappingDirective { implicit contexts =>  
            eServiceService.getEServiceDocument(eServiceId = eServiceId, descriptorId = descriptorId, documentId = documentId)
        }
        }
        } ~
        path("eservices") { 
        get { wrappingDirective { implicit contexts => 
            parameters("producerId".as[String].?, "status".as[String].?) { (producerId, status) => 
            eServiceService.getEServices(producerId = producerId, status = status)
            }
        }
        }
        } ~
        path("eservices" / Segment / "descriptors" / Segment / "publish") { (eServiceId, descriptorId) => 
        post { wrappingDirective { implicit contexts =>  
            eServiceService.publishDescriptor(eServiceId = eServiceId, descriptorId = descriptorId)
        }
        }
        } ~
        path("eservices" / Segment / "descriptors" / Segment / "suspend") { (eServiceId, descriptorId) => 
        post { wrappingDirective { implicit contexts =>  
            eServiceService.suspendDescriptor(eServiceId = eServiceId, descriptorId = descriptorId)
        }
        }
        } ~
        path("eservices" / Segment / "descriptors" / Segment) { (eServiceId, descriptorId) => 
        put { wrappingDirective { implicit contexts =>  
            entity(as[UpdateEServiceDescriptorSeed]){ updateEServiceDescriptorSeed =>
              eServiceService.updateDescriptor(eServiceId = eServiceId, descriptorId = descriptorId, updateEServiceDescriptorSeed = updateEServiceDescriptorSeed)
            }
        }
        }
        } ~
        path("eservices" / Segment) { (eServiceId) => 
        put { wrappingDirective { implicit contexts =>  
            entity(as[UpdateEServiceSeed]){ updateEServiceSeed =>
              eServiceService.updateEServiceById(eServiceId = eServiceId, updateEServiceSeed = updateEServiceSeed)
            }
        }
        }
        } ~
        path("eservices" / Segment / "descriptors" / Segment / "documents" / Segment / "update") { (eServiceId, descriptorId, documentId) => 
        post { wrappingDirective { implicit contexts =>  
            entity(as[UpdateEServiceDescriptorDocumentSeed]){ updateEServiceDescriptorDocumentSeed =>
              eServiceService.updateEServiceDocument(eServiceId = eServiceId, descriptorId = descriptorId, documentId = documentId, updateEServiceDescriptorDocumentSeed = updateEServiceDescriptorDocumentSeed)
            }
        }
        }
        }
    }


    trait EServiceApiService {
          def archiveDescriptor204: Route =
            complete((204, "EService Descriptor status archived"))
  def archiveDescriptor400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
  def archiveDescriptor404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 204, Message: EService Descriptor status archived
   * Code: 400, Message: Invalid input, DataType: Problem
   * Code: 404, Message: Not found, DataType: Problem
        */
        def archiveDescriptor(eServiceId: String, descriptorId: String)
            (implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

          def cloneEServiceByDescriptor200(responseEService: EService)(implicit toEntityMarshallerEService: ToEntityMarshaller[EService]): Route =
            complete((200, responseEService))
  def cloneEServiceByDescriptor400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
  def cloneEServiceByDescriptor404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 200, Message: Cloned EService with a new draft descriptor updated., DataType: EService
   * Code: 400, Message: Invalid input, DataType: Problem
   * Code: 404, Message: Not found, DataType: Problem
        */
        def cloneEServiceByDescriptor(eServiceId: String, descriptorId: String)
            (implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], toEntityMarshallerEService: ToEntityMarshaller[EService], contexts: Seq[(String, String)]): Route

          def createDescriptor200(responseEServiceDescriptor: EServiceDescriptor)(implicit toEntityMarshallerEServiceDescriptor: ToEntityMarshaller[EServiceDescriptor]): Route =
            complete((200, responseEServiceDescriptor))
  def createDescriptor400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
  def createDescriptor404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 200, Message: EService Descriptor created., DataType: EServiceDescriptor
   * Code: 400, Message: Invalid input, DataType: Problem
   * Code: 404, Message: Not found, DataType: Problem
        */
        def createDescriptor(eServiceId: String, eServiceDescriptorSeed: EServiceDescriptorSeed)
            (implicit toEntityMarshallerEServiceDescriptor: ToEntityMarshaller[EServiceDescriptor], toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

          def createEService200(responseEService: EService)(implicit toEntityMarshallerEService: ToEntityMarshaller[EService]): Route =
            complete((200, responseEService))
  def createEService400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
        /**
           * Code: 200, Message: EService created, DataType: EService
   * Code: 400, Message: Invalid input, DataType: Problem
        */
        def createEService(eServiceSeed: EServiceSeed)
            (implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], toEntityMarshallerEService: ToEntityMarshaller[EService], contexts: Seq[(String, String)]): Route

          def createEServiceDocument200(responseEService: EService)(implicit toEntityMarshallerEService: ToEntityMarshaller[EService]): Route =
            complete((200, responseEService))
  def createEServiceDocument400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
  def createEServiceDocument404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 200, Message: EService Document created, DataType: EService
   * Code: 400, Message: Invalid input, DataType: Problem
   * Code: 404, Message: Not found, DataType: Problem
        */
        def createEServiceDocument(eServiceId: String, descriptorId: String, kind: String, description: String, doc: (FileInfo, File))
            (implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], toEntityMarshallerEService: ToEntityMarshaller[EService], contexts: Seq[(String, String)]): Route

          def deleteDraft204: Route =
            complete((204, "EService draft Descriptor deleted"))
  def deleteDraft400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
  def deleteDraft404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 204, Message: EService draft Descriptor deleted
   * Code: 400, Message: Invalid input, DataType: Problem
   * Code: 404, Message: Not found, DataType: Problem
        */
        def deleteDraft(eServiceId: String, descriptorId: String)
            (implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

          def deleteEService204: Route =
            complete((204, "EService deleted"))
  def deleteEService400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
  def deleteEService404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 204, Message: EService deleted
   * Code: 400, Message: Invalid input, DataType: Problem
   * Code: 404, Message: Not found, DataType: Problem
        */
        def deleteEService(eServiceId: String)
            (implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

          def deleteEServiceDocument204: Route =
            complete((204, "Document deleted."))
  def deleteEServiceDocument404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
  def deleteEServiceDocument400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
        /**
           * Code: 204, Message: Document deleted.
   * Code: 404, Message: E-Service descriptor document not found, DataType: Problem
   * Code: 400, Message: Bad request, DataType: Problem
        */
        def deleteEServiceDocument(eServiceId: String, descriptorId: String, documentId: String)
            (implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

          def deprecateDescriptor204: Route =
            complete((204, "EService Descriptor status deprecated"))
  def deprecateDescriptor400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
  def deprecateDescriptor404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 204, Message: EService Descriptor status deprecated
   * Code: 400, Message: Invalid input, DataType: Problem
   * Code: 404, Message: Not found, DataType: Problem
        */
        def deprecateDescriptor(eServiceId: String, descriptorId: String)
            (implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

          def draftDescriptor204: Route =
            complete((204, "EService Descriptor status changed in draft"))
  def draftDescriptor400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
  def draftDescriptor404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 204, Message: EService Descriptor status changed in draft
   * Code: 400, Message: Invalid input, DataType: Problem
   * Code: 404, Message: Not found, DataType: Problem
        */
        def draftDescriptor(eServiceId: String, descriptorId: String)
            (implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

          def getEService200(responseEService: EService)(implicit toEntityMarshallerEService: ToEntityMarshaller[EService]): Route =
            complete((200, responseEService))
  def getEService404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
  def getEService400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
        /**
           * Code: 200, Message: EService retrieved, DataType: EService
   * Code: 404, Message: EService not found, DataType: Problem
   * Code: 400, Message: Bad request, DataType: Problem
        */
        def getEService(eServiceId: String)
            (implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], toEntityMarshallerEService: ToEntityMarshaller[EService], contexts: Seq[(String, String)]): Route

          def getEServiceDocument200(responseEServiceDoc: EServiceDoc)(implicit toEntityMarshallerEServiceDoc: ToEntityMarshaller[EServiceDoc]): Route =
            complete((200, responseEServiceDoc))
  def getEServiceDocument404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
  def getEServiceDocument400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
        /**
           * Code: 200, Message: EService document retrieved, DataType: EServiceDoc
   * Code: 404, Message: EService not found, DataType: Problem
   * Code: 400, Message: Bad request, DataType: Problem
        */
        def getEServiceDocument(eServiceId: String, descriptorId: String, documentId: String)
            (implicit toEntityMarshallerEServiceDoc: ToEntityMarshaller[EServiceDoc], toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

          def getEServices200(responseEServicearray: Seq[EService])(implicit toEntityMarshallerEServicearray: ToEntityMarshaller[Seq[EService]]): Route =
            complete((200, responseEServicearray))
        /**
           * Code: 200, Message: A list of EService, DataType: Seq[EService]
        */
        def getEServices(producerId: Option[String], status: Option[String])
            (implicit toEntityMarshallerEServicearray: ToEntityMarshaller[Seq[EService]], contexts: Seq[(String, String)]): Route

          def publishDescriptor204: Route =
            complete((204, "EService Descriptor status published."))
  def publishDescriptor400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
  def publishDescriptor404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 204, Message: EService Descriptor status published.
   * Code: 400, Message: Invalid input, DataType: Problem
   * Code: 404, Message: Not found, DataType: Problem
        */
        def publishDescriptor(eServiceId: String, descriptorId: String)
            (implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

          def suspendDescriptor204: Route =
            complete((204, "EService Descriptor status suspended"))
  def suspendDescriptor400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
  def suspendDescriptor404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 204, Message: EService Descriptor status suspended
   * Code: 400, Message: Invalid input, DataType: Problem
   * Code: 404, Message: Not found, DataType: Problem
        */
        def suspendDescriptor(eServiceId: String, descriptorId: String)
            (implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

          def updateDescriptor200(responseEService: EService)(implicit toEntityMarshallerEService: ToEntityMarshaller[EService]): Route =
            complete((200, responseEService))
  def updateDescriptor400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
  def updateDescriptor404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 200, Message: EService Descriptor published, DataType: EService
   * Code: 400, Message: Invalid input, DataType: Problem
   * Code: 404, Message: Not found, DataType: Problem
        */
        def updateDescriptor(eServiceId: String, descriptorId: String, updateEServiceDescriptorSeed: UpdateEServiceDescriptorSeed)
            (implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], toEntityMarshallerEService: ToEntityMarshaller[EService], contexts: Seq[(String, String)]): Route

          def updateEServiceById200(responseEService: EService)(implicit toEntityMarshallerEService: ToEntityMarshaller[EService]): Route =
            complete((200, responseEService))
  def updateEServiceById404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
  def updateEServiceById400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
        /**
           * Code: 200, Message: E-Service updated, DataType: EService
   * Code: 404, Message: E-Service not found, DataType: Problem
   * Code: 400, Message: Bad request, DataType: Problem
        */
        def updateEServiceById(eServiceId: String, updateEServiceSeed: UpdateEServiceSeed)
            (implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], toEntityMarshallerEService: ToEntityMarshaller[EService], contexts: Seq[(String, String)]): Route

          def updateEServiceDocument200(responseEServiceDoc: EServiceDoc)(implicit toEntityMarshallerEServiceDoc: ToEntityMarshaller[EServiceDoc]): Route =
            complete((200, responseEServiceDoc))
  def updateEServiceDocument404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
  def updateEServiceDocument400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
        /**
           * Code: 200, Message: EService Descriptor Document updated., DataType: EServiceDoc
   * Code: 404, Message: EService not found, DataType: Problem
   * Code: 400, Message: Bad request, DataType: Problem
        */
        def updateEServiceDocument(eServiceId: String, descriptorId: String, documentId: String, updateEServiceDescriptorDocumentSeed: UpdateEServiceDescriptorDocumentSeed)
            (implicit toEntityMarshallerEServiceDoc: ToEntityMarshaller[EServiceDoc], toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

    }

        trait EServiceApiMarshaller {
          implicit def fromEntityUnmarshallerEServiceDescriptorSeed: FromEntityUnmarshaller[EServiceDescriptorSeed]

  implicit def fromEntityUnmarshallerUpdateEServiceDescriptorSeed: FromEntityUnmarshaller[UpdateEServiceDescriptorSeed]

  implicit def fromEntityUnmarshallerUpdateEServiceDescriptorDocumentSeed: FromEntityUnmarshaller[UpdateEServiceDescriptorDocumentSeed]

  implicit def fromEntityUnmarshallerEServiceSeed: FromEntityUnmarshaller[EServiceSeed]

  implicit def fromEntityUnmarshallerUpdateEServiceSeed: FromEntityUnmarshaller[UpdateEServiceSeed]


        
          implicit def toEntityMarshallerEServiceDescriptor: ToEntityMarshaller[EServiceDescriptor]

  implicit def toEntityMarshallerEServicearray: ToEntityMarshaller[Seq[EService]]

  implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem]

  implicit def toEntityMarshallerEServiceDoc: ToEntityMarshaller[EServiceDoc]

  implicit def toEntityMarshallerEService: ToEntityMarshaller[EService]

        }

