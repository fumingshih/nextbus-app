package com.danielstiner.cyride.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

import org.dom4j.DocumentException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

import com.danielstiner.cyride.util.CallbackManager;
import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.Functor1;
import com.danielstiner.cyride.util.LocationUtil;
import com.danielstiner.cyride.util.NextBusAPI;
import com.danielstiner.cyride.util.NextBusAPI.Cache;
import com.danielstiner.cyride.util.NextBusAPI.Prediction;
import com.danielstiner.cyride.util.NextBusAPI.Stop;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;

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

			if (null != location) {

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
		return ServiceConnector.createConnection(LocalService.class,
				new Functor1<IBinder, ILocalService>() {
					@Override
					public ILocalService apply(IBinder service) {
						return ((LocalBinder) service).getService();
					}

				});
	}

	private final IBinder mBinder = new LocalBinder();

	private NextBusAPI mNextBusAPI;

	private CallbackManager<Collection<StopPrediction>> NearbyStopPredictionsByRouteListeners = new CallbackManager<Collection<StopPrediction>>();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		this.mNextBusAPI = new NextBusAPI(Constants.AGENCY,
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

					@Override
					public void saveCache(Cache cache) {
						ObjectOutputStream objectOut = null;
						try {

							FileOutputStream fileOut = LocalService.this
									.openFileOutput(
											Constants.NEXTBUS_API_CACHE_FILENAME,
											Activity.MODE_PRIVATE);
							objectOut = new ObjectOutputStream(fileOut);
							objectOut.writeObject(cache);
							fileOut.getFD().sync();

						} catch (IOException e) {
							e.printStackTrace();
							// TODO: Log
						} finally {
							if (objectOut != null) {
								try {
									objectOut.close();
								} catch (IOException e) {
									// TODO: Log
								}
							}
						}
					}

					@Override
					public Cache loadCache() {
						ObjectInputStream objectIn = null;
						Cache object = null;
						try {

							FileInputStream fileIn = LocalService.this
									.getApplicationContext()
									.openFileInput(
											Constants.NEXTBUS_API_CACHE_FILENAME);
							objectIn = new ObjectInputStream(fileIn);
							object = (Cache) objectIn.readObject();

						} catch (FileNotFoundException e) {
							// NOOP
						} catch (IOException e) {
							e.printStackTrace();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						} finally {
							if (objectIn != null) {
								try {
									objectIn.close();
								} catch (IOException e) {
									// TODO: Log
								}
							}
						}

						return object;
					}
				});
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		mNextBusAPI.shutdown();
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
