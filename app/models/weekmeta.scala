package models

import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.Form
import java.util.Date

/**
 * @author Leevi
 */
case class WeekMeta (
    dayNumbers: List[Int] // Array of Integers representing days of the week. 0 is Sunday. Alternative: use Java's calendar object?
)
{
    var recurrenceType = RecurrenceType.Weekly
}

object WeekMeta {
    
    implicit object WeekMetaReader extends BSONDocumentReader[WeekMeta] {
        def read(doc: BSONDocument): WeekMeta = {
            WeekMeta(
                doc.getAs[List[Int]]("dayNumbers").get
            )
        }
    }
    
    implicit object WeekMetaWriter extends BSONDocumentWriter[WeekMeta] {
        def write(weekmeta: WeekMeta): BSONDocument = BSONDocument(
            "dayNumbers" -> weekmeta.dayNumbers
        )
    }
      
    val form = Form(
        mapping(
            "dayNumbers" -> list(number)
        ) { (dayNumbers) =>
            WeekMeta (
                dayNumbers
            )
        } { weekmeta =>
            Some(
                (weekmeta.dayNumbers)
            )
          }
    )
}