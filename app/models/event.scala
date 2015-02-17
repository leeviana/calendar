package models

import models.enums.AccessType
import play.api.data.Form
import play.api.data.Forms.list
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.optional
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._
import models.enums.EventType

case class Event(
    _id: BSONObjectID = BSONObjectID.generate,
    calendar: BSONObjectID, // pointer/foreign reference to Calendar
    timeRange: TimeRange,
    name: String,
    description: String,
    master: Option[BSONObjectID] = None, // foreign reference to a "master" event, if shared
    rules: List[Rule], // list of rule objects
    recurrenceMeta: Option[RecurrenceMeta], //TimeRange object, reminder time, one of the following: day, monthly, yearly, weekly
    nextRecurrence: Option[BSONObjectID], // BSONID pointer, can be null if not recurring
    accessType: Option[AccessType.AccessType],
    eventType: EventType.EventType)

object Event {
    implicit val EventFormat = Json.format[Event]

    val form = Form(
        mapping(
            "calendar" -> nonEmptyText, // BSONObjectID
            "timeRange" -> TimeRange.form.mapping,
            "name" -> nonEmptyText,
            "description" -> nonEmptyText,
            "rules" -> optional(list(Rule.form.mapping)),
            "recurrenceMeta" -> optional(RecurrenceMeta.form.mapping),
            "nextRecurrence" -> optional(nonEmptyText), // BSONObjectID
            "eventType" -> nonEmptyText
            ) { (calendar, timeRange, name, description, rules, recurrenceMeta, nextRecurrence, eventType) =>
                Event(
                    BSONObjectID.generate,
                    BSONObjectID.apply(calendar),
                    timeRange,
                    name,
                    description,
                    None,
                    rules.getOrElse(List[Rule]()),
                    recurrenceMeta,
                    nextRecurrence.map(id => BSONObjectID.apply(id)),
                    Some(AccessType.Private),
                    EventType.withName(eventType))
            } { event =>
                Some((
                    event.calendar.stringify,
                    event.timeRange,
                    event.name,
                    event.description,
                    Some(event.rules),
                    event.recurrenceMeta,
                    event.nextRecurrence.map(id => id.stringify),
                    event.eventType.toString()))
            })
}