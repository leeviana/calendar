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

        val driver = new MongoDriver
        val connection = driver.connection(List(Constants.DBLocation))
        val db = connection("caldb")
        val collection = db[BSONCollection]("authstate")

        val objectID = BSONObjectID.apply(sessionUserID)

        val cursor = collection.find(BSONDocument("userID" -> objectID)).cursor[AuthInfo]
        var temp = false
        val irrelevant = cursor.collect[List]().map { authinfos =>
            authinfos.map { authinfo =>
                temp = (authinfo.lastAuthToken == sessionAuthToken)
            }
        }
        Await.ready(irrelevant, Duration(5000, MILLISECONDS))
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