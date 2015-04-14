package models

import org.joda.time.DateTime
import play.api.data.Form
import play.api.data.Forms.jodaDate
import play.api.data.Forms.mapping
import play.api.data.Forms.optional
import play.api.data.Forms.longNumber
import play.api.libs.json.Json
import org.joda.time.Duration
import models.JsonDuration.DurationFormat

/**
 * @author Leevi
 */
case class TimeRange(
    start: DateTime = DateTime.now(),
    end: Option[DateTime] = None,
    duration: Duration = Duration.ZERO) {

    def this(start: DateTime, end: DateTime) {
        this(start, Some(end), new Duration(end.getMillis - start.getMillis));
    }
}

object TimeRange {
    implicit val TimeRangeFormat = Json.format[TimeRange]

    def validateTimes(allday: Boolean, start: DateTime, end: Option[DateTime], duration: Duration) = {
        end match {
            case _ if !(end.isEmpty) => (end.get.getMillis >= start.getMillis);
            case _                   => true;
        }
    }

    // TODO: temp workaround for time zone
    val form = Form(
        mapping(
            "startDate" -> optional(jodaDate),
            "startTime" -> optional(jodaDate("h:mm a")),
            "endDate" -> optional(jodaDate),
            "endTime" -> optional(jodaDate("h:mm a")),
            "durationMin" -> optional(longNumber),
            "durationHour" -> optional(longNumber),
            "durationDay" -> optional(longNumber)) { (startDate, startTime, endDate, endTime, durationMin, durationHour, durationDay) =>
                val start = if (startDate.isDefined) { new DateTime(startDate.get.getMillis + startTime.getOrElse(new DateTime(0)).getMillis).minusHours(5) } else { DateTime.now() }
                val end = if (endDate.isDefined) { Some(new DateTime(endDate.get.getMillis + endTime.getOrElse(new DateTime(0)).getMillis).minusHours(5)) } else { None }
                TimeRange(
                    start,
                    end,
                    if (durationMin.isDefined | durationHour.isDefined | durationDay.isDefined) {
                        new Duration(
                            Duration.standardMinutes(durationMin.getOrElse(0)).getMillis +
                                Duration.standardHours(durationHour.getOrElse(0)).getMillis +
                                Duration.standardDays(durationDay.getOrElse(0)).getMillis)
                    } else if (end.isDefined) {
                        new Duration(end.get.getMillis - start.getMillis)
                    } else if (startDate.isDefined) {
                        Duration.standardDays(1)
                    } else { Duration.ZERO })
            } { timerange =>
                Some(
                    (
                        Some(timerange.start),
                        Some(timerange.start),
                        timerange.end,
                        timerange.end,
                        Some(timerange.duration.getStandardMinutes), // does this work?
                        Some(timerange.duration.getStandardHours),
                        Some(timerange.duration.getStandardDays)))
            } //.verifying("Your start/end time/date combination doesn't make sense! (start must be before end, with positive duration)", f => validateTimes(f.allday, f.start, f.end, f.duration))
            )
}