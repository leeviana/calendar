package controllers

import play.api._
import play.api.mvc._
import models._
import models.Event._
import apputils.AuthStateDAO
import apputils.CalendarDAO
import apputils.UserDAO
import apputils._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONObjectIDIdentity
import reactivemongo.bson.Producer.nameValue2Producer
//import reactivemongo.extensions.dsl.BsonDsl._
import reactivemongo.extensions.json.dsl.JsonDsl._
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.ws._
import play.api.Play.current

import java.io.File
import java.io.FileOutputStream
import play.api.libs.iteratee._

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS

import play.api.libs.mailer._

object Application extends Controller with MongoController{


    def index = Action {
        Ok(views.html.index())
    }

    def signUp = Action {
        Ok(views.html.editUser(User.form))
    }

    def signIn = Action {
        Ok(views.html.login())
    }

    def newCalendarForm = Action{  implicit request =>
        Ok(views.html.createCalendar(Calendar.form, AuthStateDAO.getUserID().stringify))
    }

    def requestEmail = Action.async { implicit request =>
        if (AuthStateDAO.isAuthenticated()) {
        val endpoints = List("Fixed","PUD","SignUp");
        UserDAO.findById(AuthStateDAO.getUserID()).flatMap { user =>
            for (endpoint <- endpoints) {
                val file = new File("./"+endpoint+"-"+user.get.username+".pdf")
                val requestForm = Map("url" -> Seq("http://nautical-dev.colab.duke.edu/events/" + endpoint + "/" + AuthStateDAO.getUserID().stringify), "netid" -> Seq("nautical"));
                val url = "http://devilprint.colab.duke.edu:8080/pdf";
                //WS.url(url).post(requestForm);


                val futureResponse: Future[(WSResponseHeaders, Enumerator[Array[Byte]])] =
                    WS.url(url).withMethod("POST").withBody(requestForm).stream();

                Await.ready(futureResponse, Duration(5000, MILLISECONDS));

                val downloadedFile: Future[File] = futureResponse.flatMap {
                    case (headers, body) =>
                        val outputStream = new FileOutputStream(file)

                        // The iteratee that writes to the output stream
                        val iteratee = Iteratee.foreach[Array[Byte]] { bytes =>
                            outputStream.write(bytes)
                        }

                        // Feed the body into the iteratee
                        (body |>>> iteratee).andThen {
                            case result =>
                                // Close the output stream whether there was an error or not
                                outputStream.close()
                                // Get the result or rethrow the error
                                result.get
                        }.map(_ => file)
                        //outputStream.close()
                }
                Await.ready(downloadedFile, Duration(5000, MILLISECONDS))
                println("sent one!");
            }
            //Future.successful(Redirect(routes.Events.index("Fixed")));
            sendEmail(user.get.username + " <"+ user.get.email +">", "This is the copy of your schedule you requested.",user.get.username);
            Future.successful(Ok(views.html.email(models.Email.form)));
        }
    } else {
        Future.successful(Redirect(routes.Application.index))
    }
    }
    
    def addCalendar = Action.async { implicit request =>
        CalendarDAO.findAll("owner" $eq AuthStateDAO.getUserID()).map { calendars =>
          
          Calendar.form.bindFromRequest.fold(
              errors => Ok(views.html.createCalendar(Calendar.form, AuthStateDAO.getUserID().stringify)),
              
              calendar => {               
                val updatedCalendar = calendar.copy(owner = AuthStateDAO.getUserID())
                CalendarDAO.insert(updatedCalendar)
                UserDAO.updateById(AuthStateDAO.getUserID(), $push("subscriptions", updatedCalendar._id))
                Redirect(routes.Application.newCalendarForm())
              })
          
        }
    }

    def sendEmail(destination: String, content: String, fileID: String) = {
        var attachmentfiles: Seq[AttachmentFile] = Seq();
        if (fileID.length > 0) {
            attachmentfiles = Seq(AttachmentFile("Events.pdf", new File("./Fixed-" + fileID + ".pdf")),
                AttachmentFile("PUDs.pdf", new File("./PUD-" + fileID + ".pdf")),
                AttachmentFile("SignUps.pdf", new File("./SignUp-" + fileID + ".pdf")));
        }
        
        val email = play.api.libs.mailer.Email(
            "Nautical Reminder",
            "NautiCal Nonsense <nauticalnonsense999@gmail.com>",
            Seq(destination),
            attachments = attachmentfiles,
            // sends text, HTML or both...
            bodyText = Some(content),
            bodyHtml = Some("<html><body><p>" + content + "</p></body></html>")
        )
        MailerPlugin.send(email)
    }
}