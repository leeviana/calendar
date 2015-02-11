package controllers

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS
import scala.util.Failure
import scala.util.Success
import org.joda.time.DateTime
import models._
import apputils.AuthStateDAO
import play.api.data.Form
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.Producer.nameValue2Producer
import models.enums.RecurrenceType
import scala.concurrent.Future
import apputils._
import reactivemongo.extensions.dsl.BsonDsl._

/**
 * The Users controllers encapsulates the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
 * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
 * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
 * @author Leevi
 */
object Events extends Controller with MongoController {
    val collection = db[BSONCollection]("events")

    /*
     * Shows the user's events and events shared with the user via rules
     * TODO: finish refactoring queries into DSL
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

                val sort = BSONDocument("timeRange.startDate" -> 1, "timeRange.startTime" -> 1)
                
                EventDAO.findAll(query, sort).map { events =>
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
                            val future = collection.insert(updatedEvent)
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
        Ok(views.html.confirmDelete(eventID, Event.form))
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
     * Unused? Should be able to replace with scala list sorting anyways
     */
    def sortRules(eventID: String): Boolean = {
        val objectID = BSONObjectID.apply(eventID)

        val cursor = collection.find(BSONDocument("_id" -> objectID)).cursor[Event]
        cursor.collect[List]().map { event =>
            var rules = event.headOption.get.rules
            rules.sortBy(_.orderNum)

        }
        return true;
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

    def deleteRule(eventID: String, ruleID: String) = Action { implicit request =>
        val objectID = BSONObjectID.apply(eventID)

        val modifier = BSONDocument(
            "$pull" -> BSONDocument(
                "rules" -> BSONDocument(
                    "orderNum" -> ruleID.toInt)))

        val future = collection.update(BSONDocument("_id" -> objectID), modifier)

        var cursor = collection.find(BSONDocument("_id" -> objectID)).cursor[Event]
        cursor.collect[List]().map { event =>
            var rules = event.headOption.get.rules.toBuffer
            for (x <- rules.length - 1 to 0 by -1) {
                if (x >= ruleID.toInt) {
                    val newRule1 = new Rule(rules(x).orderNum - 1, rules(x).entityType, rules(x).entityID, rules(x).accessType)
                    val modifier1 = BSONDocument(
                        "$pull" -> BSONDocument(
                            "rules" -> rules(x)))
                    val future1 = collection.update(BSONDocument("_id" -> objectID), modifier1)
                    val modifier3 = BSONDocument(
                        "$push" -> BSONDocument(
                            "rules" -> newRule1))
                    val future3 = collection.update(BSONDocument("_id" -> objectID), modifier3)

                }

            }

        }
        future.onComplete {
            case Failure(e) => throw e
            case Success(lastError) => {
                Redirect(routes.Events.showEvent(eventID))
            }
        }

        Redirect(routes.Events.showEvent(eventID))
    }

    /*
     * Confirmation page before actually deleting a rule
     */
    def confirmDeleteRule(eventID: String, ruleID: String) = Action {
        Ok(views.html.confirmDeleteRule(eventID, Event.form, ruleID))
    }

    def moveRule(eventID: String, ruleID: String, dir: String) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)

        val cursor = collection.find(BSONDocument("_id" -> objectID)).cursor[Event]
        cursor.collect[List]().map { event =>
            //val rules = event.headOption.get.rules
            var rules = event.headOption.get.rules.toBuffer
            if (ruleID.toInt <= 0 && dir.equals("up")) {
                Redirect(routes.Events.showEvent(eventID))
            } else if (ruleID.toInt >= rules.length - 1 && dir.equals("down")) {
                Redirect(routes.Events.showEvent(eventID))
            } else if (dir.equals("up")) {
                println("length of list - " + rules.length)
                var one = 0;
                var two = 0;
                for (x <- 0 to rules.length - 1) {
                    if (rules(x).orderNum == ruleID.toInt) {
                        one = x;
                    } else if (rules(x).orderNum == ruleID.toInt - 1) {
                        two = x;
                    }
                }

                val newRule1 = new Rule(rules(one).orderNum - 1, rules(one).entityType, rules(one).entityID, rules(one).accessType)

                val newRule2 = new Rule(rules(two).orderNum + 1, rules(two).entityType, rules(two).entityID, rules(two).accessType)

                val modifier1 = BSONDocument(
                    "$pull" -> BSONDocument(
                        "rules" -> rules(one)))
                val future1 = collection.update(BSONDocument("_id" -> objectID), modifier1)

                val modifier2 = BSONDocument(
                    "$pull" -> BSONDocument(
                        "rules" -> rules(two)))
                val future2 = collection.update(BSONDocument("_id" -> objectID), modifier2)

                val modifier3 = BSONDocument(
                    "$push" -> BSONDocument(
                        "rules" -> newRule1))
                val future3 = collection.update(BSONDocument("_id" -> objectID), modifier3)

                val modifier4 = BSONDocument(
                    "$push" -> BSONDocument(
                        "rules" -> newRule2))
                val future4 = collection.update(BSONDocument("_id" -> objectID), modifier4)

            } else if (dir.equals("down")) {
                var one = 0;
                var two = 0;
                for (x <- 0 to rules.length - 1) {
                    if (rules(x).orderNum == ruleID.toInt) {
                        one = x;
                    } else if (rules(x).orderNum == ruleID.toInt + 1) {
                        two = x;
                    }
                }
                val newRule1 = new Rule(rules(one).orderNum + 1, rules(one).entityType, rules(one).entityID, rules(one).accessType)
                val newRule2 = new Rule(rules(two).orderNum - 1, rules(two).entityType, rules(two).entityID, rules(two).accessType)

                val modifier1 = BSONDocument(
                    "$pull" -> BSONDocument(
                        "rules" -> rules(one)))
                val future1 = collection.update(BSONDocument("_id" -> objectID), modifier1)

                val modifier2 = BSONDocument(
                    "$pull" -> BSONDocument(
                        "rules" -> rules(two)))
                val future2 = collection.update(BSONDocument("_id" -> objectID), modifier2)

                val modifier3 = BSONDocument(
                    "$push" -> BSONDocument(
                        "rules" -> newRule1))
                val future3 = collection.update(BSONDocument("_id" -> objectID), modifier3)

                val modifier4 = BSONDocument(
                    "$push" -> BSONDocument(
                        "rules" -> newRule2))
                val future4 = collection.update(BSONDocument("_id" -> objectID), modifier4)

            }
            Redirect(routes.Events.showEvent(eventID))
        }
    }
}
