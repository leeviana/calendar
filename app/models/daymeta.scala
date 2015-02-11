package models

import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.Form
import scala.collection.mutable.ListBuffer
import org.joda.time.DateTime
import models.enums.RecurrenceType

/**
 * @author Leevi
 */
case class DayMeta(
    numberOfDays: Int) {
    var recurrenceType = RecurrenceType.Daily
}

object DayMeta {

    implicit val DayMetaHandler = Macros.handler[DayMeta]

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