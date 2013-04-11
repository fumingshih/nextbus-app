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
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.CallbackManager;
import com.danielstiner.cyride.util.Constants;
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

	public static class LocalServiceConnection {

		private LocalService mBoundService;

		private ServiceConnection mConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName className,
					IBinder service) {
				mBoundService = ((LocalService.LocalBinder) service)
						.getService();

				while (!mScheduledCallbacks.isEmpty())
					mScheduledCallbacks.poll().run(mBoundService);

				mBoundService.registerLocalServiceConnection(this);
			}

			public void onServiceDisconnected(ComponentName className) {
				mBoundService = null;
			}
		};
		private boolean mIsBound = false;
		private Queue<Callback<ILocalService>> mScheduledCallbacks = new LinkedList<Callback<ILocalService>>();

		private LocalServiceConnection() {

		}

		public void bind(Context context) {
			if (!mIsBound)
				context.bindService(new Intent(context, LocalService.class),
						mConnection, Context.BIND_AUTO_CREATE);
			mIsBound = true;
		}

		public void schedule(Callback<ILocalService> callback) {
			if (mIsBound && null != mBoundService) {
				callback.run(mBoundService);
			} else {
				mScheduledCallbacks.add(callback);
			}
		}

		public void unbind(Context context) {
			if (mIsBound) {
				// Detach our existing connection.
				context.unbindService(mConnection);
				mIsBound = false;
			}
		}

	}

	private class UpdateNearbyTask extends
			AsyncTask<Void, Void, Collection<StopPrediction>> {

		protected Collection<StopPrediction> doInBackground(Void... stuff) {
			LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			Location location = LocationUtil.getBestCurrentLocation(lm);

			// FIXME: Returning data temp for testing
			// if (location != null)
			// return DEFAULT_PREDICTION_DATA;

			if (null == location) {
				return DEFAULT_PREDICTION_DATA;
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
	private static final String AGENCY = "cyride";

	public static final List<StopPrediction> DEFAULT_PREDICTION_DATA;

	private final static int NOTIFICATION = R.string.local_service_started;

	private static final int UPDATE_FREQUENCY_SEC = 1;

	static {
		LinkedList<StopPrediction> p = new LinkedList<StopPrediction>();
		StopPrediction s = new StopPrediction();
		s.route = new Route("NXN", "NX-N Express", 333);
		s.route.direction = "Outbound to Balboa Park Station";
		s.stop = new Stop("San Jose Ave & Mt Vernon Ave");
		s.predictions.add(new Prediction(
				new Date(new Date().getTime() + 100000)));
		p.add(s);
		DEFAULT_PREDICTION_DATA = p;
	}

	/**
	 * Call at most once per context
	 * 
	 * @param context
	 * @return
	 */
	public static LocalServiceConnection createConnection() {
		return new LocalServiceConnection();
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

	private NotificationManager mNM;

	private Notification mNotification;

	private final List<StopPrediction> mNotificationStops = new LinkedList<StopPrediction>();

	private boolean mRepeatingAlarm;

	private CallbackManager<Collection<StopPrediction>> NearbyStopPredictionsByRouteListeners = new CallbackManager<Collection<StopPrediction>>();

	@Override
	public void addNearbyStopPredictionsByRouteListener(
			StopPredictionsListener predictionListener) {
		NearbyStopPredictionsByRouteListeners.addListener(predictionListener);
	}

	// method to construct the alarm intent
	private PendingIntent alarmIntent() {
		return PendingIntent.getService(this, 0, new Intent(this,
				LocalService.class), PendingIntent.FLAG_UPDATE_CURRENT);
	}

	protected void cancelRepeatingAlarm() {
		PendingIntent alarmIntent = alarmIntent();

		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(alarmIntent);
		mRepeatingAlarm = false;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		startRepeatingAlarm();

		showNotification();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		mNM.cancel(NOTIFICATION);

		// Tell the user we stopped.
		Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT)
				.show();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		// TODO: Parse intent, either a start service or a display bus
		showNotification();

		return START_STICKY;
	}

	private void registerLocalServiceConnection(
			ServiceConnection serviceConnection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeNearbyStopPredictionsByRouteListener(
			StopPredictionsListener predictionListener) {
		NearbyStopPredictionsByRouteListeners
				.removeListener(predictionListener);
	}

	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.local_service_started);

		if (!mNotificationStops.isEmpty())
			text = TextFormat.toString(mNotificationStops.get(0).route)
					+ "   "
					+ TextFormat
							.toString(mNotificationStops.get(0).predictions);

		// Set the icon, scrolling text and timestamp
		if (mNotification == null) {
			mNotification = new Notification(R.drawable.ic_launcher, text,
					System.currentTimeMillis());

		}

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, MainActivity.class),
				PendingIntent.FLAG_UPDATE_CURRENT);

		// Set the info for the views that show in the notification panel.
		mNotification.setLatestEventInfo(this,
				getText(R.string.local_service_started), text, contentIntent);

		// Send the notification.
		mNM.notify(NOTIFICATION, mNotification);
	}

	@Override
	public void showNotification(StopPrediction prediction) {
		mNotificationStops.clear();
		mNotificationStops.add(prediction);
		this.showNotification();
	}

	private void startRepeatingAlarm() {
		if (mRepeatingAlarm)
			return;
		mRepeatingAlarm = true;

		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		PendingIntent alarmIntent = alarmIntent();

		long timeToRefresh = SystemClock.elapsedRealtime()
				+ UPDATE_FREQUENCY_SEC * 1000;
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, timeToRefresh,
				UPDATE_FREQUENCY_SEC * 1000, alarmIntent);
	}

	@Override
	public void updateNearbyStopPredictionsByRoute() {
		new UpdateNearbyTask().execute();
	}
}
