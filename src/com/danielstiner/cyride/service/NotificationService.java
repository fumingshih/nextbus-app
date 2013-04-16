package com.danielstiner.cyride.service;

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
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.danielstiner.cyride.MainActivity;
import com.danielstiner.cyride.R;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.NextBusAPI;
import com.danielstiner.cyride.util.NextBusAPI.RouteStop;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;
import com.danielstiner.cyride.util.TextFormat;

import de.akquinet.android.androlog.Log;

public class NotificationService extends Service {

	private final static String CLASS = "com.danielstiner.cyride.service";

	private final static int NOTIFICATION = R.string.local_service_started;

	private static final String INTENT_EXTRA_SHOW_STOP_PREDICTIONS = CLASS
			+ ".show_routestop_predictions";

	private final ServiceConnector<ILocalService> mPredictionsService = LocalService
			.createConnection();

	private NotificationManager mNotificationManager;

	private NotificationCompat.Builder mBuilder;

	private Notification mNotification;

	private boolean mNotificationInProgress = false;

	public static void showStopPredictions(Context context,
			NextBusAPI.RouteStop stopAndRoute) {
		Intent i = new Intent(context, NotificationService.class);
		i.putExtra(INTENT_EXTRA_SHOW_STOP_PREDICTIONS, stopAndRoute);
		context.startService(i);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Log.init(this);

		mPredictionsService.bind(this);

		mBuilder = new NotificationCompat.Builder(this);

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		handleIntent(intent);

		return START_NOT_STICKY;
	}

	private void handleIntent(Intent intent) {

		if (intent.hasExtra(INTENT_EXTRA_SHOW_STOP_PREDICTIONS)) {
			// Parse out and show stop from intent
			showRouteStop((RouteStop) intent
					.getSerializableExtra(INTENT_EXTRA_SHOW_STOP_PREDICTIONS));
		}
	}

	private void showRouteStop(final RouteStop rs) {
		mPredictionsService.schedule(new Callback<ILocalService>() {
			@Override
			public void run(ILocalService predictions) {
				predictions.addRouteStopListener(rs,
						new Callback<StopPrediction>() {
							@Override
							public void run(StopPrediction prediction) {
								setRouteStopPredictions(rs, prediction);
							}
						});
			}
		});
	}

	private void setRouteStopPredictions(RouteStop rs, StopPrediction prediction) {
		updateNotification(prediction);
	}

	public class LocalBinder extends Binder {
		NotificationService getService() {
			return NotificationService.this;
		}
	}

	private final IBinder mBinder = new LocalBinder();

	private int mNotificationId;

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		mNotificationManager.cancel(NOTIFICATION);

		mPredictionsService.unbind(this);
	}

	private void updateNotification(StopPrediction p) {
		if (!mNotificationInProgress) {
			buildNotification();
			mBuilder.setTicker("");
		}

		if (p == null || p.predictions.isEmpty()) {
			// TODO
		}

		mBuilder.setContentText("");

		// Copy when of first prediction
		long when = p.predictions.get(0).arrival.getTime();

		// (first row) of the notification, in a standard notification.
		CharSequence title = TextFormat.toString(p.route);

		// ((second row) of the notification, in a standard notification.
		CharSequence text = TextFormat.toString(p.stop);

		// large text at the right-hand side of the notification.
		CharSequence info = TextFormat.toString(p.predictions.get(0));

		mBuilder.setContentTitle(title).setContentText(text)
				.setContentInfo(info).setWhen(when);

		mNotificationManager
				.notify(mNotificationId, mBuilder.getNotification());

		mNotificationInProgress = true;
	}

	private void buildNotification() {
		mBuilder.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("My Notification")
				.setOnlyAlertOnce(true)
				.setOngoing(true)
				.setAutoCancel(true)
				.setContentIntent(
						PendingIntent.getActivity(this, 0, new Intent(this,
								MainActivity.class),
								PendingIntent.FLAG_UPDATE_CURRENT))
				.setDeleteIntent(
						PendingIntent.getService(this, 0, new Intent(this, // TODO This intent should kill the service
								NotificationService.class),
								PendingIntent.FLAG_UPDATE_CURRENT));
		// setUsesChronometer

	}

	// private void showNotification() {
	// // In this sample, we'll use the same text for the ticker and the
	// // expanded notification
	// CharSequence text = getText(R.string.local_service_started);
	//
	// if (!mNotificationStops.isEmpty())
	// text = TextFormat.toString(mNotificationStops.get(0).route)
	// + "   "
	// + TextFormat
	// .toString(mNotificationStops.get(0).predictions);
	//
	// // Set the icon, scrolling text and timestamp
	// if (mNotification == null) {
	// mNotification = new Notification(R.drawable.ic_launcher, text,
	// System.currentTimeMillis());
	//
	// }
	//
	// // The PendingIntent to launch our activity if the user selects this
	// // notification
	// PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
	// new Intent(this, MainActivity.class),
	// PendingIntent.FLAG_UPDATE_CURRENT);
	//
	// // Set the info for the views that show in the notification panel.
	// mNotification.setLatestEventInfo(this,
	// getText(R.string.local_service_started), text, contentIntent);
	//
	// // Send the notification.
	// mNM.notify(NOTIFICATION, mNotification);
	// }
}
