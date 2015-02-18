package models

import play.api.data.Form
import play.api.data.Forms.ignored
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.libs.json.Json
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._

/**
 * @author Leevi
 */
case class Group(
    _id: BSONObjectID = BSONObjectID.generate,
    name: String,
    owner: BSONObjectID, // foreign ref
    userIDs: List[BSONObjectID] // List[BSONObjectID]
    )

object Group {
    implicit val GroupFormat = Json.format[Group]

    val form = Form(
        mapping(
            "_id" -> ignored(BSONObjectID.generate),
            "name" -> nonEmptyText,
            "owner" -> nonEmptyText
            ) { (id, name, owner) =>
                val ownID = BSONObjectID.apply(owner)
                
                Group(
                    id,
                    name,
                    ownID,
                    List[BSONObjectID](ownID))
            } { group =>
                Some((
                    group._id,
                    group.name,
                    group.owner.toString()))
            })
}
