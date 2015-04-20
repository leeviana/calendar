package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson.BSONObjectID


case class BulkAddRequest(
	calendar: BSONObjectID = BSONObjectID.generate, 
    data: String
    )

object BulkAddRequest {
    val form = Form(
        mapping(
        	"calendar" -> nonEmptyText,
        	"data" -> nonEmptyText
        	) { (calendar, data) =>
        	BulkAddRequest(
        		BSONObjectID.apply(calendar),
        		data)
        	}{ bulkAddRequest =>
                Some((
                    bulkAddRequest.calendar.stringify,
                    bulkAddRequest.data
                    ))
            }
    )
}
