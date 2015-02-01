package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._
import org.joda.time.DateTime
import java.util.Date

/**
 * @author Leevi
 */
case class TimeRange ( // refactoring idea, split up into DayTimeRange and SpecificTimeRange based on allday boolean
    allday: Boolean, // True if all day
    start: DateTime, // used for start time and day if allday is true
    end: Option[DateTime] // if allday is false
)

object TimeRange {
    implicit object TimeRangeReader extends BSONDocumentReader[TimeRange] {
        def read(doc: BSONDocument): TimeRange = {
            TimeRange(
                doc.getAs[Boolean]("allday").get,
                doc.getAs[BSONDateTime]("start").map (dt => new DateTime(dt.value)).get,
                doc.getAs[BSONDateTime]("end").map ( dt => new DateTime(dt.value))
            )
        }
    }
    
    implicit object TimeRangeWriter extends BSONDocumentWriter[TimeRange] {
        def write(timerange: TimeRange): BSONDocument = {
            val bson = BSONDocument(
                "allday" -> timerange.allday,
                "start" -> BSONDateTime(timerange.start.getMillis),
                "end" -> BSONDateTime(timerange.end.get.getMillis)
            )
            
            // TODO: add async so that this check works?
            //if (timerange.end.isDefined) {
            //    bson.add("end" -> timerange.end.get.getMillis)
            //}
        bson
    }
}
      
    val form = Form(
        mapping(
            "allday" -> boolean,
            "start" -> date,
            "end" -> optional(date)
        )  { (allday, start, end) =>
            TimeRange (
              allday,
              new DateTime(start),
              end.map(dt => new DateTime(dt))
            )
        } { timerange =>
            Some(
              (timerange.allday,
              new Date(timerange.start.getMillis),
              timerange.end.map (dt => new Date(dt.getMillis))))
          }
    )
}