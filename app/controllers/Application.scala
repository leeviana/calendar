package controllers

import play.api._
import play.api.mvc._
import models._
import models.Event._

object Application extends Controller {

<<<<<<< HEAD
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  
  def signUp = Action {
    Ok(views.html.editUser(User.form))
  }
  
  def signIn = Action {
    Ok(views.html.login())
  }

=======
    def index = Action {
        Ok(views.html.index())
    }

    def signUp = Action {
        Ok(views.html.editUser(User.form))
    }

    def signIn = Action {
        Ok(views.html.login())
    }
>>>>>>> 1c8c0df7199ed2c5cb3a06b106c7e1c959837ce7
}