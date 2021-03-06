package controllers

import models.Event
import models.TimeRange
import play.api.data.Form
import play.api.data.Forms.boolean
import play.api.data.Forms.list
import play.api.data.Forms.number
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.optional
import play.api.data.Forms.tuple
import play.api.data.Forms.jodaDate
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import apputils.GroupDAO
import reactivemongo.bson.BSONObjectID
import models.User
import scala.collection.mutable.ListBuffer
import apputils.EventDAO
import models.Calendar
import reactivemongo.extensions.json.dsl.JsonDsl._
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.collection.mutable.Map
import apputils.UserDAO
import apputils.AuthStateDAO
import org.joda.time.DateTime
import models.RecurrenceMeta
import org.joda.time.Period
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat

/**
 * @author Leevi
 */
object Scheduling extends Controller with MongoController {
    val schedulingForm = Form(
        tuple(
            "timeRanges" -> list(TimeRange.form.mapping), // specifically specify duration on the frontend to use in determining slots
            "recurrenceMeta" -> optional(RecurrenceMeta.form.mapping),
            "entities" -> optional(list(nonEmptyText)), // BSONObjectIDs
            "name" -> nonEmptyText,
            "description" -> optional(nonEmptyText),
            "duration" -> number,
            "entitiesCount" -> optional(number)))
            

    /**
     * Render a page where the user can specify their "free time" query
     */
    def showForm = Action { implicit request =>
        if (AuthStateDAO.isAuthenticated()) {
        val userID = AuthStateDAO.getUserID()
        Ok(views.html.scheduler(schedulingForm, None, userID))
        } else {
            Redirect(routes.Application.index)
        }

    }

    /**
     * Render a page with the returned data from the "free time" query
     */
    def schedulingOptions = Action(parse.multipartFormData) { implicit request =>
        val userID = AuthStateDAO.getUserID()
        
        schedulingForm.bindFromRequest.fold(
            errors => Ok(views.html.scheduler(errors, None, userID)),

            scheduleFormVals => {

                // Form values    
                val timeRanges = scheduleFormVals._1
                val recurrenceMeta = scheduleFormVals._2
                val entities = scheduleFormVals._3.getOrElse(List.empty)
                val duration = new Period(0, scheduleFormVals._6, 0, 0).toStandardDuration()
                val entitiesCount = scheduleFormVals._7.getOrElse(0)
                
                var scheduleMap = Map[TimeRange, (List[Event], Form[(List[TimeRange], Option[RecurrenceMeta], Option[List[String]], String, Option[String], Int, Option[Int])])]()
                        
                // get applicable user's calendars
                var calendars = ListBuffer[BSONObjectID]()

                for (entity <- entities.slice(0, entitiesCount)) {
                    val users = GroupDAO.getUsersOfEntity(BSONObjectID.apply(entity))
                    users.foreach { user =>
                        calendars.appendAll(user.subscriptions)
                    }
                }
                
                var calQuery = Json.obj(
                    "calendar" -> Json.obj(
                        "$in" -> calendars.toList))
                                    
                val futureUser = UserDAO.findById(AuthStateDAO.getUserID()).map { user =>

                    for (timeRange <- timeRanges) {

                        var currentStart = timeRange.start
                        var currentEnd = timeRange.start.plus(duration)

                        // uses duration to break up timeRange into "slots"
                        if (duration.getMillis != 0) {
                            while (currentEnd.compareTo(timeRange.end.getOrElse(DateTime.now())) <= 0) {

                                // returns conflicting events (that you have at least view access to)
                                if (!recurrenceMeta.isDefined) {
                                    EventDAO.findAll($and(calQuery, "timeRange.start" $lte currentEnd, "timeRange.end" $gte currentStart, Events.getEventFilter(user.get))).map { events =>

                                        val newForm = schedulingForm.fill(scheduleFormVals.copy(_1 = List[TimeRange](new TimeRange(currentStart, currentEnd))))
                                        scheduleMap += (new TimeRange(currentStart.minus(duration), currentStart) -> (events, newForm))
                                    }

                                } else {
                                    // check sample in the same way as above
                                    var conflictEvents = new ListBuffer[Event]
                                    
                                    var current = timeRange.copy(start = currentStart, end = Some(currentEnd), duration = duration)
                                    while (current.end.get.compareTo(recurrenceMeta.get.timeRange.end.get) <= 0) {

                                        val futureEvents = EventDAO.findAll($and(calQuery, "timeRange.start" $lte current.end.get, "timeRange.end" $gte current.start, Events.getEventFilter(user.get))).map { events =>
                                            conflictEvents.appendAll(events)
                                        }

                                        Await.ready(futureEvents, Duration(5000, MILLISECONDS))
                                        current = timeRange.copy(start = current.start.plus(recurrenceMeta.get.recurDuration), end = Some(current.end.get.plus(recurrenceMeta.get.recurDuration)))
                                    }

                                    val newForm = schedulingForm.fill(scheduleFormVals.copy(_1 = List[TimeRange](new TimeRange(currentStart.minus(duration), currentStart))))
                                    scheduleMap += (new TimeRange(currentStart.minus(duration), currentStart) -> (conflictEvents.toList, newForm))
                                }
                                
                                currentStart = currentEnd
                                currentEnd = currentEnd.plus(duration)
                            }
                        }

                    }
                }
                
                Await.ready(futureUser, Duration(10000, MILLISECONDS))

                Ok(views.html.scheduler(schedulingForm.fill(scheduleFormVals), Some(scheduleMap), userID))
            })
    }

    /**
     * Create an event and send requests based on query forms
     */
    def createEventAndRequests() = Action { implicit request =>
        val userID = AuthStateDAO.getUserID()
        
        schedulingForm.bindFromRequest.fold(
            errors => Ok(views.html.scheduler(errors, None, userID)),

            scheduleFormVals => {
                var newEvents = ListBuffer[Event]()

                var calendar = UserDAO.getFirstCalendarFromUserID(AuthStateDAO.getUserID())
                val newEvent = new Event(calendar = calendar, timeRange = scheduleFormVals._1, recurrenceMeta = scheduleFormVals._2, name = scheduleFormVals._4, description = scheduleFormVals._5)
                newEvents.append(newEvent)

                EventDAO.insert(newEvent)

                // if recurrence, recur
                if (newEvent.recurrenceMeta.isDefined) {
                    newEvents.appendAll(Events.createRecurrences(newEvent))
                }

                // create creation requests
                for (event <- newEvents.toList) {
                    for (entity <- scheduleFormVals._3.getOrElse(List.empty)) {
                        for (user <- GroupDAO.getUsersOfEntity(BSONObjectID.apply(entity))) {
                            CreationRequests.createCreationRequest(event._id, user.firstCalendar)
                        }
                    }
                }

                Redirect(routes.Events.showEvent(newEvent._id.stringify))
            })
    }
}