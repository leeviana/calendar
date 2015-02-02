package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._

/**
 * @author Leevi
 */
case class Group (
    name: String,
    owner: BSONObjectID, // foreign ref
    userIDs: BSONArray // List[BSONObjectID]
)

object Group {
    
    implicit object GroupReader extends BSONDocumentReader[Group] {
        def read(doc: BSONDocument): Group = {
            Group(
                doc.getAs[String]("name").get,
                doc.getAs[BSONObjectID]("owner").get,
                doc.getAs[BSONArray]("userIDs").get
            )
        }
    }
    
    implicit object GroupWriter extends BSONDocumentWriter[Group] {
        def write(group: Group): BSONDocument = BSONDocument(
            "name" -> group.name,
            "owner" -> group.owner,
            "userIDs" -> group.userIDs
        )
    }
      
    val form = Form(
            
        mapping(
            "name" -> nonEmptyText,
            "owner" -> nonEmptyText,
            "userIDs" -> ignored(BSONArray.empty)
        )  { (name, owner, userIDs) =>
            val ownID = BSONObjectID.apply(owner)
            Group (
                name,
                ownID,
                userIDs.add(ownID)
            )
        } { group =>
            Some(
                (group.name,
                group.owner.toString(),
                group.userIDs)
            )
          }
    )
}