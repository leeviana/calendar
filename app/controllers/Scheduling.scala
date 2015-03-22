package controllers

import models.Event
import models.TimeRange
import play.api.data.Form
import play.api.data.Forms.boolean
import play.api.data.Forms.list
import play.api.data.Forms.longNumber
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.optional
import play.api.data.Forms.tuple
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

/**
 * @author Leevi
 */
object Scheduling extends Controller with MongoController {
    val schedulingForm = Form(
        tuple(
            "isRecurring" -> boolean, // recurring event or onetime event
            "timeRanges" -> list(TimeRange.form.mapping),
            "duration" -> optional(longNumber), // duration needed to schedule
            "recurrenceType" -> optional(nonEmptyText),
            "entities" -> optional(list(nonEmptyText)) // BSONObjectIDs
        ))
            
    /**
     * Render a page where the user can specify their "free time" query
     */
    def showForm = Action { implicit request =>
        Ok(views.html.scheduler(schedulingForm, None))     
    }
    
    /**
     * Render a page with the returned data from the "free time" query
     */
    def schedulingOptions = Action(parse.multipartFormData) { implicit request =>
        schedulingForm.bindFromRequest.fold(
            errors => Ok(views.html.scheduler(errors, None)),
            
            scheduleFormVals => {
                // Form values    
                val isRecurring = scheduleFormVals._1
                val timeRanges = scheduleFormVals._2
                val duration = scheduleFormVals._3.getOrElse(None)
                val recurrenceType = scheduleFormVals._4.getOrElse(None)
                val entities = scheduleFormVals._5.getOrElse(List.empty)
                
                var scheduleMap = Map[TimeRange, List[Event]]()
                
                for(timeRange <- timeRanges){
                    // get user's calendars
                    val calendars = ListBuffer[BSONObjectID]()
                    
                    for(entity <- entities){
                        val users = GroupDAO.getUsersOfEntity(BSONObjectID.apply(entity))
                        users.foreach { user => 
                            calendars.appendAll(user.subscriptions)
                        }
                    }
                   
                    if(!isRecurring){
                        // query for conflicting events
                        val conflicts = List[Event]()
                        
                        EventDAO.findAll($and("calendar" $in calendars, "timeRange.start" $lte timeRange.end, "timeRange.end" $gte timeRange.start)).map { events => 
                            scheduleMap += (timeRange -> events)
                        }
                    }
                    else {
                        print("TODO: generate recurrence conflict data")       
                    }
                }
                
                Ok(views.html.scheduler(schedulingForm, Some(scheduleMap)))
            }
        )
    }
}