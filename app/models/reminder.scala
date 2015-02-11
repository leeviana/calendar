package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._
import reactivemongo.api.collections.default.BSONCollection
import org.joda.time.DateTime
import models.enums.ReminderType

/**
 * @author Leevi
 */
case class Reminder(
	_id: BSONObjectID,
    eventID: BSONObjectID, // foreign ref
    timestamp: TimeRange,
    user: BSONObjectID, // foreign ref
    reminderType: ReminderType.ReminderType)

object Reminder {
    implicit val ReminderHandler = Macros.handler[Reminder]

    val form = Form(
        mapping(
            "eventID" -> nonEmptyText,
            "timestamp" -> TimeRange.form.mapping,
            "user" -> nonEmptyText, // BSONID
            "reminderType" -> nonEmptyText) { (eventID, timestamp, user, reminderType) =>
                Reminder(
					BSONObjectID.generate,
                    BSONObjectID.apply(eventID),
                    timestamp,
                    BSONObjectID.apply(user),
                    ReminderType.withName(reminderType))
            } { reminder =>
                Some((
                    reminder.eventID.stringify,
                    reminder.timestamp,
                    reminder.user.stringify,
                    reminder.reminderType.toString()))
            })
}