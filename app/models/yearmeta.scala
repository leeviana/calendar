package models

import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.Form
import scala.collection.mutable.ListBuffer
import org.joda.time.DateTime

/**
 * @author Leevi
 */
case class YearMeta (
    month: Int, // integer representation of month
    day: Int // integer representation of day
)
{
    var recurrenceType = RecurrenceType.Yearly
}

object YearMeta {
    
    implicit object YearMetaReader extends BSONDocumentReader[YearMeta] {
        def read(doc: BSONDocument): YearMeta = {
            YearMeta(
                doc.getAs[Int]("month").get,
                doc.getAs[Int]("day").get
            )
        }
    }
    
    implicit object YearMetaWriter extends BSONDocumentWriter[YearMeta] {
        def write(yearmeta: YearMeta): BSONDocument = BSONDocument(
            "month" -> yearmeta.month,
            "day" -> yearmeta.day
        )
    }
      
    val form = Form(
        mapping(
            "month" -> number,
            "day" -> number
        ) { (month, day) =>
            YearMeta (
                month,
                day
            )
        } { yearmeta =>
            Some(
                (yearmeta.month,
                yearmeta.day)
            )
          }
    )
    
    def generateRecurrence(start: DateTime, end: DateTime): List[Long] = {
        var current = start.plusYears(1)
        var timestamps = ListBuffer[Long]()
        
        while(current.compareTo(end) <= 0) {
            timestamps += current.getMillis - start.getMillis
            current = current.plusYears(1)
        }
        
        timestamps.toList
    }
}