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
import java.io.File

class SendEmailActor extends Actor {

	def receive = { 
		case reminder: Reminder => {
			//println("***SendEmailActor got Reminder message: " + reminder)
			val user = UserDAO.getUserFromID(reminder.user)
			val futureEvent = EventDAO.findById(reminder.eventID)
			val event = Await.result(futureEvent, Duration(5000, MILLISECONDS))
			sendReminder(user, event.get)
			// Send an email using this event data
		}
		case username: String => {
			val user = UserDAO.getUserFromUsername(username)(0);
			sendEmail(user.username + " <"+ user.email +">", "This is the copy of your schedule you requested.",user.username);

		}
		case z => {
			println("***SendEmailActor got non-event message: " + z)
		}
	}
	
	def sendReminder(user: User, event: Event) = {
		sendEmail(user.username + " <"+ user.email +">", "This is the reminder that you requested regarding your event (" + event.name + ").", "")
	}

	def sendEmail(destination: String, content: String, fileID: String) = {
		var attachmentfiles: Seq[AttachmentFile] = Seq();
		if (fileID.length > 0) {
			attachmentfiles = Seq(AttachmentFile("Events.pdf", new File("./Fixed-" + fileID + ".pdf")),
				AttachmentFile("PUDs.pdf", new File("./PUD-" + fileID + ".pdf")),
				AttachmentFile("SignUps.pdf", new File("./SignUp-" + fileID + ".pdf")));
		}
		
		val email = Email(
			"Nautical Reminder",
			"NautiCal Nonsense <nauticalnonsense999@gmail.com>",
			Seq(destination),
			attachments = attachmentfiles,
			// sends text, HTML or both...
			bodyText = Some(content),
			bodyHtml = Some("<html><body><p>" + content + "</p></body></html>")
		)
		MailerPlugin.send(email)
	}
}
