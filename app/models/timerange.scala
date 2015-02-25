package models

import org.joda.time.DateTime
import play.api.data.Form
import play.api.data.Forms.boolean
import play.api.data.Forms.jodaDate
import play.api.data.Forms.mapping
import play.api.data.Forms.optional
import play.api.data.Forms.longNumber
import play.api.libs.json.Json
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Format
import models.enums.EventType
import org.joda.time.Duration
import play.api.libs.json.JsNumber
import models.JsonDuration.DurationFormat
import org.joda.time.Period

/**
 * @author Leevi
 */
case class TimeRange(
    allday: Boolean = false, // TODO: not necessary any more since if there's an allday event end is just not defined
    start: DateTime,
    end: Option[DateTime] = None,
    duration: Duration = Duration.ZERO) {
    def this() {
        this(false, new DateTime(), Some(new DateTime()), Duration.ZERO);
    }
}

object TimeRange {
    implicit val TimeRangeFormat = Json.format[TimeRange]

    val form = Form(
        mapping(
            "allday" -> boolean,
            "startDate" -> optional(jodaDate),
            "startTime" -> optional(jodaDate("h:mm a")),
            "endDate" -> optional(jodaDate),
            "endTime" -> optional(jodaDate("h:mm a")),
            "duration" -> optional(longNumber)) { (allday, startDate, startTime, endDate, endTime, duration) =>
                TimeRange(
                    allday,
                    if (startDate.isDefined) {new DateTime(startDate.get.getMillis+startTime.getOrElse(new DateTime()).getMillis)} else {DateTime.now()},
                    if (endDate.isDefined) {Some(new DateTime(endDate.get.getMillis+endTime.getOrElse(new DateTime()).getMillis))} else {None},
                    if (duration.isDefined) {new Duration(duration.get*Duration.standardMinutes(1).getMillis) } else if (startDate.isDefined) {Duration.standardDays(1)} else {Duration.ZERO}
            )} { timerange =>
                Some(
                    (timerange.allday,
                        Some(timerange.start),
                        Some(timerange.start),
                        timerange.end,
                        timerange.end,
                        Some(timerange.duration.getStandardMinutes)))
            })
}