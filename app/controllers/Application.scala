package controllers

import play.api._
import play.api.mvc._
import models._
import models.Event._
import apputils.AuthStateDAO
import apputils.CalendarDAO
import apputils.UserDAO
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONObjectIDIdentity
import reactivemongo.bson.Producer.nameValue2Producer
//import reactivemongo.extensions.dsl.BsonDsl._
import reactivemongo.extensions.json.dsl.JsonDsl._
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.ws._
import play.api.Play.current

object Application extends Controller with MongoController{


    def index = Action {
        Ok(views.html.index())
    }

    def signUp = Action {
        Ok(views.html.editUser(User.form))
    }

    def signIn = Action {
        Ok(views.html.login())
    }

    def newCalendarForm = Action{  implicit request =>
        Ok(views.html.createCalendar(Calendar.form, AuthStateDAO.getUserID().stringify))
    }

    def requestEmail = Action { implicit request =>
        val requestForm = Map("url" -> Seq("http://nautical-dev.colab.duke.edu/"), "netid" -> Seq("nautical"));
        val url = "http://devilprint.colab.duke.edu/pdf";
        WS.url(url).post(requestForm);
        println("send it!");
        Ok(views.html.index());
    }
    
    def addCalendar = Action.async { implicit request =>
        CalendarDAO.findAll("owner" $eq AuthStateDAO.getUserID()).map { calendars =>
          
          Calendar.form.bindFromRequest.fold(
              errors => Ok(views.html.createCalendar(Calendar.form, AuthStateDAO.getUserID().stringify)),
              
              calendar => {               
                val updatedCalendar = calendar.copy(owner = AuthStateDAO.getUserID())
                CalendarDAO.insert(updatedCalendar)
                UserDAO.updateById(AuthStateDAO.getUserID(), $push("subscriptions", updatedCalendar._id))
                Redirect(routes.Application.newCalendarForm())
              })
          
        }
    }
}