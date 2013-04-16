package com.danielstiner.cyride.widget;

import java.util.ArrayList;
import java.util.List;

import com.danielstiner.cyride.R;
import com.danielstiner.cyride.service.ILocalService;
import com.danielstiner.cyride.service.IPredictions;
import com.danielstiner.cyride.service.LocalService;
import com.danielstiner.cyride.service.ServiceConnector;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

public class MyWidgetService extends android.widget.RemoteViewsService {
	
	ServiceConnector<ILocalService> conn;
	
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new MyRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class MyRemoteViewsFactory implements android.widget.RemoteViewsService.RemoteViewsFactory {
    private static final int mCount = 10;
    private List<com.danielstiner.cyride.util.NextBusAPI.StopPrediction> mWidgetItems = 
    		new ArrayList<com.danielstiner.cyride.util.NextBusAPI.StopPrediction>();
    private Context mContext;
    private int mAppWidgetId;
    ServiceConnector<ILocalService> mConn = LocalService.createConnection();
    
    public MyRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    // Initialize the data set.
        public void onCreate() {
        	mConn.bind(mContext);
            // In onCreate() you set up any connections / cursors to your data source. Heavy lifting,
            // for example downloading or creating content etc, should be deferred to onDataSetChanged()
            // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.
        	//TODO - do this shit
        }
    
        // Given the position (index) of a WidgetItem in the array, use the item's text value in 
        // combination with the app widget item XML file to construct a RemoteViews object.
        public RemoteViews getViewAt(int position) {
            // position will always range from 0 to getCount() - 1.
    
            // Construct a RemoteViews item based on the app widget item XML file, and set the
            // text based on the position.
            RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.stop_prediction_list_item);
            rv.setTextViewText(R.id.text_route, mWidgetItems.get(position).toString());
    
            // Next, set a fill-intent, which will be used to fill in the pending intent template
            // that is set on the collection view in StackWidgetProvider.
            Bundle extras = new Bundle();
            extras.putInt(MyWidgetProvider.EXTRA_ITEM, position);
            Intent fillInIntent = new Intent();
            fillInIntent.putExtras(extras);
            // Make it possible to distinguish the individual on-click
            // action of a given item
            rv.setOnClickFillInIntent(R.id.text_route, fillInIntent);
                
            // Return the RemoteViews object.
            return rv;
        }

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public RemoteViews getLoadingView() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getViewTypeCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onDataSetChanged() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onDestroy() {
			mConn.unbind(mContext);
		}
    }
