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
import models.Event
import models.CreationRequest
import models.enums.CreationRequestStatus
import reactivemongo.bson.BSONObjectID
import apputils.EventDAO
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS
import apputils.AuthStateDAO
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import models.enums.EventType
import apputils.UserDAO

/**
 * @author Leevi
 */
object SlotSignUp extends Controller with MongoController {

    /**
     * Sign up for a free slot and create a slave event on your calendar
     */
    def signUpForSlot(eventID: String, slotID: String) = Action { implicit request =>
        val objectID = BSONObjectID(eventID)

        if(canSignUp(objectID, AuthStateDAO.getUserID())){
            val future = EventDAO.findById(objectID).map { event =>
                val signUpSlot = event.get.signUpSlots.get.filter { slot => slot._id == slotID }.head
                val signedUpSlot = signUpSlot.copy(userID = Some(AuthStateDAO.getUserID()))
    
                EventDAO.updateById(objectID, $pull("signUpSlots", Json.obj("_id" $eq slotID)))
                EventDAO.updateById(objectID, $push("signUpSlots", signedUpSlot))
            
                var calendar = UserDAO.getFirstCalendarFromUserID(AuthStateDAO.getUserID())
                val newEvent = new Event(calendar = calendar, timeRange = List[TimeRange](signUpSlot.timeRange), master = Some(event.get._id))
                EventDAO.insert(newEvent)
            }
            
            Await.ready(future, Duration(5000, MILLISECONDS))
        }   
        
        Redirect(routes.Events.index(EventType.Fixed.toString()))    
    }

    /**
     * Returns true if the user has not exceeded the maximum number of signups for the event
     */
    def canSignUp(eventID: BSONObjectID, userID: BSONObjectID): Boolean = {
        val future = EventDAO.findById(eventID).map { event =>
            var signUpSlots = event.get.signUpSlots.get

            var signedUp = signUpSlots.count { signUpSlot =>
                signUpSlot.userID.getOrElse(-1) == userID
            }

            signedUp < event.get.maxSlots.get
        }

        Await.result(future, Duration(5000, MILLISECONDS))
    }

    // Delete slot is handled in DeleteEvent
}