package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._

/**
 * @author Leevi
 */
case class Group(
    _id: BSONObjectID = BSONObjectID.generate,
    name: String,
    owner: BSONObjectID, // foreign ref
    userIDs: BSONArray // List[BSONObjectID]
    )

object Group {
    implicit val UserHandler = Macros.handler[Group]

    val form = Form(

        mapping(
            "_id" -> ignored(BSONObjectID.generate),
            "name" -> nonEmptyText,
            "owner" -> nonEmptyText,
            "userIDs" -> ignored(BSONArray.empty)) { (id, name, owner, userIDs) =>
                val ownID = BSONObjectID.apply(owner)
                Group(
                    id,
                    name,
                    ownID,
                    userIDs.add(ownID))
            } { group =>
                Some(
                    (group._id,
                        group.name,
                        group.owner.toString(),
                        group.userIDs))
            })
}
