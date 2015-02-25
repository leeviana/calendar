package actor

import akka.actor.Actor
import akka.actor.Props
import akka.actor.PoisonPill
import apputils._
import reactivemongo.bson._
import play.api.libs.json.Json
import models.Reminder
import org.joda.time.DateTime
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class ReminderCheckActor extends Actor {
	def receive = {
		case _ => {
		
			val emailActor = context.actorOf(Props(new SendEmailActor()))
			
			//val noTime = new DateTime(1970,1,1,0,0,0,0) // 0 ms + timezone offset
			val rightNow = new DateTime() // current ms + timezone offset
			//val today = rightNow.withMillisOfDay(0) // current ms as of last midnight
			//val time = new DateTime((rightNow.getMillis() - today.getMillis()) + noTime.getMillis()) // time as if today started with noTime (factors in timezone offset)
			
			val query = Json.obj(
        "timestamp.start" -> Json.obj("$lte" -> new DateTime(rightNow.getMillis())), // reminder occurs before now
				"timestamp.start" -> Json.obj("$gte" -> new DateTime(rightNow.minusMinutes(1).getMillis())) // reminder occurs no earlier than 1 minute ago
      )
      
			println(query)
      
			val tempFuture = ReminderDAO.findAll(query).map { reminders =>
				for (reminder <- reminders) {
          println(reminder)
					if (isValid(reminder)) {
             handleRecurrence(reminder)
//						ReminderDAO.findAndUpdate(BSONDocument("_id" -> BSONObjectID.apply(reminder._id.stringify)),BSONDocument("$set" -> BSONDocument("hasSent" -> true )))
						emailActor ! reminder
					}
				}
			}
			
			Await.ready(tempFuture, Duration(5000, MILLISECONDS))
			emailActor ! PoisonPill
		}
	}
	
	def isValid(reminder: Reminder): Boolean = {
     var output = false;
   val futureEvent = EventDAO.findById(reminder.eventID).map {event =>
	    val rightNow = new DateTime()
		  val start = event.get.timeRange.start
		// TODO: check how interacts with PUDs
		  //if ( rightNow.minusMinutes(1).getMillis < start.getMillis) { 
			  // Event has already started as of 1 minute ago
			  //output = false
	  	//} else {
			  output = true
		  //}
    }
		val event = Await.ready(futureEvent, Duration(5000, MILLISECONDS))
   return output
	}
 
 def handleRecurrence(reminder: Reminder) = {
   
 }
}
