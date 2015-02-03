package models

import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.Form

/**
 * @author Leevi
 */
case class YearMeta (
    month: Int, // integer representation of month
    day: Int // integer representation of day
    
    // TODO: extend this to be a list of days?
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
}