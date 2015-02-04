package controllers

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import scala.concurrent.Future

import org.joda.time.DateTime

import models._
import play.api.data.Form
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
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

    // PLACEHOLDER UNTIL AUTHENTICATION
    val userID = BSONObjectID.apply("54d1d37c1efe0f8e01cdbfb2")
    
    def index = Action.async { implicit request =>         
      
        val calendarID = BSONObjectID.apply("54d1d3801efe0fa201cdbfb4")
        
        val sorted = collection.find(BSONDocument()).sort(BSONDocument("timeRange.startDate" -> 1, "timeRange.startTime" -> 1)).cursor[Event]
        //val sorted = collection.find(query).sort((models.event.scala.timeRange -> 1))
        sorted.collect[List]().map { events =>
           Ok(views.html.events(events))
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
        // get a user
        // send the user's calendars or iterators to front
        
        val userCollection = db[BSONCollection]("users")
        val cursor = userCollection.find(BSONDocument("_id" -> userID)).cursor[User]
        
        cursor.collect[List]().map { user =>
            var calMap:Map[String, String] = Map()
            for(calID <- user.headOption.get.subscriptions) {
                calMap += (calID.stringify -> "name")
            } 
            
            Ok(views.html.editEvent(Event.form, iterator, calMap)) 
        }
        
        Ok(views.html.editEvent(Event.form, iterator, Map()))
    }
    
    def create = Action { implicit request =>
        val iterator = RecurrenceType.values.iterator
        
        Event.form.bindFromRequest.fold(
            errors => Ok(views.html.editEvent(errors, iterator, Map())),
            
            event => {
                
                collection.insert(event)
                
                if(event.recurrenceMeta.isDefined) {
                    val recurrenceDates = new ListBuffer[Long]()
                    // TODO: With the implementation of a RecurrenceMeta hierarchy, refactor this
                    val recType = event.recurrenceMeta.get.recurrenceType
                    if(event.timeRange.startDate.isDefined) {
                        if(event.recurrenceMeta.get.timeRange.endDate.isDefined) {
                            val start = event.timeRange.startDate.get
                            val end = event.recurrenceMeta.get.timeRange.endDate.get
                            
                            if(recType.compare(RecurrenceType.Daily) == 0) {
                                recurrenceDates ++= DayMeta.generateRecurrence(start, end)
                            }
                            if(recType.compare(RecurrenceType.Weekly) == 0) {
                                recurrenceDates ++= WeekMeta.generateRecurrence(start, end)
                            }
                            if(recType.compare(RecurrenceType.Monthly) == 0) {
                                recurrenceDates ++= MonthMeta.generateRecurrence(start, end)
                            }
                            if(recType.compare(RecurrenceType.Yearly) == 0) {
                                recurrenceDates ++= YearMeta.generateRecurrence(start, end)
                            }    
                        }
                        else {
                            // for future expansion, infinite recurrence
                        }
                    }
                    
                    for(difference <- recurrenceDates) {
                        println("Difference:" + difference)
                        val newStartDate = new DateTime(event.timeRange.startDate.get.getMillis + difference)
                        var newTimeRange = new TimeRange
                        
                        if(event.timeRange.endDate.isDefined) {
                            val newEndDate = new DateTime(event.timeRange.endDate.get.getMillis + difference)
                            newTimeRange = event.timeRange.copy(startDate = Some(newStartDate), endDate = Some(newEndDate))   
                        }
                        else {
                            newTimeRange = event.timeRange.copy(startDate = Some(newStartDate))
                        }
                        
                        val updatedEvent = event.copy(id = BSONObjectID.generate, timeRange = newTimeRange) 
                        val future = collection.insert(updatedEvent)
                    } 
                }
                    
                Redirect(routes.Events.index())
            }
        )
    }
    
    def deleteEvent(eventID: String) = Action { implicit request =>
        val objectID = BSONObjectID.apply(eventID)  
        
        val future = collection.remove(BSONDocument("_id" -> objectID), firstMatchOnly = true)
      
        future.onComplete {
          case Failure(e) => throw e
          case Success(lastError) => {
             Redirect(routes.Events.index())
          }
        }
        Redirect(routes.Events.index())
    }
    
    def confirmDelete(eventID: String) = Action{
      Ok(views.html.confirmDelete(eventID, Event.form))
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
    
//    def deleteRule (eventID: String, ruleID: Int) = Action { implicit request =>
//      val objectID = BSONObjectID.apply(eventID)  
//      
//      val event = collection.find(BSONDocument(" id" -> objectID)).cursor[Event]
//      event.collect[List]().map { event =>
//        for(e <- event.headOption.get.rules.values){
//          if(e.orderNum == ruleID)
//        }
//      }

 
//      val future = collection.remove(BSONDocument(" id" -> objectID), firstMatchOnly = true)
      
//      Redirect(routes.Events.showEvent(eventID))
      
      
//    }
        
//    // TODO: refactor out this method from the others
//    def findEvent(id: BSONObjectID): Event = {
//        val cursor = collection.find(BSONDocument("_id" -> id)).cursor[Event]
//        
//        cursor.collect[List]().map { event =>
//            return event.headOption.get
//        }
//    }
    
}