package controllers

import models._
import models.User._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._

/**
 * @author Leevi
 */
object Users extends Controller with MongoController {
    
    val collection = db[BSONCollection]("users")

    def index = Action.async { implicit request =>         
        val query = BSONDocument(
        "$query" -> BSONDocument())
    
        val found = collection.find(query).cursor[User]
        
        found.collect[List]().map { users =>
            Ok(views.html.users(users))
        }
    }
    
    def showCreationForm = Action {
        Ok(views.html.editUser(User.form))
    }
    
    def create = Action { implicit request =>
        User.form.bindFromRequest.fold(
            errors => Ok(views.html.editUser(errors)),
            
            user => {
                collection.insert(user)
                Redirect(routes.Users.index())
            }
        )
    }
}