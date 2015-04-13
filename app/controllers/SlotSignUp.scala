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
import play.api.data.Forms.tuple
import play.api.data.Forms.list
import play.api.data.Forms.number
import play.api.data.Form
import play.api.data._
import models.SignUpPreferences
import models.Rule
import models.Reminder
import models.UserSignUpOption
import scala.collection.mutable.ListBuffer
import models.Event
import models.enums.ViewType

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

            // Update while retaining order
             val newSignUpSlots = event.get.signUpMeta.get.signUpSlots.map { slot => 
                if(slot._id == slotObjectID) {
                    slot.copy(userID = Some(AuthStateDAO.getUserID()))
                } else {
                    slot
                } 
            }
            val newSignUpMeta = event.get.signUpMeta.get.copy(signUpSlots = newSignUpSlots)
            EventDAO.save(event.get.copy(signUpMeta = Some(newSignUpMeta)))

            val calendar = UserDAO.getFirstCalendarFromUserID(AuthStateDAO.getUserID())
            
            val newEvent = event.get.copy(_id = BSONObjectID.generate, calendar = calendar, timeRange = List[TimeRange](signUpSlot.timeRange), master = Some(event.get._id), eventType = EventType.Fixed)
            EventDAO.insert(newEvent)
            
            Redirect(routes.Events.index(EventType.Fixed.toString()))  
        }
    }

    /**
     * Indicate your sign up slot preferences
     */
    def indicatePreferences(eventID: String) = Action.async(parse.multipartFormData) { implicit request =>
        val objectID = BSONObjectID(eventID)
        EventDAO.findById(objectID).map { event =>
            SignUpPreferences.form.bindFromRequest.fold(
                errors => Ok(views.html.EventInfo(event.get, Reminder.form, Rule.form, AuthStateDAO.getUserID().stringify, Json.parse(request.cookies.get("userList").get.value).as[List[models.User]])),
                    
                signUpPreferences => {
                        // TODO: edit this so that if old entries are in the table, they are removed and the corresponding events are deleted
                        val calendar = UserDAO.getFirstCalendarFromUserID(AuthStateDAO.getUserID())
                        val preferences = signUpPreferences.preferences
                        var tentativeEvents = ListBuffer[Event]()
                        
                        val newSignUpSlots = event.get.signUpMeta.get.signUpSlots.zipWithIndex.map { case (slot, index) => 
                            val options = slot.userOptions.getOrElse(List[UserSignUpOption]())
                            val newOptions = options.+:(new UserSignUpOption(AuthStateDAO.getUserID(), preferences(index)))
                            
                            val newEvent = event.get.copy(_id = BSONObjectID.generate, calendar = calendar, timeRange = List[TimeRange](slot.timeRange), master = Some(event.get._id), eventType = EventType.Fixed, viewType = Some(ViewType.SignUpPossibility))
                            tentativeEvents.append(newEvent)
                            
                            slot.copy(userOptions = Some(newOptions))
                        }
                        
                        val newSignUpMeta = event.get.signUpMeta.get.copy(signUpSlots = newSignUpSlots)
                        EventDAO.save(event.get.copy(signUpMeta = Some(newSignUpMeta)))
            
                        // add new SignUpPossibilityEvents
                        EventDAO.bulkInsert(tentativeEvents)
                        
                        // if PUD, check to see if PUD exists with this as master and calendar = calendar. delete it.
                        
                        Redirect(routes.Events.index(EventType.SignUp.toString()))  
                    }
                
            ) 
        }
    }
    
    def getPreferenceForm(eventID: String): Form[SignUpPreferences] = {
        val future = EventDAO.findById(BSONObjectID.apply(eventID))
        val event = Await.result(future, Duration(5000, MILLISECONDS))
        
        val size = event.get.signUpMeta.get.signUpSlots.length
        
        SignUpPreferences.form.fill(new SignUpPreferences(size, List.fill(size)(0)))
    }
    
    // Delete slot is handled in DeleteEvent
}