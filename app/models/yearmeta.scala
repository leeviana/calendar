package models

import scala.collection.mutable.ListBuffer

import org.joda.time.DateTime

import models.enums.RecurrenceType
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.libs.json.Json

/**
 * @author Leevi
 */
case class YearMeta(
    month: Int, // integer representation of month
    day: Int // integer representation of day
    ) {
    var recurrenceType = RecurrenceType.Yearly
}

object YearMeta {
    implicit val YearMetaFormat = Json.format[YearMeta]

    val form = Form(
        mapping(
            "month" -> number,
            "day" -> number)(YearMeta.apply)(YearMeta.unapply))

    def generateRecurrence(start: DateTime, end: DateTime): List[Long] = {
        var current = start.plusYears(1)
        var timestamps = ListBuffer[Long]()

        while (current.compareTo(end) <= 0) {
            timestamps += current.getMillis - start.getMillis
            current = current.plusYears(1)
        }

        timestamps.toList
    }
}