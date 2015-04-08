package controllers

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS

import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import apputils.AuthStateDAO
import apputils.EventDAO
import apputils.UserDAO
import models.enums.EventType
import models.TimeRange
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import reactivemongo.bson.BSONObjectID
import reactivemongo.extensions.json.dsl.JsonDsl._
import reactivemongo.extensions.json.dsl.JsonDsl.$push
import reactivemongo.extensions.json.dsl.JsonDsl.ElementBuilderLike
import reactivemongo.extensions.json.dsl.JsonDsl.toElement

/**
 * @author Leevi
 */
object SlotSignUp extends Controller with MongoController {

    /**
     * Sign up for a free slot and create a slave event on your calendar
     */
    def signUpForSlot(eventID: String, slotID: String) = Action.async { implicit request =>
        val objectID = BSONObjectID(eventID)
        val slotObjectID = BSONObjectID(slotID)
        
        EventDAO.findById(objectID).map { event =>
            val signUpSlot = event.get.signUpMeta.get.signUpSlots.filter { slot => slot._id == slotObjectID}.head
            val signedUpSlot = signUpSlot.copy(userID = Some(AuthStateDAO.getUserID()))
            
            EventDAO.updateById(objectID, $pull("signUpMeta.signUpSlots", Json.obj("_id" $eq slotObjectID)))
            EventDAO.updateById(objectID, $push("signUpMeta.signUpSlots", signedUpSlot)) 
            
            // TODO: To update while retaining order, use following tactic
         /* val newSignUpSlots = master.get.signUpSlots.get.map { signUpSlot => 
                if(signUpSlot.timeRange.start == oldEvent.get.getFirstTimeRange().start) {
                    signUpSlot.copy(userID = None)
                } else {
                    signUpSlot
                } 
            }
            EventDAO.save(master.get.copy(signUpSlots = Some(newSignUpSlots))) */

            
            val calendar = UserDAO.getFirstCalendarFromUserID(AuthStateDAO.getUserID())
            
            val newEvent = event.get.copy(_id = BSONObjectID.generate, calendar = calendar, timeRange = List[TimeRange](signUpSlot.timeRange), master = Some(event.get._id), eventType = EventType.Fixed)
            EventDAO.insert(newEvent)
            
            Redirect(routes.Events.index(EventType.Fixed.toString()))  
        }
    }

    /**
     * Returns true if the user has not exceeded the maximum number of signups for the event
     */
    def canSignUp(eventID: BSONObjectID, userID: BSONObjectID): Boolean = {
        val future = EventDAO.findById(eventID).map { event =>
            val signUpSlots = event.get.signUpMeta.get.signUpSlots

            val signedUp = signUpSlots.count { signUpSlot =>
                signUpSlot.userID.getOrElse(-1) == userID
            }

            signedUp < event.get.signUpMeta.get.maxSlots
        }

        Await.result(future, Duration(5000, MILLISECONDS))
    }

    // Delete slot is handled in DeleteEvent
}