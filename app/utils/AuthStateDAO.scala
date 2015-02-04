package models.utils

import play.api._
import play.api.mvc._
import reactivemongo.api.MongoDriver
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import models.AuthInfo
import scala.concurrent.ExecutionContext.Implicits.global

case class AuthStateDAO ()

object AuthStateDAO {
  def isAuthenticated()(implicit req: RequestHeader): Boolean = {
    val sessionUserID = ""
	val sessionAuthToken = ""
	val output = false
    req.session.get("userID").map { thisID =>
	  val sessionUserID = thisID
    }.getOrElse {
      return output
    }
	
	req.session.get("authToken").map { thisToken =>
	  val sessionAuthToken = thisToken
    }.getOrElse {
      return output
    }
	
    val driver = new MongoDriver
	val connection = driver.connection(List("127.0.0.1:27017"))
	val db = connection("caldb")
	val collection = db[BSONCollection]("authstate")
	
    val objectID = BSONObjectID.apply(sessionUserID)

    val cursor = collection.find(BSONDocument("userID" -> sessionUserID)).cursor[AuthInfo]
	val temp = false
	val irrelevant = cursor.collect[List]().map { authinfos =>      
	  authinfos.map { authinfo => 
	    val temp = (authinfo.lastAuthToken == sessionAuthToken)
	  }
	}
	return temp
  }
  
  def getUserID()(implicit req: RequestHeader): String = {
    val temp = ""
    req.session.get("userID").map { thisID =>
	  val temp = thisID
	}
	temp
  }
}