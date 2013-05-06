package com.danielstiner.cyride.service;

import java.io.Serializable;
import java.util.Collection;

import android.location.Location;

import com.danielstiner.cyride.behavior.IPredictionUpdateStrategy;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.nextbus.NextBusAPI.RouteStop;
import com.danielstiner.nextbus.NextBusAPI.StopPrediction;

public interface IPredictions {

	public class NearbyStopPredictions implements Serializable {
		private static final long serialVersionUID = -5795359665009295231L;

		public NearbyStopPredictions(Collection<StopPrediction> predictions,
				double latitude, double longitude) {
			this.predictions = predictions;
			this.latitude = latitude;
			this.longitude = longitude;
		}

		public final Collection<StopPrediction> predictions;
		public final double latitude;
		public final double longitude;
	}

	public interface NearbyStopPredictionsListener extends
			Callback<NearbyStopPredictions> {
	}

	public abstract void addNearbyStopPredictionsByRouteListener(
			NearbyStopPredictionsListener predictionsListener,
			IPredictionUpdateStrategy predictionUpdateStrategy);

	public abstract void addRouteStopListener(RouteStop rs,
			Callback<StopPrediction> predictionListener,
			IPredictionUpdateStrategy updateStrategy);

	public abstract NearbyStopPredictions getLatestNearbyStopPredictions(
			IPredictionUpdateStrategy mPredictionUpdateStrategy);

	public abstract void removeNearbyStopPredictionsByRouteListener(
			NearbyStopPredictionsListener predictionsListener);

	public abstract void removeRouteStopListener(RouteStop rs,
			Callback<StopPrediction> predictionListener);

	public abstract void updateNearbyStopPredictionsByRoute();

	void updateRouteStopPredictions(RouteStop rs,
			IPredictionUpdateStrategy updateStrategy);

	Location getLastKnownLocation();
}
