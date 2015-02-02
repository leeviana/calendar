package models

import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.Form

/**
 * @author Leevi
 */
case class MonthMeta (
    monthDays: List[Int] // list of integer representations of days of the month
)
{
    var recurrenceType = RecurrenceType.Monthly
}

object MonthMeta {
    
    implicit object MonthMetaReader extends BSONDocumentReader[MonthMeta] {
        def read(doc: BSONDocument): MonthMeta = {
            MonthMeta(
                doc.getAs[List[Int]]("monthDays").get
            )
        }
    }
    
    implicit object MonthMetaWriter extends BSONDocumentWriter[MonthMeta] {
        def write(monthmeta: MonthMeta): BSONDocument = BSONDocument(
            "monthDays" -> monthmeta.monthDays
        )
    }
      
    val form = Form(
        mapping(
            "monthDays" -> list(number)
        ) { (monthDays) =>
            MonthMeta (
                monthDays
            )
        } { monthmeta =>
            Some(
                (monthmeta.monthDays)
            )
          }
    )
}