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
    masterEvent: BSONObjectID, // master event that created the request (and can check status)
    requestStatus: CreationRequestStatus.CreationRequestStatus
    )
    
object CreationRequest {
    implicit val CreationRequestFormat = Json.format[CreationRequest]

    val form = Form(
        mapping(
            "eventID" -> nonEmptyText,
            "masterEvent" -> nonEmptyText,
            "requestStatus" -> nonEmptyText) { (eventID, masterEvent, requestStatus) =>
                CreationRequest(
                    BSONObjectID.generate,
                    BSONObjectID.apply(eventID),
                    BSONObjectID.apply(masterEvent),
                    CreationRequestStatus.withName(requestStatus))
            } { request =>
                Some((
                    request.eventID.stringify,
                    request.masterEvent.stringify,
                    request.requestStatus.toString()))
            })
}