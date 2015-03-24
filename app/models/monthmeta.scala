package models

import scala.collection.mutable.ListBuffer
import org.joda.time.DateTime
import models.enums.RecurrenceType
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.libs.json.Json
import org.joda.time.Duration
import org.joda.time.Interval
import org.joda.time.Period

/**
 * @author Leevi
 */
case class MonthMeta(
    monthDay: Option[Int] = None, // TODO: remove. Just use required number of months
    numberOfMonths: Option[Int] = Some(1)) {
    var recurrenceType = RecurrenceType.Monthly
}

object MonthMeta {
    implicit val MonthMetaFormat = Json.format[MonthMeta]

    val form = Form(
        mapping(
            "monthDay" -> optional(number),
            "numberOfMonths" -> optional(number))(MonthMeta.apply)(MonthMeta.unapply))
}