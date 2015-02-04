package controllers

import play.api._
import play.api.mvc._
import models._
import models.Event._


object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  
  def showAgenda = Action {
    Ok(views.html.agenda())
  }
  
  def signUp = Action {
    Ok(views.html.editUser(User.form))
  }
  
  def signIn = Action {
    Ok(views.html.login())
  }

    def addEvent = Action {
      Ok(views.html.addEvent(Event.form))
    }
    
	
    //def settings = Action {
      //Ok(views.html.settings())
    //}
    
    //def newGroup = Action {
      //Ok(views.html.createGroup())
    //}
}