package models.enums

import reactivemongo.bson.BSON
import reactivemongo.bson.BSONHandler
import reactivemongo.bson.BSONString

object ReminderType extends Enumeration {
    type ReminderType = Value

    val Email = Value
    
    implicit object BSONEnumHandler extends BSONHandler[BSONString, ReminderType] {
        def read(doc: BSONString) = ReminderType.Value(doc.value)
        
        def write(reminderType: ReminderType) = BSON.write(reminderType.toString)
    }
}