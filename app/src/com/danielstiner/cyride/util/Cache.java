package com.danielstiner.cyride.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import android.app.Activity;
import android.content.Context;

import com.danielstiner.cyride.service.IPredictions.NearbyStopPredictions;
import com.danielstiner.nextbus.NextBusAPI.Stop;

import de.akquinet.android.androlog.Log;

public class Cache implements Serializable {
	
	private static final long serialVersionUID = -5003315853529912609L;

	private static final String TAG = "Cache";

	public static Cache loadCache(String agency, Context context) {
		
		Cache c = loadCacheInternal(context);
		
		if (c == null || !agency.equals(c.agency))
			c = new Cache(agency);
		
		return c;
	}

	private static Cache loadCacheInternal(Context context) {
		ObjectInputStream objectIn = null;
		Cache object = null;
		try {

			FileInputStream fileIn = context.getApplicationContext()
					.openFileInput(Constants.NEXTBUS_API_CACHE_FILENAME);
			objectIn = new ObjectInputStream(fileIn);
			object = (Cache) objectIn.readObject();

		} catch (FileNotFoundException e) {
			// NOOP
		} catch (IOException e) {
			Log.e(TAG, "mNextBusAPI.loadCache", e);
		} catch (ClassNotFoundException e) {
			Log.e(TAG, "mNextBusAPI.loadCache", e);
		} finally {
			if (objectIn != null) {
				try {
					objectIn.close();
				} catch (IOException e) {
					Log.e(TAG, "mNextBusAPI.loadCache objectIn.close", e);
				}
			}
		}

		return object;
	}

	private String agency;

//	private Map<String, Route> mRoutes = new HashMap<String, NextBusAPI.Route>();
//
//	private Map<RouteStop, StopPrediction> mStopRoutePredictionsCache = new HashMap<NextBusAPI.RouteStop, NextBusAPI.StopPrediction>();
//
//	private Map<String, Stop> mStopsByTitle = new HashMap<String, NextBusAPI.Stop>();

	private Date mStopsLastUpdated;

	private Collection<Stop> mStops;
	
	private Date mNearbyLastUpdated;
	
	private NearbyStopPredictions mNearbyPredictions;
	
	public Cache(String agency) {
		this.agency = agency;
	}

	public void save(Context context) {
		ObjectOutputStream objectOut = null;
		try {

			FileOutputStream fileOut = context
					.openFileOutput(Constants.NEXTBUS_API_CACHE_FILENAME,
							Activity.MODE_PRIVATE);
			objectOut = new ObjectOutputStream(fileOut);
			objectOut.writeObject(this);
			fileOut.getFD().sync();

		} catch (IOException e) {
			Log.e(this, "mNextBusAPI.saveCache", e);
		} finally {
			if (objectOut != null) {
				try {
					objectOut.close();
				} catch (IOException e) {
					Log.e(this, "mNextBusAPI.saveCache objectOut.close", e);
				}
			}
		}
	}

	public Collection<Stop> getStops() {
		return mStops;
	}

	public Date lastStopsUpdated() {
		return mStopsLastUpdated;
	}

	public void setStops(Collection<Stop> stops) {
		mStopsLastUpdated = new Date();
		mStops = stops;
	}
	
	public Date nearbyLastUpdated() {
		return mNearbyLastUpdated;
	}

	public NearbyStopPredictions getNearbyPredictions() {
		return mNearbyPredictions;
	}

	public NearbyStopPredictions setNearbyPredictions(
			NearbyStopPredictions predictions) {
		mNearbyPredictions = predictions;
		mNearbyLastUpdated = new Date();
		return predictions;
	}
}
