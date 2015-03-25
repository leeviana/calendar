package models

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._

/**
 * @author Leevi
 */
case class AuthInfo(
    _id: BSONObjectID,
    userID: BSONObjectID, // foreign pointer to user in question
    lastAuthToken: String,
    passwordHash: String)

object AuthInfo {
    implicit val AuthInfoFormat = Json.format[AuthInfo]
}