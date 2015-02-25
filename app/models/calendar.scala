package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._

/**
 * @author Leevi
 */
case class Calendar(
    _id: BSONObjectID,
    owner: BSONObjectID, // foreign ref, owner
    name: String,
    rules: List[Rule], // list of Rules
    settings: List[UserSetting] // list of [User]Settings,
    ) {
    def this(owner: BSONObjectID, name: String) {
        this(BSONObjectID.generate, owner, name, List[Rule](), List[UserSetting]())
    }
}

object Calendar {
    implicit val CalendarFormat = Json.format[Calendar]

    val form = Form(
        mapping(
            "owner" -> nonEmptyText,
            "name" -> nonEmptyText,
            "rules" -> optional(list(Rule.form.mapping)),
            "settings" -> optional(list(UserSetting.form.mapping))) { (owner, name, rules, settings) =>
                Calendar(
                    BSONObjectID.generate,
                    BSONObjectID.apply(owner),
                    name,
                    rules.getOrElse(List[Rule]()),
                    settings.getOrElse(List[UserSetting]()))
            } { calendar =>
                Some((
                    calendar.owner.stringify,
                    calendar.name,
                    Some(calendar.rules),
                    Some(calendar.settings)))
            })
}