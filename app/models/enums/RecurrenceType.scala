package models.enums

import reactivemongo.bson.BSONDocumentReader
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONDocumentWriter
import reactivemongo.bson.BSONString
import reactivemongo.bson.BSONHandler
import reactivemongo.bson.BSON

object RecurrenceType extends Enumeration {
    type RecurrenceType = Value

    val Daily, Weekly, Monthly, Yearly = Value
    
    implicit object BSONEnumHandler extends BSONHandler[BSONString, RecurrenceType] {
        def read(doc: BSONString) = RecurrenceType.Value(doc.value)
        
        def write(recurrenceType: RecurrenceType) = BSON.write(recurrenceType.toString)
    }
}