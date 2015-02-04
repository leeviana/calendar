package controllers

import models._
import models.User._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import scala.util.Failure
import scala.util.Success

/**
 * @author Leevi
 */
object Users extends Controller with MongoController {
    
    val collection = db[BSONCollection]("users")
    
    // PLACEHOLDER UNTIL AUTHENTICATION
    val userID = BSONObjectID.apply("54d1ed9c1efe0fe905808d8c")
    
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
                val calendarColl = db[BSONCollection]("calendars")
                val calName = user.username + "'s personal calendar"
                val personalCalendar = new Calendar(BSONObjectID.generate, user.id, calName, BSONArray.empty, BSONArray.empty)
            
                calendarColl.insert(personalCalendar)
                
                val updatedUser = user.copy(subscriptions = List[BSONObjectID](personalCalendar.id))
                collection.insert(updatedUser)
                Redirect(routes.Application.index())
            }
        )
    }
    
    // TODO: ...yeah. Users and Groups should probably have their own controllers, despite their similarities
    def showGroups = Action.async { implicit request =>
        
        val query = BSONDocument(
            "$query" -> BSONDocument(
                "owner" -> userID))
    
        val groupCollection = db[BSONCollection]("groups")
    
        val found = groupCollection.find(query).cursor[Group]
        
        found.collect[List]().map { groups =>
            Ok(views.html.groups(groups, Group.form))
        }  
    }
    
    def newGroupForm = Action {
        Ok(views.html.createGroup(Group.form))
    }
    
    def addGroup = Action.async { implicit request =>
        val query = BSONDocument(
            "$query" -> BSONDocument(
                "owner" -> userID))
    
        val groupCollection = db[BSONCollection]("groups")
    
        val found = groupCollection.find(query).cursor[Group]
        
        found.collect[List]().map { groups =>
            Group.form.bindFromRequest.fold(
                errors => Ok(views.html.groups(groups, errors)),
                
                user => {
                    val updatedUser = user.copy(owner = userID)
                    groupCollection.insert(updatedUser)
                    Redirect(routes.Users.showGroups())
                }
            )
        }
    }
    
    def addUsertoGroup = Action { implicit request =>
        val requestMap = (request.body.asFormUrlEncoded)
        val groupID = BSONObjectID.apply(requestMap.get.get("groupID").get.head)        
        val addUserID = BSONObjectID.apply(requestMap.get.get("userID").get.head)
        
        val query = BSONDocument(
            "$query" -> BSONDocument(
                //"owner" -> userID,
                "_id" -> groupID))
        
        val groupCollection = db[BSONCollection]("groups")
    
        val modifier = BSONDocument(
            "$push" -> BSONDocument(
                "userIDs" -> addUserID))
          
        val future = groupCollection.update(BSONDocument("_id" -> groupID), modifier)
                    
        Redirect(routes.Users.showGroups())

       
    }
    
//    def findUser(id: BSONObjectID): User = {
//
//        val query = BSONDocument(
//            "$query" -> BSONDocument("_id" -> id)
//        )
//    
//        val found = collection.find(query).one[User]
//        
//    }
}