package models

import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.Form
import java.util.Date
import scala.collection.mutable.ListBuffer
import org.joda.time.DateTime

/**
 * @author Leevi
 */
case class WeekMeta (
    dayNumber: Int // Array of Integers representing days of the week. 0 is Sunday. Alternative: use Java's calendar object?
)
{
    var recurrenceType = RecurrenceType.Weekly
}

object WeekMeta {
    
    implicit object WeekMetaReader extends BSONDocumentReader[WeekMeta] {
        def read(doc: BSONDocument): WeekMeta = {
            WeekMeta(
                doc.getAs[Int]("dayNumber").get
            )
        }
    }
    
    implicit object WeekMetaWriter extends BSONDocumentWriter[WeekMeta] {
        def write(weekmeta: WeekMeta): BSONDocument = BSONDocument(
            "dayNumber" -> weekmeta.dayNumber
        )
    }
      
    val form = Form(
        mapping(
            "dayNumber" -> number
        ) { (dayNumber) =>
            WeekMeta (
                dayNumber
            )
        } { weekmeta =>
            Some(
                (weekmeta.dayNumber)
            )
          }
    )
    
    def generateRecurrence(start: DateTime, end: DateTime): List[Long] = {
        var current = start
        var timestamps = ListBuffer[Long]()
        
        while(current.compareTo(end) <= 0) {
            timestamps += current.getMillis - start.getMillis
            current = current.plusWeeks(1)
        }
        
        timestamps.toList
    }
}