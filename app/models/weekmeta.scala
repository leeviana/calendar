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
    dayNumber: Option[Int] = None, // integer representing days of the week. 0 is Sunday. Alternative: use another object?
    numberOfWeeks: Option[Int] = Some(1)
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
}