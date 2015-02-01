package controllers

import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONArray
import reactivemongo.bson.BSONObjectID
import org.joda.time.DateTime
    
/**
 * The Users controllers encapsulates the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
 * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
 * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
 * @author Leevi
 */
object Events extends Controller with MongoController {
    // def collection: JSONCollection = db.collection[JSONCollection]("events")
    val collection = db[BSONCollection]("events")

        
    def index = Action.async { implicit request =>         
        val query = BSONDocument(
        "$query" -> BSONDocument())
    
        val found = collection.find(query).cursor[Event]
        
        found.collect[List]().map { events =>
            Ok(views.html.events(events))
        }
    }
    
    def showCreationForm = Action {
        Ok(views.html.editEvent(Event.form))
    }
    
    def create = Action { implicit request =>
        Event.form.bindFromRequest.fold(
            errors => Ok(views.html.editEvent(errors)),
            
            event => {
                //val timeRange = new TimeRange(false, new DateTime(), Some(new DateTime()))
                val updatedEvent = event.copy(calendar = BSONObjectID.generate, rules = BSONArray.empty)
                collection.insert(updatedEvent)
                Redirect(routes.Events.index())
            }
        )
    }
}