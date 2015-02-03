package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._

import org.joda.time.DateTime

/**
 * Metadata for event recurrence information
 * 
 * @author Leevi
 */
case class RecurrenceMeta (
    timeRange: TimeRange,
    reminderTime: Option[Long], // how many milliseconds before event to create reminder
    reminderType: Option[ReminderType.ReminderType],
    recurrenceType: RecurrenceType.RecurrenceType, 
    daily: Option[DayMeta],
    weekly: Option[WeekMeta],
    monthly: Option[MonthMeta],
    yearly: Option[YearMeta]
    // TODO: remove 'recurrenceType', which is inside of meta objects and just have all meta objects extend from the same class
    // learn more about Scala's hierarchy system and possibly use it to clean up this class
    // move all the recurrence meta classes into their own package
)

object RecurrenceType extends Enumeration {
    type RecurrenceType = Value

    val Daily, Weekly, Monthly, Yearly = Value
    
    implicit object RecurrenceTypeReader extends BSONDocumentReader[RecurrenceType] {
        def read(doc: BSONDocument): RecurrenceType = {
           RecurrenceType.withName(doc.getAs[String]("recurrenceType").get)
        }
    }
}

object RecurrenceMeta {
    
    implicit object RecurrenceMetaReader extends BSONDocumentReader[RecurrenceMeta] {
        def read(doc: BSONDocument): RecurrenceMeta = {
            RecurrenceMeta(
                doc.getAs[TimeRange]("timeRange").get,
                doc.getAs[Long]("reminderTime"),
                doc.getAs[ReminderType.ReminderType]("reminderType"),
                doc.getAs[RecurrenceType.RecurrenceType]("recurrenceType").get,
                doc.getAs[DayMeta]("daily"),
                doc.getAs[WeekMeta]("weekly"),
                doc.getAs[MonthMeta]("monthly"),
                doc.getAs[YearMeta]("yearly")
            )
        }
    }
    
    implicit object RecurrenceMetaWriter extends BSONDocumentWriter[RecurrenceMeta] {
        def write(recurrencemeta: RecurrenceMeta): BSONDocument = {
            val bson = BSONDocument(
                "timeRange" -> recurrencemeta.timeRange,
                "reminderTime" -> recurrencemeta.reminderTime,
                "reminderType" -> recurrencemeta.reminderType.toString(),
                "recurrenceType" -> recurrencemeta.recurrenceType.toString(),
                "daily" -> recurrencemeta.daily,
                "weekly" -> recurrencemeta.weekly,
                "monthly" -> recurrencemeta.monthly,
                "yearly" -> recurrencemeta.yearly  
            )
            
            bson
        }
    }
      
    val form = Form(
            
        mapping(
            "timeRange" -> TimeRange.form.mapping,
            "reminderTime" -> optional(longNumber),
            "reminderType" -> optional(nonEmptyText),
            "recurrenceType" -> nonEmptyText,
            "daily" -> optional(DayMeta.form.mapping),
            "weekly" -> optional(WeekMeta.form.mapping),
            "monthly" -> optional(MonthMeta.form.mapping),
            "yearly" -> optional(YearMeta.form.mapping)
        )  { (timeRange, reminderTime, reminderType, recurrenceType, daily, weekly, monthly, yearly) =>
             RecurrenceMeta (
                timeRange,
                reminderTime,
                reminderType.map (rt => ReminderType.withName(rt)),
                RecurrenceType.withName(recurrenceType),
                daily,
                weekly,
                monthly,
                yearly
            )
        } { recurrencemeta =>
            Some(
                (recurrencemeta.timeRange,
                recurrencemeta.reminderTime,
                recurrencemeta.reminderType.map(rt => rt.toString()),
                recurrencemeta.recurrenceType.toString(),
                recurrencemeta.daily,
                recurrencemeta.weekly,
                recurrencemeta.monthly,
                recurrencemeta.yearly)
            )
          }
    )
}