package models.enums

import play.api.libs.json.Format
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue

object CreationRequestStatus extends Enumeration {
    type CreationRequestStatus = Value

    val Pending, Confirmed, Declined, Removed = Value

    implicit val CreationRequestStatusFormat = new Format[CreationRequestStatus] {
        def reads(json: JsValue) = JsSuccess(CreationRequestStatus.withName(json.as[String]))
        def writes(creationRequestStatus: CreationRequestStatus) = JsString(creationRequestStatus.toString)
    }
}