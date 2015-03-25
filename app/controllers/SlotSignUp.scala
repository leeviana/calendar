package controllers

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS

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
import reactivemongo.extensions.json.dsl.JsonDsl.$pull
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
        //if(canSignUp(objectID, AuthStateDAO.getUserID())){
            EventDAO.findById(objectID).map { event =>
                val signUpSlot = event.get.signUpSlots.get.filter { slot => slot._id.stringify == slotID}.head
                
                val signedUpSlot = signUpSlot.copy(userID = Some(AuthStateDAO.getUserID()))
                print(signedUpSlot._id + ", " + signedUpSlot.userID.get + "\n") 
                EventDAO.updateById(objectID, $pull("signUpSlots", Json.obj("_id" $eq slotID)))
                EventDAO.updateById(objectID, $push("signUpSlots", signedUpSlot))
                val calendar = UserDAO.getFirstCalendarFromUserID(AuthStateDAO.getUserID())
                val newEvent = event.get.copy(calendar = calendar, timeRange = List[TimeRange](signUpSlot.timeRange), master = Some(event.get._id), eventType = EventType.Fixed, signUpSlots = None)
                EventDAO.insert(newEvent)
                
                Redirect(routes.Events.index(EventType.Fixed.toString()))  
            }
            
            //Await.ready(future, Duration(5000, MILLISECONDS))
        //}   
          
    }

    /**
     * Returns true if the user has not exceeded the maximum number of signups for the event
     */
    def canSignUp(eventID: BSONObjectID, userID: BSONObjectID): Boolean = {
        val future = EventDAO.findById(eventID).map { event =>
            val signUpSlots = event.get.signUpSlots.get

            val signedUp = signUpSlots.count { signUpSlot =>
                signUpSlot.userID.getOrElse(-1) == userID
            }

            signedUp < event.get.maxSlots.get
        }

        Await.result(future, Duration(5000, MILLISECONDS))
    }

    // Delete slot is handled in DeleteEvent
}