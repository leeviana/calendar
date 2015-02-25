package controllers

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS
import org.joda.time.DateTime
import apputils._
import models._
import models.enums.RecurrenceType
import models.enums.AccessType
import models.enums.EntityType
import models.enums.ViewType
import play.api.data.Form
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import reactivemongo.bson._
import reactivemongo.extensions.json.dsl.JsonDsl._
import models.enums.EventType
import models.enums.CreationRequestStatus
import org.joda.time.Period
import org.joda.time.DateTime
//import org.joda.time.Duration

/**
 * The controllers encapsulate the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
 * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
 * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
 * @author Leevi
 */
object Events extends Controller with MongoController {

    /**
     * Shows the user's fixed events and events shared with the user via rules
     */

    def index(eventType: String = "Fixed") = Action.async { implicit request =>
        if (AuthStateDAO.isAuthenticated()) {
            UserDAO.findById(AuthStateDAO.getUserID()).flatMap { user =>

                var userGroupIDs = ListBuffer[BSONObjectID]()
                val futureGroups = GroupDAO.findAll("userIDs" $eq user.head._id).map {
                    groups =>
                        for (group <- groups) {
                            userGroupIDs += group._id
                        }
                }
                Await.ready(futureGroups, Duration(5000, MILLISECONDS))

                // filter based on start time
                var timeQuery = Json.obj("timeRange.start" $gte DateTime.now())
                if(eventType == EventType.PUD.toString())
                    timeQuery = Json.obj("timeRange.start" $lte DateTime.now())
                    
                val jsonquery = Json.obj(
                    "$and" -> Json.arr(
                        Json.obj("eventType" -> eventType),
                        timeQuery,
                        Json.obj("$or" -> Json.arr(
                            Json.obj(
                                "calendar" -> Json.obj(
                                    "$in" -> user.get.subscriptions)),
                            Json.obj(
                                "rules.entityID" -> user.get._id),
                            Json.obj(
                                "rules.entityID" -> Json.obj(
                                    "$in" -> userGroupIDs))))))

                if (eventType == EventType.PUD) {
                    val sort = Json.obj("PUDPriority" -> 1)
                    
                    EventDAO.findAll(jsonquery, sort).map { events =>
                        val accessEvents = applyAccesses(events, user.get, userGroupIDs.toList)
                        Ok(views.html.events(accessEvents, eventType))
                    }
                } else {
                    val sort = Json.obj("timeRange.start" -> 1, "timeRange.startTime" -> 1)
                    EventDAO.findAll(jsonquery, sort).map { events =>
                        val accessEvents = applyAccesses(events, user.get, userGroupIDs.toList)
                        val PUDupdatedEvents = updatePUD(accessEvents)
                        Ok(views.html.events(PUDupdatedEvents, eventType))
                    }
                }
            }

        } else {
            Future.successful(Redirect(routes.Application.index))
        }
    }

    def applyAccesses(events: List[Event], user: User, groupIDs: List[BSONObjectID]): List[Event] = {
        events.map { event =>
            var newEvent = new Event()

            // user owns the event
            if (user.subscriptions.contains(event.calendar))
                newEvent = event.copy(accessType = Some(AccessType.Modify))

            else {
                val ruleIterator = event.rules.sortBy(rule => rule.orderNum).iterator

                var found = false

                while (!found) {
                    if (ruleIterator.hasNext) {
                        var rule = ruleIterator.next()

                        if (rule.entityType == EntityType.User & rule.entityID == user._id) {
                            newEvent = event.copy(accessType = Some(rule.accessType))
                            found = true
                        } else if (rule.entityType == EntityType.User & groupIDs.contains(rule.entityID)) {
                            newEvent = event.copy(accessType = Some(rule.accessType))
                            found = true
                        }
                    } else {
                        // should never happen?
                        newEvent = event.copy(accessType = Some(AccessType.Private))
                        found = true
                    }
                }
            }
            newEvent
        }.toList
    }

