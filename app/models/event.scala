package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.format.Formats._
import org.joda.time.DateTime
import java.util.Date

case class Event(
    id: BSONObjectID,
    calendar: BSONObjectID, // foreign reference?
    start: DateTime, // use date and TimeRange object, instead of start and end to account for all day events?
    // TimeRange - start, end date/datetime, toggle for entire/multiday
    end: Option[DateTime],
    name: String,
    description: String,
    // Rules: List{ }, // list of rule objects
    // RecurrenceMeta: { }, //TimeRange object, reminder time, one of the following: day, monthly, yearly, weekly
    nextRecurrence: BSONObjectID // pointer, can be null if not recurring
             ) 

object Event {
    implicit object EventReader extends BSONDocumentReader[Event] {
        def read(doc: BSONDocument): Event = {
            Event(
                doc.getAs[BSONObjectID]("_id").get,
                doc.getAs[BSONObjectID]("calendar").get,
                doc.getAs[BSONDateTime]("start").map(dt => new DateTime(dt.value)).get,
                doc.getAs[BSONDateTime]("end").map(dt => new DateTime(dt.value)),
                doc.getAs[String]("name").get,
                doc.getAs[String]("description").get,
                doc.getAs[BSONObjectID]("nextRecurrence").get)
        }
    }
    
    implicit object EventWriter extends BSONDocumentWriter[Event] {
        def write(event: Event) = {
            
            val bson = BSONDocument(
                "id" -> event.id,
                "calendar" -> event.calendar,
                "start" -> BSONDateTime(event.start.getMillis),
                "name" -> event.name,
                "description" -> event.description,
                "nextRecurrence" -> event.nextRecurrence)
                
            if(event.end.isDefined)
                bson.add("end" -> BSONDateTime(event.end.get.getMillis))
            
            bson
        } 
    }
    
    val form = Form(
        mapping(
            "id" -> ignored(BSONObjectID.generate),
//            "id" -> optional(of[String] verifying pattern(
//              """[a-fA-F0-9]{24}""".r,
//              "constraint.objectId",
//              "error.objectId")),
            "calendar" -> ignored(BSONObjectID.generate), // populate with user's calendars... currently just generating ID
            "start" -> date,
            "end" -> optional(date),
            "name" -> nonEmptyText,           
            "description" -> nonEmptyText,
            "nextRecurrence" -> ignored(BSONObjectID.generate) // pointer... also currently just generating ID
        ) { (id, calendar, start, end, name, description, nextRecurrence) =>
            Event(
              id,
              calendar,
              new DateTime(start),
              end.map(dt => new DateTime(dt)),
              name,
              description,
              nextRecurrence)
              //creationDate.map(new DateTime(_)),
              //updateDate.map(new DateTime(_)))
        } { event =>
            Some(
              (event.id,
              event.calendar,
              new Date(event.start.getMillis),
              event.end.map { dt => new Date(dt.getMillis) },
              event.name,
              event.description,
              event.nextRecurrence))
          }
        )
}