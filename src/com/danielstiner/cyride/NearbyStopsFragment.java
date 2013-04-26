package com.danielstiner.cyride;

import java.util.Comparator;

import org.joda.time.DateTime;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.danielstiner.cyride.behavior.AccuratePredictionUpdates;
import com.danielstiner.cyride.behavior.IPredictionUpdateStrategy;
import com.danielstiner.cyride.service.IPredictions;
import com.danielstiner.cyride.service.IPredictions.NearbyStopPredictions;
import com.danielstiner.cyride.service.IPredictions.NearbyStopPredictionsListener;
import com.danielstiner.cyride.service.NotificationService;
import com.danielstiner.cyride.service.PredictionsService;
import com.danielstiner.cyride.service.ServiceConnector;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.LocationUtil;
import com.danielstiner.cyride.util.PredictionsUpdater;
import com.danielstiner.cyride.util.Task;
import com.danielstiner.nextbus.NextBusAPI.StopPrediction;

import de.akquinet.android.androlog.Log;

public class NearbyStopsFragment extends ListFragment {

	private ArrayAdapter<StopPrediction> mAdapter;
	private Handler mHandler;
	protected NearbyStopPredictions mLastNearbyPredictions;

	private final NearbyStopPredictionsListener mPredictionListener = new NearbyStopPredictionsListener() {
		@Override
		public void run(final NearbyStopPredictions prediction) {
			mLastNearbyPredictions = prediction;
			
			Log.v(this, "Got " + prediction.predictions.size() + " new nearby predictions");

			mAdapter.clear();
			for (StopPrediction sp : prediction.predictions) {
				mAdapter.add(sp);
			}
			// mAdapter.notifyDataSetChanged();

			mAdapter.sort(new Comparator<StopPrediction>() {
				@Override
				public int compare(StopPrediction lhs, StopPrediction rhs) {
					return LocationUtil.compareDistance(lhs.stop, rhs.stop,
							prediction.near);
				}
			});
			
			mPredictionsUpdater.scheduleUpdate();
		}
	};

	private final ServiceConnector<IPredictions> mPredictionsService = PredictionsService
			.createConnection();

	private final IPredictionUpdateStrategy mPredictionUpdateStrategy = new AccuratePredictionUpdates();

	private final PredictionsUpdater mPredictionsUpdater = new PredictionsUpdater(
			new Task<DateTime>() {
				@Override
				public DateTime get() {
					return mPredictionUpdateStrategy.nextPredictionUpdate(mLastNearbyPredictions);
				}
			}, new Runnable() {
				@Override
				public void run() {
					mPredictionsService.schedule(new Callback<IPredictions>() {
						@Override
						public void run(IPredictions service) {
							service.updateNearbyStopPredictionsByRoute();
						}
					});
				}
			});

	private final Runnable mViewUpdater = new Runnable() {
		@Override
		public void run() {
			if (mAdapter != null)
				mAdapter.notifyDataSetChanged();

			mHandler.postDelayed(mViewUpdater, Constants.VIEW_UPDATE_INTERVAL);
		}
	};

	private ArrayAdapter<StopPrediction> buildAdapter(Context context) {
		return new StopPredictionAdapter(getActivity());
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

		mPredictionsService.bind(activity, new Callback<IPredictions>() {
			@Override
			public void run(IPredictions service) {
				service.addNearbyStopPredictionsByRouteListener(mPredictionListener);
			}
		});

	}

	@Override
	public void onStart() {
		mPredictionsUpdater.start(mHandler);
		super.onStart();
	}

	@Override
	public void onStop() {
		mPredictionsUpdater.stop();
		super.onStop();
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.init();

		mHandler = new Handler();
	};

	@Override
	public void onDetach() {
		super.onDetach();

		mPredictionsService.unbind(getActivity(), new Callback<IPredictions>() {
			@Override
			public void run(IPredictions service) {
				service.removeNearbyStopPredictionsByRouteListener(mPredictionListener);
			}
		});
	};

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		final StopPrediction selected = mAdapter.getItem(position);

		NotificationService.showStopPredictions(getActivity(), selected);
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
