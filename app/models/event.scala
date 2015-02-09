package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.format.Formats._
import org.joda.time.DateTime
import java.util.Date
import models.enums.AccessType

case class Event(
    _id: BSONObjectID,
    calendar: BSONObjectID, // pointer/foreign reference to Calendar
    timeRange: TimeRange,
    name: String,
    description: String,
    rules: List[Rule], // list of rule objects
    recurrenceMeta: Option[RecurrenceMeta], //TimeRange object, reminder time, one of the following: day, monthly, yearly, weekly
    nextRecurrence: Option[BSONObjectID], // BSONID pointer, can be null if not recurring
    accessType: AccessType.AccessType)

object Event {
    implicit val EventHandler = Macros.handler[Event]

    val form = Form(
        mapping(
            "calendar" -> nonEmptyText, // BSONObjectID
            "timeRange" -> TimeRange.form.mapping,
            "name" -> nonEmptyText,
            "description" -> nonEmptyText,
            "rules" -> optional(list(Rule.form.mapping)),
            "recurrenceMeta" -> optional(RecurrenceMeta.form.mapping),
            "nextRecurrence" -> optional(nonEmptyText) // BSONObjectID
            ) { (calendar, timeRange, name, description, rules, recurrenceMeta, nextRecurrence) =>
                Event(
                    BSONObjectID.generate,
                    BSONObjectID.apply(calendar),
                    timeRange,
                    name,
                    description,
                    rules.getOrElse(List[Rule]()),
                    recurrenceMeta,
                    nextRecurrence.map(id => BSONObjectID.apply(id)),
                    AccessType.Private)
            } { event =>
                Some((
                    event.calendar.stringify,
                    event.timeRange,
                    event.name,
                    event.description,
                    Some(event.rules),
                    event.recurrenceMeta,
                    event.nextRecurrence.map(id => id.stringify)))
            })
}