  def updatePUD(events: List[Event]): List[Event] =  {
    events.map { event =>
        var newEvent = event
      if (!event.viewType.isEmpty) {
        if (event.viewType.get.toString == models.enums.ViewType.PUDEvent.toString) {
          //val dur = Period.hours(2)
          //val dur = event.timeRange.duration.get
          //val dur = org.joda.time.Duration(event.timeRange.startTime, event.timeRange.endTime).toDuration
          Console.println("PUDEvent")
          ////val start = event.timeRange.start.getMillis
          ////val end = event.timeRange.end.getOrElse(new DateTime).getMillis
          //val startHours = event.timeRange.startTime.get
          ////Console.println("start time = " + start + "; end time = " + end)
          ////val dur = end - start
          val dur = event.timeRange.duration.getMillis
          Console.println("duration of PUD Event = " + dur)
          //- event.timeRange.endTime.get
          //need real duration
          val query = Json.obj(
            "$and" -> Json.arr(
              Json.obj("eventType" -> "PUD"),
              Json.obj("timeRange.duration" -> Json.obj(
                "$lte" -> dur))))
          //val query = Json.obj("eventType" -> "PUD")
          //val query = Json.obj("timeRange.duration" -> Json.obj("$lte" -> dur))
          val sort = Json.obj("PUDPriority" -> 1)
          Console.println("made query")
          var temp: List[models.Event] = List()

          val future = EventDAO.findAll(query, sort).map { PUDlist =>
            temp = PUDlist;
          }

          Await.ready(future, Duration(5000, MILLISECONDS))
          //EventDAO.findAll(query, sort).map { PUDlist =>
            
            if (!temp.isEmpty) {
              val PUD = temp.head
              Console.println("PUD name = " + PUD.name)
              val newName = "PUD: " + PUD.name
              Console.println(newName)
              newEvent = event.copy(name = newName, description = PUD.description)
              //val future = EventDAO.save(newEvent)
              //Await.ready(future, Duration(100000, MILLISECONDS))
          
              //EventDAO.remove(event)
              //EventDAO.insert(newEvent)
              //EventDAO.updateById(BSONObjectID.apply(event._id))
              //EventDAO.updateById(event._id, "name" = PUD.name, description = PUD.description)
            } else {
              newEvent = event.copy(name = "No PUD fits in this PUDEvent")
              //val future = EventDAO.save(newEvent)
              //Await.ready(future, Duration(5000, MILLISECONDS))
          
              Console.println("no PUD fits")
            }
          //}
        }
      }
      newEvent
    }.toList
  }

    /**
     * Shows reminders that the user has set
     * TODO: Show these on the event info page instead
     */
    def showReminders = Action.async { implicit request =>
        if (AuthStateDAO.isAuthenticated()) {
            ReminderDAO.findAll("user" $eq AuthStateDAO.getUserID()).map { reminders =>
                Ok(views.html.ReminderDisplay(reminders))
            }
        } else {
            Future.successful(Redirect(routes.Application.index))
        }
    }

    /**
     * Shows the event creation form
     */
    def showCreationForm = Action.async { implicit request =>
        val iterator = RecurrenceType.values.iterator

        UserDAO.findById(AuthStateDAO.getUserID()).map { user =>
            var calMap: Map[String, String] = Map()

            for (calID <- user.get.subscriptions) {
                calMap += (calID.stringify -> CalendarDAO.getCalendarFromID(calID).name)
            }

            Ok(views.html.editEvent(None, Event.form, iterator, calMap))
        }
    }

