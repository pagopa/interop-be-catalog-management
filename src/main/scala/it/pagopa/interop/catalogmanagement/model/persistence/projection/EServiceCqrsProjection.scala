package it.pagopa.interop.catalogmanagement.model.persistence.projection

import akka.actor.typed.ActorSystem
import it.pagopa.interop.catalogmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.catalogmanagement.model.persistence._
import it.pagopa.interop.commons.cqrs.model._
import it.pagopa.interop.commons.cqrs.service.CqrsProjection
import it.pagopa.interop.commons.cqrs.service.DocumentConversions._
import org.mongodb.scala.model._
import org.mongodb.scala.{MongoCollection, _}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import spray.json._

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

object EServiceCqrsProjection {
  def projection(offsetDbConfig: DatabaseConfig[JdbcProfile], mongoDbConfig: MongoDbConfig, projectionId: String)(
    implicit
    system: ActorSystem[_],
    ec: ExecutionContext
  ): CqrsProjection[Event] =
    CqrsProjection[Event](offsetDbConfig, mongoDbConfig, projectionId, eventHandler)

  private def eventHandler(collection: MongoCollection[Document], event: Event): PartialMongoAction = event match {
    case CatalogItemAdded(c)                             =>
      ActionWithDocument(collection.insertOne, Document(s"{ data: ${c.toJson.compactPrint} }"))
    case ClonedCatalogItemAdded(c)                       =>
      ActionWithDocument(collection.insertOne, Document(s"{ data: ${c.toJson.compactPrint} }"))
    case CatalogItemUpdated(c)                           =>
      ActionWithBson(collection.updateOne(Filters.eq("data.id", c.id.toString), _), Updates.set("data", c.toDocument))
    case CatalogItemWithDescriptorsDeleted(c, _)         =>
      // TODO remove the item only if it was the only descriptor
//      Action(collection.deleteOne(Filters.eq("data.id", c.id.toString)))
    case CatalogItemDocumentUpdated(esId, dId, docId, doc) =>


  }

}
