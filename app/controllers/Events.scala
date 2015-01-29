package controllers

import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.Future
import reactivemongo.api.Cursor
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.libs.json._

/**
 * The Users controllers encapsulates the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
 * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
 * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
 * @author Leevi
 */
class Events extends Controller with MongoController {
    def collection: JSONCollection = db.collection[JSONCollection]("events")

    import models._

//    def createEvent = Action.async(parse.json) {
//        request =>
//         /*
//         * request.body is a JsValue.
//         * There is an implicit Writes that turns this JsValue as a JsObject,
//         * so you can call insert() with this JsValue.
//         * (insert() takes a JsObject as parameter, or anything that can be
//         * turned into a JsObject using a Writes.)
//         */
//            request.body.validate[Event].map {
//                event =>
//                    // 'event' is an instance of the case class 'models.Event'
//                    collection.insert(event).map {
//                        lastError =>
//                            // logger.debug(s"Successfully inserted with LastError: $lastError")
//                            Created(s"User Created")
//                    }
//            }.getOrElse(Future.successful(BadRequest("invalid json")))
//    }
//
//    def updateEvent(firstName: String, lastName: String) = Action.async(parse.json) {
//        request =>
//            request.body.validate[Event].map {
//                event =>
//                    // find our user by first name and last name
//                    val nameSelector = Json.obj("firstName" -> firstName, "lastName" -> lastName)
//                    collection.update(nameSelector, event).map {
//                        lastError =>
//                            // logger.debug(s"Successfully updated with LastError: $lastError")
//                            Created(s"User Updated")
//                    }
//            }.getOrElse(Future.successful(BadRequest("invalid json")))
//    }
//
//    def findEvents = Action.async {
//        // let's do our query
//        val cursor: Cursor[Event] = collection.
//            // find all
//            find(Json.obj("active" -> true)).
//            // sort them by creation date
//            sort(Json.obj("created" -> -1)).
//            // perform the query and get a cursor of JsObject
//            cursor[Event]
//
//        // gather all the JsObjects in a list
//        val futureEventsList: Future[List[Event]] = cursor.collect[List]()
//
//        // transform the list into a JsArray
//        val futureEventsJsonArray: Future[JsArray] = futureEventsList.map { events =>
//            Json.arr(events)
//        }
//        // everything's ok! Let's reply with the array
//        futureEventsJsonArray.map {
//            events =>
//                Ok(events(0))
//        }
//    }
}