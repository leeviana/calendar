import scala.concurrent.duration.DurationInt
import akka.actor.Props.apply
import play.api.Application
import play.api.GlobalSettings
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka
import akka.actor.Props
import actor.ReminderActor

object Global extends GlobalSettings {
	override def onStart(app: Application) {
		reminderDaemon(app)
	}

	def reminderDaemon(app: Application) = {
		val reminderActor = Akka.system(app).actorOf(Props(new ReminderActor()))
		Akka.system(app).scheduler.schedule(0 seconds, 1 minutes, reminderActor, "reminderDaemon")
    }
}