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
import models.DayMeta
import models.MonthMeta
import models.WeekMeta
import models.enums.RecurrenceType

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
		  val start = event.get.getFirstTimeRange().start
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
 
    /**
     * Takes a reminder that has been sent and creates the recurring reminder based on recurrence information
     * TODO: refactor when recurrence is refactored
     */
    def handleRecurrence(reminder: Reminder) = {
        if(reminder.recurrenceMeta.isDefined) {
            val recType = reminder.recurrenceMeta.get.recurrenceType
            
            if (recType.compare(RecurrenceType.Daily) == 0) {
                val newReminder = reminder.copy(timestamp = reminder.timestamp.copy(start = DayMeta.generateNext(reminder.timestamp.start, reminder.recurrenceMeta.get.daily.get.numberOfDays)))
                ReminderDAO.insert(newReminder)
            }
            if (recType.compare(RecurrenceType.Weekly) == 0) {
                val newReminder = reminder.copy(timestamp = reminder.timestamp.copy(start = WeekMeta.generateNext(reminder.timestamp.start, reminder.recurrenceMeta.get.weekly.get.numberOfWeeks.getOrElse(1))))
                ReminderDAO.insert(newReminder)
            }
            if (recType.compare(RecurrenceType.Monthly) == 0) {
                val newReminder = reminder.copy(timestamp = reminder.timestamp.copy(start = DayMeta.generateNext(reminder.timestamp.start, reminder.recurrenceMeta.get.monthly.get.numberOfMonths.getOrElse(1))))
                ReminderDAO.insert(newReminder)
            }
            if (recType.compare(RecurrenceType.Yearly) == 0) {
                val newReminder = reminder.copy(timestamp = reminder.timestamp.copy(start = DayMeta.generateNext(reminder.timestamp.start, reminder.recurrenceMeta.get.yearly.get.numberOfYears.getOrElse(1))))
                ReminderDAO.insert(newReminder)
            }
        }
    }
}
