package models

import play.api.data.Forms._
import reactivemongo.bson._
import play.api.data.Form

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
}