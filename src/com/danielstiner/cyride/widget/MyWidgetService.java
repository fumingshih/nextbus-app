package com.danielstiner.cyride.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import com.danielstiner.cyride.R;
import com.danielstiner.cyride.behavior.ConservativePredictionUpdates;
import com.danielstiner.cyride.behavior.IPredictionUpdateStrategy;
import com.danielstiner.cyride.service.IPredictions;
import com.danielstiner.cyride.service.IPredictions.NearbyStopPredictions;
import com.danielstiner.cyride.service.NotificationService;
import com.danielstiner.cyride.service.PredictionsService;
import com.danielstiner.cyride.service.ServiceConnector;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.LocationUtil;
import com.danielstiner.cyride.view.RemoteViewsProvider;
import com.danielstiner.nextbus.NextBusAPI.StopPrediction;

import de.akquinet.android.androlog.Log;

class MyRemoteViewsFactory implements
		android.widget.RemoteViewsService.RemoteViewsFactory {

	@SuppressWarnings("unused")
	private int mAppWidgetId;

	private ServiceConnector<IPredictions> mConn = PredictionsService
			.createConnection();
	private Context mContext;

	private IPredictionUpdateStrategy mPredictionUpdateStrategy = new ConservativePredictionUpdates();

	private final List<StopPrediction> mRouteStopPredictions = new ArrayList<StopPrediction>();

	public MyRemoteViewsFactory(Context context, Intent intent) {
		mContext = context;
		mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
	}

	@Override
	public int getCount() {
		return mRouteStopPredictions.size();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public RemoteViews getLoadingView() {
		return null;
	}

	// Given the position (index) of a WidgetItem in the array, use the
	// item's text value in
	// combination with the app widget item XML file to construct a
	// RemoteViews object.
	public RemoteViews getViewAt(int position) {

		StopPrediction p = mRouteStopPredictions.get(position);

		RemoteViews rv = RemoteViewsProvider.getWidgetPredictionItem(mContext,
				p);
		rv.setOnClickFillInIntent(R.id.parent,
				NotificationService.getFillInIntent(p));
		return rv;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	public void onCreate() {

		Log.init(mContext);

		mConn.bind(mContext);
	}

	@Override
	public void onDataSetChanged() {
		mConn.maybeNow(new Callback<IPredictions>() {
			@Override
			public void run(IPredictions predictions) {
				final NearbyStopPredictions prediction = predictions
						.getLatestNearbyStopPredictions(mPredictionUpdateStrategy);

				mRouteStopPredictions.clear();

				if (prediction != null) {
					mRouteStopPredictions.addAll(prediction.predictions);
					Collections.sort(mRouteStopPredictions,
							new Comparator<StopPrediction>() {
								@Override
								public int compare(StopPrediction lhs,
										StopPrediction rhs) {
									return LocationUtil
											.compareDistance(lhs.stop,
													rhs.stop, prediction.near);
								}
							});
				}
			}
		});
	}

	@Override
	public void onDestroy() {
		mConn.unbind(mContext);
	}
}

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class MyWidgetService extends android.widget.RemoteViewsService {

	private final static String CLASS = "com.danielstiner.cyride.widget.NearbyWidgetService";

	private static final String INTENT_EXTRA_UPDATE_NEARBY = CLASS
			+ ".update_nearby";

	public static void updateNearbyWidgets(Context context) {
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			Intent i = new Intent(context, MyWidgetService.class);
			i.putExtra(INTENT_EXTRA_UPDATE_NEARBY, true);
			context.getApplicationContext().startService(i);
		}
	}

	private void handleIntent(Intent intent) {

		if (intent == null)
			return;

		if (intent.getBooleanExtra(INTENT_EXTRA_UPDATE_NEARBY, false)) {
			updateWidgets();
		}
	}

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new MyRemoteViewsFactory(this.getApplicationContext(), intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Log.init(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		handleIntent(intent);

		return super.onStartCommand(intent, flags, startId);
	}

	private void updateWidgets() {
		AppWidgetManager mgr = AppWidgetManager.getInstance(this);
		mgr.notifyAppWidgetViewDataChanged(
				mgr.getAppWidgetIds(new ComponentName(this,
						MyWidgetProvider.class)), R.id.widget_listview);
	}
}