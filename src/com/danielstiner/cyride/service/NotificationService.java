package com.danielstiner.cyride.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.danielstiner.cyride.MainActivity;
import com.danielstiner.cyride.R;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.NextBusAPI;
import com.danielstiner.cyride.util.NextBusAPI.RouteStop;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;
import com.danielstiner.cyride.util.TextFormat;
import com.danielstiner.cyride.view.RemoteViewsProvider;

import de.akquinet.android.androlog.Log;

public class NotificationService extends Service {

	public class LocalBinder extends Binder {
		NotificationService getService() {
			return NotificationService.this;
		}
	}

	private final static String CLASS = "com.danielstiner.cyride.service";

	public static final String INTENT_EXTRA_SHOW_STOP_PREDICTIONS = CLASS
			+ ".show_routestop_predictions";

	private final static int NOTIFICATION = R.string.local_service_started;

	public static final String ACTION_NOTIFY = CLASS + ".ACTION_NOTIFY";

	public static void putExtraRouteStop(Bundle extras, RouteStop routestop) {
		extras.putSerializable(
				NotificationService.INTENT_EXTRA_SHOW_STOP_PREDICTIONS,
				routestop);
	}

	public static void showStopPredictions(Context context,
			NextBusAPI.RouteStop stopAndRoute) {
		Intent i = new Intent(context, NotificationService.class);
		i.setAction(NotificationService.ACTION_NOTIFY);
		i.putExtra(INTENT_EXTRA_SHOW_STOP_PREDICTIONS, stopAndRoute);
		context.startService(i);
	}

	private final IBinder mBinder = new LocalBinder();

	private NotificationCompat.Builder mBuilder;

	private int mNotificationId = 1;

	private boolean mNotificationInProgress = false;

	private NotificationManager mNotificationManager;

	private final ServiceConnector<IPredictions> mPredictionsService = PredictionsService
			.createConnection();

	private void buildNotification() {
		mBuilder.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("")
				.setOnlyAlertOnce(true)
				.setOngoing(false)
				.setAutoCancel(false)
				.setContentIntent(
						PendingIntent.getActivity(this, 0, new Intent(this,
								MainActivity.class),
								PendingIntent.FLAG_UPDATE_CURRENT))
				.setDeleteIntent(
						PendingIntent.getService(this, 0, new Intent(this, // TODO
																			// This
																			// intent
																			// should
																			// kill
																			// the
																			// service
								NotificationService.class),
								PendingIntent.FLAG_UPDATE_CURRENT));
		// setUsesChronometer

	}

	private void handleIntent(Intent intent) {

		if (intent.hasExtra(INTENT_EXTRA_SHOW_STOP_PREDICTIONS)) {
			// Parse out and show stop from intent
			showRouteStop((RouteStop) intent
					.getSerializableExtra(INTENT_EXTRA_SHOW_STOP_PREDICTIONS));
		}
		
		if(ACTION_NOTIFY.equals(intent.getAction())) {
			showRouteStop((RouteStop) intent
					.getSerializableExtra(INTENT_EXTRA_SHOW_STOP_PREDICTIONS));
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
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
	public void onDestroy() {
		super.onDestroy();

		mNotificationManager.cancel(NOTIFICATION);

		mPredictionsService.unbind(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		handleIntent(intent);

		return START_NOT_STICKY;
	}

	private void setRouteStopPredictions(RouteStop rs, StopPrediction prediction) {
		updateNotification(prediction);
	}

	private void showRouteStop(final RouteStop rs) {
		mPredictionsService.schedule(new Callback<IPredictions>() {
			@Override
			public void run(IPredictions predictions) {
				predictions.addRouteStopListener(rs,
						new Callback<StopPrediction>() {
							@Override
							public void run(StopPrediction prediction) {
								setRouteStopPredictions(rs, prediction);
							}
						});
				predictions.updateRouteStopPredictions(rs);
			}
		});
	}

	private void updateNotification(StopPrediction p) {
		if (!mNotificationInProgress) {
			buildNotification();
			mBuilder.setTicker("");
		}

		if (p == null || p.predictions.isEmpty()) {
			// TODO
		}

		// Copy when of first prediction
		long when = p.predictions.get(0).arrival.getTime();

		// (first row) of the notification, in a standard notification.
		CharSequence title = TextFormat.toString(p.route);

		// ((second row) of the notification, in a standard notification.
		CharSequence text = TextFormat.toString(p.stop);

		// large text at the right-hand side of the notification.
		CharSequence info = TextFormat.toString(p.predictions.get(0));

		CharSequence tickerText = TextFormat.toString(p.predictions.get(0))
				+ " till " + TextFormat.toString(p.route) + " at "
				+ TextFormat.toString(p.stop);

		mBuilder.setContentTitle(title).setContentText(text)
				.setContentInfo(info).setWhen(when).setTicker(tickerText);

		mNotificationManager
				.notify(mNotificationId, mBuilder.getNotification());

		mNotificationInProgress = true;
	}
}
