package models

import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.Form
import org.joda.time.DateTime
import scala.collection.mutable.ListBuffer

/**
 * @author Leevi
 */
case class MonthMeta (
    monthDay: Int
)
{
    var recurrenceType = RecurrenceType.Monthly
}

object MonthMeta {
    
    implicit object MonthMetaReader extends BSONDocumentReader[MonthMeta] {
        def read(doc: BSONDocument): MonthMeta = {
            MonthMeta(
                doc.getAs[Int]("monthDay").get
            )
        }
    }
    
    implicit object MonthMetaWriter extends BSONDocumentWriter[MonthMeta] {
        def write(monthmeta: MonthMeta): BSONDocument = BSONDocument(
            "monthDay" -> monthmeta.monthDay
        )
    }
      
    val form = Form(
        mapping(
            "monthDay" -> number
        ) { (monthDay) =>
            MonthMeta (
                monthDay
            )
        } { monthmeta =>
            Some(
                (monthmeta.monthDay)
            )
          }
    )
    
    def generateRecurrence(start: DateTime, end: DateTime): List[Long] = {
        var current = start.plusMonths(1)
        var timestamps = ListBuffer[Long]()
        
        while(current.compareTo(end) <= 0) {
            timestamps += current.getMillis - start.getMillis
            current = current.plusMonths(1)
        }
        
        timestamps.toList
    }
}