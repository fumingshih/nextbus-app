package com.danielstiner.cyride.behavior;

import org.joda.time.DateTime;

import com.danielstiner.cyride.service.IPredictions.NearbyStopPredictions;
import com.danielstiner.nextbus.NextBusAPI.StopPrediction;

public interface IPredictionUpdateStrategy {

	DateTime nextPredictionUpdate(NearbyStopPredictions predictions);

	DateTime nextPredictionUpdate(StopPrediction prediction);

}
