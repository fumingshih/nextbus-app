package com.danielstiner.cyride.service;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.dom4j.DocumentException;
import org.joda.time.DateTime;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.danielstiner.cyride.behavior.IPredictionUpdateStrategy;
import com.danielstiner.cyride.util.Cache;
import com.danielstiner.cyride.util.CachePolicy;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.CallbackManager;
import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.DateUtil;
import com.danielstiner.cyride.util.Functor1;
import com.danielstiner.cyride.util.LocationUtil;
import com.danielstiner.cyride.util.PredictionsUpdater;
import com.danielstiner.cyride.util.Task;
import com.danielstiner.cyride.widget.MyWidgetService;
import com.danielstiner.nextbus.NextBusAPI;
import com.danielstiner.nextbus.NextBusAPI.RouteStop;
import com.danielstiner.nextbus.NextBusAPI.Stop;
import com.danielstiner.nextbus.NextBusAPI.StopPrediction;

import de.akquinet.android.androlog.Log;

public class PredictionsService extends android.app.Service implements
		IPredictions {

	public class LocalBinder extends Binder {
		PredictionsService getService() {
			return PredictionsService.this;
		}
	}

	private class UpdateNearbyTask extends
			AsyncTask<Void, Void, NearbyStopPredictions> {

		protected NearbyStopPredictions doInBackground(Void... stuff) {
			return getLatestNearbyStopPredictions();
		}

		protected void onPostExecute(NearbyStopPredictions predictions) {
			if (predictions == null) {
				// TODO schedule reasonable timeout on mNearbyUpdateScheduler
				return;
			}

			NearbyStopPredictionsByRouteListeners.runAll(predictions);

			mNearbyUpdateScheduler.schedule(mHandler);

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

	private Handler mHandler;

	private CallbackManager<NearbyStopPredictions> NearbyStopPredictionsByRouteListeners = new CallbackManager<NearbyStopPredictions>();

	private Map<RouteStop, CallbackManager<StopPrediction>> RouteStopListeners = new HashMap<RouteStop, CallbackManager<StopPrediction>>();

	private Map<NearbyStopPredictionsListener, IPredictionUpdateStrategy> mNearbyUpdateStrategies = new HashMap<NearbyStopPredictionsListener, IPredictionUpdateStrategy>();

	private Map<Callback<StopPrediction>, IPredictionUpdateStrategy> mRouteStopUpdateStrategies = new HashMap<Callback<StopPrediction>, IPredictionUpdateStrategy>();

	private PredictionsUpdater mNearbyUpdateScheduler = new PredictionsUpdater(
			new Task<DateTime>() {
				@Override
				public DateTime get() {
					DateTime nextUpdate = DateTime.now().plus(
							Constants.LONGEST_UPDATE_TIME);
					for (IPredictionUpdateStrategy s : mNearbyUpdateStrategies.values()) {
						nextUpdate = DateUtil.earlier(nextUpdate, s
								.nextPredictionUpdate(mCache
										.getNearbyPredictions()));
					}
					return nextUpdate;
				}
			}, new Runnable() {
				public void run() {
					new UpdateNearbyTask().execute();
				}
			});

	private PredictionsUpdater mRouteStopUpdateScheduler = new PredictionsUpdater(
			new Task<DateTime>() {
				@Override
				public DateTime get() {
					DateTime nextUpdate = DateTime.now().plus(
							Constants.LONGEST_UPDATE_TIME);
					for (IPredictionUpdateStrategy s : mRouteStopUpdateStrategies
							.values()) {
						nextUpdate = DateUtil.earlier(nextUpdate, s
								.nextPredictionUpdate(mCache
										.getNearbyPredictions()));
					}
					return nextUpdate;
				}
			}, new Runnable() {
				public void run() {
					new UpdateNearbyTask().execute();
				}
			});

	@Override
	public void addNearbyStopPredictionsByRouteListener(
			NearbyStopPredictionsListener predictionListener,
			IPredictionUpdateStrategy predictionUpdateStrategy) {

		NearbyStopPredictionsByRouteListeners.addListener(predictionListener);

		addNearbyUpdateStrategy(predictionListener, predictionUpdateStrategy);

		if (mCache.getNearbyPredictions() != null)
			predictionListener.run(mCache.getNearbyPredictions());
	}

	private void addNearbyUpdateStrategy(NearbyStopPredictionsListener l,
			IPredictionUpdateStrategy s) {
		mNearbyUpdateStrategies.put(l, s);
		mNearbyUpdateScheduler.schedule(mHandler);
	}

	@Override
	public void addRouteStopListener(RouteStop rs,
			Callback<StopPrediction> predictionListener,
			IPredictionUpdateStrategy updateStrategy) {

		// TODO Use updateStrategy
		addRouteStopUpdateStrategy(predictionListener, updateStrategy);

		if (!RouteStopListeners.containsKey(rs))
			RouteStopListeners.put(rs,
					new CallbackManager<NextBusAPI.StopPrediction>());

		RouteStopListeners.get(rs).addListener(predictionListener);
	}

	private void addRouteStopUpdateStrategy(Callback<StopPrediction> l,
			IPredictionUpdateStrategy s) {
		mRouteStopUpdateStrategies.put(l, s);
		mRouteStopUpdateScheduler.schedule(mHandler);
	}

	@Override
	public NearbyStopPredictions getLatestNearbyStopPredictions(
			IPredictionUpdateStrategy updateStrategy) {

		if (updateStrategy.nextPredictionUpdate(mCache.getNearbyPredictions())
				.isAfterNow()) {
			return getLatestNearbyStopPredictions();
		} else {
			return mCache.getNearbyPredictions();
		}
	}

	private NearbyStopPredictions getLatestNearbyStopPredictions() {

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
				return mCache
						.setNearbyPredictions(new NearbyStopPredictions(
								mNextBusAPI.getStopPredictions(nearestStops),
								location));

			} catch (MalformedURLException e) {
				Log.w(this, "UpdateNearbyTask.doInBackground objectIn.close", e);
			} catch (DocumentException e) {
				Log.w(this, "UpdateNearbyTask.doInBackground objectIn.close", e);
			}

		}

		return new NearbyStopPredictions(new LinkedList<StopPrediction>(), null);
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

		mHandler = new Handler();

		mCachePolicy = new CachePolicy() {

			@Override
			public boolean shouldUpdateStops(Cache cache) {
				return mCache.lastStopsUpdated() == null; // TODO: Also update
															// if more than a
															// day old or
															// something
			}
		};

		Log.v(this, "Prediction service started");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.v(this, "Prediction service shutting down");
		mNearbyUpdateScheduler.stop();
		mCache.save(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		return START_NOT_STICKY;
	}

	@Override
	public void removeNearbyStopPredictionsByRouteListener(
			NearbyStopPredictionsListener l) {
		NearbyStopPredictionsByRouteListeners.removeListener(l);
		mNearbyUpdateStrategies.remove(l);
	}

	@Override
	public void removeRouteStopListener(RouteStop rs, Callback<StopPrediction> l) {

		mRouteStopUpdateStrategies.remove(l);

		if (RouteStopListeners.containsKey(rs)) {
			CallbackManager<StopPrediction> m = RouteStopListeners.get(rs);

			m.removeListener(l);

			if (!m.active()) {
				RouteStopListeners.remove(rs);
			}
		}
	}

	@Override
	public void updateNearbyStopPredictionsByRoute() {
		// TODO Make it a request for updates that schedules an update as soon
		// as possible
		// if
		// (updateStrategy.nextPredictionUpdate(mCache.getNearbyPredictions())
		// .isAfterNow())
		new UpdateNearbyTask().execute();
	}

	@Override
	public void updateRouteStopPredictions(RouteStop rs,
			IPredictionUpdateStrategy updateStrategy) {
		// TODO Try to cache these
		// TODO Make it a request for updates that schedules an update as soon
		// as possible
		new UpdateRouteStopTask().execute(rs);
	}

	@Override
	public Location getLastKnownLocation() {
		// TODO Base on a constantly updated best known location
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Location location = LocationUtil.getBestCurrentLocation(lm);
		return location;
	}
}
