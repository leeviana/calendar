package models

import play.api.data.Form
import play.api.data.Forms.list
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.libs.json.Json

/**
 * @author Leevi
 */
case class SignUpPreferences(
    numSlots: Int,
    preferences: List[Int])

object SignUpPreferences {
    implicit val SignUpPreferencesFormat = Json.format[SignUpPreferences]

    val form = Form(
        mapping(
            "numSlots" -> number, // hidden, for frontend to render slots
            "preferences" -> list(number))(SignUpPreferences.apply)(SignUpPreferences.unapply))
            
}