package controllers

import models._
import play.api.data.Form
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONObjectID
import org.mindrot.jbcrypt.BCrypt
import apputils.CalendarDAO
import apputils.UserDAO
import apputils.AuthInfoDAO

/**
 * @author Leevi
 */
object Users extends Controller with MongoController {

    /*
     * Deprecated page used for testing purposes. When no longer needed, remove
     */
    def index = Action.async { implicit request =>
        UserDAO.findAll().map { users =>
            Ok(views.html.users(users))
        }
    }

    /*
     * Creates a new user with a default personal calendar and authentication information
     */
    def create = Action { implicit request =>
        User.form.bindFromRequest.fold(
            errors => Ok(views.html.editUser(errors)),

            user => {
                val calName = user.username + "'s personal calendar"
                val personalCalendar = new Calendar(user._id, calName)
                CalendarDAO.insert(personalCalendar)

                val updatedUser = user.copy(subscriptions = List[BSONObjectID](personalCalendar._id))
                UserDAO.insert(updatedUser)

                val requestMap = (request.body.asFormUrlEncoded)
                val password = requestMap.get.get("password").get.head
                val hash = BCrypt.hashpw(password, BCrypt.gensalt());
                val newAuthData = AuthInfo(_id = BSONObjectID.generate, userID = updatedUser._id, lastAuthToken = "", passwordHash = hash)
                AuthInfoDAO.insert(newAuthData)
                Redirect(routes.Application.signIn())
            })
    }
}