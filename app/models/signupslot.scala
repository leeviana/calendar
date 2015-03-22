package models

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._

/**
 * @author Leevi
 */
case class SignUpSlot(
	_id: BSONObjectID = BSONObjectID.generate,
    userID: Option[BSONObjectID] = None, // foreign ref
    timeRange: TimeRange)
    
object SignUpSlot {
    implicit val SignUpSlotFormat = Json.format[SignUpSlot]
}