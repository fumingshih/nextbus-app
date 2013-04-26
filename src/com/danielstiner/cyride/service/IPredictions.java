package com.danielstiner.cyride.service;

import java.util.Collection;

import android.location.Location;

import com.danielstiner.cyride.behavior.IPredictionUpdateStrategy;
import com.danielstiner.cyride.util.Callback;
import com.danielstiner.nextbus.NextBusAPI.RouteStop;
import com.danielstiner.nextbus.NextBusAPI.StopPrediction;

public interface IPredictions {
	
	public class NearbyStopPredictions {
		public NearbyStopPredictions(Collection<StopPrediction> predictions, Location near) {
			this.predictions = predictions;
			this.near = near;
		}
		public final Collection<StopPrediction> predictions;
		public final Location near;
	}

	public interface NearbyStopPredictionsListener extends
			Callback<NearbyStopPredictions> {
	}

	public abstract void addNearbyStopPredictionsByRouteListener(
			NearbyStopPredictionsListener predictionsListener, IPredictionUpdateStrategy predictionUpdateStrategy);

	public abstract void addRouteStopListener(RouteStop rs,
			Callback<StopPrediction> predictionListener, IPredictionUpdateStrategy updateStrategy);

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
