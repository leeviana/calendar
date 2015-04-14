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
	_id: BSONObjectID = BSONObjectID.generate, // may not need this
    userID: Option[BSONObjectID] = None, // foreign ref
    timeRange: TimeRange,
    userOptions: Option[List[UserSignUpOption]] = None)
    
object SignUpSlot {
    implicit val SignUpSlotFormat = Json.format[SignUpSlot]
    
    val form = Form(
        mapping(
            "userID" -> optional(nonEmptyText),
            "timeRange" -> TimeRange.form.mapping) { (userID, timeRange) =>
                if(userID.isDefined) { 
                    SignUpSlot(userID = Some(BSONObjectID.apply(userID.get)), timeRange = timeRange) 
                }
                else {            
                    SignUpSlot(timeRange = timeRange)   
                }
            } { signUpSlot =>
                Some(
                    (
                    if(signUpSlot.userID.isDefined) {
                        Some(signUpSlot.userID.get.stringify)
                    }
                    else {
                        None
                    }, signUpSlot.timeRange))
            })
}