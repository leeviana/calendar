package models.enums

import reactivemongo.bson.BSONDocumentReader
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONDocumentWriter
import reactivemongo.bson.BSONString
import reactivemongo.bson.BSONHandler
import reactivemongo.bson.BSON

object EntityType extends Enumeration {
    type EntityType = Value

    val User, Group = Value

    implicit object BSONEnumHandler extends BSONHandler[BSONString, EntityType] {
        def read(doc: BSONString) = EntityType.Value(doc.value)

        def write(entityType: EntityType) = BSON.write(entityType.toString)
    }
}