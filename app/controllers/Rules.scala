package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import reactivemongo.extensions.json.dsl.JsonDsl._
import models.CreationRequest
import models.Reminder
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.MongoController
import apputils.AuthStateDAO
import apputils.EventDAO
import apputils.GroupDAO
import models.Event
import play.mvc.Results.Redirect
import models.enums.AccessType
import models.Rule
import play.api.mvc.Controller
import play.api.mvc.Action
import play.api.libs.json.Json
import models.enums.EventType
import models.TimeRange
import models.PUDMeta
import org.joda.time.DateTime
import org.joda.time.Duration

/**
 * @author Leevi
 */
object Rules extends Controller with MongoController {

    /**
     * Adds a new rule to an Event
     * TODO: May want to do this for a calendar too in the future
     * @param ID of the event that rule is to be added to
     */
    def addRule(eventID: String) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)

        EventDAO.findById(objectID).map { event =>
            Rule.form.bindFromRequest.fold(
                errors => Ok(views.html.EventInfo(event.get, Reminder.form, errors, AuthStateDAO.getUserID().stringify, Json.parse(request.cookies.get("userList").get.value).as[List[models.User]])),

                rule => {
                    // if sharing a signupevent with determination time and PUD generation, create PUDs
                    if(event.get.eventType == EventType.SignUp) {
                        val signUpMeta = event.get.signUpMeta.get
                        if(signUpMeta.prefDeterminationTime.isDefined) {
                                if(signUpMeta.createPUD.getOrElse(false)) {
                                    val userIDs = GroupDAO.getUsersOfEntity(rule.entityID)
                                    for (user <- userIDs) {
                                        createSignUpPUD(objectID, user.firstCalendar, signUpMeta.signUpPUDPriority.get)
                                    }
                                }
                        }
                    }    
                    EventDAO.updateById(objectID, $push("rules", rule))
                    EventDAO.updateById(objectID, $set("accessType" -> AccessType.Private.toString()))
                    Redirect(routes.Events.showEvent(eventID))
                })
        }
    }

    /**
     * Creates a PUD for a SignUp Event
     */
    def createSignUpPUD(eventID: BSONObjectID, calendar: BSONObjectID, priority: Int) = {
        EventDAO.findById(eventID).map { event =>

            // if there isn't already a related PUD, make one
            EventDAO.findOne(($and("master" $eq Some(eventID), "calendar" $eq calendar, "eventType" $eq EventType.PUD))).map { pudevent =>
              if(!pudevent.isDefined) {
                    val PUDMeta = new PUDMeta(priority = priority)
                    val timeRange = new TimeRange(start = DateTime.now(), end = Some(event.get.signUpMeta.get.prefDeterminationTime.get), duration = Duration.standardMinutes(10))
                    val newEvent = new Event(_id = BSONObjectID.generate, name = "Sign up for " + event.get.name, master = Some(eventID), calendar = calendar, eventType = EventType.PUD, pudMeta = Some(PUDMeta), timeRange = List[TimeRange](timeRange))
              
                    EventDAO.insert(newEvent)
                }
            }
        }
    }
    
    /**
     * Deletes a rule
     */
    def deleteRule(eventID: String, ruleID: String) = Action { implicit request =>
        val objectID = BSONObjectID.apply(eventID)

        EventDAO.updateById(objectID, $pull("rules", Json.obj("orderNum" $eq ruleID.toInt)))
        EventDAO.findById(objectID).map { event =>
            var rules = event.get.rules.toBuffer
            for (x <- rules.length - 1 to 0 by -1) {
                if (x >= ruleID.toInt) {
                    val newRule1 = new Rule(rules(x).orderNum - 1, rules(x).entityType, rules(x).entityID, rules(x).accessType)

                    EventDAO.updateById(objectID, $pull("rules", rules(x)))
                    EventDAO.updateById(objectID, $push("rules", newRule1))
                    EventDAO.updateById(objectID, $set("accessType" -> AccessType.Private.toString()))
                }
            }
        }

        Redirect(routes.Events.showEvent(eventID))
    }

    /**
     * Confirmation page before actually deleting a rule
     */
    def confirmDeleteRule(eventID: String, ruleID: String) = Action {
        Ok(viewComponents.html.confirmDeleteRule(eventID, Event.form, ruleID))
    }

    /**
     * Moves around rules, depending on the rule and the direction of movement
     */
    def moveRule(eventID: String, ruleID: String, dir: String) = Action.async { implicit request =>
        val objectID = BSONObjectID.apply(eventID)

        EventDAO.findById(objectID).map { event =>
            var adjustment = 0
            if (dir.equals("up"))
                adjustment = 1
            else if (dir.equals("down"))
                adjustment = -1

            var rules = event.get.rules.toBuffer

            if (ruleID.toInt <= 0 && dir.equals("up") | ruleID.toInt >= rules.length - 1 && dir.equals("down")) {
                Redirect(routes.Events.showEvent(eventID))
            } else {
                var one = 0;
                var two = 0;
                for (x <- 0 to rules.length - 1) {
                    if (rules(x).orderNum == ruleID.toInt) {
                        one = x;
                    } else if (rules(x).orderNum == ruleID.toInt - adjustment) {
                        two = x;
                    }
                }

                val newRule1 = new Rule(rules(one).orderNum - adjustment, rules(one).entityType, rules(one).entityID, rules(one).accessType)
                val newRule2 = new Rule(rules(two).orderNum + adjustment, rules(two).entityType, rules(two).entityID, rules(two).accessType)

                EventDAO.updateById(objectID, $pull("rules", rules(one)))
                EventDAO.updateById(objectID, $pull("rules", rules(two)))
                EventDAO.updateById(objectID, $push("rules", newRule1))
                EventDAO.updateById(objectID, $push("rules", newRule2))
                EventDAO.updateById(objectID, $set("accessType" -> AccessType.Private.toString()))
            }
            
            Redirect(routes.Events.showEvent(eventID))
        }
    }
}