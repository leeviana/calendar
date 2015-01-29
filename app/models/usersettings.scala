package models

import reactivemongo.bson._

/**
 * @author Leevi
 */
case class UserSettings (
//    settingType: String
)

object UserSettings {
    implicit object UserReader extends BSONDocumentReader[UserSettings] {
        def read(doc: BSONDocument): UserSettings = {
            UserSettings()
//                doc.getAs[String]("settingType").get)
        }
    }
    
    implicit object UserWriter extends BSONDocumentWriter[UserSettings] {
        def write(usersettings: UserSettings): BSONDocument = BSONDocument(
//            "settingType" -> usersettings.settingType
            )
    }
}