package models

import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.Macros

/**
 * @author Leevi
 */
case class AuthInfo(
    _id: BSONObjectID,
    userID: BSONObjectID, // foreign pointer to user in question
    lastAuthToken: String,
    passwordHash: String)

object AuthInfo {
    implicit val AuthInfoHandler = Macros.handler[AuthInfo]
}