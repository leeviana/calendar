package models

import org.joda.time.DateTime

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

/**
 * Metadata for Sign Up Events
 *
 * @author Leevi
 */
case class SignUpMeta(
    signUpSlots: List[SignUpSlot] = List[SignUpSlot](), // list of slots people can sign up for
    minSignUpSlotDuration: Int = 30, // minimum number of minutes slots can be
    maxSlots: Int = 1, // max number of slots someone can sign up for
    prefDeterminationTime: Option[DateTime] = None, // if this exists, sign up slots/events are not determined yet
    createPUD: Option[Boolean] = None, // should PUDs be created for those that are eligible for a preference signup (via rules)?
    signUpPUDPriority: Option[Int] = None) // if PUD should be created, what should its priority be?

object SignUpMeta {
    implicit val EventFormat = Json.format[SignUpMeta]

    val form = Form(
        mapping(
            "minSignUpSlotDuration" -> number,
            "maxSlots" -> number,
            "prefDeterminationTime" -> optional(jodaDate),
            "createPUD" -> optional(boolean),
            "signUpPUDPriority" -> optional(number)) { (minSignUpSlotDuration, maxSlots, prefDeterminationTime, createPUD, signUpPUDPriority) =>
                SignUpMeta(
                    List[SignUpSlot](),
                    minSignUpSlotDuration,
                    maxSlots,
                    prefDeterminationTime,
                    createPUD,
                    signUpPUDPriority)
            } { signUpMeta =>
                Some(
                    (signUpMeta.minSignUpSlotDuration,
                        signUpMeta.maxSlots,
                        signUpMeta.prefDeterminationTime,
                        signUpMeta.createPUD,
                        signUpMeta.signUpPUDPriority))
            })
}