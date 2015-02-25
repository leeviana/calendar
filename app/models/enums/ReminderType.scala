package models.enums

import play.api.libs.json.Format
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue

object ReminderType extends Enumeration {
    type ReminderType = Value

    val Email = Value

    implicit val ReminderTypeFormat = new Format[ReminderType] {
        def reads(json: JsValue) = JsSuccess(ReminderType.withName(json.as[String]))
        def writes(reminderType: ReminderType) = JsString(reminderType.toString)
    }
}