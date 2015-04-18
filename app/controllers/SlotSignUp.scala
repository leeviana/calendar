package controllers

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS
import apputils.AuthStateDAO
import apputils.EventDAO
import apputils.UserDAO
import models.Event
import models.Reminder
import models.Rule
import models.SignUpMeta
import models.SignUpPreferences
import models.TimeRange
import models.User
import models.UserSignUpOption
import models.enums.EventType
import models.enums.ViewType
import play.api.data._
import play.api.data.Form
import play.api.data.Forms.list
import play.api.data.Forms.number
import play.api.data.Forms.tuple
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import reactivemongo.bson.BSONObjectID
import reactivemongo.extensions.json.dsl.JsonDsl._
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import models.SignUpSlot
import scala.collection.mutable.HashSet

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
            val signUpSlot = event.get.signUpMeta.get.signUpSlots.filter { slot => slot._id == slotObjectID }.head

            // Update while retaining order
            val newSignUpSlots = event.get.signUpMeta.get.signUpSlots.map { slot =>
                if (slot._id == slotObjectID) {
                    slot.copy(userID = Some(AuthStateDAO.getUserID()))
                } else {
                    slot
                }
            }
            val newSignUpMeta = event.get.signUpMeta.get.copy(signUpSlots = newSignUpSlots)
            EventDAO.save(event.get.copy(signUpMeta = Some(newSignUpMeta)))

            createSlave(event.get, signUpSlot, AuthStateDAO.getUserID())
            
            Redirect(routes.Events.index(EventType.Fixed.toString()))
        }
    }
    
    /**
     * create sign up slave event
     */
    def createSlave(masterEvent: Event, signUpSlot: SignUpSlot, userID: BSONObjectID) {
        val calendar = UserDAO.getFirstCalendarFromUserID(userID)
        
        val newEvent = masterEvent.copy(_id = BSONObjectID.generate, calendar = calendar, timeRange = List[TimeRange](signUpSlot.timeRange), master = Some(masterEvent._id), eventType = EventType.Fixed)
        EventDAO.insert(newEvent)
    }

    /**
     * Indicate your sign up slot preferences
     */
    def indicatePreferences(eventID: String) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)
        EventDAO.findById(objectID).map { event =>
            SignUpPreferences.form.bindFromRequest.fold(
                errors => Ok(views.html.EventInfo(event.get, Reminder.form, Rule.form, AuthStateDAO.getUserID().stringify, Json.parse(request.cookies.get("userList").get.value).as[List[models.User]])),

                signUpPreferences => {
                    // TODO: edit this so that if old entries are in the table, they are removed and the corresponding events are deleted
                    val calendar = UserDAO.getFirstCalendarFromUserID(AuthStateDAO.getUserID())
                    val preferences = signUpPreferences.preferences
                    print("BLAH" + preferences + "\n")
                    var tentativeEvents = ListBuffer[Event]()

                    val newSignUpSlots = event.get.signUpMeta.get.signUpSlots.zipWithIndex.map {
                        case (slot, index) =>
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

                    // TODO: if PUD, check to see if PUD exists with this as master and calendar = calendar. delete it.

                    Redirect(routes.Events.index(EventType.SignUp.toString()))
                })
        }
    }

    def getPreferenceForm(eventID: String): Form[SignUpPreferences] = {
        val future = EventDAO.findById(BSONObjectID.apply(eventID))
        val event = Await.result(future, Duration(5000, MILLISECONDS))

        val size = event.get.signUpMeta.get.signUpSlots.length

        SignUpPreferences.form.fill(new SignUpPreferences(size, List.fill(size)(0)))
    }

    def signUpDetermination(eventID: String) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)
        
        EventDAO.findById(objectID).map { event =>
            val signUpInfo = event.get.signUpMeta.get
            val maxSlots = signUpInfo.maxSlots

            var hashSet = HashSet[BSONObjectID]() // list of users
            signUpInfo.signUpSlots.foreach { slot =>
                slot.userOptions.get.foreach { userOption =>
                    userOption.userID
                }
            }

            // multiple entries for "max slots". is this logical?
            var userList = new ListBuffer[BSONObjectID]()
            for(i <- 1 to maxSlots) {
               userList.appendAll(hashSet) 
            }
            
            var tempSignUpSlots = signUpInfo.signUpSlots.toBuffer
            var minNum = 1
            var validSlots = true

            // if there are unassigned users and valid slots left
            while (!userList.isEmpty & validSlots) {
                var processing = true
                validSlots = false

                // while slots are still being assigned, keep going
                while (processing) {
                    processing = false
                    // assigns slots with minNum of options filled
                    tempSignUpSlots = tempSignUpSlots.map { slot =>

                        // calculate valid userOptions using userList
                        var userOptions = slot.userOptions.get.filter { option => userList.contains(option.userID) }

                        // if nobody is assigned to it and possible userOptions
                        if (slot.userID.isEmpty & userOptions.size > 0) {
                            validSlots = true

                            if (userOptions.size <= minNum) {
                                // assign slot to person with minimum preference number
                                val userOption = userOptions.minBy { _.preference }
                                processing = true
                                minNum = 1

                                userList.remove(userList.indexOf(userOption.userID))

                                slot.copy(userID = Some(userOption.userID))
                            } else {
                                slot
                            }

                        } else {
                            slot
                        }
                    }
                }

                minNum += 1
            }

            // new sign up info with new slots and cleared determination data.
            val newSignUpInfo = new SignUpMeta(signUpSlots = tempSignUpSlots.toList, minSignUpSlotDuration = signUpInfo.minSignUpSlotDuration, maxSlots = signUpInfo.maxSlots)

            Ok(views.html.signUpResolution(SignUpMeta.form.fill(signUpInfo), eventID))

         }
    }

    def resolveSlots(eventID: String) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)
        EventDAO.findById(objectID).map { event =>

            SignUpMeta.form.bindFromRequest.fold(
                errors => Ok(views.html.signUpResolution(errors, eventID)),

                signUpMeta => {
                    val newEvent = event.get.copy(signUpMeta = Some(signUpMeta))
                    EventDAO.save(newEvent)
                    
                    // delete slave events
                    EventDAO.findAndRemove("master" $eq event.get._id)
                    
                    // make new slave events
                    for(signUpSlot <- signUpMeta.signUpSlots) {
                        if(signUpSlot.userID.isDefined) {
                             createSlave(event.get, signUpSlot, signUpSlot.userID.get)
                        }
                    }
                    
                    Redirect(routes.Events.showEvent(eventID))
                })
        }
    }

    // Delete slot is handled in DeleteEvent
}