package com.danielstiner.cyride.util;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.danielstiner.cyride.util.NextBusAPI.Prediction;
import com.danielstiner.cyride.util.NextBusAPI.Route;
import com.danielstiner.cyride.util.NextBusAPI.Stop;

public class TextFormat {

	public static CharSequence toString(Route route) {
		return (route == null) ? null : route.title;
	}

	public static CharSequence toString(Stop stop) {
		return (stop == null) ? null : stop.title;
	}

	public static CharSequence toString(List<Prediction> predictions) {
		if(predictions == null)
			return null;
		
		StringBuilder sb = new StringBuilder();
		for(Prediction p : predictions) {
			sb.append(' ');
			
			long seconds = (p.arrival.getTime() - new Date().getTime())/1000;
			
			if(seconds <= 60)
			{
				sb.append(seconds);
				sb.append('s');
			} else {
				sb.append(seconds/60);
				sb.append('m');
			}
		}
		return sb.substring(1);
	}

	public static CharSequence distanceToString(float distance) {
		return Float.toString(distance) + "ft";
	}

}