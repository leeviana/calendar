package models

import play.api.data.Form
import play.api.data.Forms._


case class BulkAddRequest(
    data: String
    )

object BulkAddRequest {
    val form = Form(
        mapping("data" -> nonEmptyText)(BulkAddRequest.apply)(BulkAddRequest.unapply)
    )
}
