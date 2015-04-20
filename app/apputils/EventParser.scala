package apputils

import scala.collection.mutable.{ Map => MapBuffer }


object EventParser {
    // Dont forget to check for reminders too
    val reminderRegex = "reminder\\d\\d?"
    val fieldNames = List("type","name","description","start","end","recurrencerate","recurrencecount","endrecurring","forpud","priority","expiretime","escalationstart","escalationvalue","escalationrate","escalationcount","maxslots","minduration","createsignuppud","start2","start3","start4","start5","start6","start7","start8","start9","start10","end2","end3","end4","end5","end6","end7","end8","end9","end10","users","determination");
    val tsRegex = "(\\d{4})\\-(\\d{2})\\-(\\d{2})t(\\d{2})\\:(\\d{2})\\:(\\d{2})(([+-](\\d{2})\\:(\\d{2}))|z)"
    val tsRegex2 = "((^$)|(" + tsRegex +"))"
    val fieldRegexs = MapBuffer("type"->"(pud|fixed|signup)","name"->".+","description"->".*","start"->tsRegex2,"end"->tsRegex2,"recurrencerate"->"((^$)|daily|weekly|monthly|yearly)","recurrencecount"->"((^$)|\\d+)","endrecurring"->tsRegex2,"forpud"->"((^$)|true|false)","priority"->"((^$)|\\d+)","expiretime"->tsRegex2,"escalationstart"->tsRegex2,"escalationvalue"->"((^$)|\\d+)","escalationrate"->"((^$)|daily|weekly|monthly|yearly)","escalationcount"->"((^$)|\\d+)","maxslots"->"((^$)|\\d+)","minduration"->"((^$)|\\d+)","createsignuppud"->"((^$)|true|false)","reminder"->tsRegex2,"users"->".*","determination"->tsRegex2);

    def parseText(text: String): (List[MapBuffer[String,List[String]]], String) = {
    	var out: List[MapBuffer[String,List[String]]] = List();
        var lineNo = 0;

    	val lines = text.split('\n');
    	for (line <- lines) {
    		var parsedLine: MapBuffer[String,List[String]] = MapBuffer();
    		for (property <- line.trim.split(',')) {
    			var arr = property.trim.split('=');
    			var key = arr(0).trim.toLowerCase;
    			var values = Array[String]();
                if (!(fieldNames.contains(key)) && !(key.matches(reminderRegex))) {
                    return (out, "Line:" + lineNo + " invalid key: " + key)
                }
    			if (arr.length > 1) {
    				values = arr(1).trim.split('|');
    			} else {
    				values = Array("");
                    return (out,"Line:" + lineNo + " invalid syntax (not a key=value pair)" + key)
    			}
                var simpleKey = key filterNot("0123456789" contains _)
                if (!(values(0).toLowerCase.matches(fieldRegexs(simpleKey)))) {
                    return (out, "Line:" + lineNo + " invalid data in key: " + key + ", " + values(0).toLowerCase)
                }
                if (values.length > 0) {
                    parsedLine += (key -> values.toList);
                }
    		}
    		out = out :+ parsedLine;
            lineNo = lineNo + 1;
    	}    	
    	return (out, "");
    }

    def validateDataset(data: List[MapBuffer[String,List[String]]]): String = {
        var error = "";
        var lineNo = 0;
        for (line <- data) {
            var isInvalid = (!line.keySet.contains("name"));
            isInvalid = isInvalid || (!line.keySet.contains("start"));
            isInvalid = isInvalid || (line("type") == "fixed" && !line.keySet.contains("end"));
            isInvalid = isInvalid || (line("type") == "pud" && (!line.keySet.contains("end") || !line.keySet.contains("priority")));
            isInvalid = isInvalid || (line("type") == "signup" && (!line.keySet.contains("end") || !line.keySet.contains("maxslots")));
            isInvalid = isInvalid || (line.keySet.contains("recurrencerate") && (!line.keySet.contains("recurrencecount") || !line.keySet.contains("endrecurring")));
            isInvalid = isInvalid || (line.keySet.contains("createsignuppud") && (!line.keySet.contains("priority")));

            if (isInvalid){
                error = "Line: " + lineNo + " missing a required key=value pair"
                return error;
            }
            for (key <- line.keys) {
                var count = "";
                if (key.matches("start\\d")) {
                    count = key.takeRight(1);
                    if ((!line.keySet.contains("end"+count))) {
                        error = "Line: " + lineNo + " unmatched start/end pair"
                        return error;
                    }
                }

                if (key.matches("end\\d")) {
                    count = key.takeRight(1);
                    if ((!line.keySet.contains("start"+count))) {
                        error = "Line: " + lineNo + " unmatched start/end pair"
                        return error;
                    }
                }
            }
            lineNo = lineNo+1;
        }
        return error;
    }
}