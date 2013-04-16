package com.danielstiner.cyride.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.danielstiner.cyride.util.NextBusAPI.Prediction;
import com.danielstiner.cyride.util.NextBusAPI.Route;
import com.danielstiner.cyride.util.NextBusAPI.Stop;

public class TextFormat {
	
	private static final SimpleDateFormat ABSOLUTE_TIME_FORMAT = new SimpleDateFormat("HH:mm");

	public static CharSequence distanceToString(float distance) {
		return Float.toString(distance) + "ft";
	}

	public static CharSequence toString(List<Prediction> predictions) {
		if (predictions == null)
			return null;

		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (Prediction p : predictions) {

			if (i++ == 2)
				break;

			sb.append(' ');

			long seconds = (p.arrival.getTime() - new Date().getTime()) / 1000;

			if (seconds <= 0) {
				if (seconds < -10)
					continue;
				else
					sb.append("arriving");
			} else if (seconds <= 60) {
				sb.append(seconds);
				sb.append('s');
			} else {
				sb.append((seconds + 29) / 60); // Round
				sb.append('m');
			}
		}
		return sb.substring(1);
	}

	public static CharSequence toString(Prediction p) {
		if (p == null)
			return null;

		StringBuilder sb = new StringBuilder();

		long seconds = (p.arrival.getTime() - new Date().getTime()) / 1000;

		if (seconds <= 0) {
			if (seconds < -10)
				return null;
			else
				sb.append("arriving");
		} else if (seconds <= 60) {
			sb.append(seconds);
			sb.append('s');
		} else {
			sb.append((seconds + 29) / 60); // Round
			sb.append('m');
		}

		return sb.toString();
	}
	
	public static CharSequence singleAbsoluteTime(List<Prediction> predictions) {
		if (predictions == null || predictions.isEmpty())
			return null;
		
		Prediction p = predictions.get(0);
		
		return ABSOLUTE_TIME_FORMAT.format(p.arrival);
	}

	public static CharSequence toString(Route route) {
		return (route == null) ? null : route.title;
	}

	public static CharSequence toString(Stop stop) {
		return (stop == null) ? null : stop.title;
	}

}
