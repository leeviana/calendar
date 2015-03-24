package models

import models.enums.AccessType
import play.api.data.Form
import play.api.data.Forms.list
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.optional
import play.api.data.Forms.number
import play.api.data.Forms.longNumber
import play.api.data.Forms.boolean
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._
import models.enums.EventType
import models.enums.ViewType
import apputils.UserDAO
import org.joda.time.Duration
import models.JsonDuration.DurationFormat

case class Event(
    _id: BSONObjectID = BSONObjectID.generate,
    calendar: BSONObjectID = BSONObjectID.generate, // pointer/foreign reference to Calendar
    timeRange: List[TimeRange] = List[TimeRange](new TimeRange()),
    name: String = "My Event",
    description: Option[String] = None,
    master: Option[BSONObjectID] = None, // foreign reference to a "master" event, if shared
    rules: List[Rule] = List[Rule](), // list of rule objects
    reminders: Option[List[Reminder]] = Some(List[Reminder]()), // list of reminder objects
    recurrenceMeta: Option[RecurrenceMeta] = None, //TimeRange object, reminder time, one of the following: day, monthly, yearly, weekly
    nextRecurrence: Option[BSONObjectID] = None, // BSONID pointer, can be null if not recurring
    accessType: Option[AccessType.AccessType] = None,
    eventType: EventType.EventType = EventType.Fixed,
    viewType: Option[ViewType.ViewType] = None,
    PUDPriority: Option[Int] = None,
    signUpSlots: Option[List[SignUpSlot]] = None,
    minSignUpSlotDuration: Option[Int] = None, // minutes
    maxSlots: Option[Int] = None ){
    
    def getFirstTimeRange(): TimeRange = {
        return this.timeRange.headOption.getOrElse(new TimeRange())
    }
}
object Event {
    implicit val EventFormat = Json.format[Event]

    val form = Form(
        mapping(
            "calendar" -> nonEmptyText, // BSONObjectID
            "timeRangeList" -> optional(list(TimeRange.form.mapping)),
            "timeRange" -> optional(TimeRange.form.mapping),
            "timeRangeCount" -> number,
            "name" -> nonEmptyText,
            "description" -> optional(nonEmptyText),
            "rules" -> optional(list(Rule.form.mapping)),
            "recurrenceMeta" -> optional(RecurrenceMeta.form.mapping),
            "nextRecurrence" -> optional(nonEmptyText), // BSONObjectID
            "eventType" -> nonEmptyText,
            "PUDPriority" -> optional(number),
            "isPUDEvent" -> boolean,
            "minSignUpSlotDuration" -> optional(number), // in minutes
            "maxSlots" -> optional(number)
            ) { (calendar, timeRangeList, timeRange, timeRangeCount, name, description, rules, recurrenceMeta, nextRecurrence, eventType, PUDPriority, isPUDEvent, minSignUpSlotDuration, maxSlots) =>
                Event(
                    BSONObjectID.generate,
                    BSONObjectID.apply(calendar),
                    if(timeRangeList.isDefined) {timeRangeList.get.slice(0, timeRangeCount)} else if (timeRange.isDefined) {List[TimeRange](timeRange.get)} else {List[TimeRange]()},
                    name,
                    description,
                    None,
                    List[Rule](),
                    Some(List[Reminder]()),
                    recurrenceMeta,
                    nextRecurrence.map(id => BSONObjectID.apply(id)),
                    Some(AccessType.Private),
                    EventType.withName(eventType),
                    if(isPUDEvent) {Some(ViewType.PUDEvent)} else {None},
                    PUDPriority,
                    None,
                    minSignUpSlotDuration,
                    maxSlots)
            } { event =>
                Some((
                    event.calendar.stringify,
                    Some(event.timeRange),
                    Some(event.getFirstTimeRange()),
                    event.timeRange.size,
                    event.name,
                    event.description,
                    Some(event.rules),
                    event.recurrenceMeta,
                    event.nextRecurrence.map(id => id.stringify),
                    event.eventType.toString(),
                    event.PUDPriority,
                    if(event.viewType.isDefined) {event.viewType.get.toString() == ViewType.PUDEvent} else {false},
                    event.minSignUpSlotDuration,
                    event.maxSlots))
            })
}