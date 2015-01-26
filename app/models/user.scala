package models

import reactivemongo.bson._

/**
 * @author Leevi
 */
case class User (
    id: BSONObjectID,
    username: String,
    subscriptions: List[BSONObjectID], // list of calIDs
    settings: UserSettings // settings parameters
)

object User {
    implicit object UserReader extends BSONDocumentReader[User] {
        def read(doc: BSONDocument): User = {
            User(
                doc.getAs[BSONObjectID]("_id").get,
                doc.getAs[String]("username").get,
                doc.getAs[List[BSONObjectID]]("subscriptions").get,
                doc.getAs[UserSettings]("settings").get)
        }
    }
    
    implicit object UserWriter extends BSONDocumentWriter[User] {
        def write(user: User): BSONDocument = BSONDocument(
            "id" -> user.id,
            "username" -> user.username,
            "subscriptions" -> user.subscriptions,
            "settings" -> user.settings)
    }
}