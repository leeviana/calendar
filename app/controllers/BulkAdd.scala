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

    def bulkAdd = Action.async { implicit request =>
        if (AuthStateDAO.isAuthenticated()) {
            val iterator = RecurrenceType.values.iterator

            UserDAO.findById(AuthStateDAO.getUserID()).map { user =>
                var calMap: MapBuffer[String, String] = MapBuffer()

                for (calID <- user.get.subscriptions) {
                    calMap += (calID.stringify -> CalendarDAO.getCalendarFromID(calID).name)
                }
                Ok(views.html.bulkAdd(BulkAddRequest.form,"",calMap)).withCookies(Cookie("calMap", Json.stringify(JsonConverter.mapToJson(calMap))));
            }
        } else {
            Future.successful(Redirect(routes.Application.index))
        }

    }

    def addEvents() = Action { implicit request =>

        BulkAddRequest.form.bindFromRequest.fold(
            errors => Ok(views.html.bulkAdd(errors, "", JsonConverter.jsonToMap(Json.parse(request.cookies.get("calMap").get.value)))),
              
            formdata => {
                val (data,err) = EventParser.parseText(formdata.data);
                if (err != "") {
                    Ok(views.html.bulkAdd(BulkAddRequest.form.fill(formdata), err, JsonConverter.jsonToMap(Json.parse(request.cookies.get("calMap").get.value))))
                } else {
                    val error_state = EventParser.validateDataset(data);
                    if (error_state != "") {
                        Ok(views.html.bulkAdd(BulkAddRequest.form.fill(formdata), error_state, JsonConverter.jsonToMap(Json.parse(request.cookies.get("calMap").get.value))))
                    } else {
                        var calendar = formdata.calendar;
                        for (line <- data) {
                            var name = line("name")(0);
                            var eventType = EventType.withName("Fixed");
                            var viewType: Option[ViewType.ViewType] = None;
                            var pudMeta: Option[PUDMeta] = None;
                            var signUpMeta: Option[SignUpMeta] = None;
                            if (line("type")(0).matches("pud")) {
                                eventType = EventType.withName("PUD");
                                viewType = None;
                                //if line("escalationvalue").isDefined {
                                    //currEvent.pudMeta = Some(new PUDMeta(line("priority")(0).toInt), ,line("escalationvalue")(0).toInt);
                                //} else {
                                if ((line.keySet.contains("priority"))) {
                                    pudMeta = Some(new PUDMeta(line("priority")(0).toInt));
                                }
                                //}
                            } else if (line("type")(0).matches("fixed")) {
                                eventType = EventType.withName("Fixed");
                                if ((line.keySet.contains("forpud")) && line("forpud")(0).matches("true")) {
                                    viewType = Some(ViewType.PUDEvent)
                                } else {
                                    viewType = None;
                                }
                            } else {
                                eventType = EventType.withName("SignUp");
                                viewType = None;
                                
                                var signupslots:List[SignUpSlot] = List();
                                var start = DateTime.parse(line("start")(0));
                                var end = Some(DateTime.parse(line("end")(0)));
                                signupslots = signupslots :+ new SignUpSlot(timeRange=new TimeRange(start,end, new JodaDuration(end.get.getMillis - start.getMillis)));
                                var counter = 2;
                                while (line.keySet contains "start"+counter) {
                                    counter = counter + 1;
                                }
                                var c = 2;
                                for (c <- 2 until counter) {
                                    var start = DateTime.parse(line("start"+c)(0));
                                    var end = Some(DateTime.parse(line("end"+c)(0)));
                                    signupslots = signupslots :+ new SignUpSlot(timeRange=new TimeRange(start,end, new JodaDuration(end.get.getMillis - start.getMillis)));
                                }
                                signUpMeta = Some(new SignUpMeta(
                                    signupslots,
                                    line("minduration")(0).toInt,
                                    line("maxslots")(0).toInt))
                            }
                            var description = Some("");
                            if (line.keySet contains "description") {
                                description = Some(line("description")(0));
                            }
                            // timerange list
                            var timerangelist: List[TimeRange] = List();
                            var start = DateTime.parse(line("start")(0));
                            var end = Some(DateTime.parse(line("end")(0)));
                            var duration = new JodaDuration(end.get.getMillis - start.getMillis)
                            if (eventType == EventType.withName("PUD")) {
                                if ((line.keySet.contains("expiretime"))) {
                                    end = Some(DateTime.parse(line("expiretime")(0))); // overrides end time (for PUDs)
                                }
                            }
                            timerangelist = timerangelist :+ new TimeRange(start,end, duration);
                            var counter = 2;
                            while (line.keySet contains "start"+counter) {
                                counter = counter + 1;
                            }
                            var c = 2;
                            for (c <- 2 until counter) {
                                var start = DateTime.parse(line("start"+c)(0));
                                var end = Some(DateTime.parse(line("end"+c)(0)));
                                timerangelist = timerangelist :+ new TimeRange(start, end, new JodaDuration(end.get.getMillis - start.getMillis))
                            }
                            var rules = List[Rule]();
                            var reminders = Some(List[Reminder]());
                            var accessType = Some(AccessType.Private);

                            var currEvent = new Event(
                                BSONObjectID.generate,
                                calendar,
                                timerangelist,
                                name,
                                description,
                                None,
                                rules,
                                reminders,
                                accessType=accessType,
                                eventType=eventType,
                                viewType=viewType,
                                pudMeta=pudMeta,
                                signUpMeta=signUpMeta);
                            EventDAO.insert(currEvent);
                        }
                        Redirect(routes.Events.index(EventType.Fixed.toString(),userID=""))
                    }
                }
            }
        )
    }
}
