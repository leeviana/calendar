package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._
import models.enums.EntityType
import models.enums.AccessType

/**
 * @author Leevi
 */
case class Rule(
    orderNum: Int,
    entityType: EntityType.EntityType,
    entityID: BSONObjectID, // foreign ref to entity
    accessType: AccessType.AccessType)

object Rule {

    implicit val RuleHandler = Macros.handler[Rule]

    val form = Form(
        mapping(
            "orderNum" -> number,
            "entityType" -> nonEmptyText,
            "entityID" -> nonEmptyText,
            "accessType" -> nonEmptyText) { (orderNum, entityType, entityID, accessType) =>
                Rule(
                    orderNum,
                    EntityType.withName(entityType),
                    BSONObjectID.apply(entityID),
                    AccessType.withName(accessType))
            } { rule =>
                Some((
                    rule.orderNum,
                    rule.entityType.toString(),
                    rule.entityID.stringify,
                    rule.accessType.toString()))
            })
}