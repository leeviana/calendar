package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._
import models.AccessType

/**
 * @author Leevi
 */
case class Rule (
    orderNum: Int,
    entityType: EntityType.EntityType,
    entityID: BSONObjectID, // foreign ref
    accessType: AccessType.AccessType
)

object EntityType extends Enumeration {
    type EntityType = Value

    val User, Group = Value
}

object AccessType extends Enumeration {
    type AccessType = Value

    val Private, BusyOnly, SeeAll, Modify = Value
}

object Rule {
    
    implicit object RuleReader extends BSONDocumentReader[Rule] {
        def read(doc: BSONDocument): Rule = {
            Rule(
                doc.getAs[Int]("orderNum").get,
                EntityType.withName(doc.getAs[String]("entityType").get),
                doc.getAs[BSONObjectID]("entityID").get,
                AccessType.withName(doc.getAs[String]("accessType").get)
            )
        }
    }
    
    implicit object RuleWriter extends BSONDocumentWriter[Rule] {
        def write(rule: Rule): BSONDocument = BSONDocument(
            "orderNum" -> rule.orderNum,
            "entityType" -> rule.entityType.toString(),
            "entityID" -> rule.entityID,
            "accessType" -> rule.accessType.toString()
        )
    }
      
    val form = Form(
        mapping(
            "orderNum" -> number,
            "entityType" -> nonEmptyText,
            "entityID" -> nonEmptyText,
            "accessType" -> nonEmptyText
        )  { (orderNum, entityType, entityID, accessType) =>
            Rule (
              orderNum,
              EntityType.withName(entityType),
              BSONObjectID.apply(entityID),
              AccessType.withName(accessType)
            )
        } { rule =>
            Some(
              (rule.orderNum,
              rule.entityType.toString(),
              rule.entityID.stringify,
              rule.accessType.toString()))
          }
    )
}