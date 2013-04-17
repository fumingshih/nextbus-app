package com.danielstiner.cyride.widget;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.danielstiner.cyride.R;
import com.danielstiner.cyride.service.IPredictions;
import com.danielstiner.cyride.service.PredictionsService;
import com.danielstiner.cyride.service.NotificationService;
import com.danielstiner.cyride.service.ServiceConnector;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;
import com.danielstiner.cyride.util.TextFormat;

class MyRemoteViewsFactory implements
		android.widget.RemoteViewsService.RemoteViewsFactory {

	private int mAppWidgetId;

	private ServiceConnector<IPredictions> mConn = PredictionsService
			.createConnection();
	private Context mContext;

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

		// Construct a RemoteViews item based on the app widget item XML
		// file, and set the
		// text based on the position.
		RemoteViews rv = new RemoteViews(mContext.getPackageName(),
				R.layout.stop_prediction_list_item);
		rv.setTextViewText(R.id.text_route, TextFormat.toString(p.route));
		rv.setTextViewText(R.id.text_stop, TextFormat.toString(p.stop));
		rv.setTextViewText(R.id.text_times,
				TextFormat.singleAbsoluteTime(p.predictions));

		// Next, set a fill-intent, which will be used to fill in the
		// pending intent template
		// that is set on the collection view in StackWidgetProvider.
		Bundle extras = new Bundle();
		extras.putInt(MyWidgetProvider.EXTRA_ITEM, position);
		NotificationService.putExtraRouteStop(extras, p.routestop);
		Intent fillInIntent = new Intent();
		fillInIntent.putExtras(extras);
		// Make it possible to distinguish the individual on-click
		// action of a given item
		rv.setOnClickFillInIntent(R.id.text_route, fillInIntent);

		// Return the RemoteViews object.
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
		mConn.bind(mContext);
	}

	@Override
	public void onDataSetChanged() {
		mConn.maybeNow(new Callback<IPredictions>() {
			@Override
			public void run(IPredictions predictions) {
				mRouteStopPredictions.clear();
				mRouteStopPredictions.addAll(predictions
						.getLatestNearbyStopPredictions());
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

	private final static String CLASS = "com.danielstiner.cyride.service";

	private static final String INTENT_EXTRA_UPDATE_NEARBY = CLASS
			+ ".update_nearby";

	public static void updateNearbyWidgets(Context context) {
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			Intent i = new Intent(context, MyWidgetService.class);
			i.putExtra(INTENT_EXTRA_UPDATE_NEARBY, true);
			context.startService(i);
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
	public int onStartCommand(Intent intent, int flags, int startId) {

		handleIntent(intent);

		return super.onStartCommand(intent, flags, startId);
	}

	public void updateWidgets() {
		AppWidgetManager mgr = AppWidgetManager.getInstance(this);
		mgr.notifyAppWidgetViewDataChanged(
				mgr.getAppWidgetIds(new ComponentName(this,
						MyWidgetProvider.class)), R.id.widget_listview);
	}
}