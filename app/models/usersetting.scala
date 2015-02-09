package models

import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.Form

/**
 * @author Leevi
 */
case class UserSetting(
    settingType: String,
    settingValue: String)

object UserSettingType extends Enumeration {
    type UserSettingType = Value

    val Placeholder = Value

    implicit object UserSettingTypeReader extends BSONDocumentReader[UserSettingType] {
        def read(doc: BSONDocument): UserSettingType = {
            UserSettingType.withName(doc.getAs[String]("userSettingType").get)
        }
    }
}

object UserSetting {
    implicit object UserSettingReader extends BSONDocumentReader[UserSetting] {
        def read(doc: BSONDocument): UserSetting = {
            UserSetting(
                // TODO: replace this
                doc.getAs[String]("settingType").get,
                doc.getAs[String]("settingValue").get)
        }
    }

    implicit object UserSettingWriter extends BSONDocumentWriter[UserSetting] {
        def write(usersettings: UserSetting): BSONDocument = BSONDocument(
            "settingType" -> usersettings.settingType,
            "settingValue" -> usersettings.settingValue)
    }

    val form = Form(
        mapping(
            "settingType" -> nonEmptyText,
            "settingValue" -> nonEmptyText) { (settingType, settingValue) =>
                UserSetting(
                    settingType,
                    settingValue)
            } { usersetting =>
                Some(
                    (usersetting.settingType,
                        usersetting.settingValue))
            })
}