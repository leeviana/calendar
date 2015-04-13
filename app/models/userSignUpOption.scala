package models

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._

/**
 * @author Leevi
 */
case class UserSignUpOption(
	userID: BSONObjectID, // foreign ref
    preference: Int)
    
object UserSignUpOption {
    implicit val UserSignUpOptionsFormat = Json.format[UserSignUpOption]
}