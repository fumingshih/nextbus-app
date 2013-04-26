package com.danielstiner.cyride.util;

import org.joda.time.Period;

public class Constants {
	
	public static final String AGENCY = "cyride";
	
	public static final int MAX_STOPS = 4;

	public static final String NEXTBUS_API_CACHE_FILENAME = "nextbus_api_cache.dat";

	public static final long VIEW_UPDATE_INTERVAL = 1 * 1000;
	
	public static final Period NEARBY_CACHE_TIME = new Period().withMinutes(1);
	
	public static final Period SHORTEST_UPDATE_TIME = new Period().withMinutes(1);
	
	public static final Period LONGEST_UPDATE_TIME = new Period().withHours(6);
}
