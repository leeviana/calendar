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



/**
 * The Users controllers encapsulates the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
 * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
 * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
 * @author Leevi
 */
object Events extends Controller with MongoController {

    /*
     * Shows the user's events and events shared with the user via rules
     */
    def index = Action.async { implicit request =>
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

                val query = BSONDocument(
                    "$or" -> List[BSONDocument](BSONDocument(
                        "calendar" -> BSONDocument(
                            "$in" -> user.head.subscriptions)),
                        BSONDocument("rules.entityID" -> user.head._id),
                        BSONDocument("rules.entityID" -> BSONDocument(
                            "$in" -> userGroupIDs))))

                val jsonquery = Json.obj(
                    "$or" -> Json.arr(
                        Json.obj(
                            "calendar" -> Json.obj(
                                "$in" -> user.get.subscriptions)),
                        Json.obj(
                            "rules.entityID" -> user.get._id),
                        Json.obj(
                            "rules.entityID" -> Json.obj(
                                "$in" -> userGroupIDs))))

                val sort = Json.obj("timeRange.startDate" -> 1, "timeRange.startTime" -> 1)

                EventDAO.findAll(jsonquery, sort).map { events =>
                    // TODO: applyAccesses(events)
                    Ok(views.html.events(events))
                }
            }
        } else {
            Future.successful(Redirect(routes.Application.index))
        }
    }

    /*
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

    /*
     * Shows the event creation form
     */
    def showCreationForm = Action.async { implicit request =>
        val iterator = RecurrenceType.values.iterator

        UserDAO.findById(AuthStateDAO.getUserID()).map { user =>
            var calMap: Map[String, String] = Map()

            for (calID <- user.get.subscriptions) {
                calMap += (calID.stringify -> CalendarDAO.getCalendarFromID(calID).name)
            }

            Ok(views.html.editEvent(Event.form, iterator, calMap))
        }
    }

    /*
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
                errors => Ok(views.html.editEvent(errors, iterator, calMap)),

                event => {
                    val calendar = event.calendar
                    EventDAO.insert(event)

                    // Recurrence. TODO: With the implementation of a RecurrenceMeta hierarchy, refactor this
                    if (event.recurrenceMeta.isDefined) {
                        val recurrenceDates = new ListBuffer[Long]()
                        val recType = event.recurrenceMeta.get.recurrenceType
                        if (event.timeRange.startDate.isDefined) {
                            if (event.recurrenceMeta.get.timeRange.endDate.isDefined) {
                                val start = event.timeRange.startDate.get
                                val end = event.recurrenceMeta.get.timeRange.endDate.get

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
                        }

                        for (difference <- recurrenceDates) {
                            val newStartDate = new DateTime(event.timeRange.startDate.get.getMillis + difference)
                            var newTimeRange = new TimeRange

                            if (event.timeRange.endDate.isDefined) {
                                val newEndDate = new DateTime(event.timeRange.endDate.get.getMillis + difference)
                                newTimeRange = event.timeRange.copy(startDate = Some(newStartDate), endDate = Some(newEndDate))
                            } else {
                                newTimeRange = event.timeRange.copy(startDate = Some(newStartDate))
                            }

                            val updatedEvent = event.copy(_id = BSONObjectID.generate, calendar = calendar, timeRange = newTimeRange)
                            // val future = collection.insert(updatedEvent)
                            EventDAO.insert(updatedEvent)
                        }
                    }

                    Redirect(routes.Events.index())
                })
        }
    }

    /*
     * Deletes an event
     */
    def deleteEvent(eventID: String) = Action { implicit request =>
        val objectID = BSONObjectID.apply(eventID)

        EventDAO.removeById(objectID)

        Redirect(routes.Events.index())
    }

    /*
     * Confirmation page before actually deleting an event
     */
    def confirmDelete(eventID: String) = Action {
        Ok(viewComponents.html.confirmDelete(eventID, Event.form))
    }

    /*
     * Renders a page that shows details about a specific event
     */
    def showEvent(eventID: String, reminderForm: Form[Reminder] = Reminder.form, ruleForm: Form[Rule] = Rule.form) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)

        EventDAO.findById(objectID).map { event =>
            if (event.isDefined)
                Ok(views.html.EventInfo(event.get, reminderForm, ruleForm, AuthStateDAO.getUserID().stringify))
            else
                throw new Exception("Database incongruity: Event ID not found")
        }
    }

    /*
     * Adds a reminder for an event
     * TODO: extend this so it can remind for PUDs too
     */
    def addReminder(eventID: String) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)

        EventDAO.findById(objectID).map { event =>
            Reminder.form.bindFromRequest.fold(
                errors => Ok(views.html.EventInfo(event.get, errors, Rule.form, AuthStateDAO.getUserID().stringify)),

                reminder => {
                    ReminderDAO.insert(reminder)
                    Redirect(routes.Events.showEvent(eventID))
                })
        }
    }

    /*
     * Adds a new rule to an Event
     * TODO: May want to do this for a calendar too in the future
     */
    def addRule(eventID: String) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)

        EventDAO.findById(objectID).map { event =>
            Rule.form.bindFromRequest.fold(
                errors => Ok(views.html.EventInfo(event.get, Reminder.form, errors, AuthStateDAO.getUserID().stringify)),

                rule => {
                    EventDAO.updateById(objectID, $push("rules", rule))
                    Redirect(routes.Events.showEvent(eventID))
                })
        }
    }

    /*
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
                }
            }
        }

        Redirect(routes.Events.showEvent(eventID))
    }

    /*
     * Confirmation page before actually deleting a rule
     */
    def confirmDeleteRule(eventID: String, ruleID: String) = Action {
        Ok(viewComponents.html.confirmDeleteRule(eventID, Event.form, ruleID))
    }

    /*
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
            }
            Redirect(routes.Events.showEvent(eventID))
        }
    }
}
