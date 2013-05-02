package com.danielstiner.cyride.service;

import org.joda.time.DateTime;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

import com.danielstiner.cyride.MainActivity;
import com.danielstiner.cyride.R;
import com.danielstiner.cyride.behavior.AccuratePredictionUpdates;
import com.danielstiner.cyride.behavior.IPredictionUpdateStrategy;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.TextFormat;
import com.danielstiner.nextbus.NextBusAPI;
import com.danielstiner.nextbus.NextBusAPI.RouteStop;
import com.danielstiner.nextbus.NextBusAPI.StopPrediction;

import de.akquinet.android.androlog.Log;

public class NotificationService extends IntentService {

	public NotificationService() {
		super(NAME);
	}
	
	private final static String NAME = "Notifications";

	private final static String CLASS = "com.danielstiner.cyride.service";

	private static final String INTENT_ACTION_DELETE = CLASS + ".ACTION_DELETE";

	private static final String INTENT_ACTION_NOTIFY = CLASS + ".ACTION_NOTIFY";

	private static final String INTENT_ACTION_ALARM = CLASS + ".ACTION_ALARM";

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

	private NotificationCompat.Builder mBuilder;

	private Callback<StopPrediction> mCallbackPredictionUpdate = new Callback<StopPrediction>() {
		@Override
		public void run(StopPrediction prediction) {
			updateCurrentPredictions(prediction);
		}
	};

	private StopPrediction mCurrentRouteStopPrediction;

	private int mNotificationId = 1;

	private boolean mNotificationInProgress = false;

	private NotificationManager mNotificationManager;

	private final ServiceConnector<IPredictions> mPredictionsService = PredictionsService
			.createConnection();

	private IPredictionUpdateStrategy mPredictionUpdateStrategy = new AccuratePredictionUpdates();

	private PendingIntent mAlarmPendingIntent;

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

	public void onHandleIntent(Intent intent) {
		if (INTENT_ACTION_NOTIFY.equals(intent.getAction())) {
			if (intent.hasExtra(INTENT_EXTRA_ROUTE_STOP_PREDICTION)) {
				setRouteStopWithInitialPrediction((StopPrediction) intent
						.getSerializableExtra(INTENT_EXTRA_ROUTE_STOP_PREDICTION));
			} else if (intent.hasExtra(INTENT_EXTRA_ROUTE_STOP)) {
				setRouteStop((RouteStop) intent
						.getSerializableExtra(INTENT_EXTRA_ROUTE_STOP));
			} else {
				Log.e(CLASS,
						"Got a handleIntent call with the action notify, but not the correct extra");
			}
		} else if (INTENT_ACTION_DELETE.equals(intent.getAction())) {
			Log.v(this, "Stopping notifcation service");
			stopSelf();
		} else if (INTENT_ACTION_ALARM.equals(intent.getAction())) {
			onAlarm();
		} else {
			Log.i(CLASS, "Got a handleIntent call with an unknown action");
		}
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

//	@Override
//	public int onStartCommand(Intent intent, int flags, int startId) {
//		super.onStartCommand(intent, flags, startId);
//		
//		Log.v(this, "Got intent: " + intent.getAction());
//
//		onHandleIntent(intent);
//
//		return START_NOT_STICKY;
//	}

	private void updateCurrentPredictions(StopPrediction prediction) {
		mCurrentRouteStopPrediction = prediction;
		Log.v(this, "setCurrentPredictions");
		updateNotification(prediction);
		updateAlarm(prediction);
	}

	private void cancelAlarm() {
		final AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		am.cancel(mAlarmPendingIntent);
	}

	private void updateAlarm(StopPrediction prediction) {
		final AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		if (mAlarmPendingIntent == null) {
			mAlarmPendingIntent = PendingIntent.getService(this, 0, new Intent(
					this, NotificationService.class)
					.setAction(INTENT_ACTION_ALARM),
					PendingIntent.FLAG_UPDATE_CURRENT);
		} else {
			am.cancel(mAlarmPendingIntent);
		}

		DateTime triggerAt = calculateNextAlarmWakeup();

		if (triggerAt.isBeforeNow()) {
			Log.v(this, "Alarm has past");
		} else {
			Log.v(this,
					"Scheduling notification alarm in "
							+ (triggerAt.getMillis() - DateTime.now()
									.getMillis()) / 1000 + "s");

			am.set(AlarmManager.RTC_WAKEUP, triggerAt.getMillis(),
					mAlarmPendingIntent);
		}
	}

	private DateTime calculateNextAlarmWakeup() {
		DateTime dt = new DateTime(
				mCurrentRouteStopPrediction.predictions.get(0).arrival);
		DateTime dt5min = dt.minusMinutes(5);
		DateTime dt2min = dt.minusMinutes(2);

		if (dt5min.isAfterNow())
			return dt5min;
		else if (dt2min.isAfterNow())
			return dt2min;
		else
			return dt;
	}

	private void onAlarm() {
		Log.i(CLASS, "onAlarm");
		mNotificationManager.notify(
				mNotificationId,
				updateNotificationBuilder(mCurrentRouteStopPrediction)
						.setOnlyAlertOnce(false)
						.setDefaults(
								Notification.DEFAULT_VIBRATE
										| Notification.DEFAULT_LIGHTS)
						.getNotification());
		updateAlarm(mCurrentRouteStopPrediction);
	}

	private void setRouteStop(final RouteStop rs) {
		final RouteStop previous = (null == mCurrentRouteStopPrediction) ? null
				: mCurrentRouteStopPrediction.routestop;

		Log.v(this, "showRouteStop " + rs);

		cancelAlarm();

		mPredictionsService.schedule(new Callback<IPredictions>() {
			@Override
			public void run(IPredictions predictions) {
				if (!rs.equals(previous)) {
					if (previous != null)
						predictions.removeRouteStopListener(rs,
								mCallbackPredictionUpdate);
					predictions.addRouteStopListener(rs,
							mCallbackPredictionUpdate,
							mPredictionUpdateStrategy);
				}
			}
		});
	}

	private void setRouteStopWithInitialPrediction(final StopPrediction p) {
		updateCurrentPredictions(p);
		setRouteStop(p.routestop);
	}

	private void updateNotification(StopPrediction p) {
		mNotificationManager.notify(mNotificationId,
				updateNotificationBuilder(p).getNotification());

		mNotificationInProgress = true;
	}

	private Builder updateNotificationBuilder(StopPrediction p) {
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
		// CharSequence info = TextFormat.toString(p.predictions.get(0));
		CharSequence infotext = "";
		if (p.predictions.size() > 1) {
			infotext = "Next " + TextFormat.toString(p.predictions.get(1));
		}

		CharSequence tickerText = TextFormat.toString(p.predictions.get(0))
				+ " till " + TextFormat.toString(p.route) + " at "
				+ TextFormat.toString(p.stop);

		mBuilder.setContentTitle(title).setContentText(text)
				.setContentInfo(infotext).setWhen(when).setTicker(tickerText);

		return mBuilder.setDefaults(0).setOnlyAlertOnce(true);
	}
}
