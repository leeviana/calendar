package controllers

import play.api._
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import models._
import models.Event._
import models.utils.AuthStateDAO


object Authentication extends Controller with MongoController {

  val collection = db[BSONCollection]("authstate")

  def signUp = Action { implicit request =>
    val authStatus = AuthStateDAO.isAuthenticated()
    Unauthorized(authStatus.toString)
  }

  def signIn = Action { implicit request =>
    Ok(views.html.index("Your new application is ready.")).withSession(
	  request.session + ("test" -> "cookies work!")
	)
  }

  def signOut = Action { implicit request =>
    Ok(views.html.index("Your new application is ready.")).withNewSession
  }
}