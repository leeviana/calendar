package models

import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.Form

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
}