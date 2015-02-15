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
    startDate: Option[DateTime],
    startTime: Option[DateTime],
    endDate: Option[DateTime],
    endTime: Option[DateTime]
)
{
    def this() {
    this(false, Some(new DateTime()), Some(new DateTime()), Some(new DateTime()), Some(new DateTime()));
  }
}

object TimeRange {
    implicit object TimeRangeReader extends BSONDocumentReader[TimeRange] {
        def read(doc: BSONDocument): TimeRange = {
            TimeRange(
                doc.getAs[Boolean]("allday").get, 
                doc.getAs[BSONDateTime]("startDate").map (dt => new DateTime(dt.value)),
                doc.getAs[BSONDateTime]("startTime").map (dt => new DateTime(dt.value)),
                doc.getAs[BSONDateTime]("endDate").map (dt => new DateTime(dt.value)),
                doc.getAs[BSONDateTime]("endTime").map (dt => new DateTime(dt.value))
            )
        }
    }
    
    implicit object TimeRangeWriter extends BSONDocumentWriter[TimeRange] {
        def write(timerange: TimeRange): BSONDocument = {
            val bson = BSONDocument(
                "allday" -> timerange.allday,
                "startDate" -> BSONDateTime(timerange.startDate.getOrElse(new DateTime()).getMillis),
                "startTime" -> BSONDateTime(timerange.startTime.getOrElse(new DateTime()).getMillis),
                "endDate" -> BSONDateTime(timerange.endDate.getOrElse(timerange.startDate.getOrElse(new DateTime())).getMillis), // if no endDate, assume same as startDate
                "endTime" -> BSONDateTime(timerange.endTime.getOrElse(new DateTime()).getMillis)
            )
            
        bson
    }
}
      
    val form = Form(
        mapping(
            "allday" -> boolean,
            "startDate" -> optional(jodaDate),
            "startTime" -> optional(jodaDate("h:mm a")),
            "endDate" -> optional(jodaDate),
            "endTime" -> optional(jodaDate("h:mm a"))
        )  { (allday, startDate, startTime, endDate, endTime) =>
            TimeRange (
              allday,
              startDate,
              startTime,
              endDate,
              endTime
            )
        } { timerange =>
            Some(
              (timerange.allday,
              timerange.startDate,
              timerange.startTime,
              timerange.endDate,
              timerange.endTime))
          }
    )
}