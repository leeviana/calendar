package models

import scala.collection.mutable.ListBuffer
import org.joda.time.DateTime
import models.enums.RecurrenceType
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.libs.json.Json
import reactivemongo.bson.Macros
import org.joda.time.Period

/**
 * @author Leevi
 */
case class WeekMeta(
    dayNumber: Option[Int], // integer representing days of the week. 0 is Sunday. Alternative: use another object?
    numberOfWeeks: Option[Int]
    ) {
    var recurrenceType = RecurrenceType.Weekly
}

object WeekMeta {

    implicit val WeekMetaHandler = Macros.handler[WeekMeta]
    implicit val WeekMetaFormat = Json.format[WeekMeta]

    val form = Form(
        mapping(
            "dayNumber" -> optional(number),
            "numberOfWeeks" -> optional(number))(WeekMeta.apply)(WeekMeta.unapply))

    def generateRecurrence(start: DateTime, end: DateTime): List[Long] = {
        var current = start.plusWeeks(1)
        var timestamps = ListBuffer[Long]()

        while (current.compareTo(end) <= 0) {
            timestamps += current.getMillis - start.getMillis
            current = current.plusWeeks(1)
        }

        timestamps.toList
    }
    
    def generateNext(start: DateTime): DateTime = {
        start.plusWeeks(1)
    }
}