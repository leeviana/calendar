package models

import models.enums.RecurrenceType
import models.enums.ReminderType
import play.api.data.Form
import play.api.data.Forms.longNumber
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.optional
import play.api.libs.json.Json

/**
 * Metadata for event recurrence information
 *
 * @author Leevi
 */
case class RecurrenceMeta(
    timeRange: TimeRange,
    reminderTime: Option[Long], // how many milliseconds before event to create reminder
    reminderType: Option[ReminderType.ReminderType],
    recurrenceType: RecurrenceType.RecurrenceType,
    daily: Option[DayMeta],
    weekly: Option[WeekMeta],
    monthly: Option[MonthMeta],
    yearly: Option[YearMeta] // TODO: remove 'recurrenceType', which is inside of meta objects and just have all meta objects extend from the same class
    // learn more about Scala's hierarchy system and possibly use it to clean up this class
    // move all the recurrence meta classes into their own package
    )

object RecurrenceMeta {
    implicit val EventFormat = Json.format[RecurrenceMeta]

    val form = Form(

        mapping(
            "timeRange" -> TimeRange.form.mapping,
            "reminderTime" -> optional(longNumber),
            "reminderType" -> optional(nonEmptyText),
            "recurrenceType" -> nonEmptyText,
            "daily" -> optional(DayMeta.form.mapping),
            "weekly" -> optional(WeekMeta.form.mapping),
            "monthly" -> optional(MonthMeta.form.mapping),
            "yearly" -> optional(YearMeta.form.mapping)) { (timeRange, reminderTime, reminderType, recurrenceType, daily, weekly, monthly, yearly) =>
                RecurrenceMeta(
                    timeRange,
                    reminderTime,
                    reminderType.map(rt => ReminderType.withName(rt)),
                    RecurrenceType.withName(recurrenceType),
                    daily,
                    weekly,
                    monthly,
                    yearly)
            } { recurrencemeta =>
                Some(
                    (recurrencemeta.timeRange,
                        recurrencemeta.reminderTime,
                        recurrencemeta.reminderType.map(rt => rt.toString()),
                        recurrencemeta.recurrenceType.toString(),
                        recurrencemeta.daily,
                        recurrencemeta.weekly,
                        recurrencemeta.monthly,
                        recurrencemeta.yearly))
            })
}