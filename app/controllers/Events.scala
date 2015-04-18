package controllers

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{ Map => MapBuffer }
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS

import org.joda.time.DateTime
import org.joda.time.{ Duration => JodaDuration }
import org.joda.time.Period

import apputils._
import models._
import models.enums._
import play.api.data.Form
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Cookie
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import reactivemongo.bson.BSONObjectID
import reactivemongo.extensions.json.dsl.JsonDsl._

/**
 * The controllers encapsulate the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
 * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
 * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
 * @author Leevi
 */
object Events extends Controller with MongoController {

    /**
     * Returns a JSObject that performs an access filter on events
     */
    def getEventFilter(user: User): JsObject = {
        val userGroupIDs = GroupDAO.getUsersGroups(user._id)

        val myEventsQuery =
            Json.obj("$or" -> Json.arr(
                Json.obj(
                    "calendar" -> Json.obj(
                        "$in" -> user.subscriptions)),
                Json.obj(
                    "rules.entityID" -> user._id),
                Json.obj(
                    "rules.entityID" -> Json.obj(
                        "$in" -> userGroupIDs))))
        myEventsQuery
    }

    /**
     * Shows the user's events of type "eventType" and events shared with the user via rules
     */
    def index(eventType: String = "Fixed") = Action.async { implicit request =>
        if (AuthStateDAO.isAuthenticated()) {
            UserDAO.findById(AuthStateDAO.getUserID()).flatMap { user =>
                
                // TODO: delete PUD events with timeRange.end $lte DateTime.now() and remove check from query
                
                // filter based on start time
                var timeQuery = Json.obj("timeRange.start" $gte DateTime.now())
                if (eventType == EventType.PUD.toString())
                    timeQuery =
                        Json.obj("$and" -> Json.arr(
                            Json.obj(
                                "timeRange.start" $lte DateTime.now()),
                            Json.obj(
                                "$or" -> Json.arr(
                                    Json.obj(
                                        "timeRange.end" $gte DateTime.now()),
                                    Json.obj(
                                        "timeRange.end" $exists false)))))

                val jsonquery = Json.obj(
                    "$and" -> Json.arr(
                        Json.obj("eventType" -> eventType),
                        timeQuery,
                        getEventFilter(user.get)))

                if (eventType == EventType.PUD) {
                    val sort = Json.obj("pudMeta.priority" -> 1)

                    EventDAO.findAll(jsonquery, sort).map { events =>
                        val accessEvents = applyAccesses(events, user.get)
                        val escalatedEvents = applyEscalations(events)
                        Ok(views.html.events(escalatedEvents, eventType))
                    }
                } else {
                    val sort = Json.obj("timeRange.start" -> 1, "timeRange.startTime" -> 1)
                    EventDAO.findAll(jsonquery, sort).map { events =>
                        val accessEvents = applyAccesses(events, user.get)
                        val PUDupdatedEvents = updatePUD(accessEvents, user.get)
                        Ok(views.html.events(PUDupdatedEvents, eventType))
                    }
                }
            }

        } else {
            Future.successful(Redirect(routes.Application.index))
        }
    }

