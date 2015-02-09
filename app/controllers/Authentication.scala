package controllers

import play.api._
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import models._
import models.Event._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import models.utils.AuthStateDAO
import org.mindrot.jbcrypt.BCrypt;
import scala.util.Random

object Authentication extends Controller with MongoController {

    val collection = db[BSONCollection]("authstate")

    def signUp() = Action { implicit request =>
        Redirect(routes.Application.signUp())
    }

    def signIn = Action { implicit request =>

        val requestMap = (request.body.asFormUrlEncoded)
        val email = requestMap.get.get("inputEmail").get.head
        val password = requestMap.get.get("inputPassword").get.head

        val query2 = BSONDocument(
            "$query" -> BSONDocument(
                "email" -> email))

        val collection2 = db[BSONCollection]("users")
        val cursor2 = collection2.find(query2).cursor[User]
        var userID = ""
        var pwHash = ""
        val irrelevant2 = cursor2.collect[List]().map { users =>
            users.map { user =>
                userID = user.id.stringify

                val query = BSONDocument(
                    "$query" -> BSONDocument(
                        "userID" -> BSONObjectID.apply(userID)))

                val cursor = collection.find(query).cursor[AuthInfo]
                val irrelevant = cursor.collect[List]().map { authinfos =>
                    authinfos.map { authinfo =>
                        pwHash = authinfo.passwordHash
                    }
                }
                Await.ready(irrelevant, Duration(5000, MILLISECONDS))
            }
        }
        Await.ready(irrelevant2, Duration(5000, MILLISECONDS))

        if (BCrypt.checkpw(password, pwHash)) {
            val random = new Random().nextString(15)
            val updatedAuthData = AuthInfo(id = BSONObjectID.generate, userID = BSONObjectID.apply(userID), lastAuthToken = random, passwordHash = pwHash)
            collection.insert(updatedAuthData)
            Ok(views.html.index("Your new application is ready.")).withSession(
                request.session + ("authToken" -> random) + ("userID" -> userID))
        } else {
            Redirect(routes.Application.signIn())
        }
    }

    def signOut = Action { implicit request =>
        Ok(views.html.index("Your new application is ready.")).withNewSession
    }
}