package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._
import reactivemongo.api.collections.default.BSONCollection

/**
 * @author Leevi
 */
case class AuthInfo (
    id: BSONObjectID,
    userID: BSONObjectID, // foreign pointer to user in question
    lastAuthToken: String,
	passwordHash: String
)

object AuthInfo {
    implicit object AuthInfoReader extends BSONDocumentReader[AuthInfo] {
        def read(doc: BSONDocument): AuthInfo = {
            AuthInfo(
                doc.getAs[BSONObjectID]("_id").get,
                doc.getAs[BSONObjectID]("userID").get,
                doc.getAs[String]("lastAuthToken").get,
				doc.getAs[String]("passwordHash").get
            )
        }
    }
    
    implicit object AuthInfoWriter extends BSONDocumentWriter[AuthInfo] {
        def write(authinfo: AuthInfo): BSONDocument = BSONDocument(
            "_id" -> authinfo.id,
            "userID" -> authinfo.userID,
            "lastAuthToken" -> authinfo.lastAuthToken,
			"passwordHash" -> authinfo.passwordHash
        )
    }
}