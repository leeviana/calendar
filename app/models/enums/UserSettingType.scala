package models.enums

import reactivemongo.bson.BSONDocumentReader
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONDocumentWriter
import reactivemongo.bson.BSONString
import reactivemongo.bson.BSONHandler
import reactivemongo.bson.BSON

object UserSettingType extends Enumeration {
    type UserSettingType = Value

    val Placeholder = Value

    implicit object BSONEnumHandler extends BSONHandler[BSONString, UserSettingType] {
        def read(doc: BSONString) = UserSettingType.Value(doc.value)

        def write(userSettingType: UserSettingType) = BSON.write(userSettingType.toString)
    }
}