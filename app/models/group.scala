package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._

/**
 * @author Leevi
 */
case class Group (
    _id: BSONObjectID = BSONObjectID.generate,    
    name: String,
    owner: BSONObjectID, // foreign ref
    userIDs: BSONArray // List[BSONObjectID]
)

object Group {
    
/*    implicit object GroupReader extends BSONDocumentReader[Group] {
        def read(doc: BSONDocument): Group = {
            Group(
                doc.getAs[BSONObjectID]("_id").get,
                doc.getAs[String]("name").get,
                doc.getAs[BSONObjectID]("owner").get,
                doc.getAs[BSONArray]("userIDs").get
            )
        }
    }
    
    implicit object GroupWriter extends BSONDocumentWriter[Group] {
        def write(group: Group): BSONDocument = BSONDocument(
            "_id" -> group.id, // is this necessary?
            "name" -> group.name,
            "owner" -> group.owner,
            "userIDs" -> group.userIDs
        )
    }
*/
    
    implicit val UserHandler = Macros.handler[Group]
    
    val form = Form(
            
        mapping(
            "_id" -> ignored(BSONObjectID.generate),
            "name" -> nonEmptyText,
            "owner" -> nonEmptyText,
            "userIDs" -> ignored(BSONArray.empty)
        )  { (id, name, owner, userIDs) =>
            val ownID = BSONObjectID.apply(owner)
            Group (
                id,
                name,
                ownID,
                userIDs.add(ownID)
            )
        } { group =>
            Some(
                (group._id,
                group.name,
                group.owner.toString(),
                group.userIDs)
            )
          }
    )
}
