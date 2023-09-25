package it.pagopa.interop.catalogmanagement.utils

import it.pagopa.interop.catalogmanagement.ItSpecData
import it.pagopa.interop.catalogmanagement.model.{
  Automatic,
  CatalogAttributes,
  CatalogDescriptor,
  CatalogDescriptorState,
  CatalogDocument,
  CatalogItem,
  CatalogItemTechnology,
  EService,
  EServiceDescriptor,
  EServiceDoc
}
import it.pagopa.interop.catalogmanagement.model.CatalogAdapters._
import it.pagopa.interop.catalogmanagement.model.CatalogItemMode

object PersistentAdapters {

  implicit class EServiceWrapper(private val p: EService) extends AnyVal {
    def toPersistent: CatalogItem =
      CatalogItem(
        id = p.id,
        producerId = p.producerId,
        name = p.name,
        description = p.description,
        technology = CatalogItemTechnology.fromApi(p.technology),
        attributes = Some(CatalogAttributes.empty),
        descriptors = p.descriptors.map(_.toPersistent),
        createdAt = ItSpecData.timestamp, // TODO Replace this when createdAt will be added to API,
        riskAnalysis = Seq.empty,
        mode = CatalogItemMode.default
      )
  }

  implicit class EServiceDescriptorWrapper(private val p: EServiceDescriptor) extends AnyVal {
    def toPersistent: CatalogDescriptor =
      CatalogDescriptor(
        id = p.id,
        version = p.version,
        description = p.description,
        interface = p.interface.map(_.toPersistent),
        docs = p.docs.map(_.toPersistent),
        state = CatalogDescriptorState.fromApi(p.state),
        audience = p.audience,
        voucherLifespan = p.voucherLifespan,
        dailyCallsPerConsumer = p.dailyCallsPerConsumer,
        dailyCallsTotal = p.dailyCallsTotal,
        agreementApprovalPolicy = Some(Automatic),
        serverUrls = p.serverUrls.toList,
        createdAt = ItSpecData.timestamp, // TODO Replace this when createdAt will be added to API
        publishedAt = None,
        suspendedAt = None,
        deprecatedAt = None,
        archivedAt = None,
        attributes = CatalogAttributes.empty
      )
  }

  implicit class EServiceDocumentWrapper(private val p: EServiceDoc) extends AnyVal {
    def toPersistent: CatalogDocument =
      CatalogDocument(
        id = p.id,
        name = p.name,
        contentType = p.contentType,
        prettyName = p.prettyName,
        path = p.path,
        checksum = p.checksum,
        uploadDate = p.uploadDate
      )
  }

}
