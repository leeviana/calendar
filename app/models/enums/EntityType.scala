package models.enums

import play.api.libs.json.Format
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue

object EntityType extends Enumeration {
    type EntityType = Value

    val User, Group = Value

    implicit val EventFormat = new Format[EntityType] {
        def reads(json: JsValue) = JsSuccess(EntityType.withName(json.as[String]))
        def writes(entityType: EntityType) = JsString(entityType.toString)
    }
}