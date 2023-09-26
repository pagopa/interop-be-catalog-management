package it.pagopa.interop.catalogmanagement.model.persistence.projection

import akka.actor.typed.ActorSystem
import it.pagopa.interop.catalogmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.catalogmanagement.model.persistence._
import it.pagopa.interop.commons.cqrs.model._
import it.pagopa.interop.commons.cqrs.service.CqrsProjection
import it.pagopa.interop.commons.cqrs.service.DocumentConversions._
import org.mongodb.scala.bson.{BsonArray, BsonString}
import org.mongodb.scala.model.{Updates, _}
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
    case CatalogItemAdded(c) =>
      ActionWithDocument(collection.insertOne, Document(s"{ data: ${c.toJson.compactPrint} }"))

    case CatalogItemRiskAnalysisAdded(c, _) =>
      ActionWithBson(collection.updateOne(Filters.eq("data.id", c.id.toString), _), Updates.set("data", c.toDocument))
   
    case CatalogItemRiskAnalysisUpdated(esId, ra) =>
      ActionWithBson(
        collection.updateMany(
          Filters.eq("data.id", esId),
          _,
          UpdateOptions().arrayFilters(List(Filters.eq("elem.id", ra.id.toString)).asJava)
        ),
        Updates.set("data.riskAnalysis.$[elem]", ra.toDocument)
      )

    case CatalogItemDescriptorAdded(esId, descriptor) =>
      ActionWithBson(
        collection.updateOne(Filters.eq("data.id", esId), _),
        Updates.push(s"data.descriptors", descriptor.toDocument)
      )

    case CatalogItemUpdated(c) =>
      ActionWithBson(collection.updateOne(Filters.eq("data.id", c.id.toString), _), Updates.set("data", c.toDocument))

    case CatalogItemDescriptorUpdated(cId, d) =>
      ActionWithBson(
        collection.updateMany(
          Filters.eq("data.id", cId),
          _,
          UpdateOptions().arrayFilters(List(Filters.eq("elem.id", d.id.toString)).asJava)
        ),
        Updates.set("data.descriptors.$[elem]", d.toDocument)
      )

    case CatalogItemDeleted(cId) =>
      Action(collection.deleteOne(Filters.eq("data.id", cId)))

    case CatalogItemWithDescriptorsDeleted(c, dId) =>
      ActionWithBson(
        collection.updateOne(Filters.eq("data.id", c.id.toString), _),
        Updates.pull("data.descriptors", Document(s"{ id : \"$dId\" }"))
      )

    case CatalogItemDocumentAdded(esId, dId, doc, isInterface, serverUrls) =>
      if (isInterface)
        ActionWithBson(
          collection.updateMany(
            Filters.eq("data.id", esId),
            _,
            UpdateOptions().arrayFilters(List(Filters.eq("elem.id", dId)).asJava)
          ),
          Updates.combine(
            Updates.set("data.descriptors.$[elem].interface", doc.toDocument),
            Updates.set("data.descriptors.$[elem].serverUrls", BsonArray.fromIterable(serverUrls.map(BsonString(_))))
          )
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

    case CatalogItemDocumentDeleted(esId, dId, docId) =>
      MultiAction(
        Seq(
          // Generic Doc
          ActionWithBson(
            collection.updateOne(
              Filters.eq("data.id", esId),
              _,
              UpdateOptions().arrayFilters(List(Filters.eq("descriptor.id", dId)).asJava)
            ),
            Updates.pull("data.descriptors.$[descriptor].docs", Filters.eq("id", docId))
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
            Updates.combine(
              Updates.unset("data.descriptors.$[elem].interface"),
              Updates.set("data.descriptors.$[elem].serverUrls", BsonArray.fromIterable(List.empty))
            )
          )
        )
      )

    case CatalogItemDocumentUpdated(esId, dId, docId, doc, serverUrls) =>
      MultiAction(
        Seq(
          // Generic Doc
          ActionWithBson(
            collection.updateOne(
              Filters.eq("data.id", esId),
              _,
              UpdateOptions()
                .arrayFilters(List(Filters.eq("descriptor.id", dId), Filters.eq("document.id", docId)).asJava)
            ),
            Updates.set("data.descriptors.$[descriptor].docs.$[document]", doc.toDocument)
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
            Updates.combine(
              Updates.set("data.descriptors.$[elem].interface", doc.toDocument),
              Updates.set("data.descriptors.$[elem].serverUrls", BsonArray.fromIterable(serverUrls.map(BsonString(_))))
            )
          )
        )
      )

    case ClonedCatalogItemAdded(c) =>
      ActionWithDocument(collection.insertOne, Document(s"{ data: ${c.toJson.compactPrint} }"))

    case MovedAttributesFromEserviceToDescriptors(c) =>
      ActionWithBson(collection.updateOne(Filters.eq("data.id", c.id.toString), _), Updates.set("data", c.toDocument))
  }

}
