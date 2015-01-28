package models

import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints._
import play.api.data.format.Formats._

import reactivemongo.bson._

/**
 * @author Leevi
 */
case class User (
    id: BSONObjectID,
    username: String
    //subscriptions: BSONArray, // list of calIDs
    //settings: UserSettings // settings parameters
)

object User {
    implicit object UserReader extends BSONDocumentReader[User] {
        def read(doc: BSONDocument): User = {
            User(
                doc.getAs[BSONObjectID]("_id").get,
                doc.getAs[String]("username").get)
                // doc.getAs[List[BSONObjectID]]("subscriptions").get,
                //doc.getAs[BSONArray]("subscriptions").get,
                //doc.getAs[UserSettings]("settings").get)
        }
    }
    
    implicit object UserWriter extends BSONDocumentWriter[User] {
        def write(user: User): BSONDocument = BSONDocument(
            "_id" -> user.id,
            "username" -> user.username
            //"subscriptions" -> user.subscriptions,
            //"settings" -> user.settings
            )
    }
      
    val form = Form(
        mapping(
            // "id" -> text verifying pattern(
            //    """[a-fA-F0-9]{24}""".r,
            //    "constraint.objectId",
            //    "error.objectId"),
            "id" -> ignored(BSONObjectID.generate),
            "username" -> nonEmptyText
            //"subscriptions" -> ignored(BSONArray.empty),
            //"settings" -> ignored(UserSettings()) 
        ) (User.apply) (User.unapply) 
    )
}