package models.enums

import play.api.libs.json.Format
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue

object UserSettingType extends Enumeration {
    type UserSettingType = Value

    val Placeholder = Value

    implicit val UserSettingTypeFormat = new Format[UserSettingType] {
        def reads(json: JsValue) = JsSuccess(UserSettingType.withName(json.as[String]))
        def writes(userSettingType: UserSettingType) = JsString(userSettingType.toString)
    }
}