    /**
     * Creates an event on a calendar
     */
    def create = Action.async { implicit request =>
        val iterator = RecurrenceType.values.iterator

        UserDAO.findById(AuthStateDAO.getUserID()).map { user =>
            var calMap: Map[String, String] = Map()

            for (calID <- user.get.subscriptions) {
                CalendarDAO.findById(calID).map { cal =>
                    calMap += (calID.stringify -> cal.get.name)
                }
            }

            Event.form.bindFromRequest.fold(
                errors => Ok(views.html.editEvent(None, errors, iterator, calMap)),

                event => {
                    val calendar = event.calendar
                    EventDAO.insert(event)

                    // Recurrence. TODO: With the implementation of a RecurrenceMeta hierarchy, refactor this
                    if ((event.recurrenceMeta.isDefined) && (event.eventType == EventType.Fixed)) {
                        val recurrenceDates = new ListBuffer[Long]()
                        val recType = event.recurrenceMeta.get.recurrenceType
                        //if (event.timeRange.startDate.isDefined) {
                            if (event.recurrenceMeta.get.timeRange.end.isDefined) {
                                val start = event.timeRange.start
                                val end = event.recurrenceMeta.get.timeRange.end.get

                                if (recType.compare(RecurrenceType.Daily) == 0) {
                                    recurrenceDates ++= DayMeta.generateRecurrence(start, end)
                                }
                                if (recType.compare(RecurrenceType.Weekly) == 0) {
                                    recurrenceDates ++= WeekMeta.generateRecurrence(start, end)
                                }
                                if (recType.compare(RecurrenceType.Monthly) == 0) {
                                    recurrenceDates ++= MonthMeta.generateRecurrence(start, end)
                                }
                                if (recType.compare(RecurrenceType.Yearly) == 0) {
                                    recurrenceDates ++= YearMeta.generateRecurrence(start, end)
                                }
                            } else {
                                // for future expansion, infinite recurrence
                            }
                        //}

                        for (difference <- recurrenceDates) {
                            val newStartDate = new DateTime(event.timeRange.start.getMillis + difference)
                            var newTimeRange = new TimeRange

                            if (event.timeRange.end.isDefined) {
                                val newEndDate = new DateTime(event.timeRange.end.get.getMillis + difference)
                                newTimeRange = event.timeRange.copy(start = newStartDate, end = Some(newEndDate))
                            } else {
                                newTimeRange = event.timeRange.copy(start = newStartDate)
                            }

                            val updatedEvent = event.copy(_id = BSONObjectID.generate, calendar = calendar, timeRange = newTimeRange)
                            // val future = collection.insert(updatedEvent)
                            EventDAO.insert(updatedEvent)
                        }
                    }

                    Redirect(routes.Events.index(event.eventType.toString()))
                })
        }
    }

    /**
     * Shows the form for editing an event
     */
    def showEventEditForm(eventID: String) = Action.async { implicit request =>

        EventDAO.findById(BSONObjectID.apply(eventID)).flatMap { event =>
            val iterator = RecurrenceType.values.iterator

            UserDAO.findById(AuthStateDAO.getUserID()).map { user =>
                var calMap: Map[String, String] = Map()

                for (calID <- user.get.subscriptions) {
                    calMap += (calID.stringify -> CalendarDAO.getCalendarFromID(calID).name)
                }

                Ok(views.html.editEvent(Some(eventID), Event.form.fill(event.get), iterator, calMap))
            }
        }
    }

    /**
     *  Edits an event
     */
    def editEvent(eventID: String) = Action.async { implicit request =>
        val iterator = RecurrenceType.values.iterator
        println("Edit event is called")
        UserDAO.findById(AuthStateDAO.getUserID()).flatMap { user =>
            var calMap: Map[String, String] = Map()

            for (calID <- user.get.subscriptions) {
                CalendarDAO.findById(calID).map { cal =>
                    calMap += (calID.stringify -> cal.get.name)
                }
            }
            println("User found, map made")
        
            EventDAO.findById(BSONObjectID(eventID)).map { oldEvent =>
                Event.form.bindFromRequest.fold(
                    errors => Ok(views.html.editEvent(None, errors, iterator, calMap)),

                    event => {
                        println("No errors")
        
                        if (!oldEvent.get.master.isDefined & oldEvent.get.master != oldEvent.get._id) {
                            val newEvent = event.copy(_id = BSONObjectID(eventID))
                            EventDAO.save(newEvent)

                            CreationRequestDAO.update($and("master" $eq oldEvent.get._id, "requestStatus" $ne CreationRequestStatus.Removed.toString()), $set("requestStatus" -> CreationRequestStatus.Pending.toString()))

                            // updates all slave events that are not on your own calendar (which are pending master requests)
                            EventDAO.findAll($and("master" $eq oldEvent.get._id, "calendar" $ne oldEvent.get.calendar)).map { slaveEvents =>
                                slaveEvents.map { slaveEvent =>
                                    val updatedEvent = event.copy(_id = slaveEvent._id, calendar = slaveEvent.calendar, master = slaveEvent.master, rules = slaveEvent.rules, viewType = Some(ViewType.Request))
                                    EventDAO.save(updatedEvent)
                                }
                            }
                            Redirect(routes.Events.index(newEvent.eventType.toString()))
                        } else {
                            // if event master is not the master
                            createMasterRequest(event, oldEvent.get.master.get)
                            Redirect(routes.Events.index(event.eventType.toString()))
                        }
                    })
            }

        }
    }

