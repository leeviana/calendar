package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  
  def showAgenda = Action {
    Ok(views.html.agenda())
  }

    def login = Action {
      Ok(views.html.login())
    }

    def addEvent = Action {
      Ok(views.html.addEvent())
    }
}