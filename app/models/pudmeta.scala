package models

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.libs.json.Json

/**
 * Metadata for PUD Events
 *
 * @author Leevi
 */
case class PUDMeta(
    priority: Int = 1,
    escalationInfo: Option[RecurrenceMeta] = None,
    escalationAmount: Option[Int] = None)

object PUDMeta {
    implicit val EventFormat = Json.format[PUDMeta]

    val form = Form(
        mapping(
            "priority" -> number,
            "escalationInfo" -> optional(RecurrenceMeta.form.mapping),
            "escalationAmount" -> optional(number)) { (priority, escalationInfo, escalationAmount) =>
                PUDMeta(
                    priority,
                    escalationInfo,
                    escalationAmount)
            } { pudMeta =>
                Some(
                    (pudMeta.priority,
                        pudMeta.escalationInfo,
                        pudMeta.escalationAmount))
            })
}