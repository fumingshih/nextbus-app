package com.danielstiner.cyride.service;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.dom4j.DocumentException;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.widget.Toast;

import com.danielstiner.cyride.MainActivity;
import com.danielstiner.cyride.R;
import com.danielstiner.cyride.R.drawable;
import com.danielstiner.cyride.R.string;
import com.danielstiner.cyride.service.IPredictions.StopPredictionsListener;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.CallbackManager;
import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.Functor1;
import com.danielstiner.cyride.util.LocationUtil;
import com.danielstiner.cyride.util.NextBusAPI;
import com.danielstiner.cyride.util.NextBusAPI.CachePolicy;
import com.danielstiner.cyride.util.NextBusAPI.Prediction;
import com.danielstiner.cyride.util.NextBusAPI.Route;
import com.danielstiner.cyride.util.NextBusAPI.Stop;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;
import com.danielstiner.cyride.util.TextFormat;

public class LocalService extends android.app.Service implements ILocalService {

	public class LocalBinder extends Binder {
		LocalService getService() {
			return LocalService.this;
		}
	}
	@Override
	public void addNearbyStopPredictionsByRouteListener(
			StopPredictionsListener predictionListener) {
		NearbyStopPredictionsByRouteListeners.addListener(predictionListener);
	}
	
	private class UpdateNearbyTask extends
			AsyncTask<Void, Void, Collection<StopPrediction>> {

		protected Collection<StopPrediction> doInBackground(Void... stuff) {
			LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			Location location = LocationUtil.getBestCurrentLocation(lm);

			if (null == location) {
				return null;
			}

			try {
				Collection<Stop> stops = NextBusAPI.nearestStopPerRoute(
						mNextBusAPI.getStops(), location);

				return mNextBusAPI.getStopPredictions(stops);

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DocumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return new LinkedList<StopPrediction>();
		}

		protected void onPostExecute(Collection<StopPrediction> predictions) {
			NearbyStopPredictionsByRouteListeners.runAll(predictions);
		}
	}

	/**
	 * Call at most once per context
	 * 
	 * @param context
	 * @return
	 */
	public static ServiceConnector<ILocalService> createConnection() {
		return ServiceConnector
				.createConnection(LocalService.class, new Functor1<IBinder, ILocalService>() {
					@Override
					public ILocalService apply(IBinder service) {
						return ((LocalBinder) service).getService();
					}

				});
	}

	private final IBinder mBinder = new LocalBinder();

	private final NextBusAPI mNextBusAPI = new NextBusAPI(Constants.AGENCY,
			new NextBusAPI.CachePolicy() {

				@Override
				public boolean shouldUpdateStopPredictions(
						StopPrediction stopPrediction) {
					if (stopPrediction == null)
						return true;

					for (Prediction p : stopPrediction.predictions) {
						if (new Date().after(p.arrival))
							return true;
					}

					return false;
				}

				@Override
				public boolean shouldUpdateStops(Date lastUpdate) {
					return lastUpdate == null;
				}
			});

	private CallbackManager<Collection<StopPrediction>> NearbyStopPredictionsByRouteListeners = new CallbackManager<Collection<StopPrediction>>();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
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
	public void updateNearbyStopPredictionsByRoute() {
		new UpdateNearbyTask().execute();
	}
}
