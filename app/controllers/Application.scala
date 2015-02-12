package controllers

import scala.concurrent.Future

import play.api._
import play.api.mvc._
import models.Calendar
import models.User
import apputils.CalendarDAO
import apputils.AuthStateDAO
import apputils.UserDAO
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONObjectIDIdentity
import reactivemongo.bson.Producer.nameValue2Producer
import reactivemongo.extensions.dsl.BsonDsl._


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
    
    def addCalendar = Action.async { implicit request =>
        CalendarDAO.findAll("owner" $eq AuthStateDAO.getUserID()).map { calendars =>
          
          Calendar.form.bindFromRequest.fold(
              errors => Ok(views.html.createCalendar(Calendar.form, AuthStateDAO.getUserID().stringify)),
              
              calendar => {
                
                val updatedCalendar = calendar.copy(owner = AuthStateDAO.getUserID())
                CalendarDAO.insert(updatedCalendar)
                UserDAO.updateById(AuthStateDAO.getUserID(), $push("subscriptions", updatedCalendar._id))
                Redirect(routes.Events.index())
              })
          
        }
    }
    
    

}