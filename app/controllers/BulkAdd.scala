package controllers

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{ Map => MapBuffer }
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS

import org.joda.time.DateTime
import org.joda.time.{ Duration => JodaDuration }
import org.joda.time.Period

import apputils._
import models._
import models.enums._
import play.api.data.Form
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Cookie
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import reactivemongo.bson.BSONObjectID
import reactivemongo.extensions.json.dsl.JsonDsl._

object BulkAdd extends Controller with MongoController {

    def bulkAdd = Action {
        Ok(views.html.bulkAdd(BulkAddRequest.form))
    }

	def addEvents() = Action { implicit request =>
        BulkAddRequest.form.bindFromRequest.fold(
            errors => Ok(views.html.bulkAdd(errors)),
              
            formdata => {
                val test = EventParser.parseText(formdata.data);
                Redirect(routes.Events.index(EventType.Fixed.toString()))
            }
        )
	}

    def editEvent(eventID: String) = Action.async { implicit request =>
        val iterator = RecurrenceType.values.iterator

        EventDAO.findById(BSONObjectID(eventID)).map { oldEvent =>
            Event.form.bindFromRequest.fold(
                errors => {
                    Ok(views.html.editEvent(Some(oldEvent.get._id.stringify), errors, iterator, JsonConverter.jsonToMap(Json.parse(request.cookies.get("calMap").get.value))));
                },

                event => {
                    if (!oldEvent.get.master.isDefined & oldEvent.get.master != oldEvent.get._id) {
                        val newEvent = event.copy(_id = BSONObjectID(eventID))
                        EventDAO.save(newEvent)

                        CreationRequestDAO.update($and("master" $eq oldEvent.get._id, "requestStatus" $ne CreationRequestStatus.Removed.toString()), $set("requestStatus" -> CreationRequestStatus.Pending.toString()))

                        // updates all slave events that are not on your own calendar (which are pending master requests)
                        EventDAO.findAll($and("master" $eq oldEvent.get._id, "calendar" $ne oldEvent.get.calendar)).map { slaveEvents =>
                            slaveEvents.map { slaveEvent =>
                                val updatedEvent = event.copy(_id = slaveEvent._id, calendar = slaveEvent.calendar, master = slaveEvent.master, rules = slaveEvent.rules, viewType = Some(ViewType.Request))
                                EventDAO.save(updatedEvent)
                            }
                        }
                        Redirect(routes.Events.index(newEvent.eventType.toString()))
                    } else {
                        // if event master is not the master
                        CreationRequests.createMasterRequest(event, oldEvent.get.master.get)
                        Redirect(routes.Events.index(event.eventType.toString()))
                    }
                })
        }
    }
}
