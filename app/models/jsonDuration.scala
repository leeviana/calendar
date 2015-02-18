package models

import play.api.libs.json.JsSuccess
import play.api.libs.json.JsNumber
import play.api.libs.json.JsValue
import play.api.libs.json.Format
import org.joda.time.Duration

/**
 * @author Leevi
 */

object JsonDuration {
    implicit val DurationFormat = new Format[Duration] {
        def reads(json: JsValue) = JsSuccess(new Duration(Integer.parseInt(json.toString())))
        def writes(duration: Duration) = JsNumber(duration.getMillis)
    }
}