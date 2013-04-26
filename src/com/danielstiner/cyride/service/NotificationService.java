package com.danielstiner.cyride.service;

import org.joda.time.DateTime;

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

import com.danielstiner.cyride.MainActivity;
import com.danielstiner.cyride.R;
import com.danielstiner.cyride.behavior.AccuratePredictionUpdates;
import com.danielstiner.cyride.behavior.IPredictionUpdateStrategy;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.PredictionsUpdater;
import com.danielstiner.cyride.util.Task;
import com.danielstiner.cyride.util.TextFormat;
import com.danielstiner.nextbus.NextBusAPI;
import com.danielstiner.nextbus.NextBusAPI.RouteStop;
import com.danielstiner.nextbus.NextBusAPI.StopPrediction;

import de.akquinet.android.androlog.Log;

public class NotificationService extends Service {

	public class LocalBinder extends Binder {
		NotificationService getService() {
			return NotificationService.this;
		}
	}

	private final static String CLASS = "com.danielstiner.cyride.service";

	private static final String INTENT_ACTION_DELETE = CLASS + ".ACTION_DELETE";

	private static final String INTENT_ACTION_NOTIFY = CLASS + ".ACTION_NOTIFY";

	private static final String INTENT_EXTRA_ROUTE_STOP = CLASS + ".ROUTE_STOP";

	private static final String INTENT_EXTRA_ROUTE_STOP_PREDICTION = CLASS
			+ ".ROUTE_STOP_PREDICTION";
	private final static int NOTIFICATION = R.string.local_service_started;

	public static Intent getFillInIntent(StopPrediction p) {
		Bundle extras = new Bundle();
		extras.putSerializable(INTENT_EXTRA_ROUTE_STOP_PREDICTION, p);
		Intent fillInIntent = new Intent();
		fillInIntent.putExtras(extras);
		return fillInIntent;
	}

	public static PendingIntent getTemplatePendingIntent(Context context,
			Bundle extras) {
		Intent templateIntent = new Intent(context, NotificationService.class);
		templateIntent.putExtras(extras);
		templateIntent.setAction(NotificationService.INTENT_ACTION_NOTIFY);
		return PendingIntent.getService(context, 0, templateIntent,
				PendingIntent.FLAG_CANCEL_CURRENT);
	}

	public static void showStopPredictions(Context context,
			NextBusAPI.RouteStop stopAndRoute) {
		Intent i = new Intent(context, NotificationService.class);
		i.setAction(NotificationService.INTENT_ACTION_NOTIFY);
		i.putExtra(INTENT_EXTRA_ROUTE_STOP, stopAndRoute);
		context.startService(i);
	}

	public static void showStopPredictions(Context context,
			NextBusAPI.StopPrediction stopAndRoutePrediction) {
		Intent i = new Intent(context, NotificationService.class);
		i.setAction(NotificationService.INTENT_ACTION_NOTIFY);
		i.putExtra(INTENT_EXTRA_ROUTE_STOP_PREDICTION, stopAndRoutePrediction);
		context.startService(i);
	}

	private final IBinder mBinder = new LocalBinder();

	private NotificationCompat.Builder mBuilder;

	private Callback<StopPrediction> mCallbackPredictionUpdate = new Callback<StopPrediction>() {
		@Override
		public void run(StopPrediction prediction) {
			setCurrentPredictions(prediction);
			mPredictionsUpdater.scheduleUpdate();
		}
	};

	private StopPrediction mCurrentRouteStopPrediction;

	private Handler mHandler;

	private int mNotificationId = 1;

	private boolean mNotificationInProgress = false;

	private NotificationManager mNotificationManager;

	private final ServiceConnector<IPredictions> mPredictionsService = PredictionsService
			.createConnection();

	private IPredictionUpdateStrategy mPredictionUpdateStrategy = new AccuratePredictionUpdates();

	private Callback<IPredictions> mScheduledPredictionsUpdater = new Callback<IPredictions>() {
		@Override
		public void run(IPredictions predictions) {
			if (null != mCurrentRouteStopPrediction)
				predictions.updateRouteStopPredictions(
						mCurrentRouteStopPrediction.routestop,
						mPredictionUpdateStrategy);
		}
	};

	private final PredictionsUpdater mPredictionsUpdater = new PredictionsUpdater(
			new Task<DateTime>() {
				@Override
				public DateTime get() {
					return mPredictionUpdateStrategy.nextPredictionUpdate(mCurrentRouteStopPrediction);
				}
			}, new Runnable() {
				@Override
				public void run() {
					mPredictionsService.schedule(mScheduledPredictionsUpdater);
				}
			});

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
						PendingIntent.getService(this, 0, new Intent(this,
								NotificationService.class)
								.setAction(INTENT_ACTION_DELETE),
								PendingIntent.FLAG_CANCEL_CURRENT));
		// setUsesChronometer

	}

	private void handleIntent(Intent intent) {
		if (INTENT_ACTION_NOTIFY.equals(intent.getAction())) {
			if (intent.hasExtra(INTENT_EXTRA_ROUTE_STOP_PREDICTION)) {
				showRouteStopWithInitialPrediction((StopPrediction) intent
						.getSerializableExtra(INTENT_EXTRA_ROUTE_STOP_PREDICTION));
			} else if (intent.hasExtra(INTENT_EXTRA_ROUTE_STOP)) {
				showRouteStop((RouteStop) intent
						.getSerializableExtra(INTENT_EXTRA_ROUTE_STOP));
			} else {
				Log.e(this,
						"Got a handleIntent call with the action notify, but not the correct extra");
			}
		} else if (INTENT_ACTION_DELETE.equals(intent.getAction())) {
			Log.v(this,
					"Stopping notifcation service");
			stopSelf();
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

		mHandler = new Handler();

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

	private void setCurrentPredictions(StopPrediction prediction) {
		mCurrentRouteStopPrediction = prediction;
		Log.v(this, "setCurrentPredictions");
		updateNotification(prediction);
	}

	private void showRouteStop(final RouteStop rs) {
		final RouteStop previous = (null == mCurrentRouteStopPrediction) ? null
				: mCurrentRouteStopPrediction.routestop;

		Log.v(this, "showRouteStop " + rs);

		mPredictionsService.schedule(new Callback<IPredictions>() {
			@Override
			public void run(IPredictions predictions) {
				if (!rs.equals(previous)) {
					if (previous != null)
						predictions.removeRouteStopListener(rs,
								mCallbackPredictionUpdate);
					predictions.addRouteStopListener(rs,
							mCallbackPredictionUpdate);
				}
			}
		});

		mPredictionsUpdater.start(mHandler);
	}

	private void showRouteStopWithInitialPrediction(final StopPrediction p) {
		setCurrentPredictions(p);
		showRouteStop(p.routestop);
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
				.setWhen(when).setTicker(tickerText);

		mNotificationManager
				.notify(mNotificationId, mBuilder.getNotification());

		mNotificationInProgress = true;
	}
}
