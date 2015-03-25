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
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS

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
            "duration" -> number))

    /**
     * Render a page where the user can specify their "free time" query
     */
    def showForm = Action { implicit request =>
        var entities = getEntities
        
        Ok(views.html.scheduler(schedulingForm, None, entities))
    }

    /**
     * Render a page with the returned data from the "free time" query
     */
    // TODO: use duration to break timeRange into multiple timeranges
    def schedulingOptions = Action(parse.multipartFormData) { implicit request =>
        var entities = getEntities
        
        schedulingForm.bindFromRequest.fold(
            errors => Ok(views.html.scheduler(errors, None, entities)),

            scheduleFormVals => {
                
                // Form values    
                val timeRanges = scheduleFormVals._1
                val recurrenceMeta = scheduleFormVals._2
                val entities = scheduleFormVals._3.getOrElse(List.empty)
                val duration = scheduleFormVals._6

                var scheduleMap = Map[TimeRange, (List[Event], Form[(List[TimeRange], Option[RecurrenceMeta], Option[List[String]], String, Option[String], Int)])]()

                for (timeRange <- timeRanges) {
                    // get user's calendars
                    val calendars = ListBuffer[BSONObjectID]()

                    for (entity <- entities) {
                        val users = GroupDAO.getUsersOfEntity(BSONObjectID.apply(entity))
                        users.foreach { user =>
                            calendars.appendAll(user.subscriptions)
                        }
                    }

                    // returns conflicting events (that you have at least view access to)
                    val futureUser = UserDAO.findById(AuthStateDAO.getUserID()).map { user =>
                        if (!recurrenceMeta.isDefined) {
                            EventDAO.findAll($and("calendar" $in calendars, "timeRange.start" $lte timeRange.end, "timeRange.end" $gte timeRange.start, Events.getEventFilter(user.get))).map { events =>

                                val newForm = schedulingForm.fill(scheduleFormVals.copy(_1 = List[TimeRange](timeRange)))
                                scheduleMap += (timeRange -> (events, newForm))
                            }

                        } else {
                            // check sample in the same way as above
                            var conflictEvents = new ListBuffer[Event]
                            var current = timeRange.copy(start = timeRange.start, end = timeRange.end, duration = timeRange.duration)
                            while (current.end.get.compareTo(recurrenceMeta.get.timeRange.end.get) <= 0) {

                                // TODO: check if this code works without using futures
                                EventDAO.findAll($and("calendar" $in calendars, "timeRange.start" $lte current.end, "timeRange.end" $gte current.start, Events.getEventFilter(user.get))).map { events =>
                                    conflictEvents.appendAll(events)
                                }

                                current = timeRange.copy(start = timeRange.start.plus(recurrenceMeta.get.recurDuration), end = Some(timeRange.end.get.plus(recurrenceMeta.get.recurDuration)))
                            }

                            val newForm = schedulingForm.fill(scheduleFormVals.copy(_1 = List[TimeRange](timeRange)))
                            scheduleMap += (timeRange -> (conflictEvents.toList, newForm))
                        }
                    }
                }

                Ok(views.html.scheduler(schedulingForm, Some(scheduleMap), entities))
            })
    }

    /**
     * Create an event and send requests based on query forms
     */
    def createEventAndRequests() = Action { implicit request =>
        var entities = getEntities
        
        schedulingForm.bindFromRequest.fold(
            errors => Ok(views.html.scheduler(errors, None, entities)),

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
                            Events.createCreationRequest(event._id, user.firstCalendar)
                        }
                    }
                }

                Redirect(routes.Events.showEvent(newEvent._id.stringify))
            })
    }
    
    def getEntities : List[String] = {
        var temp:  List[models.User] = List()          
        val future = UserDAO.findAll().map { users =>
            temp = users;
        }
        Await.ready(future, Duration(5000, MILLISECONDS))
        var temp2: List[models.Group] = List()
        val future2 = GroupDAO.findAll().map { groups =>
            temp2 = groups
        }
        Await.ready(future2, Duration(5000, MILLISECONDS))
        var entities: List[String] = List()
        Console.println("check")
        for(t <- temp){
          Console.println("user")
          entities = entities :+ t.username
        }
        for(t <- temp2){
          Console.println("group")
          entities = entities :+ t.name
        }
        for(e <- entities){
          Console.println("is there anything in this list?")
        }
        return entities
      
    }
    
}