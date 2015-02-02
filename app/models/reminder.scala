package models

import org.joda.time.DateTime

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._

/**
 * @author Leevi
 */
case class Reminder (
    id: BSONObjectID,
    timestamp: DateTime,
    user: User, // foreign ref
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
                doc.getAs[BSONObjectID]("_id").get,
                doc.getAs[BSONDateTime]("timestamp").map(dt => new DateTime(dt.value)).get, // get BSONDateTime, convert to joda
                doc.getAs[User]("user").get,
                doc.getAs[ReminderType.ReminderType]("reminderType").get
            )
        }
    }
    
    implicit object ReminderWriter extends BSONDocumentWriter[Reminder] {
        def write(reminder: Reminder): BSONDocument = BSONDocument(
            "id" -> reminder.id,
            "timestamp" -> BSONDateTime(reminder.timestamp.getMillis),
            "user" -> reminder.user,
            "reminderType" -> reminder.reminderType.toString()
        )
    }
      
    val form = Form(
        mapping(
            "id" -> nonEmptyText,
            "timestamp" -> longNumber,
            "user" -> nonEmptyText, // BSONID
            "reminderType" -> nonEmptyText
        )  { (id, timestamp, user, reminderType) =>
            Reminder (
              BSONObjectID.apply(id),
              new DateTime(timestamp),
              controllers.Users.findUser(BSONObjectID.apply(user)),
              ReminderType.withName(reminderType)
            )
        } { reminder =>
            Some(
              (reminder.id.stringify,
              reminder.timestamp.getMillis,
              reminder.user.id.stringify,
              reminder.reminderType.toString()))
          }
    )
}