package com.danielstiner.cyride;

import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

import org.dom4j.DocumentException;

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
import android.widget.Toast;

import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.CallbackManager;
import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.LocationUtil;
import com.danielstiner.cyride.util.NextBusAPI;
import com.danielstiner.cyride.util.NextBusAPI.Route;
import com.danielstiner.cyride.util.NextBusAPI.Stop;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;

public class LocalService extends android.app.Service implements ILocalService {

	public class LocalBinder extends Binder {
		LocalService getService() {
			return LocalService.this;
		}
	}

	public static class LocalServiceConnection {

		private LocalService mBoundService;

		private boolean mIsBound = false;
		private Queue<Callback<ILocalService>> mScheduledCallbacks = new LinkedList<Callback<ILocalService>>();
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

	public static final List<StopPrediction> DEFAULT_PREDICTION_DATA;
	static {
		LinkedList<StopPrediction> p = new LinkedList<StopPrediction>();
		StopPrediction s = new StopPrediction();
		s.stopTitle = "A Stop";
		p.add(s);
		DEFAULT_PREDICTION_DATA = p;
	}

	private class UpdateNearbyTask extends
			AsyncTask<Void, Void, List<StopPrediction>> {

		protected List<StopPrediction> doInBackground(Void... stuff) {
			LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			Location location = LocationUtil.getBestCurrentLocation(lm);

			if (location != null)
				return DEFAULT_PREDICTION_DATA;

			if (null == location) {
				return DEFAULT_PREDICTION_DATA;
			}

			try {
				List<Stop> stops = NextBusAPI.nearestStops(
						mNextBusAPI.getStops(Constants.AGENCY), location,
						Constants.MAX_STOPS);

				return mNextBusAPI.getStopPredictions(stops, Constants.AGENCY);

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DocumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return new LinkedList<StopPrediction>();
		}

		protected void onPostExecute(List<StopPrediction> predictions) {
			NearbyStopPredictionsByRouteListeners.runAll(predictions);
		}
	}

	private static final String AGENCY = "cyride";

	private final static int NOTIFICATION = R.string.local_service_started;

	/**
	 * Call at most once per context
	 * 
	 * @param context
	 * @return
	 */
	public static LocalServiceConnection createConnection() {
		return new LocalServiceConnection();
	}

	private final List<StopPrediction> mNotificationStops = new LinkedList<StopPrediction>();

	private final NextBusAPI mNextBusAPI = new NextBusAPI();

	private NotificationManager mNM;

	private CallbackManager<List<StopPrediction>> NearbyStopPredictionsByRouteListeners = new CallbackManager<List<StopPrediction>>();

	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

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

		return START_STICKY;
	}

	protected void registerLocalServiceConnection(
			ServiceConnection serviceConnection) {
		// TODO Auto-generated method stub

	}

	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.local_service_started);
		
		if(!mNotificationStops.isEmpty())
			text = mNotificationStops.get(0).stopTitle;

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_launcher,
				text, System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, NearbyStopsFragment.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this,
				getText(R.string.local_service_started), text, contentIntent);

		// Send the notification.
		mNM.notify(NOTIFICATION, notification);
	}

	@Override
	public void showNotification(StopPrediction prediction) {
		mNotificationStops.clear();
		mNotificationStops.add(prediction);
		this.showNotification();
	}

	@Override
	public void addNearbyStopPredictionsByRouteListener(
			StopPredictionsListener predictionListener) {
		NearbyStopPredictionsByRouteListeners.addListener(predictionListener);
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
