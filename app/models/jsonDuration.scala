package models

import org.joda.time.Duration

import play.api.libs.json.Format
import play.api.libs.json.JsNumber
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue

/**
 * @author Leevi
 */

object JsonDuration {
    implicit val DurationFormat = new Format[Duration] {
      // TODO: Make this better
        def reads(json: JsValue) = JsSuccess(new Duration(json.toString().substring(0, json.toString().length-2).toInt))
        def writes(duration: Duration) = JsNumber(duration.getMillis)
    }
}