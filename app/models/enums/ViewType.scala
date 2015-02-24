package models.enums

import play.api.libs.json.Format
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue

object ViewType extends Enumeration {
    type ViewType = Value

    val Confirmed, Request, Declined = Value

    implicit val ViewTypeFormat = new Format[ViewType] {
        def reads(json: JsValue) = JsSuccess(ViewType.withName(json.as[String]))
        def writes(viewType: ViewType) = JsString(viewType.toString)
    }
}