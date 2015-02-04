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
import play.api.data.Form
/**
 * The Users controllers encapsulates the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
 * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
 * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
 * @author Leevi
 */
object Events extends Controller with MongoController {
    val collection = db[BSONCollection]("events")

    // PLACEHOLDER UNTIL AUTHENTICATION
    val userID = BSONObjectID.apply("54cee76d1efe0fc108e5e698")
    
    def index = Action.async { implicit request =>         
        val query = BSONDocument(
        "$query" -> BSONDocument())
    
        val found = collection.find(query).cursor[Event]

        found.collect[List]().map { events =>
            Ok(views.html.EventDisplay(events))
        } 
    }
    
    def showReminders = Action.async{ implicit request =>         
        val reminders = db[BSONCollection]("reminders")
        val reminderCursor = reminders.find(BSONDocument("user" -> userID)).cursor[Reminder]
        
        reminderCursor.collect[List]().map { reminders =>         
                Ok(views.html.ReminderDisplay(reminders))
        }
    }
    
    def showCreationForm = Action {
        val iterator = RecurrenceType.values.iterator
        Ok(views.html.editEvent(Event.form, iterator))
    }
    
    def create = Action { implicit request =>
        val iterator = RecurrenceType.values.iterator
        
        Event.form.bindFromRequest.fold(
            errors => Ok(views.html.editEvent(errors, iterator)),
            
            event => {
                
                if(event.recurrenceMeta.isDefined) {
                    // TODO: With the implementation of a RecurrenceMeta hierarchy, refactor this
                    val recType = event.recurrenceMeta.get.recurrenceType
                
                    if(event.recurrenceMeta.get.timeRange.endDate.isDefined) {
                        if(recType.compare(RecurrenceType.Daily) == 0) {
                            println("Let's recur, daily!")
                        }
                        if(recType.compare(RecurrenceType.Weekly) == 0) {
                            println("Let's recur, weekly!")
                        }
                        if(recType.compare(RecurrenceType.Monthly) == 0) {
                            println("Let's recur, monthly!")
                            //MonthMeta.generateRecurrence(event.timeRange.startDate), 
                        }
                        if(recType.compare(RecurrenceType.Yearly) == 0) {
                            println("Let's recur, yearly!")
                        }    
                    }
                    else {
                        // for future expansion, infinite recurrence
                    }
                    
                }
                    
                val updatedEvent = event.copy()
                collection.insert(updatedEvent)
                Redirect(routes.Events.index())
            }
        )
    }
    
    def showEvent(eventID: String, reminderForm: Form[Reminder] = Reminder.form, ruleForm: Form[Rule] = Rule.form) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)

        val cursor = collection.find(BSONDocument("_id" -> objectID)).cursor[Event]
        
        cursor.collect[List]().map { event =>
            Ok(views.html.EventInfo(event.headOption.get, reminderForm, ruleForm))
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
                    reminders.insert(reminder)
                    Redirect(routes.Events.showEvent(eventID))
                }
            )
        }
    }

    def addRule(eventID: String) = Action.async { implicit request => 
        val objectID = BSONObjectID.apply(eventID)
        
        val cursor = collection.find(BSONDocument("_id" -> objectID)).cursor[Event]
        
        cursor.collect[List]().map { event =>
                
            Rule.form.bindFromRequest.fold(
                errors => Ok(views.html.EventInfo(event.headOption.get, Reminder.form, errors)),
            
                rule => {
                    val modifier = BSONDocument(
                        "$push" -> BSONDocument(
                            "rules" -> rule))
                      
                    val future = collection.update(BSONDocument("_id" -> objectID), modifier)
                    
                    
                    Redirect(routes.Events.showEvent(eventID))
                }
            )
        }
    }
        
//    // TODO: refactor out this method from the others
//    def findEvent(id: BSONObjectID): Event = {
//        val cursor = collection.find(BSONDocument("_id" -> id)).cursor[Event]
//        
//        cursor.collect[List]().map { event =>
//            return event.headOption.get
//        }
//    }
    
}