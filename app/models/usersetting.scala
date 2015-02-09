package models

import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.Form
import models.enums.UserSettingType

/**
 * @author Leevi
 */
case class UserSetting(
    settingType: UserSettingType.UserSettingType,
    settingValue: String)

object UserSetting {
    implicit val UserSettingHandler = Macros.handler[UserSetting]

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