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
case class DayMeta(
    numberOfDays: Int) {
    var recurrenceType = RecurrenceType.Daily
}

object DayMeta {
    implicit val DayMetaFormat = Json.format[DayMeta]

    val form = Form(
        mapping(
            "numberOfDays" -> number)(DayMeta.apply)(DayMeta.unapply))

    def generateRecurrence(start: DateTime, end: DateTime): List[Long] = {
        var current = start.plusDays(1)
        var timestamps = ListBuffer[Long]()

        while (current.compareTo(end) <= 0) {
            timestamps += current.getMillis - start.getMillis
            current = current.plusDays(1)
        }

        timestamps.toList
    }
}