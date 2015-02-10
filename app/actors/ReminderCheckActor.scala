package actor

import akka.actor.Actor
import akka.actor.Props
import akka.actor.PoisonPill

class ReminderCheckActor extends Actor {
	def receive = {
		case _ => {
			val emailActor = context.actorOf(Props(new SendEmailActor()))
			// looping through each reminder instance in database
			//emailActor ! Event
			emailActor ! "Test message"
			// at end of loop
			emailActor ! PoisonPill
		}
	}
}
