package com.danielstiner.cyride.service;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.widget.Toast;

import com.danielstiner.cyride.MainActivity;
import com.danielstiner.cyride.NearbyStopsFragment;
import com.danielstiner.cyride.R;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.Functor1;
import com.danielstiner.cyride.util.NextBusAPI;
import com.danielstiner.cyride.util.NextBusAPI.RouteStop;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;
import com.danielstiner.cyride.util.TextFormat;

import de.akquinet.android.androlog.Log;

public class NotificationService extends Service implements INotifications {
	
	private final static String CLASS = "com.danielstiner.cyride.service";

	private final static int NOTIFICATION = R.string.local_service_started;

	private static final String INTENT_EXTRA_SHOW_STOP_PREDICTIONS = CLASS
			+ ".show_routestop_predictions";

	private final List<StopPrediction> mNotificationStops = new LinkedList<StopPrediction>();

	private final ServiceConnector<ILocalService> mPredictionsService = LocalService.createConnection();
	
	private NotificationManager mNM;

	private Notification mNotification;

	private boolean mRepeatingAlarm;

	// method to construct the alarm intent
	private PendingIntent alarmIntent() {
		return PendingIntent.getService(this, 0, new Intent(this,
				LocalService.class), PendingIntent.FLAG_UPDATE_CURRENT);
	}

	public static void showStopPredictions(Context context,
			NextBusAPI.RouteStop stopAndRoute) {
		Intent i = new Intent(context, NotificationService.class);
		i.putExtra(INTENT_EXTRA_SHOW_STOP_PREDICTIONS, stopAndRoute);
		context.startService(i);
	}

	protected void cancelRepeatingAlarm() {
		PendingIntent alarmIntent = alarmIntent();

		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(alarmIntent);
		mRepeatingAlarm = false;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.init(this);
		
		mPredictionsService.bind(this);

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// TODO: Only run alarm if neccessary
		// startRepeatingAlarm();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		handleIntent(intent);

		return START_NOT_STICKY;
	}

	private void handleIntent(Intent intent) {
		
		if(intent.hasExtra(INTENT_EXTRA_SHOW_STOP_PREDICTIONS)) {
			// Should parse out and show intent
			RouteStop rs = (RouteStop) intent.getSerializableExtra(INTENT_EXTRA_SHOW_STOP_PREDICTIONS);
			showRouteStop(rs);
		}
	}

	private void showRouteStop(final RouteStop rs) {
		
		mPredictionsService.schedule(new Callback<ILocalService>() {
			@Override
			public void run(ILocalService predictions) {
				predictions.getPredictionsForRouteStop(rs, new Callback<StopPrediction>() {
					
					@Override
					public void run(StopPrediction prediction) {
						setRouteStopPredictions(rs, prediction);
					}					
				});
			}
		});
	}
	
	private void setRouteStopPredictions(RouteStop rs,
			StopPrediction prediction) {
		mNotificationStops.clear();
		mNotificationStops.add(prediction);
		showNotification();
	}

	public class LocalBinder extends Binder {
		NotificationService getService() {
			return NotificationService.this;
		}
	}
	
	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		mNM.cancel(NOTIFICATION);
		
		mPredictionsService.unbind(this);

		// Tell the user we stopped.
		Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT)
				.show();
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
				+ Constants.VIEW_UPDATE_INTERVAL;
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, timeToRefresh,
				Constants.VIEW_UPDATE_INTERVAL, alarmIntent);
	}

}
