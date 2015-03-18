package controllers

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import reactivemongo.extensions.json.dsl.JsonDsl._
import play.modules.reactivemongo.json.BSONFormats._
import play.api.data.Form
import play.api.data.Forms._
import models.TimeRange
import models.CreationRequest
import models.enums.CreationRequestStatus
import reactivemongo.bson.BSONObjectID

/**
 * @author Leevi
 */
object Scheduling extends Controller with MongoController {
    /*
     * Render a page where the user can specify their "free time" query
     */
    def schedulingForm = Action { implicit request =>
        val schedulingForm = Form(
        tuple(
            "isRecurring" -> boolean, // recurring event or onetime event
            "timeRanges" -> list(TimeRange.form.mapping),
            "duration" -> optional(longNumber), // duration needed to schedule
            "recurrenceType" -> optional(nonEmptyText),
            "entities" -> optional(list(nonEmptyText)) // BSONObjectIDs
            ))
  
            Ok(views.html.scheduler(schedulingForm))     
    }
}