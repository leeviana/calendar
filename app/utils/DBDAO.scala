package models.utils

import scala.concurrent.ExecutionContext.Implicits.global

import models.Group
import reactivemongo.api.DB
import reactivemongo.api.MongoDriver
import reactivemongo.bson.BSONObjectID
import reactivemongo.extensions.dao.BsonDao
import utils.Constants

object MongoContext {
    val driver = new MongoDriver
    val connection = driver.connection(List(Constants.DBLocation))
    def db: DB = connection("caldb")
}

object GroupDAO extends BsonDao[Group, BSONObjectID](MongoContext.db, "groups")