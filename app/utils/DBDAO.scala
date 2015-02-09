package models.utils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.MILLISECONDS
import models.Calendar
import models.Group
import reactivemongo.api.DB
import reactivemongo.api.MongoDriver
import reactivemongo.bson.BSONObjectID
import reactivemongo.extensions.dao.BsonDao
import utils.Constants
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object MongoContext {
    val driver = new MongoDriver
    val connection = driver.connection(List(Constants.DBLocation))
    def db: DB = connection("caldb")
}

object GroupDAO extends BsonDao[Group, BSONObjectID](MongoContext.db, "groups")
object CalendarDAO extends BsonDao[Calendar, BSONObjectID](MongoContext.db, "calendars") {
    def getCalendarFromID(id: BSONObjectID): Calendar = { 
        val futureCalendar = this.findById(id)
              
        var calendar = Await.result(futureCalendar, Duration(5000, MILLISECONDS))
        if(calendar.isDefined)
            calendar.get
        else
            throw new Exception("Database incongruity: Calendar ID not found")
    } 
}