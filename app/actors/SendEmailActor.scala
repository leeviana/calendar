package actor

import akka.actor.Actor
import models.Event

class SendEmailActor extends Actor {

	def receive = {
		case y: Event => {
			// Send an email using this event data
		}
		case _ => {
			println("SendEmailActor got non-event message!")
		}
	}
}
