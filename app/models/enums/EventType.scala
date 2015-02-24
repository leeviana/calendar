package models.enums

import play.api.libs.json.Format
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue

object EventType extends Enumeration {
    type EventType = Value

    val Fixed, PUD, PUDEvent = Value

    implicit val EventTypeFormat = new Format[EventType] {
        def reads(json: JsValue) = JsSuccess(EventType.withName(json.as[String]))
        def writes(eventType: EventType) = JsString(eventType.toString)
    }
}