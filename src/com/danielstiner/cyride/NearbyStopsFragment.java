package com.danielstiner.cyride;

import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.danielstiner.cyride.IPredictions.StopPredictionsListener;
import com.danielstiner.cyride.LocalService.LocalServiceConnection;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;

public class NearbyStopsFragment extends ListFragment {

	private LocalServiceConnection mServiceConnector = LocalService
			.createConnection();
	private ArrayAdapter<StopPrediction> mAdapter;

	private final StopPredictionsListener mPredictionListener = new StopPredictionsListener() {

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
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		final StopPrediction selected = mAdapter.getItem(position);
		
		mServiceConnector.schedule(new Callback<ILocalService>() {
			@Override
			public void run(ILocalService service) {
				service.showNotification(selected);
			}
		});
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAdapter = new ArrayAdapter<StopPrediction>(getActivity(),
				android.R.layout.simple_list_item_1);
		
		setListAdapter(mAdapter);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		mServiceConnector.bind(activity);
		mServiceConnector.schedule(new Callback<ILocalService>() {
			@Override
			public void run(ILocalService service) {
				service.addNearbyStopPredictionsByRouteListener(mPredictionListener);
				service.updateNearbyStopPredictionsByRoute();
			}
		});
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		
		mServiceConnector.schedule(new Callback<ILocalService>() {
			@Override
			public void run(ILocalService service) {
				service.removeNearbyStopPredictionsByRouteListener(mPredictionListener);
			}
		});
		mServiceConnector.unbind(getActivity());
	}
}
