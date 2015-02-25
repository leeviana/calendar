package models.enums

import play.api.libs.json.Format
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue

object RecurrenceType extends Enumeration {
    type RecurrenceType = Value

    val Daily, Weekly, Monthly, Yearly = Value

    implicit val EventFormat = new Format[RecurrenceType] {
        def reads(json: JsValue) = JsSuccess(RecurrenceType.withName(json.as[String]))
        def writes(recurrenceType: RecurrenceType) = JsString(recurrenceType.toString)
    }
}