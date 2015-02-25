package models


import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._
import reactivemongo.api.collections.default.BSONCollection
import org.joda.time.DateTime
import models.enums.ReminderType
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.optional
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._

/**
 * @author Leevi
 */
case class Reminder(
	_id: BSONObjectID,
    eventID: BSONObjectID, // foreign ref
    timestamp: TimeRange,
    user: BSONObjectID, // foreign ref
    reminderType: ReminderType.ReminderType,
    recurrenceMeta: Option[RecurrenceMeta],
    hasSent: Boolean)

object Reminder {
    implicit val ReminderFormat = Json.format[Reminder]

    val form = Form(
        mapping(
            "eventID" -> nonEmptyText,
            "timestamp" -> TimeRange.form.mapping,
            "user" -> nonEmptyText, // BSONID
            "reminderType" -> nonEmptyText,
            "recurrenceMeta" -> optional(RecurrenceMeta.form.mapping)) { (eventID, timestamp, user, reminderType, recurrenceMeta) =>
                Reminder(
					BSONObjectID.generate,
                    BSONObjectID.apply(eventID),
                    timestamp,
                    BSONObjectID.apply(user),
                    ReminderType.withName(reminderType),
                    recurrenceMeta,
                    false)
            } { reminder =>
                Some((
                    reminder.eventID.stringify,
                    reminder.timestamp,
                    reminder.user.stringify,
                    reminder.reminderType.toString(),
                    reminder.recurrenceMeta))
            })
}