package io.github.rbajek.rasa.sdk.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateUtil {
	private static Logger logger = LoggerFactory.getLogger(DateUtil.class);
	private static DateTimeFormatter formatterToUse = DateTimeFormatter.ISO_INSTANT;

	public static void setFormatterToUse(DateTimeFormatter formatterToUse) {
		DateUtil.formatterToUse = formatterToUse;
	}

	public static LocalDateTime parseDate(String date){
		try {
			return LocalDateTime.parse(date, formatterToUse);
		}
		catch (Exception e){
			logger.warn("Could not parse string for date - unparsable string is "+date+ ". You likely want to set correct DateTimeFormatter");
			return null;
		}
	}
}
