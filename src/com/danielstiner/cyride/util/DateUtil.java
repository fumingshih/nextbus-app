package com.danielstiner.cyride.util;

import org.joda.time.DateTime;

public class DateUtil {

	public static DateTime later(DateTime date1, DateTime date2) {
		return (date1 != null && date1.isAfter(date2)) ? date1 : date2;
	}

	public static DateTime earlier(DateTime date1, DateTime date2) {
		return (date1 != null && date1.isBefore(date2)) ? date1 : date2;
	}

}
