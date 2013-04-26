package com.danielstiner.cyride.view;

import android.content.Context;
import android.widget.RemoteViews;

import com.danielstiner.cyride.R;
import com.danielstiner.cyride.util.TextFormat;
import com.danielstiner.nextbus.NextBusAPI.StopPrediction;

public class RemoteViewsProvider {

	public static RemoteViews getWidgetPredictionItem(Context context,
			StopPrediction p) {

		RemoteViews rv = new RemoteViews(context.getPackageName(),
				R.layout.widget_prediction_item);
		rv.setTextViewText(R.id.text_route, TextFormat.toString(p.route));
		rv.setTextViewText(R.id.text_stop, TextFormat.toString(p.stop));
		rv.setTextViewText(R.id.text_times,
				TextFormat.singleAbsoluteTime(p.predictions));
		rv.setInt(R.id.color, "setBackgroundColor", p.route.color);

		return rv;
	}

	public static RemoteViews getNotificationView(Context context,
			StopPrediction p) {
		return getWidgetPredictionItem(context, p);
	}

}
