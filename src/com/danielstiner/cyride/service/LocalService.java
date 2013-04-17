package com.danielstiner.cyride.service;

import java.net.MalformedURLException;
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

public class LocalService extends android.app.Service implements ILocalService {

	public class LocalBinder extends Binder {
		LocalService getService() {
			return LocalService.this;
		}
	}

	private class UpdateNearbyTask extends
			AsyncTask<Void, Void, Collection<StopPrediction>> {

		protected Collection<StopPrediction> doInBackground(Void... stuff) {
			return getLatestNearbyStopPredictions();
		}

		protected void onPostExecute(Collection<StopPrediction> predictions) {
			NearbyStopPredictionsByRouteListeners.runAll(predictions);

			MyWidgetService.updateNearbyWidgets(LocalService.this);
		}
	}

	/**
	 * Call at most once per context
	 * 
	 * @param context
	 * @return
	 */
	public static ServiceConnector<ILocalService> createConnection() {
		return ServiceConnector.createConnection(LocalService.class,
				new Functor1<IBinder, ILocalService>() {
					@Override
					public ILocalService apply(IBinder service) {
						return ((LocalBinder) service).getService();
					}

				});
	}

	private final IBinder mBinder = new LocalBinder();

	private Cache mCache;

	private NextBusAPI mNextBusAPI;

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
				Collection<Stop> stops = NextBusAPI.nearestStopPerRoute(
						mNextBusAPI.getStops(), location);

				return mNextBusAPI.getStopPredictions(stops);

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
}
