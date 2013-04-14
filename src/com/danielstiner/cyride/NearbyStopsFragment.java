package com.danielstiner.cyride;

import java.util.Collection;
import java.util.Comparator;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.danielstiner.cyride.service.ILocalService;
import com.danielstiner.cyride.service.INotifications;
import com.danielstiner.cyride.service.IPredictions.StopPredictionsListener;
import com.danielstiner.cyride.service.LocalService;
import com.danielstiner.cyride.service.NotificationService;
import com.danielstiner.cyride.service.ServiceConnector;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;

public class NearbyStopsFragment extends ListFragment {

	private ArrayAdapter<StopPrediction> mAdapter;
	private Handler mHandler;

	private final StopPredictionsListener mPredictionListener = new StopPredictionsListener() {

		@Override
		public void run(Collection<StopPrediction> predictions) {
			mAdapter.clear();
			for (StopPrediction sp : predictions) {
				mAdapter.add(sp);
			}
			// mAdapter.notifyDataSetChanged();

			mAdapter.sort(new Comparator<StopPrediction>() {
				@Override
				public int compare(StopPrediction lhs, StopPrediction rhs) {
					return lhs.stop.title.compareTo(rhs.stop.title);
				}
			});
		}
	};

	private ServiceConnector<ILocalService> mPredictionsService = LocalService
			.createConnection();

	private final Runnable mViewUpdater = new Runnable() {
		@Override
		public void run() {
			if (mAdapter != null)
				mAdapter.notifyDataSetChanged();

			mHandler.postDelayed(mViewUpdater, Constants.VIEW_UPDATE_INTERVAL);
		}
	};

	private ArrayAdapter<StopPrediction> buildAdapter(Context context) {
		ArrayAdapter<StopPrediction> a = new StopPredictionAdapter(
				getActivity());

		// a.sort(new Comparator<StopPrediction>() {
		// @Override
		// public int compare(StopPrediction lhs, StopPrediction rhs) {
		// return lhs.stop.title.compareTo(rhs.stop.title);
		// }
		// });

		return a;
	};

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAdapter = buildAdapter(getActivity());

		setListAdapter(mAdapter);

	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		mPredictionsService.bind(activity);
		mPredictionsService.schedule(new Callback<ILocalService>() {
			@Override
			public void run(ILocalService service) {
				service.addNearbyStopPredictionsByRouteListener(mPredictionListener);
				service.updateNearbyStopPredictionsByRoute();
			}
		});
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
	};

	@Override
	public void onDetach() {
		super.onDetach();

		mPredictionsService.schedule(new Callback<ILocalService>() {
			@Override
			public void run(ILocalService service) {
				service.removeNearbyStopPredictionsByRouteListener(mPredictionListener);
			}
		});
		mPredictionsService.unbind(getActivity());
	};

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		final StopPrediction selected = mAdapter.getItem(position);

		NotificationService.showStopPredictions(getActivity(), selected.routestop);
	}

	public void onPause() {
		super.onPause();
		mHandler.removeCallbacks(mViewUpdater);
	}

	public void onResume() {
		super.onResume();
		mViewUpdater.run();
	}
}
