package models

import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.Form
import models.enums.UserSettingType
import play.api.libs.json.Json

/**
 * @author Leevi
 */
case class UserSetting(
    settingType: UserSettingType.UserSettingType,
    settingValue: String)

object UserSetting {
    //implicit val UserSettingHandler = Macros.handler[UserSetting]
    implicit val UserSettingFormat = Json.format[UserSetting]
    
    val form = Form(
        mapping(
            "settingType" -> nonEmptyText,
            "settingValue" -> nonEmptyText) { (settingType, settingValue) =>
                UserSetting(
                    UserSettingType.withName(settingType),
                    settingValue)
            } { usersetting =>
                Some((
                    usersetting.settingType.toString(),
                    usersetting.settingValue))
            })
}