    /**
     * Returns a list with accessType set appropriately depending on rules
     */
    def applyAccesses(events: List[Event], user: User): List[Event] = {
        events.map { event =>

            val groupIDs = GroupDAO.getUsersGroups(user._id)
            var newEvent = new Event()

            // user owns the event
            if (user.subscriptions.contains(event.calendar)) {
                newEvent = event.copy(accessType = Some(AccessType.Modify))
            } //pudEvents should not by modifiable if you don't own it
            else if (event.viewType.getOrElse(None) == models.enums.ViewType.PUDEvent) {
                newEvent = event.copy(accessType = Some(AccessType.BusyOnly))
            } else {
                if (event.viewType.getOrElse(None) == ViewType.PUDEvent) {
                    newEvent = event.copy(accessType = Some(AccessType.SeePUD))
                } else {
                    val ruleIterator = event.rules.sortBy(rule => rule.orderNum).iterator

                    var found = false

                    while (!found) {
                        if (ruleIterator.hasNext) {
                            var rule = ruleIterator.next()

                            if (rule.entityID == user._id | groupIDs.contains(rule.entityID)) {
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
            }
            newEvent
        }.toList
    }

    /**
     * Updates PUDs with escalation info
     */
    def applyEscalations(events: List[Event]): List[Event] = {
        events.map { event =>

            if(event.pudMeta.get.escalationInfo.isDefined) {
                val escalationInfo = event.pudMeta.get.escalationInfo.get
                val diff = (DateTime.now().getMillis - escalationInfo.timeRange.start.getMillis)
                // if within the time period of escalation
                if(diff > 0) {
                    val escalationPeriods = diff/escalationInfo.recurDuration.toStandardDuration().getMillis
                    val newPriority = event.pudMeta.get.priority - (escalationPeriods * event.pudMeta.get.escalationAmount.get)
                    
                    if(newPriority > 1) {
                        val newpudMeta = event.pudMeta.get.copy(priority = newPriority.toInt)
                        event.copy(pudMeta = Some(newpudMeta))
                    }
                    else {
                        val newpudMeta = event.pudMeta.get.copy(priority = 1)    
                        event.copy(pudMeta = Some(newpudMeta))
                    }
                }
                else {
                    event
                }
            } else {            
                event    
            }  
        }
    }
    
    /**
     * Updates PUDEvents with PUD information
     */
    def updatePUD(events: List[Event], user: User): List[Event] = {
        events.map { event =>
            var newEvent = event
            if (!event.viewType.isEmpty) {
                if (event.viewType.get.toString == models.enums.ViewType.PUDEvent.toString) {
                    val dur = event.getFirstTimeRange().duration.getMillis
                    val query = Json.obj(
                        "$and" -> Json.arr(
                            Json.obj("eventType" -> "PUD"),
                            Json.obj("timeRange.duration" -> Json.obj(
                                "$lte" -> dur)),
                            getEventFilter(user)))

                    val sort = Json.obj("pudMeta.priority" -> 1)
                    var temp: List[models.Event] = List()

                    val future = EventDAO.findAll(query, sort).map { PUDlist =>
                        temp = PUDlist;
                    }

                    Await.ready(future, Duration(5000, MILLISECONDS))

                    if (!temp.isEmpty) {
                        val PUD = temp.head
                        val newName = "PUD: " + PUD.name
                        newEvent = event.copy(name = newName, description = PUD.description)
                        val future = EventDAO.save(newEvent)
                        Await.ready(future, Duration(100000, MILLISECONDS))

                    } else {
                        newEvent = event.copy(name = "No PUD is available")
                        val future = EventDAO.save(newEvent)
                        Await.ready(future, Duration(5000, MILLISECONDS))

                    }
                }
            }
            newEvent
        }.toList
    }

    /**
     * Shows the event creation form
     */
    def showCreationForm = Action.async { implicit request =>
        val iterator = RecurrenceType.values.iterator

        UserDAO.findById(AuthStateDAO.getUserID()).map { user =>
            var calMap: MapBuffer[String, String] = MapBuffer()

            for (calID <- user.get.subscriptions) {
                calMap += (calID.stringify -> CalendarDAO.getCalendarFromID(calID).name)
            }

            Ok(views.html.editEvent(None, Event.form, iterator, calMap)).withCookies(Cookie("calMap", Json.stringify(mapToJson(calMap))));
        }
    }

    /**
     * Creates an event on a calendar
     */
    def create = Action { implicit request =>
        val iterator = RecurrenceType.values.iterator
        Event.form.bindFromRequest.fold(
            errors => Ok(views.html.editEvent(None, errors, iterator, jsonToMap(Json.parse(request.cookies.get("calMap").get.value)))),

            event => {

                var myEvent = new Event

                if ((event.eventType == EventType.SignUp)) {
                    myEvent = createSignUpSlots(event)
                } else {
                    myEvent = event
                }

                if (event.recurrenceMeta.isDefined) {
                    val newTimeRange = event.recurrenceMeta.get.timeRange.copy(start = event.getFirstTimeRange().start)
                    val newRecurrenceMeta = event.recurrenceMeta.get.copy(timeRange = newTimeRange)
                    EventDAO.insert(myEvent.copy(recurrenceMeta = Some(newRecurrenceMeta)))
                } else {
                    EventDAO.insert(myEvent)
                }

                if ((event.recurrenceMeta.isDefined) && (event.eventType != EventType.PUD)) {
                    val newTimeRange = event.recurrenceMeta.get.timeRange.copy(start = event.getFirstTimeRange().start)
                    val newRecurrenceMeta = event.recurrenceMeta.get.copy(timeRange = newTimeRange)
                    createRecurrences(myEvent.copy(recurrenceMeta = Some(newRecurrenceMeta)))
                }

                Redirect(routes.Events.index(event.eventType.toString()))
            })
    }

    /**
     * Creates recurring events.
     */
    def createRecurrences(event: Event): List[Event] = {
        val newEvents = ListBuffer[Event]()

        val calendar = event.calendar
        val recType = event.recurrenceMeta.get.recurrenceType

        if (event.recurrenceMeta.get.timeRange.end.isDefined) {
            val end = event.recurrenceMeta.get.timeRange.end.get

            for (timeRange <- event.timeRange) {
                val recurrencePeriod = event.recurrenceMeta.get.recurDuration
                if (recurrencePeriod.toStandardDuration().getMillis > 0 & timeRange.end.isDefined) {
                    var currentStart = timeRange.start.plus(recurrencePeriod)
                    var currentEnd = timeRange.end.get.plus(recurrencePeriod)
                    var thisPointer = BSONObjectID.generate
                    var nextPointer = BSONObjectID.generate

                    while (currentStart.compareTo(end) <= 0) {
                        var newTimeRange = new TimeRange(currentStart, currentEnd)

                        val updatedEvent = event.copy(_id = thisPointer, calendar = calendar, timeRange = List[TimeRange](newTimeRange), nextRecurrence = Some(nextPointer))
                        newEvents.append(updatedEvent)
                        EventDAO.insert(updatedEvent)

                        currentStart = currentEnd.plus(recurrencePeriod)
                        currentEnd = currentEnd.plus(recurrencePeriod)
                        thisPointer = nextPointer
                        nextPointer = BSONObjectID.generate
                    }
                }
            }
        }
        newEvents.toList
    }

    /**
     * Creates sign up slots for a new event.
     */
    def createSignUpSlots(event: Event): Event = {
        var signUpSlots = ListBuffer[SignUpSlot]()

        for (timeRange <- event.timeRange) {
            var currentStart = timeRange.start
            val duration = new Period(0, event.signUpMeta.get.minSignUpSlotDuration, 0, 0).toStandardDuration()

            var currentEnd = new DateTime(currentStart.getMillis + (duration.getMillis))

            if (duration.getMillis != 0) {
                while (currentEnd.compareTo(timeRange.end.getOrElse(DateTime.now())) <= 0) {

                    val newSlot = SignUpSlot(timeRange = new TimeRange(start = currentStart, end = Some(currentEnd), duration = duration))

                    signUpSlots.append(newSlot)

                    currentStart = currentEnd
                    currentEnd = currentEnd.plus(duration)
                }
            }
        }

        val updatedSignUpMeta = event.signUpMeta.get.copy(signUpSlots = signUpSlots.toList)
        val updatedEvent = event.copy(signUpMeta = Some(updatedSignUpMeta))
        updatedEvent
    }

    /**
     * Shows the form for editing an event
     */
    def showEventEditForm(eventID: String) = Action.async { implicit request =>

        EventDAO.findById(BSONObjectID.apply(eventID)).flatMap { event =>
            val iterator = RecurrenceType.values.iterator

            UserDAO.findById(AuthStateDAO.getUserID()).map { user =>
                var calMap: MapBuffer[String, String] = MapBuffer()

                for (calID <- user.get.subscriptions) {
                    calMap += (calID.stringify -> CalendarDAO.getCalendarFromID(calID).name)
                }

                Ok(views.html.editEvent(Some(eventID), Event.form.fill(event.get), iterator, calMap)).withCookies(Cookie("calMap", Json.stringify(mapToJson(calMap))));
            }
        }
    }

    def jsonToMap(json: JsValue): MapBuffer[String, String] = {
        var output = MapBuffer[String, String]()
        val test = json match {
            case o: JsObject => {
                val keys = o.keys;
                for (k <- keys) {
                    output += (k -> (o \ k).as[String]);
                }
            }
            case _ => Set()
        }
        return output
    }

    def mapToJson(map: MapBuffer[String, String]): JsValue = {
        var output = new JsObject(Seq[(String, JsValue)]());
        map.foreach {
            case (k, v) => {
                output = output + (k -> Json.toJson(v));
            }
        }
        return output;
    }

    /**
     *  Edits an event
     */
    def editEvent(eventID: String) = Action.async { implicit request =>
        val iterator = RecurrenceType.values.iterator

        EventDAO.findById(BSONObjectID(eventID)).map { oldEvent =>
            Event.form.bindFromRequest.fold(
                errors => {
                    Ok(views.html.editEvent(Some(oldEvent.get._id.stringify), errors, iterator, jsonToMap(Json.parse(request.cookies.get("calMap").get.value))));
                },

                event => {
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
                        CreationRequests.createMasterRequest(event, oldEvent.get.master.get)
                        Redirect(routes.Events.index(event.eventType.toString()))
                    }
                })
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
                    val lastStart = event.get.getFirstTimeRange().start

                    // TODO: make sure this works
                    if (event.get.pudMeta.get.escalationInfo.isDefined) {
                        val lastEscalationStart = event.get.pudMeta.get.escalationInfo.get.timeRange.start
                        val newEscalationInfo = event.get.pudMeta.get.escalationInfo.get.copy(new TimeRange(start = lastEscalationStart.plus(event.get.recurrenceMeta.get.recurDuration)))
                        val newPUDMeta = event.get.pudMeta.get.copy(escalationInfo = Some(newEscalationInfo))
                        val newEvent = event.get.copy(pudMeta = Some(newPUDMeta), _id = BSONObjectID.generate, timeRange = List[TimeRange](new TimeRange(start = lastStart.plus(event.get.recurrenceMeta.get.recurDuration), duration = event.get.getFirstTimeRange().duration)))
                        regenerateReminders(newEvent)
                        EventDAO.insert(newEvent)
                    }

                    val newEvent = event.get.copy(_id = BSONObjectID.generate, timeRange = List[TimeRange](new TimeRange(start = lastStart.plus(event.get.recurrenceMeta.get.recurDuration), duration = event.get.getFirstTimeRange().duration)))
                    regenerateReminders(newEvent)
                    EventDAO.insert(newEvent)
                }
            })

        EventDAO.removeById(objectID)
        Redirect(routes.Events.index(EventType.PUD.toString()))
    }

    /**
     * Regenerates first reminder of recurring reminders
     */
    def regenerateReminders(event: Event) {
        if (event.reminders.isDefined) {
            event.reminders.get.map { reminder =>
                if (reminder.recurrenceMeta.isDefined) {
                    val newReminder = reminder.copy(timestamp = new TimeRange(start = event.getFirstTimeRange().start.plus(reminder.timestamp.duration)))
                    ReminderDAO.insert(newReminder)
                }
            }
        }
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

        // TODO: use recurrencepointer to delete events afterwards... make new deleteAll method for it?

        EventDAO.findAndRemove("_id" $eq objectID).map { oldEvent =>
            // Check for any associated creation request and update those
            if (oldEvent.get.master.isDefined) {
                val query = $and("master" $eq oldEvent.get.master.get, "eventID" $eq oldEvent.get._id)

                EventDAO.findById(oldEvent.get.master.get).map { master =>

                    // if master event is SignUp event, clear slot or remove sign up option
                    if (master.get.eventType == EventType.SignUp) {
                        val newSignUpSlots = master.get.signUpMeta.get.signUpSlots.map { signUpSlot =>
                            if (signUpSlot.timeRange.start == oldEvent.get.getFirstTimeRange().start) {
                                if (oldEvent.get.viewType == ViewType.SignUpPossibility) {
                                    val newOptions = signUpSlot.userOptions.get.filter { userOption =>
                                        userOption.userID != AuthStateDAO.getUserID()
                                    }
                                    signUpSlot.copy(userOptions = Some(newOptions))
                                } else {
                                    signUpSlot.copy(userID = None)
                                }
                            } else {
                                signUpSlot
                            }
                        }

                        val newSignUpMeta = master.get.signUpMeta.get.copy(signUpSlots = newSignUpSlots)
                        EventDAO.save(master.get.copy(signUpMeta = Some(newSignUpMeta)))
                    } else { // normal shared event
                        // if you are the owner of the master event also
                        if (master.get.calendar == oldEvent.get.calendar)
                            CreationRequestDAO.remove(query)
                        else {
                            val update = $set("requestStatus" -> CreationRequestStatus.Removed.toString())
                            CreationRequestDAO.update(query, update)
                        }
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
    def showEvent(eventID: String) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID);
        var userList: List[models.User] = List();
        var reminderForm: Form[Reminder] = Reminder.form;
        var ruleForm: Form[Rule] = Rule.form;

        val future = UserDAO.findAll().map { users =>
            userList = users;
        }

        Await.ready(future, Duration(5000, MILLISECONDS))

        EventDAO.findById(objectID).map { event =>
            if (event.isDefined) {
                Ok(views.html.EventInfo(event.get, reminderForm, ruleForm, AuthStateDAO.getUserID().stringify, userList)).withCookies(Cookie("userList", Json.stringify(Json.toJson(userList))));
            } else {
                throw new Exception("Database incongruity: Event ID not found")
            }
        }
    }

    /**
     * Adds a reminder for an event
     */
    def addReminder(eventID: String) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)

        EventDAO.findById(objectID).map { event =>
            Reminder.form.bindFromRequest.fold(
                errors => {
                    Ok(views.html.EventInfo(event.get, errors, Rule.form, AuthStateDAO.getUserID().stringify, Json.parse(request.cookies.get("userList").get.value).as[List[models.User]]));
                },

                reminder => {
                    var newReminder = reminder
                    if (event.get.eventType == EventType.PUD) {
                        newReminder = reminder.copy(timestamp = reminder.timestamp.copy(start = event.get.getFirstTimeRange().start.plus(reminder.timestamp.duration)))
                    } else if (event.get.eventType == EventType.Fixed) {
                        newReminder = reminder.copy(timestamp = reminder.timestamp.copy(duration = new JodaDuration(event.get.getFirstTimeRange().start, reminder.timestamp.start)))
                    }
                    ReminderDAO.insert(newReminder)
                    EventDAO.updateById(objectID, $push("reminders", newReminder))

                    Redirect(routes.Events.showEvent(eventID)).flashing("test" -> reminder.toString())
                })
        }
    }

    def email = Action {
        Ok(views.html.email())
    }

    def bulkAdd = Action {
        Ok(views.html.bulkAdd())
    }
}
