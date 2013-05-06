package com.danielstiner.cyride.util;


public interface CachePolicy {

	boolean shouldUpdateStops(Cache mCache);

}