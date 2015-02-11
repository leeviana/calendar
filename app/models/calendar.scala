package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._

/**
 * @author Leevi
 */
case class Calendar (
    _id: BSONObjectID,
    owner: BSONObjectID, // foreign ref calendar
    name: String,
    rules: List[Rule], // list of Rules
    settings: List[UserSetting] // list of [User]Settings, do we need a seperate calendarsetting object? probably not
) {
    def this(owner: BSONObjectID, name: String) {
        this(BSONObjectID.generate, owner, name, List[Rule](), List[UserSetting]())
    }
}

object Calendar {
    implicit val CalendarHandler = Macros.handler[Calendar]
      
    val form = Form(
        mapping(
           // "id" -> ignored(BSONObjectID.generate),
            "owner" -> nonEmptyText,
            "name" -> nonEmptyText,
            "rules" -> optional(list(Rule.form.mapping)),
            "settings" -> optional(list(UserSetting.form.mapping))
        )  { (owner, name, rules, settings) =>
            Calendar (
              BSONObjectID.generate,
              BSONObjectID.apply(owner),
              name,
              rules.getOrElse(List[Rule]()),
              settings.getOrElse(List[UserSetting]())
            )
        } { calendar =>
            Some(
              (
              //calendar.id,
              calendar.owner.stringify,
              calendar.name,
              Some(calendar.rules),
              Some(calendar.settings)))
          }
    )
}