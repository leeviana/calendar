package models

import org.joda.time.DateTime

import play.api.data.Form
import play.api.data.Forms.boolean
import play.api.data.Forms.jodaDate
import play.api.data.Forms.mapping
import play.api.data.Forms.optional
import play.api.libs.json.Json

/**
 * @author Leevi
 */
case class TimeRange(
    allday: Boolean, // True if all day event
    startDate: Option[DateTime],
    startTime: Option[DateTime],
    endDate: Option[DateTime],
    endTime: Option[DateTime]) {
    def this() {
        this(false, Some(new DateTime()), Some(new DateTime()), Some(new DateTime()), Some(new DateTime()));
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
            "endTime" -> optional(jodaDate("h:mm a"))) { (allday, startDate, startTime, endDate, endTime) =>
                TimeRange(
                    allday,
                    startDate,
                    startTime,
                    endDate,
                    endTime)
            } { timerange =>
                Some(
                    (timerange.allday,
                        timerange.startDate,
                        timerange.startTime,
                        timerange.endDate,
                        timerange.endTime))
            })
}