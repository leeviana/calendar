package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._
import reactivemongo.api.collections.default.BSONCollection

/**
 * @author Leevi
 */
case class User(
    _id: BSONObjectID,
    username: String,
    email: String,
    subscriptions: List[BSONObjectID], // list of calIDs
    settings: List[UserSetting] // list of UserSettings
    )

object User {
    implicit val UserHandler = Macros.handler[User]

    val form = Form(
        mapping(
            "username" -> nonEmptyText,
            "email" -> nonEmptyText,
            "subscriptions" -> list(ignored(BSONObjectID.generate)),
            "settings" -> optional(list(UserSetting.form.mapping))) { (username, email, subscriptions, settings) =>
                User(
                    BSONObjectID.generate,
                    username,
                    email,
                    subscriptions,
                    settings.getOrElse(List[UserSetting]()))
            } { user =>
                Some((
                    user.username,
                    user.email,
                    user.subscriptions,
                    Some(user.settings)))
            })
}