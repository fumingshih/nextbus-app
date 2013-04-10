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
import com.danielstiner.cyride.util.CallbackHandler;
import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.LocationUtil;
import com.danielstiner.cyride.util.NextBusAPI;
import com.danielstiner.cyride.util.NextBusAPI.Stop;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;

public class LocalService extends android.app.Service {

	private static final String AGENCY = "cyride";

	private final static int NOTIFICATION = R.string.local_service_started;

	private final NextBusAPI mNextBusAPI = new NextBusAPI();

	private NotificationManager mNM;

	public CallbackHandler<List<StopPrediction>> StopPredictions = new CallbackHandler<List<StopPrediction>>();

	public class LocalBinder extends Binder {
		LocalService getService() {
			return LocalService.this;
		}
	}

	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		// TODO: Parse intent, either a start service or a display bus

		return START_STICKY;
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

	private Location getGPS() {
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		return LocationUtil.getBestCurrentLocation(lm);
	}

	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.local_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_launcher,
				text, System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, NearbyStops.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this,
				getText(R.string.local_service_started), text, contentIntent);

		// Send the notification.
		mNM.notify(NOTIFICATION, notification);
	}

	void updatePredictions() {
		new UpdateNearbyTask().execute();
	}

	private class UpdateNearbyTask extends
			AsyncTask<Void, Void, List<StopPrediction>> {

		protected List<StopPrediction> doInBackground(Void... stuff) {
			LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			Location location = LocationUtil.getBestCurrentLocation(lm);
			
			if(null == location) {
				return new LinkedList<StopPrediction>();
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
			StopPredictions.runAll(predictions);
		}
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

	public static class LocalServiceConnection {

		private LocalServiceConnection() {

		}

		private LocalService mBoundService;
		private boolean mIsBound = false;
		private Queue<Callback<LocalService>> mScheduledCallbacks = new LinkedList<Callback<LocalService>>();

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

		public void bind(Context context) {
			if (!mIsBound)
				context.bindService(new Intent(context, LocalService.class),
						mConnection, Context.BIND_AUTO_CREATE);
			mIsBound = true;
		}

		public void unbind(Context context) {
			if (mIsBound) {
				// Detach our existing connection.
				context.unbindService(mConnection);
				mIsBound = false;
			}
		}

		public void schedule(Callback<LocalService> callback) {
			if (mIsBound && null != mBoundService) {
				callback.run(mBoundService);
			} else {
				mScheduledCallbacks.add(callback);
			}
		}

	}

	protected void registerLocalServiceConnection(
			ServiceConnection serviceConnection) {
		// TODO Auto-generated method stub

	}

	// private void showNearbyNotification(){
	// Intent notificationIntent = new Intent(ctx, YourClass.class);
	// PendingIntent contentIntent = PendingIntent.getActivity(ctx,
	// YOUR_PI_REQ_CODE, notificationIntent,
	// PendingIntent.FLAG_CANCEL_CURRENT);
	//
	// NotificationManager nm = (NotificationManager) ctx
	// .getSystemService(Context.NOTIFICATION_SERVICE);
	//
	// Resources res = ctx.getResources();
	// Notification.Builder builder = new Notification.Builder(ctx);
	//
	// builder.setContentIntent(contentIntent)
	// .setSmallIcon(R.drawable.arrow_down_float)
	// .setLargeIcon(BitmapFactory.decodeResource(res,
	// R.drawable.alert_dark_frame))
	// .setTicker(res.getString(R.string.nearby_ticker))
	// .setWhen(System.currentTimeMillis())
	// .setAutoCancel(true)
	// .setContentTitle(res.getString(R.string.your_notif_title))
	// .setContentText(res.getString(R.string.your_notif_text));
	// Notification n = builder.build();
	//
	// nm.notify(YOUR_NOTIF_ID, n);
	// }

	// "http://webservices.nextbus.com/service/publicXMLFeed?command=routeList&a=cyride"

	// "http://webservices.nextbus.com/service/publicXMLFeed?command=routeConfig&a=cyride&terse=true"

}
