package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._

/**
 * @author Leevi
 */
case class Calendar (
    _id: BSONObjectID,
    owner: BSONObjectID, // foreign ref calendar
    name: String,
    rules: List[Rule], // list of Rules
    settings: List[UserSetting] // list of [User]Settings, do we need a seperate calendarsetting object? probably not
)

object Calendar {
    implicit val CalendarHandler = Macros.handler[Calendar]

//    implicit object CalendarReader extends BSONDocumentReader[Calendar] {
//        def read(doc: BSONDocument): Calendar = {
//            Calendar(
//                doc.getAs[BSONObjectID]("_id").get,    
//                doc.getAs[BSONObjectID]("owner").get,
//                doc.getAs[String]("name").get,
//                doc.getAs[List[Rule]]("rules").get,
//                doc.getAs[List[UserSetting]]("settings").get
//            )
//        }
//    }
//    
//    implicit object CalendarWriter extends BSONDocumentWriter[Calendar] {
//        def write(calendar: Calendar): BSONDocument = BSONDocument(
//            "_id" -> calendar._id,
//            "owner" -> calendar.owner,
//            "name" -> calendar.name,
//            "rules" -> calendar.rules,
//            "settings" -> calendar.settings
//        )
//    }
      
    val form = Form(
        mapping(
           // "id" -> ignored(BSONObjectID.generate),
            "owner" -> nonEmptyText,
            "name" -> nonEmptyText,
            "rules" -> optional(list(Rule.form.mapping)),
            "settings" -> optional(list(UserSetting.form.mapping))
        )  { (owner, name, rules, settings) =>
            Calendar (
              BSONObjectID.generate,
              BSONObjectID.apply(owner),
              name,
              rules.getOrElse(List[Rule]()),
              settings.getOrElse(List[UserSetting]())
            )
        } { calendar =>
            Some(
              (
              //calendar.id,
              calendar.owner.stringify,
              calendar.name,
              Some(calendar.rules),
              Some(calendar.settings)))
          }
    )
}