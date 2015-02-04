package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._

/**
 * @author Leevi
 */
case class Calendar (
    id: BSONObjectID,
    owner: BSONObjectID, // foreign ref calendar
    name: String,
    rules: BSONArray, // list of Rules
    settings: BSONArray // list of [User]Settings, do we need a seperate calendarsetting object? probably not
)

object Calendar {
    implicit object CalendarReader extends BSONDocumentReader[Calendar] {
        def read(doc: BSONDocument): Calendar = {
            Calendar(
                doc.getAs[BSONObjectID]("_id").get,    
                doc.getAs[BSONObjectID]("owner").get,
                doc.getAs[String]("name").get,
                doc.getAs[BSONArray]("rules").get,
                doc.getAs[BSONArray]("settings").get
            )
        }
    }
    
    implicit object CalendarWriter extends BSONDocumentWriter[Calendar] {
        def write(calendar: Calendar): BSONDocument = BSONDocument(
            "_id" -> calendar.id,
            "owner" -> calendar.owner,
            "name" -> calendar.name,
            "rules" -> calendar.rules,
            "settings" -> calendar.settings
        )
    }
      
    val form = Form(
        mapping(
            "id" -> ignored(BSONObjectID.generate),
            "owner" -> nonEmptyText,
            "name" -> nonEmptyText,
            "rules" -> ignored(BSONArray.empty),
            "settings" -> ignored(BSONArray.empty)
        )  { (id, owner, name, rules, settings) =>
            Calendar (
              id,
              BSONObjectID.apply(owner),
              name,
              rules,
              settings
            )
        } { calendar =>
            Some(
              (calendar.id,
              calendar.owner.stringify,
              calendar.name,
              calendar.rules,
              calendar.settings))
          }
    )
}