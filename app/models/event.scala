package models

import reactivemongo.bson._
import org.joda.time.DateTime

case class Event(id: BSONObjectID,
                 calendar: BSONObjectID, // foreign reference?
                 start: BSONDateTime, // use date and TimeRange object, instead of start and end to account for all day events?
                 // TimeRange - start, end date/datetime, toggle for entire/multiday
                 end: Option[BSONDateTime],
                 name: String,
                 description: String,
                 // Rules: List{ }, // list of rule objects
                 // RecurrenceMeta: { }, //TimeRange object, reminder time, one of the following: day, monthly, yearly, weekly
                 nextRecurrence: Int // pointer, can be null if not recurring
                 ) 

object Event {
    implicit object EventReader extends BSONDocumentReader[Event] {
        def read(doc: BSONDocument): Event = {
            Event(
                doc.getAs[BSONObjectID]("_id").get,
                doc.getAs[BSONObjectID]("calendar").get,
                doc.getAs[BSONDateTime]("start").get,
                doc.getAs[BSONDateTime]("end"),
                doc.getAs[String]("name").get,
                doc.getAs[String]("description").get,
                doc.getAs[Int]("nextRecurrence").get)
        }
    }
    
    implicit object EventWriter extends BSONDocumentWriter[Event] {
        def write(event: Event): BSONDocument = BSONDocument(
            "id" -> event.id,
            "calendar" -> event.calendar,
            "start" -> event.start,
            "end" -> event.end,
            "name" -> event.name,
            "description" -> event.description,
            "nextRecurrence" -> event.nextRecurrence)
    }
}