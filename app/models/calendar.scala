package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._
import models.AccessType

/**
 * @author Leevi
 */
case class Calendar (
    orderNum: Int,
    entityType: EntityType.EntityType,
    entityID: BSONObjectID, // foreign ref
    access: AccessType.AccessType
)

object Calendar {
    implicit object CalendarReader extends BSONDocumentReader[Calendar] {
        def read(doc: BSONDocument): Calendar = {
            Calendar(
                doc.getAs[Int]("orderNum").get,
                doc.getAs[EntityType.EntityType]("entityType").get,
                doc.getAs[BSONObjectID]("entityID").get,
                doc.getAs[AccessType.AccessType]("access").get
            )
        }
    }
    
    implicit object CalendarWriter extends BSONDocumentWriter[Calendar] {
        def write(calendar: Calendar): BSONDocument = BSONDocument(
            "orderNum" -> calendar.orderNum,
            "entityType" -> calendar.entityType.toString(),
            "entityID" -> calendar.entityID,
            "access" -> calendar.access.toString()
        )
    }
      
    val form = Form(
        mapping(
            "orderNum" -> number,
            "entityType" -> nonEmptyText,
            "entityID" -> nonEmptyText,
            "access" -> nonEmptyText
        )  { (orderNum, entityType, entityID, access) =>
            Calendar (
              orderNum,
              EntityType.withName(entityType),
              BSONObjectID.apply(entityID),
              AccessType.withName(access)
            )
        } { calendar =>
            Some(
              (calendar.orderNum,
              calendar.entityType.toString(),
              calendar.entityID.toString(),
              calendar.access.toString()))
          }
    )
}