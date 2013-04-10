package com.danielstiner.cyride;

import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.danielstiner.cyride.LocalService.LocalServiceConnection;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;

public class NearbyStops extends ListActivity {

	private LocalServiceConnection mServiceConnector = LocalService
			.createConnection();
	private ArrayAdapter<StopPrediction> mAdapter;

	private final PredictionsListener mPredictionListener = new PredictionsListener() {

		@Override
		public void run(List<StopPrediction> predictions) {
			mAdapter.clear();
			for (StopPrediction sp : predictions) {
				mAdapter.add(sp);
			}
			mAdapter.notifyDataSetChanged();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nearby_stops);

		final ListView listview = (ListView) findViewById(android.R.id.list);

		mAdapter = new ArrayAdapter<StopPrediction>(this,
				android.R.layout.simple_list_item_1);

		listview.setAdapter(mAdapter);

	}

	@Override
	protected void onResume() {
		super.onResume();
		mServiceConnector.bind(this);
		mServiceConnector.schedule(new Callback<LocalService>() {
			@Override
			public void run(LocalService service) {
				service.StopPredictions.addListener(mPredictionListener);
				service.updatePredictions();
			}
		});
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mServiceConnector.schedule(new Callback<LocalService>() {
			@Override
			public void run(LocalService service) {
				service.StopPredictions.removeListener(mPredictionListener);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.nearby_stops, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mServiceConnector.unbind(this);
	}
}
