package controllers

import play.api._
import play.api.mvc._
import models._
import models.Event._

object Application extends Controller {

    def index = Action {
        Ok(views.html.index())
    }

    def signUp = Action {
        Ok(views.html.editUser(User.form))
    }

    def signIn = Action {
        Ok(views.html.login())
    }
    
    def addCalendar = Action{
      Ok(views.html.addCalendar(Calendar.form))
    }

}