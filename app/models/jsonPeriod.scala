package models

import org.joda.time.Duration
import play.api.libs.json.Format
import play.api.libs.json.JsNumber
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import scala.math.BigDecimal
import scala.math.BigInt
import org.joda.time.Period

/**
 * @author Leevi
 */

object JsonPeriod {
    implicit val PeriodFormat = new Format[Period] {
        def reads(json: JsValue) = JsSuccess(new Period((json).as[Long]))
        def writes(period: Period) = JsNumber(period.getMillis)
    }
}