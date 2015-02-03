package controllers

import scala.concurrent.Future

import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONArray
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONObjectID
    
import scala.util.Failure
import scala.util.Success
/**
 * The Users controllers encapsulates the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
 * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
 * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
 * @author Leevi
 */
object Events extends Controller with MongoController {
    val collection = db[BSONCollection]("events")

        
    def index = Action.async { implicit request =>         
        val query = BSONDocument(
        "$query" -> BSONDocument())
    
        val found = collection.find(query).cursor[Event]
        
        found.collect[List]().map { events =>
            Ok(views.html.EventDisplay(events))
        }
    }
    
    def showCreationForm = Action {
        Ok(views.html.EventForms(Event.form))
    }
    
    def create = Action { implicit request =>
        Event.form.bindFromRequest.fold(
            errors => Ok(views.html.EventForms(errors)),
            
            event => {
                val updatedEvent = event.copy(calendar = BSONObjectID.generate, rules = BSONArray.empty)
                collection.insert(updatedEvent)
                Redirect(routes.Events.index())
            }
        )
    }
    
    def showEvent(eventID: String) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)

        val cursor = collection.find(BSONDocument("_id" -> objectID)).cursor[Event]
        
        cursor.collect[List]().map { event =>
            Ok(views.html.EventInfo(event.headOption.get, Reminder.form, Rule.form))
        }
    }
    
    def addReminder(eventID: String) = Action.async { implicit request =>
        
        val objectID = BSONObjectID.apply(eventID)
        
        val cursor = collection.find(BSONDocument("_id" -> objectID)).cursor[Event]
        
        cursor.collect[List]().map { event =>
            val reminders = db[BSONCollection]("reminders")

            Reminder.form.bindFromRequest.fold(
                errors => Ok(views.html.EventInfo(event.headOption.get, errors, Rule.form)),
       
                reminder => {
                    val updatedReminder = reminder.copy(reminderType = ReminderType.Email)
                    reminders.insert(updatedReminder)
                    Redirect(routes.Events.showEvent(eventID))
                }
            )
        }
    }

    def addRule(eventID: String) = Action.async { implicit request => 
        val objectID = BSONObjectID.apply(eventID)
        
        val cursor = collection.find(BSONDocument("_id" -> objectID)).cursor[Event]
        
        cursor.collect[List]().map { event =>
                
            val newRule = Rule.form.bindFromRequest.get

            val modifier = BSONDocument(
                "$push" -> BSONDocument(
                  "rules" -> newRule))
              
            val future = collection.update(BSONDocument("_id" -> objectID), modifier)
            
            
            Redirect(routes.Events.showEvent(eventID))
        }
    }
        
    // TODO: refactor out this method from the others
//    def findEvent(id: BSONObjectID): Event = {
//        val cursor = collection.find(BSONDocument("_id" -> id)).cursor[Event]
//        
//        cursor.collect[List]().map { event =>
//            return event.headOption.get
//        }
//    }
    
}