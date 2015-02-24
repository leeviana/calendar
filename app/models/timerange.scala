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
    allday: Boolean = false, // True if all day event
    startDate: Option[DateTime] = None,
    startTime: Option[DateTime] = None,
    endDate: Option[DateTime] = None,
    endTime: Option[DateTime] = None,
    duration: Option[Duration] = None) {
    def this() {
        this(false, Some(new DateTime()), Some(new DateTime()), Some(new DateTime()), Some(new DateTime()), Some(Duration.ZERO));
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
                    startDate,
                    startTime,
                    endDate,
                    endTime,
                    duration.map (duration => new Duration(duration*Duration.standardMinutes(1).getMillis)))
            } { timerange =>
                Some(
                    (timerange.allday,
                        timerange.startDate,
                        timerange.startTime,
                        timerange.endDate,
                        timerange.endTime,
                        timerange.duration.map(duration => duration.getStandardMinutes)))
            })
}