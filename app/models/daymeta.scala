package models

import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.Form
import scala.collection.mutable.ListBuffer
import org.joda.time.DateTime

/**
 * @author Leevi
 */
case class DayMeta (
    numberOfDays: Int
)
{
    var recurrenceType = RecurrenceType.Daily
}

object DayMeta {
    
    implicit object DayMetaReader extends BSONDocumentReader[DayMeta] {
        def read(doc: BSONDocument): DayMeta = {
            DayMeta(
                doc.getAs[Int]("numberOfDays").get
            )
        }
    }
    
    implicit object DayMetaWriter extends BSONDocumentWriter[DayMeta] {
        def write(daymeta: DayMeta): BSONDocument = BSONDocument(
            "numberOfDays" -> daymeta.numberOfDays
        )
    }
      
    val form = Form(
        mapping(
            "numberOfDays" -> number
        ) { (numberOfDays) =>
            DayMeta (
                numberOfDays
            )
        } { daymeta =>
            Some(
                (daymeta.numberOfDays)
            )
          }
    )
    
    def generateRecurrence(start: DateTime, end: DateTime): List[Long] = {
        var current = start.plusDays(1)
        var timestamps = ListBuffer[Long]()
        
        while(current.compareTo(end) <= 0) {
            timestamps += current.getMillis - start.getMillis
            current = current.plusDays(1)
        }
        
        timestamps.toList
    }
}