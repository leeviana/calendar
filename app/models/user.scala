package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._
import reactivemongo.api.collections.default.BSONCollection

/**
 * @author Leevi
 */
case class User(
    id: BSONObjectID,
    username: String,
    email: String,
    subscriptions: List[BSONObjectID], // list of calIDs
    settings: BSONArray // list of UserSettings
    )

object User {
    implicit object UserReader extends BSONDocumentReader[User] {
        def read(doc: BSONDocument): User = {
            User(
                doc.getAs[BSONObjectID]("_id").get,
                doc.getAs[String]("username").get,
                doc.getAs[String]("email").get,
                doc.getAs[List[BSONObjectID]]("subscriptions").get,
                doc.getAs[BSONArray]("settings").get)
        }
    }

    implicit object UserWriter extends BSONDocumentWriter[User] {
        def write(user: User): BSONDocument = BSONDocument(
            "_id" -> user.id,
            "username" -> user.username,
            "email" -> user.email,
            "subscriptions" -> user.subscriptions,
            "settings" -> user.settings)
    }

    val form = Form(
        mapping(
            // "id" -> text verifying pattern(
            //    """[a-fA-F0-9]{24}""".r,
            //    "constraint.objectId",
            //    "error.objectId"),
            "id" -> ignored(BSONObjectID.generate),
            "username" -> nonEmptyText,
            "email" -> nonEmptyText,
            "subscriptions" -> list(ignored(BSONObjectID.generate)),
            "settings" -> ignored(BSONArray())) { (id, username, email, subscriptions, settings) =>
                User(
                    id,
                    username,
                    email,
                    subscriptions,
                    settings)
            } { user =>
                Some(
                    (user.id,
                        user.username,
                        user.email,
                        user.subscriptions,
                        user.settings))
            })
}