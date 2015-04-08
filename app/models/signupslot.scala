package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.bson.BSONObjectID

/**
 * @author Leevi
 */
case class SignUpSlot(
	_id: BSONObjectID = BSONObjectID.generate,
    userID: Option[BSONObjectID] = None, // foreign ref
    timeRange: TimeRange,
    userOptions: Option[List[UserSignUpOption]] = None)
    
object SignUpSlot {
    implicit val SignUpSlotFormat = Json.format[SignUpSlot]

/*    val form = Form(
        mapping(
            "userID" -> optional(nonEmptyText),
            "timeRange" -> TimeRange.form.mapping,
            "optionUserID" -> optional(nonEmptyText),
            "optionPriority" -> optional(number)) { (userID, timeRange, optionUserID, optionPriority) =>
                SignUpSlot(
                    BSONObjectID.generate,
                    BSONObjectID.apply(owner),
                    name,
                    rules.getOrElse(List[Rule]()),
                    settings.getOrElse(List[UserSetting]()))
            } { calendar =>
                Some((
                    calendar.owner.stringify,
                    calendar.name,
                    Some(calendar.rules),
                    Some(calendar.settings)))
            })*/
}