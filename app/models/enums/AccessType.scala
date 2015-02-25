package models.enums

import play.api.libs.json.Format
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue

object AccessType extends Enumeration {
    type AccessType = Value

    val Private, BusyOnly, SeeAll, Modify, SeePUD = Value

    implicit val AccessTypeFormat = new Format[AccessType] {
        def reads(json: JsValue) = JsSuccess(AccessType.withName(json.as[String]))
        def writes(accessType: AccessType) = JsString(accessType.toString)
    }
}