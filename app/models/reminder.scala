package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._
import org.joda.time.DateTime

/**
 * @author Leevi
 */
case class Reminder (
    eventID: BSONObjectID, // foreign ref
    timestamp: TimeRange,
    user: BSONObjectID, // foreign ref
    reminderType: ReminderType.ReminderType
)

object ReminderType extends Enumeration {
    type ReminderType = Value

    val Email = Value
    
    implicit object ReminderTypeReader extends BSONDocumentReader[ReminderType] {
        def read(doc: BSONDocument): ReminderType = {
           ReminderType.withName(doc.getAs[String]("reminderType").get)
        }
    }
}

object Reminder {
    
    implicit object ReminderReader extends BSONDocumentReader[Reminder] {
        def read(doc: BSONDocument): Reminder = {
            Reminder(
                doc.getAs[BSONObjectID]("eventID").get,
                doc.getAs[TimeRange]("timestamp").get,
                doc.getAs[BSONObjectID]("user").get,
                ReminderType.withName(doc.getAs[String]("reminderType").get)
            )
        }
    }
    
    implicit object ReminderWriter extends BSONDocumentWriter[Reminder] {
        def write(reminder: Reminder): BSONDocument = BSONDocument(
            "eventID" -> reminder.eventID,
            "timestamp" -> reminder.timestamp,
            "user" -> reminder.user,
            "reminderType" -> reminder.reminderType.toString()
        )
    }
      
    val form = Form(
        mapping(
            "eventID" -> nonEmptyText,
            "timestamp" -> TimeRange.form.mapping,
            "user" -> nonEmptyText, // BSONID
            "reminderType" -> nonEmptyText
        )  { (eventID, timestamp, user, reminderType) =>
            Reminder (
              BSONObjectID.apply(eventID),
              timestamp,
              //controllers.Users.findUser(BSONObjectID.apply(user)),
              BSONObjectID.apply(user),
              ReminderType.withName(reminderType)
            )
        } { reminder =>
            Some(
              (reminder.eventID.stringify,
              reminder.timestamp,
              reminder.user.stringify,
              reminder.reminderType.toString()))
          }
    )
}