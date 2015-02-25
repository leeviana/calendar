package apputils

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS

import models.AuthInfo
import play.api.mvc.RequestHeader
import reactivemongo.api.MongoDriver
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import reactivemongo.extensions.json.dsl.JsonDsl._
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat

case class AuthStateDAO()

object AuthStateDAO {
    def isAuthenticated()(implicit req: RequestHeader): Boolean = {
        var sessionUserID = ""
        var sessionAuthToken = ""
        var output = false
        req.session.get("userID").map { thisID =>
            sessionUserID = thisID
        }.getOrElse {
            return output
        }

        req.session.get("authToken").map { thisToken =>
            sessionAuthToken = thisToken
        }.getOrElse {
            return output
        }

        val objectID = BSONObjectID.apply(sessionUserID)

        
        var temp = false
        
        val future = AuthInfoDAO.findOne("userID" $eq objectID).map { authinfos =>
            authinfos.map { authinfo =>
                temp = (authinfo.lastAuthToken == sessionAuthToken)
            }
        }
        Await.ready(future, Duration(5000, MILLISECONDS))
        return temp
    }

    def getUserID()(implicit req: RequestHeader): BSONObjectID = {
        var temp = ""
        req.session.get("userID").map { thisID =>
            temp = thisID
        }
        BSONObjectID.apply(temp)
    }
}