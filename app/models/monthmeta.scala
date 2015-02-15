package models

import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.Form
import org.joda.time.DateTime
import scala.collection.mutable.ListBuffer
import models.enums.RecurrenceType

/**
 * @author Leevi
 */
case class MonthMeta(
    monthDay: Int) {
    var recurrenceType = RecurrenceType.Monthly
}

object MonthMeta {

    implicit val MonthMetaHandler = Macros.handler[MonthMeta]

    val form = Form(
        mapping(
            "monthDay" -> number)(MonthMeta.apply)(MonthMeta.unapply))

    def generateRecurrence(start: DateTime, end: DateTime): List[Long] = {
        var current = start.plusMonths(1)
        var timestamps = ListBuffer[Long]()

        while (current.compareTo(end) <= 0) {
            timestamps += current.getMillis - start.getMillis
            current = current.plusMonths(1)
        }

        timestamps.toList
    }
}