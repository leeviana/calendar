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
        def reads(json: JsValue) = JsSuccess(new Duration(Integer.getInteger(json.toString())))
        def writes(duration: Duration) = JsNumber(duration.getMillis)
    }
}