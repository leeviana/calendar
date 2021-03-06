package models

import models.enums.RecurrenceType
import models.enums.ReminderType
import play.api.data.Form
import play.api.data.Forms.longNumber
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.optional
import play.api.libs.json.Json
import org.joda.time.Duration
import models.JsonDuration.DurationFormat
import models.JsonPeriod.PeriodFormat
import org.joda.time.DateTimeConstants
import org.joda.time.Period

/**
 * Metadata for recurrence information
 *
 * @author Leevi
 */
case class RecurrenceMeta(
    timeRange: TimeRange, // range of time recurrence occurs for
    recurrenceType: RecurrenceType.RecurrenceType,
    recurDuration: Period)

object RecurrenceMeta {
    implicit val EventFormat = Json.format[RecurrenceMeta]

    val form = Form(

        mapping(
            "timeRange" -> TimeRange.form.mapping,
            "recurrenceType" -> nonEmptyText,
            "daily" -> optional(DayMeta.form.mapping),
            "weekly" -> optional(WeekMeta.form.mapping),
            "monthly" -> optional(MonthMeta.form.mapping),
            "yearly" -> optional(YearMeta.form.mapping)) { (timeRange, recurrenceType, daily, weekly, monthly, yearly) =>
                val recType = RecurrenceType.withName(recurrenceType)
                RecurrenceMeta(
                    timeRange,
                    recType,
                    if (recType.compare(RecurrenceType.Daily) == 0) {
                        new Period(0, 0, 0, daily.get.numberOfDays, 0, 0, 0, 0)
                    } else if (recType.compare(RecurrenceType.Weekly) == 0) {
                        new Period(0, 0, weekly.get.numberOfWeeks.get, 0, 0, 0, 0, 0)
                    } else if (recType.compare(RecurrenceType.Monthly) == 0) {
                        new Period(0, monthly.get.numberOfMonths.get, 0, 0, 0, 0, 0, 0)
                    } else {
                        new Period(yearly.get.numberOfYears.get, 0, 0, 0, 0, 0, 0, 0)
                    })
            } { recurrencemeta =>
                Some(
                    (recurrencemeta.timeRange,
                        recurrencemeta.recurrenceType.toString(),
                        Some(new DayMeta(recurrencemeta.recurDuration.getDays)),
                        Some(new WeekMeta(None, Some(recurrencemeta.recurDuration.getWeeks))),
                        Some(new MonthMeta(None, Some(recurrencemeta.recurDuration.getMonths))),
                        Some(new YearMeta(None, None, Some(recurrencemeta.recurDuration.getYears)))))
            })
}