    /**
     * Deletes a PUD and possibly creates another recurring PUD
     */
    def completePUD(PUDID: String) = Action { implicit request =>
        val objectID = BSONObjectID.apply(PUDID)
        EventDAO.findById(objectID).map(event =>
            if (event.isDefined) {
                if (event.get.recurrenceMeta.isDefined) {
                    
                    val recType = event.get.recurrenceMeta.get.recurrenceType
           
                    if (recType.compare(RecurrenceType.Daily) == 0) {
                        val newEvent = event.get.copy(_id = BSONObjectID.generate, timeRange = new TimeRange(start = DayMeta.generateNext(DateTime.now()), duration = event.get.timeRange.duration))
                        EventDAO.insert(newEvent)
                    }
                    if (recType.compare(RecurrenceType.Weekly) == 0) {
                        val newEvent = event.get.copy(_id = BSONObjectID.generate, timeRange = new TimeRange(start = WeekMeta.generateNext(DateTime.now()), duration = event.get.timeRange.duration))
                        EventDAO.insert(newEvent)
                    }
                    if (recType.compare(RecurrenceType.Monthly) == 0) {
                        val newEvent = event.get.copy(_id = BSONObjectID.generate, timeRange = new TimeRange(start = MonthMeta.generateNext(DateTime.now()), duration = event.get.timeRange.duration))
                        EventDAO.insert(newEvent)
                    }
                    if (recType.compare(RecurrenceType.Yearly) == 0) {
                        val newEvent = event.get.copy(_id = BSONObjectID.generate, timeRange = new TimeRange(start = YearMeta.generateNext(DateTime.now()), duration = event.get.timeRange.duration))
                        EventDAO.insert(newEvent)
                    }
                }
            })

        EventDAO.removeById(objectID)
        Redirect(routes.Events.index(EventType.PUD.toString()))
    }

