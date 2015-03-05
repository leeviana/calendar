package models

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._

/**
 * @author Leevi
 */
case class SignUpSlot(
	_id: BSONObjectID,
    userID: BSONObjectID, // foreign ref
    timeRange: TimeRange)
    
object SignUpSlot {
    implicit val SignUpSlotFormat = Json.format[SignUpSlot]
}