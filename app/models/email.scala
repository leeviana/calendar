package models

import play.api.data.Form
import play.api.data.Forms.boolean
import play.api.data.Forms.mapping
import play.api.data.Forms.optional
import play.api.libs.json.Json
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._


case class Email(
    timeRange: TimeRange,
    graphical: Boolean
    )

object Email {
  val form = Form(
        mapping(
            "timeRange" -> TimeRange.form.mapping,
            "graphical" -> boolean)(Email.apply)(Email.unapply))
}