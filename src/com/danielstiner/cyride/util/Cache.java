package com.danielstiner.cyride.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;

import com.danielstiner.cyride.service.LocalService;
import com.danielstiner.cyride.util.NextBusAPI.Route;
import com.danielstiner.cyride.util.NextBusAPI.RouteStop;
import com.danielstiner.cyride.util.NextBusAPI.Stop;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;

import de.akquinet.android.androlog.Log;

public class Cache implements Serializable {
	
	private static final String TAG = "Cache";

	public Cache(String agency) {
		this.agency = agency;
	}

	private String agency;

	private static final long serialVersionUID = -5003315853529912609L;

	private Map<String, Route> mRoutes = new HashMap<String, NextBusAPI.Route>();

	private Map<RouteStop, StopPrediction> mStopRoutePredictionsCache = new HashMap<NextBusAPI.RouteStop, NextBusAPI.StopPrediction>();

	private Map<String, Stop> mStopsByTitle = new HashMap<String, NextBusAPI.Stop>();

	private Date mStopsLastUpdated;

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
}
