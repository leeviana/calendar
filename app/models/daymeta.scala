package models

import scala.collection.mutable.ListBuffer
import org.joda.time.DateTime
import models.enums.RecurrenceType
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.libs.json.Json
import org.joda.time.Period
import org.joda.time.Duration

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
}