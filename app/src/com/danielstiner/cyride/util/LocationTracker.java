package com.danielstiner.cyride.util;

import java.util.List;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

public class LocationTracker {

	private LocationManager locationManager;

	private Looper looper;

	private static final int minDistance = 420;
	private static final long minTime = 60;

	public LocationTracker() {
		mSingleCriteria = new Criteria();
		mSingleCriteria.setAccuracy(Criteria.ACCURACY_MEDIUM);
	}

	private final LocationListener mLocationListener = new LocationListener() {

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onLocationChanged(Location location) {
			if (addLocation(location) && null != mLocationUpdateCallback)
				mLocationUpdateCallback.run(location);
		}
	};

	private Callback<Location> mLocationUpdateCallback;

	public void stop() {
		locationManager.removeUpdates(mLocationListener);
		looper = null;
	}

	public void start(Context context, Callback<Location> locationUpdateCallback) {
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(
				LocationManager.PASSIVE_PROVIDER, 0, 0, mLocationListener);
		looper = Looper.myLooper();
		mLocationUpdateCallback = locationUpdateCallback;
	}

	private boolean addLocation(Location location) {
		float accuracy = location.getAccuracy();
		long time = location.getTime();

		if ((time > minTime && accuracy < bestAccuracy)) {
			bestResult = location;
			bestAccuracy = accuracy;
			bestTime = time;
			return true;
		} else if (time < minTime && bestAccuracy == Float.MAX_VALUE
				&& time > bestTime) {
			bestResult = location;
			bestTime = time;
			return true;
		}

		return false;
	}

	public Location getBestCurrentLocation() {
		return getBestLastKnown();
	}

	private float bestAccuracy = Float.MAX_VALUE;
	private Location bestResult = null;
	private long bestTime = 0;

	private Criteria mSingleCriteria;

	private Location getBestLastKnown() {
		// Iterate through all the providers on the system, keeping
		// note of the most accurate result within the acceptable time limit.
		// If no result is found within maxTime, return the newest Location.
		List<String> matchingProviders = locationManager.getAllProviders();
		for (String provider : matchingProviders) {
			Location location = locationManager.getLastKnownLocation(provider);
			if (location != null) {
				addLocation(location);
			}
		}

		if (bestTime < minTime || bestAccuracy > minDistance) {
			locationManager.requestSingleUpdate(mSingleCriteria,
					mLocationListener, looper);
		}

		return bestResult;
	}
}
