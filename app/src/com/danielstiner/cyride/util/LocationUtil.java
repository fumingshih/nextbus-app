package com.danielstiner.cyride.util;

import java.util.List;

import android.location.Location;
import android.location.LocationManager;

import com.danielstiner.nextbus.NextBusAPI.Stop;

public class LocationUtil {

	public static double distance(double lat, double lng, Location l) {
		return distance(lat, lng, l.getLatitude(), l.getLongitude());
	}

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

	public static int compareDistance(Stop stop1, Stop stop2, Location target) {
		double dist1 = distance(stop1.latitude, stop1.longitude, target);
		double dist2 = distance(stop2.latitude, stop2.longitude, target);
		return Double.compare(dist1, dist2);
	}

	public static int compareDistance(Stop stop1, Stop stop2, double latitude,
			double longitude) {
		double dist1 = distance(stop1.latitude, stop1.longitude, latitude,
				longitude);
		double dist2 = distance(stop2.latitude, stop2.longitude, latitude,
				longitude);
		return Double.compare(dist1, dist2);
	}

	private static double distance(double latitude, double longitude,
			double latitude2, double longitude2) {
		// Approximate
		return Math.abs(latitude2 - latitude)
				+ Math.abs(longitude2 - longitude);
	}

}
