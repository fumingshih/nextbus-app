package com.danielstiner.cyride.widget;

import com.danielstiner.cyride.MainActivity;
import com.danielstiner.cyride.R;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class MyWidgetProvider extends AppWidgetProvider {
	public static final String EXTRA_ITEM = "com.danielstiner.cyride.widget.EXTRA_ITEM";
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.homescreenappwidget_layout);
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent);

            
            // Set up the intent that starts the StackViewService, which will
            // provide the views for this collection.
            Intent intent2 = new Intent(context, MyWidgetService.class);
            // Add the app widget ID to the intent extras.
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            intent.setData(Uri.parse(intent2.toUri(Intent.URI_INTENT_SCHEME)));
            // Set up the RemoteViews object to use a RemoteViews adapter. 
            // This adapter connects
            // to a RemoteViewsService  through the specified intent.
            // This is how you populate the data.
            views.setRemoteAdapter(R.id.widget_listview, intent2);
            
            // The empty view is displayed when the collection has no items. 
            // It should be in the same layout used to instantiate the RemoteViews
            // object above. 
            //FIXME - should define empty view
            views.setEmptyView(R.id.widget_listview, R.id.widget_listview);

            //
            // Do additional processing specific to this app widget...
            //
            appWidgetManager.updateAppWidget(appWidgetIds[i], views); 
        }
    }
}
