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
    /**
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

object EventDAO extends JsonDao[Event, BSONObjectID](MongoContext.db, "events") {
    def canSignUp(eventID: BSONObjectID, userID: BSONObjectID): Boolean = {
        val futureEvent = this.findById(eventID)
        val event = Await.result(futureEvent, Duration(5000, MILLISECONDS))
        
        val slots = event.get.signUpMeta.get.signUpSlots
        val count = slots.count { slot => slot.userID.getOrElse(-1) == userID }

        count < event.get.signUpMeta.get.maxSlots     
    }
    def getEventFromID(id: BSONObjectID): Event = {
        val futureEvent = this.findById(id)

        var event = Await.result(futureEvent, Duration(5000, MILLISECONDS))
        if (event.isDefined)
            event.get
        else
            throw new Exception("Database incongruity: Event ID not found")
    }
}

object GroupDAO extends JsonDao[Group, BSONObjectID](MongoContext.db, "groups") {
    def getUsersGroups(userID: BSONObjectID): List[Group] = {
        val futureUser = UserDAO.findById(userID)
        val user = Await.result(futureUser, Duration(5000, MILLISECONDS))

        val futureGroups = GroupDAO.findAll("userIDs" $eq user.get._id)
        val groups = Await.result(futureGroups, Duration(5000, MILLISECONDS))

        groups
    }
    
    def getUsersOfEntity(entityID: BSONObjectID): List[User] = {
        
        val futureGroup = this.findById(entityID)
        val group = Await.result(futureGroup, Duration(5000, MILLISECONDS))

        if (group.isDefined) {
            val futureUsers = UserDAO.findAll("_id" $in group.get.userIDs)
            return Await.result(futureUsers, Duration(5000, MILLISECONDS)) 
        }
        else {
            val futureUser = UserDAO.findById(entityID)
            val user = Await.result(futureUser, Duration(5000, MILLISECONDS))
            
            return List[User](user.get)
        }
    }
}

object ReminderDAO extends JsonDao[Reminder, BSONObjectID](MongoContext.db, "reminders")
object UserDAO extends JsonDao[User, BSONObjectID](MongoContext.db, "users") {
    /**
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

    /**
     * Blocking call for getting user object from ID for calls that don't care about performance and/or
     * would have to immediately block on the call anyways
     */
    def getUserFromUsername(username: String): List[User] = {
        val futureRequests = this.findAll("username" $eq username)

        val user = Await.result(futureRequests, Duration(5000, MILLISECONDS))
        if (user.length > 0)
            user
        else
            throw new Exception("Database incongruity: Username not found")
    }
    
    /**
     * Blocking call for getting list of all users
     */
    def getAllUsers(): List[User] = {
        val futureUsers = this.findAll()

        Await.result(futureUsers, Duration(5000, MILLISECONDS))
    }

    /**
     * Blocking call that returns ID of the first calendar that a user owns
     */
    def getFirstCalendarFromUserID(id: BSONObjectID): BSONObjectID = {
        val futureUser = this.findById(id)

        var user = Await.result(futureUser, Duration(5000, MILLISECONDS))
        if (user.isDefined)
            user.get.subscriptions.head
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

object AuthInfoDAO extends JsonDao[AuthInfo, BSONObjectID](MongoContext.db, "authstate")
