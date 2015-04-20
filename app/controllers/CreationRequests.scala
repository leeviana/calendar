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
import apputils.GroupDAO
import models.CreationRequest
import models.enums.CreationRequestStatus
import models.enums.ViewType
import apputils.CreationRequestDAO
import models.Event

/**
 * @author Leevi
 */
object CreationRequests extends Controller with MongoController {
  
    /**
     * Creates a creation request for an event with user friendly parameters
     */
    def createUserCreationRequest = Action.async { implicit request =>
        val requestMap = (request.body.asFormUrlEncoded)
        val eventID = BSONObjectID.apply(requestMap.get.get("eventID").get.head)
        val userEmail = requestMap.get.get("userEmail").get.head

        UserDAO.findOne("email" $eq userEmail).map { user =>
            if (user.isDefined) {
                createCreationRequest(eventID, user.get.subscriptions.head)
            }
            Redirect(routes.Events.showEvent(eventID.stringify))
        }
    }

    /**
     * Creates a creation request for an event with user friendly parameters for an entire group
     */
    def createGroupCreationRequest = Action.async { implicit request =>
        val requestMap = (request.body.asFormUrlEncoded)
        val eventID = BSONObjectID.apply(requestMap.get.get("eventID").get.head)
        val groupID = BSONObjectID.apply(requestMap.get.get("groupID").get.head)

        GroupDAO.findById(groupID).map { group =>
            if (group.isDefined) {
                group.get.userIDs.foreach { userID =>
                    UserDAO.findById(userID).map { user =>
                        createCreationRequest(eventID, user.get.subscriptions.head)
                    }
                }
            }
            Redirect(routes.Events.showEvent(eventID.stringify))
        }
    }

    /**
     * Creates a creation request for an event
     */
    def createCreationRequest(eventID: BSONObjectID, calendar: BSONObjectID) = {
        EventDAO.findById(eventID).map { event =>

            // else you're the master, don't do anything
            if (event.get.calendar != calendar) {
                // remove old events and creation requests
                EventDAO.findAndRemove(($and("master" $eq Some(eventID), "calendar" $eq calendar))).map { event =>
                    CreationRequestDAO.remove("eventID" $eq event.get._id)
                }

                val newEvent = event.get.copy(_id = BSONObjectID.generate, master = Some(eventID), calendar = calendar, eventType = EventType.Fixed, viewType = Some(ViewType.Request))
                val creationRequest = new CreationRequest(eventID = newEvent._id, master = eventID, requestStatus = CreationRequestStatus.Pending)
                EventDAO.insert(newEvent)
                CreationRequestDAO.insert(creationRequest)
            }
        }
    }

    /**
     * Creates a creation request back to the master event
     */
    def createMasterRequest(newEvent: Event, masterID: BSONObjectID) {
        EventDAO.findById(masterID).map { master =>
            UserDAO.findOne("subscriptions" $all (master.get.calendar)).map { user =>
                val newMasterEvent = newEvent.copy(_id = BSONObjectID.generate, master = Some(master.get._id), calendar = user.get.subscriptions.head, eventType = EventType.Fixed, viewType = Some(ViewType.Request))
                val creationRequest = new CreationRequest(eventID = newEvent._id, master = master.get._id, requestStatus = CreationRequestStatus.Pending)
                EventDAO.insert(newMasterEvent)
                CreationRequestDAO.insert(creationRequest)
            }
        }
    }

    /**
     * Updates the status of a creation request
     */
    def updateCreationStatus(eventID: String, newStatus: String) = Action.async { implicit request =>

        EventDAO.findById(BSONObjectID.apply(eventID)).map { event =>

            val query = $and("master" $eq event.get.master.get, "eventID" $eq event.get._id)

            EventDAO.findById(event.get.master.get).map { master =>
                // if you are the owner of the master event
                if (master.get.calendar == event.get.calendar) {

                    val future = CreationRequestDAO.remove("master" $eq event.get.master.get)
                    Await.ready(future, Duration(5000, MILLISECONDS))

                    if (newStatus == CreationRequestStatus.Confirmed.toString) {
                        val newMaster = event.get.copy(_id = master.get._id, master = None, viewType = Some(ViewType.Confirmed))
                        val future = EventDAO.save(newMaster)

                        Await.ready(future, Duration(5000, MILLISECONDS))

                        // createCreationRequests for all events with master as this event
                        EventDAO.findAll($and("master" $eq master.get._id, "_id" $ne event.get._id)).map { eventList =>
                            eventList.map { event =>
                                createCreationRequest(master.get._id, event.calendar)
                            }
                        }

                        // remove all old events
                        EventDAO.remove("master" $eq master.get._id)
                    } else if (newStatus == CreationRequestStatus.Declined.toString()) {
                        EventDAO.removeById(event.get._id)
                    }
                } // if you are just a sharee
                else {
                    val update = $set("requestStatus" -> newStatus)
                    CreationRequestDAO.update(query, update)
                    EventDAO.updateById(event.get._id, $set("viewType" -> newStatus))
                }
            }

            Redirect(routes.Events.index(EventType.Fixed.toString(),userID=""))
        }
    }
}