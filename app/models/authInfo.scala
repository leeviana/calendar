package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._
import reactivemongo.api.collections.default.BSONCollection

/**
 * @author Leevi
 */
case class AuthInfo (
    _id: BSONObjectID,
    userID: BSONObjectID, // foreign pointer to user in question
    lastAuthToken: String,
	passwordHash: String
)

object AuthInfo {
    implicit val AuthInfoHandler = Macros.handler[AuthInfo]
}