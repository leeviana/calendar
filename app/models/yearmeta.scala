package models

import scala.collection.mutable.ListBuffer
import org.joda.time.DateTime
import models.enums.RecurrenceType
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.libs.json.Json
import org.joda.time.Period

/**
 * @author Leevi
 */
case class YearMeta(
    month: Option[Int], // integer representation of month
    day: Option[Int], // integer representation of day
    numberOfYears: Option[Int]) {
    var recurrenceType = RecurrenceType.Yearly
}

object YearMeta {
    implicit val YearMetaFormat = Json.format[YearMeta]

    val form = Form(
        mapping(
            "month" -> optional(number),
            "day" -> optional(number),
            "numberOfYears" -> optional(number))(YearMeta.apply)(YearMeta.unapply))

    def generateRecurrence(start: DateTime, end: DateTime): List[Long] = {
        var current = start.plusYears(1)
        var timestamps = ListBuffer[Long]()

        while (current.compareTo(end) <= 0) {
            timestamps += current.getMillis - start.getMillis
            current = current.plusYears(1)
        }

        timestamps.toList
    }
    
    def generateNext(start: DateTime): DateTime = {
        start.plusYears(1)
    }
}