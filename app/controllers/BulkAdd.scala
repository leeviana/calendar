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
                val data = EventParser.parseText(formdata.data);
                val completed = EventParser.validateDataset(data);

                Redirect(routes.Events.index(EventType.Fixed.toString()))
            }
        )
	}
}
