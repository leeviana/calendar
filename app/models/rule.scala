package models

import models.enums.AccessType
import models.enums.EntityType
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.number
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._

/**
 * @author Leevi
 */
case class Rule(
    orderNum: Int,
    entityType: EntityType.EntityType,
    entityID: BSONObjectID, // foreign ref to entity
    accessType: AccessType.AccessType)

object Rule {
    implicit val RuleFormat = Json.format[Rule]

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