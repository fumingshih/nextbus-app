package com.danielstiner.cyride.service;

import java.util.Collection;
import java.util.List;

import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.NextBusAPI.RouteStop;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;

public interface IPredictions {

	public interface StopPredictionsListener extends
			Callback<Collection<StopPrediction>> {
	}

	public abstract void addNearbyStopPredictionsByRouteListener(
			StopPredictionsListener predictionsListener);

	public abstract void removeNearbyStopPredictionsByRouteListener(
			StopPredictionsListener predictionsListener);

	public abstract void updateNearbyStopPredictionsByRoute();

	public abstract void addRouteStopListener(RouteStop rs,
			Callback<StopPrediction> predictionListener);

	public abstract void removeRouteStopListener(RouteStop rs,
			Callback<StopPrediction> predictionListener);
}
