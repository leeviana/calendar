package actor

import akka.actor.Actor
import models.Event
import models.Reminder
import models.User
import apputils.UserDAO
import apputils.EventDAO
import play.api.libs.mailer._
import play.api.Play.current
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class SendEmailActor extends Actor {

	def receive = { 
		case reminder: Reminder => {
			println("***SendEmailActor got Reminder message: " + reminder)
			val user = UserDAO.getUserFromID(reminder.user)
			val futureEvent = EventDAO.findById(reminder.eventID)
			val event = Await.result(futureEvent, Duration(5000, MILLISECONDS))
			sendReminder(user, event.get)
			// Send an email using this event data
		}
		case z => {
			println("***SendEmailActor got non-event message: " + z)
		}
	}
	
	def sendReminder(user: User, event: Event) = {
		val email = Email(
			"Nautical Reminder",
			"NautiCal Nonsense <nauticalnonsense999@gmail.com>",
			Seq(user.username + " <robert@mailinator.com>"),
			// sends text, HTML or both...
			bodyText = Some("This is the reminder that you requested regarding your event (" + event.name + ")."),
			bodyHtml = Some("<html><body><p>" + "This is the reminder that you requested regarding your event (" + event.name + ")." + "</p></body></html>")
		)
		MailerPlugin.send(email)
	}
}
