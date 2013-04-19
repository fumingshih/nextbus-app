package com.danielstiner.cyride.util;

import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;

public interface CachePolicy {

	boolean shouldUpdateStops(Cache mCache);

	boolean shouldUpdateStopPredictions(StopPrediction c, Cache cache);

}