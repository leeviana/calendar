package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._
import models.enums.CreationRequestStatus

/**
 * @author Leevi
 */
case class CreationRequest(
    _id: BSONObjectID = BSONObjectID.generate,
    eventID: BSONObjectID, // foreign ref, associated event. AccessType = Modify
    master: BSONObjectID, // master event that created the request (and can check status)
    requestStatus: CreationRequestStatus.CreationRequestStatus
    )
    
object CreationRequest {
    implicit val CreationRequestFormat = Json.format[CreationRequest]

    val form = Form(
        mapping(
            "eventID" -> nonEmptyText,
            "master" -> nonEmptyText,
            "requestStatus" -> nonEmptyText) { (eventID, master, requestStatus) =>
                CreationRequest(
                    BSONObjectID.generate,
                    BSONObjectID.apply(eventID),
                    BSONObjectID.apply(master),
                    CreationRequestStatus.withName(requestStatus))
            } { request =>
                Some((
                    request.eventID.stringify,
                    request.master.stringify,
                    request.requestStatus.toString()))
            })
}