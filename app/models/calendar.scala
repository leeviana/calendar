package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson._

/**
 * @author Leevi
 */
case class Calendar (
    owner: BSONObjectID, // foreign ref calendar
    rules: List[Rule],
    settings: List[UserSetting] // do we need a seperate calendarsetting?
)

object Calendar {
    implicit object CalendarReader extends BSONDocumentReader[Calendar] {
        def read(doc: BSONDocument): Calendar = {
            Calendar(
                doc.getAs[BSONObjectID]("owner").get,
                doc.getAs[List[Rule]]("rules").get,
                doc.getAs[List[UserSetting]]("settings").get
            )
        }
    }
    
    implicit object CalendarWriter extends BSONDocumentWriter[Calendar] {
        def write(calendar: Calendar): BSONDocument = BSONDocument(
            "owner" -> calendar.owner,
            "rules" -> calendar.rules,
            "settings" -> calendar.settings
        )
    }
      
    val form = Form(
        mapping(
            "owner" -> nonEmptyText,
            "rules" -> list(Rule.form.mapping),
            "settings" -> list(UserSetting.form.mapping)
        )  { (owner, rules, settings) =>
            Calendar (
              BSONObjectID.apply(owner),
              rules,
              settings
            )
        } { calendar =>
            Some(
              (calendar.owner.stringify,
              calendar.rules,
              calendar.settings))
          }
    )
}