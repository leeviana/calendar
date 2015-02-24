package apputils

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS
import models._
import reactivemongo.api.DB
import reactivemongo.api.MongoDriver
import reactivemongo.bson.BSONObjectID
import reactivemongo.extensions.json.dao.JsonDao
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.extensions.json.dsl.JsonDsl._


object MongoContext {
    val driver = new MongoDriver
    val connection = driver.connection(List(Constants.DBLocation))
    def db: DB = connection("caldb")
}

object CalendarDAO extends JsonDao[Calendar, BSONObjectID](MongoContext.db, "calendars") {
    /*
     * Blocking call for getting a calendar object from ID for calls that don't care about performance and/or
     * would have to immediately block on the call anyways
     */
    def getCalendarFromID(id: BSONObjectID): Calendar = {
        val futureCalendar = this.findById(id)

        var calendar = Await.result(futureCalendar, Duration(5000, MILLISECONDS))
        if (calendar.isDefined)
            calendar.get
        else
            throw new Exception("Database incongruity: Calendar ID not found")
    }
}

object CreationRequestDAO extends JsonDao[CreationRequest, BSONObjectID](MongoContext.db, "creationRequests") {
    def getCreationRequestsFromMaster(masterID: BSONObjectID): List[CreationRequest] = {
        val futureRequests = this.findAll("master" $eq masterID)

        Await.result(futureRequests, Duration(5000, MILLISECONDS))
    }
}
object EventDAO extends JsonDao[Event, BSONObjectID](MongoContext.db, "events")
object GroupDAO extends JsonDao[Group, BSONObjectID](MongoContext.db, "groups")
object ReminderDAO extends JsonDao[Reminder, BSONObjectID](MongoContext.db, "reminders")
object UserDAO extends JsonDao[User, BSONObjectID](MongoContext.db, "users") {
    /*
     * Blocking call for getting user object from ID for calls that don't care about performance and/or
     * would have to immediately block on the call anyways
     */
    def getUserFromID(id: BSONObjectID): User = {
        val futureUser = this.findById(id)

        val user = Await.result(futureUser, Duration(5000, MILLISECONDS))
        if (user.isDefined)
            user.get
        else
            throw new Exception("Database incongruity: User ID not found")
    }
    
    def getOwner(eventID: BSONObjectID): User = {
        val futureEvent = EventDAO.findById(eventID)
        val event = Await.result(futureEvent, Duration(5000, MILLISECONDS))
        
        val futureUser = findOne("subscriptions" $all (event.get.calendar))
        val user = Await.result(futureUser, Duration(5000, MILLISECONDS))
        if (user.isDefined)
            user.get
        else
            throw new Exception("Database incongruity: User ID not found")
    }
}
