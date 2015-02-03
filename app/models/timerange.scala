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
    allday: Boolean, // True if all day event
    date: Option[DateTime], // date, if allday is true
    start: Option[DateTime], // start time, if allday is false
    end: Option[DateTime] // end time, exists if allday is false
)
{
    def this() {
    this(false, Some(new DateTime()), Some(new DateTime()), Some(new DateTime()));
  }
}

object TimeRange {
    implicit object TimeRangeReader extends BSONDocumentReader[TimeRange] {
        def read(doc: BSONDocument): TimeRange = {
            TimeRange(
                doc.getAs[Boolean]("allday").get, 
                doc.getAs[BSONDateTime]("date").map (dt => new DateTime(dt.value)),
                doc.getAs[BSONDateTime]("start").map (dt => new DateTime(dt.value)),
                doc.getAs[BSONDateTime]("end").map (dt => new DateTime(dt.value))
            )
        }
    }
    
    implicit object TimeRangeWriter extends BSONDocumentWriter[TimeRange] {
        def write(timerange: TimeRange): BSONDocument = {
            val bson = BSONDocument(
                "allday" -> timerange.allday,
                "date" -> BSONDateTime(timerange.date.get.getMillis),
                "start" -> BSONDateTime(timerange.start.get.getMillis),
                "end" -> BSONDateTime(timerange.end.getOrElse(new DateTime()).getMillis)
            )
            
            // TODO: make this check work?
            //if (timerange.end.nonEmpty) {
            //    bson.add("end" -> BSONDateTime(timerange.end.get.getMillis))
            //}
        bson
    }
}
      
    val form = Form(
        mapping(
            "allday" -> boolean,
            //"date" -> optional(jodaDate("MM/dd/yyyy")),
            "date" -> optional(jodaDate),
            "start" -> optional(jodaDate("h:mm a")),
            "end" -> optional(jodaDate("h:mm a"))
        )  { (allday, date, start, end) =>
            TimeRange (
              allday,
              date,
              start,
              end
            )
        } { timerange =>
            Some(
              (timerange.allday,
              timerange.date,
              timerange.start,
              timerange.end))
          }
    )
}