    /**
     * Deletes an event. Redirects to fixed view
     */
    def deleteEvent(eventID: String) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)

        // if events refer to this as their master, delete them
        EventDAO.remove("master" $eq objectID)

        // Check for any associated reminders and delete those too
        ReminderDAO.remove("eventID" $eq objectID)

        EventDAO.findAndRemove("_id" $eq objectID).map { oldEvent =>
            // Check for any associated creation request and update those
            if (oldEvent.get.master.isDefined) {
                val query = $and("master" $eq oldEvent.get.master.get, "eventID" $eq oldEvent.get._id)

                EventDAO.findById(oldEvent.get.master.get).map { master =>
                    // if you are the owner of the master event also
                    if (master.get.calendar == oldEvent.get.calendar)
                        CreationRequestDAO.remove(query)
                    else {
                        val update = $set("requestStatus" -> CreationRequestStatus.Removed.toString())
                        CreationRequestDAO.update(query, update)
                    }
                }
            }

            Redirect(routes.Events.index(EventType.Fixed.toString()))
        }

    }

    /**
     * Confirmation page before actually deleting an event
     */
    def confirmDelete(eventID: String) = Action {
        Ok(viewComponents.html.confirmDelete(eventID, Event.form))
    }

    /**
     * Renders a page that shows details about a specific event
     */
    def showEvent(eventID: String, reminderForm: Form[Reminder] = Reminder.form, ruleForm: Form[Rule] = Rule.form) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)
        var userList: List[models.User] = List()

        val future = UserDAO.findAll().map { users =>
            userList = users;
        }

        Await.ready(future, Duration(5000, MILLISECONDS))
        
        var groupList:  List[models.Group] = List()
                       
        val future2 = GroupDAO.findAll("owner" $eq AuthStateDAO.getUserID()).map { groups =>         
            groupList = groups
        }
        
        Await.ready(future, Duration(5000, MILLISECONDS))

        EventDAO.findById(objectID).map { event =>
            if (event.isDefined)
                Ok(views.html.EventInfo(event.get, reminderForm, ruleForm, AuthStateDAO.getUserID().stringify, userList, groupList))
            else
                throw new Exception("Database incongruity: Event ID not found")
        }
    }

    /**
     * Adds a reminder for an event
     */
    def addReminder(eventID: String) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)
         var userList: List[models.User] = List()

        val future = UserDAO.findAll().map { users =>
            userList = users;
        }
        
        var groupList:  List[models.Group] = List()
                       
        val future2 = GroupDAO.findAll("owner" $eq AuthStateDAO.getUserID()).map { groups =>         
            groupList = groups
        }

        EventDAO.findById(objectID).map { event =>
            Reminder.form.bindFromRequest.fold(
                errors => Ok(views.html.EventInfo(event.get, errors, Rule.form, AuthStateDAO.getUserID().stringify, userList, groupList)),

                reminder => {
                    ReminderDAO.insert(reminder)
                    Redirect(routes.Events.showEvent(eventID))
                })
        }
    }

    /**
     * Adds a new rule to an Event
     * TODO: May want to do this for a calendar too in the future
     * @param ID of the event that rule is to be added to
     */
    def addRule(eventID: String) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)
        var userList: List[models.User] = List()

        val future = UserDAO.findAll().map { users =>
            userList = users;
        }
        
        var groupList:  List[models.Group] = List()
                       
        val future2 = GroupDAO.findAll("owner" $eq AuthStateDAO.getUserID()).map { groups =>         
            groupList = groups
        }
        
        Await.ready(future, Duration(5000, MILLISECONDS))

        EventDAO.findById(objectID).map { event =>
            Rule.form.bindFromRequest.fold(
                errors => Ok(views.html.EventInfo(event.get, Reminder.form, errors, AuthStateDAO.getUserID().stringify, userList, groupList)),

                rule => {
                    EventDAO.updateById(objectID, $push("rules", rule))
                    EventDAO.updateById(objectID, $set("accessType" -> AccessType.Private.toString()))
                    Redirect(routes.Events.showEvent(eventID))
                })
        }
    }

    /**
     * Deletes a rule
     */
    def deleteRule(eventID: String, ruleID: String) = Action { implicit request =>
        val objectID = BSONObjectID.apply(eventID)

        EventDAO.updateById(objectID, $pull("rules", Json.obj("orderNum" $eq ruleID.toInt)))
        EventDAO.findById(objectID).map { event =>
            var rules = event.get.rules.toBuffer
            for (x <- rules.length - 1 to 0 by -1) {
                if (x >= ruleID.toInt) {
                    val newRule1 = new Rule(rules(x).orderNum - 1, rules(x).entityType, rules(x).entityID, rules(x).accessType)

                    EventDAO.updateById(objectID, $pull("rules", rules(x)))
                    EventDAO.updateById(objectID, $push("rules", newRule1))
                    EventDAO.updateById(objectID, $set("accessType" -> AccessType.Private.toString()))
                }
            }
        }

        Redirect(routes.Events.showEvent(eventID))
    }

    /**
     * Confirmation page before actually deleting a rule
     */
    def confirmDeleteRule(eventID: String, ruleID: String) = Action {
        Ok(viewComponents.html.confirmDeleteRule(eventID, Event.form, ruleID))
    }

    /**
     * Moves around rules, depending on the rule and the direction of movement
     */
    def moveRule(eventID: String, ruleID: String, dir: String) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)

        EventDAO.findById(objectID).map { event =>
            var adjustment = 0
            if (dir.equals("up"))
                adjustment = 1
            else if (dir.equals("down"))
                adjustment = -1

            var rules = event.get.rules.toBuffer

            if (ruleID.toInt <= 0 && dir.equals("up") | ruleID.toInt >= rules.length - 1 && dir.equals("down")) {
                Redirect(routes.Events.showEvent(eventID))
            } else {
                var one = 0;
                var two = 0;
                for (x <- 0 to rules.length - 1) {
                    if (rules(x).orderNum == ruleID.toInt) {
                        one = x;
                    } else if (rules(x).orderNum == ruleID.toInt - adjustment) {
                        two = x;
                    }
                }

                val newRule1 = new Rule(rules(one).orderNum - adjustment, rules(one).entityType, rules(one).entityID, rules(one).accessType)
                val newRule2 = new Rule(rules(two).orderNum + adjustment, rules(two).entityType, rules(two).entityID, rules(two).accessType)

                EventDAO.updateById(objectID, $pull("rules", rules(one)))
                EventDAO.updateById(objectID, $pull("rules", rules(two)))
                EventDAO.updateById(objectID, $push("rules", newRule1))
                EventDAO.updateById(objectID, $push("rules", newRule2))
                EventDAO.updateById(objectID, $set("accessType" -> AccessType.Private.toString()))
            }
            Redirect(routes.Events.showEvent(eventID))
        }
    }

    /**
     * Creates a creation request for an event with user friendly parameters
     */
    def createUserCreationRequest = Action.async { implicit request =>
        val requestMap = (request.body.asFormUrlEncoded)
        val eventID = BSONObjectID.apply(requestMap.get.get("eventID").get.head)
        val userEmail = requestMap.get.get("userEmail").get.head

        UserDAO.findOne("email" $eq userEmail).map { user =>
            if (user.isDefined) {
                createCreationRequest(eventID, user.get.subscriptions.head)
            }
            Redirect(routes.Events.showEvent(eventID.stringify))
        }
    }
    
    /**
     * Creates a creation request for an event with user friendly parameters for an entire group
     */
    def createGroupCreationRequest = Action.async { implicit request =>
        val requestMap = (request.body.asFormUrlEncoded)
        val eventID = BSONObjectID.apply(requestMap.get.get("eventID").get.head)
        val groupID = requestMap.get.get("groupID").get.head

        GroupDAO.findById(BSONObjectID(groupID)).map { group =>
            if (group.isDefined) {
                group.get.userIDs.foreach { userID =>
                    UserDAO.findById(userID).map { user => 
                        createCreationRequest(eventID, user.get.subscriptions.head)
                    }
                }
            }
            Redirect(routes.Events.showEvent(eventID.stringify))
        }
    }

    /**
     * Creates a creation request for an event
     */
    def createCreationRequest(eventID: BSONObjectID, calendar: BSONObjectID) = {
        EventDAO.findById(eventID).map { event =>
            val newEvent = event.get.copy(_id = BSONObjectID.generate, master = Some(eventID), calendar = calendar, eventType = EventType.Fixed, viewType = Some(ViewType.Request))
            val creationRequest = new CreationRequest(eventID = newEvent._id, master = eventID, requestStatus = CreationRequestStatus.Pending)
            EventDAO.insert(newEvent)
            CreationRequestDAO.insert(creationRequest)
        }
    }

    /**
     * Creates a creation request back to the master event
     */
    def createMasterRequest(newEvent: Event, masterID: BSONObjectID) {
        EventDAO.findById(masterID).map { master =>
            UserDAO.findOne("subscriptions" $all (master.get.calendar)).map { user =>
                val newMasterEvent = newEvent.copy(_id = BSONObjectID.generate, master = Some(master.get._id), calendar = user.get.subscriptions.head, eventType = EventType.Fixed, viewType = Some(ViewType.Request))
                val creationRequest = new CreationRequest(eventID = newEvent._id, master = master.get._id, requestStatus = CreationRequestStatus.Pending)
                EventDAO.insert(newMasterEvent)
                CreationRequestDAO.insert(creationRequest)
            }
        }
    }

    /**
     * Updates the status of a creation request
     */
    def updateCreationStatus(eventID: String, newStatus: String) = Action.async { implicit request =>

        EventDAO.findById(BSONObjectID.apply(eventID)).map { event =>

            val query = $and("master" $eq event.get.master.get, "eventID" $eq event.get._id)

            EventDAO.findById(event.get.master.get).map { master =>
                // if you are the owner of the master event
                if (master.get.calendar == event.get.calendar) {

                    val future = CreationRequestDAO.remove("master" $eq event.get.master.get)
                    Await.ready(future, Duration(5000, MILLISECONDS))

                    if (newStatus == CreationRequestStatus.Confirmed.toString) {
                        val newMaster = event.get.copy(_id = master.get._id, master = None, viewType = Some(ViewType.Confirmed))
                        val future = EventDAO.save(newMaster)

                        Await.ready(future, Duration(5000, MILLISECONDS))

                        // createCreationRequests for all events with master as this event
                        EventDAO.findAll($and("master" $eq master.get._id, "_id" $ne event.get._id)).map { eventList =>
                            eventList.map { event =>
                                createCreationRequest(master.get._id, event.calendar)
                            }
                        }

                        // remove all old events
                        EventDAO.remove("master" $eq master.get._id)
                    } else if (newStatus == CreationRequestStatus.Declined.toString()) {
                        EventDAO.removeById(event.get._id)
                    }
                } // if you are just a sharee
                else {
                    val update = $set("requestStatus" -> newStatus)
                    CreationRequestDAO.update(query, update)
                    EventDAO.updateById(event.get._id, $set("viewType" -> newStatus))
                    //                        if (newStatus == CreationRequestStatus.Confirmed.toString) {
                    //                            EventDAO.updateById(event.get._id, $set("viewType" -> None))
                    //                        }
                    //                        else if (newStatus == CreationRequestStatus.Declined.toString()) {
                    //                            EventDAO.updateById(event.get._id, $set("viewType" -> ViewType.Declined.toString()))    
                    //                        }
                }
            }

            Redirect(routes.Events.index(EventType.Fixed.toString()))
        }
    }
}
