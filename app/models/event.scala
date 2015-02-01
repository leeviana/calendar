package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.format.Formats._
import org.joda.time.DateTime
import java.util.Date

case class Event(
    id: BSONObjectID,
    calendar: BSONObjectID, // pointer/foreign reference to Calendar
    timeRange: TimeRange,
    name: String,
    description: String,
    rules: BSONArray, // list of rule objects
    recurrenceMeta: Option[RecurrenceMeta], //TimeRange object, reminder time, one of the following: day, monthly, yearly, weekly
    nextRecurrence: Option[BSONObjectID] // BSONID pointer, can be null if not recurring
)

object Event {
    implicit object EventReader extends BSONDocumentReader[Event] {
        def read(doc: BSONDocument): Event = {
            Event(
                doc.getAs[BSONObjectID]("_id").get,
                doc.getAs[BSONObjectID]("calendar").get,
                doc.getAs[TimeRange]("timeRange").get,
                doc.getAs[String]("name").get,
                doc.getAs[String]("description").get,
                doc.getAs[BSONArray]("rules").get,
                doc.getAs[RecurrenceMeta]("recurrenceMeta"),
                doc.getAs[BSONObjectID]("nextRecurrence"))
        }
    }
    
    implicit object EventWriter extends BSONDocumentWriter[Event] {
        def write(event: Event) = {
            
            val bson = BSONDocument(
                "_id" -> event.id,
                "calendar" -> event.calendar,
                "timeRange" -> event.timeRange,
                "name" -> event.name,
                "description" -> event.description,
                "rules" -> event.rules,
                "recurrenceMeta" -> event.recurrenceMeta,
                "nextRecurrence" -> event.nextRecurrence)
                
            bson
        } 
    }
    
    val form = Form(
        mapping(
            "id" -> ignored(BSONObjectID.generate),
            "calendar" -> nonEmptyText, // BSONObjectID
            //"start" -> date("dd-MM-yyyy hh:mm a"),
            //"end" -> optional(date("dd-MM-yyyy hh:mm a")),
            "timeRange" -> TimeRange.form.mapping,
            "name" -> nonEmptyText,
            "description" -> nonEmptyText,
            "rules" -> ignored(BSONArray.empty),
            "recurrenceMeta" -> optional(RecurrenceMeta.form.mapping),
            "nextRecurrence" -> optional(nonEmptyText) // BSONObjectID
        ) { (id, calendar, timeRange, name, description, rules, recurrenceMeta, nextRecurrence) =>
            Event(
              id,
              BSONObjectID.apply(calendar),
              timeRange,
              name,
              description,
              rules,
              recurrenceMeta,
              nextRecurrence.map(id => BSONObjectID.apply(id)))
        } { event =>
            Some(
              (event.id,
              event.calendar.stringify,
              event.timeRange,
              event.name,
              event.description,
              event.rules,
              event.recurrenceMeta,
              event.nextRecurrence.map (id => id.stringify)))
          }
        )
}