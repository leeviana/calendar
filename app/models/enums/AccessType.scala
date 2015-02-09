package models.enums

import reactivemongo.bson.BSONDocumentReader
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONDocumentWriter
import reactivemongo.bson.BSONString
import reactivemongo.bson.BSONHandler
import reactivemongo.bson.BSON

object AccessType extends Enumeration {
    type AccessType = Value

    val Private, BusyOnly, SeeAll, Modify = Value

    implicit object BSONEnumHandler extends BSONHandler[BSONString, AccessType] {
        def read(doc: BSONString) = AccessType.Value(doc.value)

        def write(accessType: AccessType) = BSON.write(accessType.toString)
    }
}