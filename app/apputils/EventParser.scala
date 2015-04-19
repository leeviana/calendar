package apputils

import scala.collection.mutable.{ Map => MapBuffer }

object EventParser {
    def parseText(text: String): List[MapBuffer[String,List[String]]] = {
    	val output: List[MapBuffer[String,List[String]]] = List();

    	val lines = text.split('\n');
    	for (line <- lines) {
    		var parsedLine: MapBuffer[String,List[String]] = MapBuffer();
    		for (property <- line.trim.split(',')) {
    			var arr = property.trim.split(':');
    			var key = arr(0).trim;
    			var values = Array[String]();
    			if (arr.length > 1) {
    				values = arr(1).trim.split('|');
    			} else {
    				values = Array("");
    			}
    			println(key)
    			println(values(0))
    			parsedLine += (key -> values.toList);
    		}
    		output :+ parsedLine;
    	}    	
    	return output;
    }
}