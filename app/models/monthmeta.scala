package models

import scala.collection.mutable.ListBuffer
import org.joda.time.DateTime
import models.enums.RecurrenceType
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.libs.json.Json
import org.joda.time.Duration
import org.joda.time.Interval
import org.joda.time.Period

/**
 * @author Leevi
 */
case class MonthMeta(
    monthDay: Int) {
    var recurrenceType = RecurrenceType.Monthly
}

object MonthMeta {
    implicit val MonthMetaFormat = Json.format[MonthMeta]

    val form = Form(
        mapping(
            "monthDay" -> number)(MonthMeta.apply)(MonthMeta.unapply))

    /**
     * Returns list of Longs, which represent the amount of time between the start and end dates
     */
    def generateRecurrence(start: DateTime, end: DateTime): List[Long] = {
        var current = start.plusMonths(1)
        var timestamps = ListBuffer[Long]()

        while (current.compareTo(end) <= 0) {
            timestamps += current.getMillis - start.getMillis
            current = current.plusMonths(1)
        }

        timestamps.toList
    }
    
    val timeDifference = Period.months(1).getMillis
}