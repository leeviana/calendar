package controllers

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS
import scala.util.Random

import org.mindrot.jbcrypt.BCrypt

import apputils.UserDAO
import apputils.AuthInfoDAO
import models.AuthInfo
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import reactivemongo.extensions.json.dsl.JsonDsl.ElementBuilderLike
import reactivemongo.extensions.json.dsl.JsonDsl.toJsObject

object Authentication extends Controller with MongoController {

    val collection = db[BSONCollection]("authstate")

    /*
     * Renders the sign up page to make a new account
     */
    def signUp() = Action { implicit request =>
        Redirect(routes.Application.signUp())
    }

    /*
     * Allows a user to sign in with the correct credentials
     */
    def signIn = Action { implicit request =>
        val requestMap = (request.body.asFormUrlEncoded)
        val email = requestMap.get.get("inputEmail").get.head
        val password = requestMap.get.get("inputPassword").get.head

        var userID = ""
        var pwHash = ""
        val irrelevant2 = UserDAO.findOne("email" $eq email).map { users =>
            users.map { user =>
//                userID = user._id.stringify
//
//                val query = BSONDocument(
//                    "$query" -> BSONDocument(
//                        "userID" -> BSONObjectID.apply(userID)))

                
                //val cursor = collection.find(query).cursor[AuthInfo]
                val future = AuthInfoDAO.findById(user._id).map { authinfos =>
                    authinfos.map { authinfo =>
                        pwHash = authinfo.passwordHash
                    }
                }
                Await.ready(future, Duration(5000, MILLISECONDS))
            }
        }
        Await.ready(irrelevant2, Duration(5000, MILLISECONDS))
        if (BCrypt.checkpw(password, pwHash)) {
            val random = new Random().nextString(15)
            val updatedAuthData = AuthInfo(_id = BSONObjectID.generate, userID = BSONObjectID.apply(userID), lastAuthToken = random, passwordHash = pwHash)
            AuthInfoDAO.insert(updatedAuthData)
            Ok(views.html.index()).withSession(
                request.session + ("authToken" -> random) + ("userID" -> userID))
        } else {
            Redirect(routes.Application.signIn())
        }
    }

    def signOut = Action { implicit request =>
        Ok(views.html.index()).withNewSession
    }
}