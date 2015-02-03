package models

import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.Form
import java.util.Date

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
}