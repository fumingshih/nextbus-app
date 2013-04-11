package com.danielstiner.cyride.service;

import java.util.Collection;

import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;

public interface IPredictions {

	public interface StopPredictionsListener extends Callback<Collection<StopPrediction>> {
	}

	public abstract void addNearbyStopPredictionsByRouteListener(
			StopPredictionsListener predictionListener);

	public abstract void removeNearbyStopPredictionsByRouteListener(
			StopPredictionsListener predictionListener);
	
	public abstract void updateNearbyStopPredictionsByRoute();
}
