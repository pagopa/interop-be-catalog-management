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
    case CatalogItemAdded(c)                                   =>
      ActionWithDocument(collection.insertOne, Document(s"{ data: ${c.toJson.compactPrint} }"))
    case CatalogItemDescriptorAdded(esId, descriptor)          =>
      ActionWithBson(
        collection.updateOne(Filters.eq("data.id", esId), _),
        Updates.push(s"data.descriptors", descriptor.toDocument)
      )
    case CatalogItemUpdated(c)                                 =>
      ActionWithBson(collection.updateOne(Filters.eq("data.id", c.id.toString), _), Updates.set("data", c.toDocument))
    case CatalogItemWithDescriptorsDeleted(c, dId)             =>
      ActionWithBson(
        collection.updateOne(Filters.eq("data.id", c.id.toString), _),
        Updates.pull("data.descriptors", Document(s"{ id : \"$dId\" }"))
      )
    case CatalogItemDocumentAdded(esId, dId, doc, isInterface) =>
      if (isInterface)
        ActionWithBson(
          collection.updateMany(
            Filters.eq("data.id", esId),
            _,
            UpdateOptions().arrayFilters(List(Filters.eq("elem.id", dId)).asJava)
          ),
          Updates.set("data.descriptors.$[elem].interface", doc.toDocument)
        )
      else
        ActionWithBson(
          collection.updateMany(
            Filters.eq("data.id", esId),
            _,
            UpdateOptions().arrayFilters(List(Filters.eq("elem.id", dId)).asJava)
          ),
          Updates.push("data.descriptors.$[elem].docs", doc.toDocument)
        )
    case CatalogItemDocumentUpdated(esId, dId, docId, doc)     =>
      // TODO Test
      MultiAction(
        Seq(
          // Generic Doc
          ActionWithBson(
            collection.updateOne(
              Filters.eq("data.id", esId),
              _,
              UpdateOptions().arrayFilters(
                List(
                  Filters.and(Filters.eq("elem.id", dId), Filters.elemMatch("elem.docs", Filters.eq("id", docId)))
                ).asJava
              )
            ),
            // TODO Verify if this combination works
            Updates.combine(
              Updates.pull("data.descriptors.$[elem].docs", Document(s"{ id : \"$docId\" }")),
              Updates.push("data.descriptors.$[elem].docs", doc.toDocument)
            )
          ),
          // Interface
          ActionWithBson(
            collection.updateOne(
              Filters.eq("data.id", esId),
              _,
              UpdateOptions().arrayFilters(
                List(Filters.and(Filters.eq("elem.id", dId), Filters.eq("elem.interface.id", docId))).asJava
              )
            ),
            Updates.set("data.descriptors.$[elem].interface", doc.toDocument)
          )
        )
      )
    case ClonedCatalogItemAdded(c)                             =>
      ActionWithDocument(collection.insertOne, Document(s"{ data: ${c.toJson.compactPrint} }"))
    // TODO Remove
    case other                                                 =>
      throw new Exception(s"Not implemented yet: $other")
  }

}
