package apputils

import scala.collection.mutable.{ Map => MapBuffer }
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json


object JsonConverter {
    def jsonToMap(json: JsValue): MapBuffer[String, String] = {
        var output = MapBuffer[String, String]()
        val test = json match {
            case o: JsObject => {
                val keys = o.keys;
                for (k <- keys) {
                    output += (k -> (o \ k).as[String]);
                }
            }
            case _ => Set()
        }
        return output
    }

    def mapToJson(map: MapBuffer[String, String]): JsValue = {
        var output = new JsObject(Seq[(String, JsValue)]());
        map.foreach {
            case (k, v) => {
                output = output + (k -> Json.toJson(v));
            }
        }
        return output;
    }
}