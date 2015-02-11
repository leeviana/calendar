package controllers

import scala.concurrent.Future

import apputils.AuthStateDAO
import apputils.GroupDAO
import models.Group
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONObjectIDIdentity
import reactivemongo.bson.Producer.nameValue2Producer
import reactivemongo.extensions.dsl.BsonDsl._

/**
 * @author Leevi
 */
object Groups extends Controller with MongoController {
    /*
     * Gets a page that lists a user's groups
     */
    def showGroups = Action.async { implicit request =>
        if (AuthStateDAO.isAuthenticated()) {
            GroupDAO.findAll("owner" $eq AuthStateDAO.getUserID()).map { groups =>
                Ok(views.html.groups(groups, Group.form))
            }
        } else {
            Future.successful(Redirect(routes.Application.index))
        }
    }

    /*
     * Gets a page that with the new group form
     */
    def newGroupForm = Action { implicit request =>
        Ok(views.html.createGroup(Group.form, AuthStateDAO.getUserID().stringify))
    }

    /*
     * Adds a new group to the user's groups
     */
    def addGroup = Action.async { implicit request =>
        GroupDAO.findAll("owner" $eq AuthStateDAO.getUserID()).map { groups =>

            Group.form.bindFromRequest.fold(
                errors => Ok(views.html.groups(groups, errors)),

                group => {
                    val updatedGroup = group.copy(owner = AuthStateDAO.getUserID())
                    GroupDAO.insert(updatedGroup)
                    Redirect(routes.Groups.showGroups())
                })
        }
    }

    /*
     * Parses groupID and userID from request and adds the corresponding user to the corresponding group
     */
    def addUsertoGroup = Action { implicit request =>
        val requestMap = (request.body.asFormUrlEncoded)
        val groupID = BSONObjectID.apply(requestMap.get.get("groupID").get.head)
        val addUserID = BSONObjectID.apply(requestMap.get.get("userID").get.head)

        GroupDAO.updateById(groupID, $push("userIDs", addUserID))
        Redirect(routes.Groups.showGroups())
    }
}