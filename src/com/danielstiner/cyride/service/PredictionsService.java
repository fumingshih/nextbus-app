package com.danielstiner.cyride.service;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.dom4j.DocumentException;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

import com.danielstiner.cyride.util.Cache;
import com.danielstiner.cyride.util.CachePolicy;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.CallbackManager;
import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.Functor1;
import com.danielstiner.cyride.util.LocationUtil;
import com.danielstiner.cyride.util.NextBusAPI;
import com.danielstiner.cyride.util.NextBusAPI.RouteStop;
import com.danielstiner.cyride.util.NextBusAPI.Stop;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;
import com.danielstiner.cyride.widget.MyWidgetService;

import de.akquinet.android.androlog.Log;

public class PredictionsService extends android.app.Service implements
		IPredictions {

	public class LocalBinder extends Binder {
		PredictionsService getService() {
			return PredictionsService.this;
		}
	}

	private class UpdateNearbyTask extends
			AsyncTask<Void, Void, Collection<StopPrediction>> {

		protected Collection<StopPrediction> doInBackground(Void... stuff) {
			return getLatestNearbyStopPredictions();
		}

		protected void onPostExecute(Collection<StopPrediction> predictions) {
			NearbyStopPredictionsByRouteListeners.runAll(predictions);

			MyWidgetService.updateNearbyWidgets(PredictionsService.this);
		}
	}

	private class UpdateRouteStopTask extends
			AsyncTask<RouteStop, Void, Collection<StopPrediction>> {

		protected Collection<StopPrediction> doInBackground(
				RouteStop... routeStops) {
			try {
				return mNextBusAPI.getRouteStopPredictions(Arrays
						.asList(routeStops));
			} catch (DocumentException e) {
				Log.e(this, "UpdateRouteStopTask.doInBackground", e);
			} catch (MalformedURLException e) {
				Log.e(this, "UpdateRouteStopTask.doInBackground", e);
			}
			return null;
		}

		protected void onPostExecute(Collection<StopPrediction> predictions) {
			for (StopPrediction p : predictions) {
				if (RouteStopListeners.containsKey(p.routestop))
					RouteStopListeners.get(p.routestop).runAll(p);
			}
		}
	}

	/**
	 * Call at most once per context
	 * 
	 * @param context
	 * @return
	 */
	public static ServiceConnector<IPredictions> createConnection() {
		return ServiceConnector.createConnection(PredictionsService.class,
				new Functor1<IBinder, IPredictions>() {
					@Override
					public IPredictions apply(IBinder service) {
						return ((LocalBinder) service).getService();
					}

				});
	}

	private final IBinder mBinder = new LocalBinder();

	private Cache mCache;

	private NextBusAPI mNextBusAPI;

	private CachePolicy mCachePolicy;

	private CallbackManager<Collection<StopPrediction>> NearbyStopPredictionsByRouteListeners = new CallbackManager<Collection<StopPrediction>>();

	private Map<RouteStop, CallbackManager<StopPrediction>> RouteStopListeners = new HashMap<RouteStop, CallbackManager<StopPrediction>>();

	@Override
	public void addNearbyStopPredictionsByRouteListener(
			StopPredictionsListener predictionListener) {
		NearbyStopPredictionsByRouteListeners.addListener(predictionListener);
	}

	@Override
	public void addRouteStopListener(RouteStop rs,
			Callback<StopPrediction> predictionListener) {

		if (!RouteStopListeners.containsKey(rs))
			RouteStopListeners.put(rs,
					new CallbackManager<NextBusAPI.StopPrediction>());

		RouteStopListeners.get(rs).addListener(predictionListener);
	}

	@Override
	public Collection<StopPrediction> getLatestNearbyStopPredictions() {
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Location location = LocationUtil.getBestCurrentLocation(lm);

		if (null != location) {

			try {
				final Collection<Stop> allStops;

				if (mCachePolicy.shouldUpdateStops(mCache)) {
					allStops = mNextBusAPI.getStops();
					mCache.setStops(allStops);
				} else {
					allStops = mCache.getStops();
				}

				Collection<Stop> nearestStops = NextBusAPI.nearestStopPerRoute(
						allStops, location);

				// TODO: Maybe only update stops that need updating

				return mNextBusAPI.getStopPredictions(nearestStops);

			} catch (MalformedURLException e) {
				Log.e(this, "UpdateNearbyTask.doInBackground objectIn.close", e);
			} catch (DocumentException e) {
				Log.e(this, "UpdateNearbyTask.doInBackground objectIn.close", e);
			}

		}

		return new LinkedList<StopPrediction>();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Log.init(this);

		mNextBusAPI = new NextBusAPI(Constants.AGENCY);

		mCache = Cache.loadCache(Constants.AGENCY, this);

		mCachePolicy = new CachePolicy() {

			@Override
			public boolean shouldUpdateStops(Cache cache) {
				return mCache.lastStopsUpdated() == null; // TODO: Also update
															// if more than a
															// day old or
															// something
			}

			@Override
			public boolean shouldUpdateStopPredictions(StopPrediction c,
					Cache cache) {
				return true;
			}
		};
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		mCache.save(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		return START_NOT_STICKY;
	}

	@Override
	public void removeNearbyStopPredictionsByRouteListener(
			StopPredictionsListener predictionListener) {
		NearbyStopPredictionsByRouteListeners
				.removeListener(predictionListener);
	}

	@Override
	public void removeRouteStopListener(RouteStop rs,
			Callback<StopPrediction> listener) {
		if (RouteStopListeners.containsKey(rs)) {
			CallbackManager<StopPrediction> m = RouteStopListeners.get(rs);

			m.removeListener(listener);

			if (!m.active()) {
				RouteStopListeners.remove(rs);
			}
		}
	}

	@Override
	public void updateNearbyStopPredictionsByRoute() {
		new UpdateNearbyTask().execute();
	}

	@Override
	public void updateRouteStopPredictions(RouteStop rs) {
		new UpdateRouteStopTask().execute(rs);
	}
}
