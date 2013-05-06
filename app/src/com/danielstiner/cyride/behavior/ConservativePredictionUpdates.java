package com.danielstiner.cyride.behavior;

import org.joda.time.DateTime;

import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.DateUtil;
import com.danielstiner.nextbus.NextBusAPI.StopPrediction;

public class ConservativePredictionUpdates extends AbstractPredictionUpdates
		implements IPredictionUpdateStrategy {

	@Override
	public DateTime nextPredictionUpdate(StopPrediction prediction) {
		if (prediction.predictions.size() < Constants.MAX_PREDICTION_COUNT) {
			return new DateTime(prediction.timestamp)
					.plus(Constants.DEFAULT_CONSERVATIVE_UPDATE_TIME);
		} else {
			DateTime lastArrival = new DateTime(
					prediction.predictions.get(prediction.predictions.size() - 1).arrival);
			DateTime earliestUpdate = new DateTime(prediction.timestamp)
			.plus(Constants.SHORTEST_CONSERVATIVE_UPDATE_TIME);
			return DateUtil.later(lastArrival, earliestUpdate);
		}
	}

}
