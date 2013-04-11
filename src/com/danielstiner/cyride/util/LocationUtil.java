package com.danielstiner.cyride.util;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import com.danielstiner.cyride.util.NextBusAPI.Stop;

import android.location.Location;
import android.location.LocationManager;

public class LocationUtil {

	public static Location getBestCurrentLocation(LocationManager lm) {
		List<String> providers = lm.getProviders(true);

		/*
		 * Loop over the array backwards, and if you get an accurate location,
		 * then break out the loop
		 */
		Location bestLoc = null;

		for (int i = providers.size() - 1; i >= 0; i--) {
			Location l = lm.getLastKnownLocation(providers.get(i));

			if (bestLoc == null) {

				bestLoc = l;
			} else if (bestLoc.getAccuracy() > l.getAccuracy()) {
				bestLoc = l;
			}
			if (l != null)
				break;
		}

		return bestLoc;
	}

	public static double distance(double lat, double lng, Location l) {

		// Approximate
		return Math.abs(l.getLatitude() - lat)
				+ Math.abs(l.getLongitude() - lng);
	}

}
