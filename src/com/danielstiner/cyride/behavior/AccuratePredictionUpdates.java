package com.danielstiner.cyride.behavior;

import org.joda.time.DateTime;

import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.DateUtil;
import com.danielstiner.nextbus.NextBusAPI.StopPrediction;

public class AccuratePredictionUpdates extends AbstractPredictionUpdates implements IPredictionUpdateStrategy {

	@Override
	public DateTime nextPredictionUpdate(StopPrediction prediction) {
		DateTime earliestUpdate = new DateTime(prediction.timestamp).plus(
				Constants.SHORTEST_UPDATE_TIME);

		// TODO: Update more often based on the timestamp of the prediction
		if (prediction.predictions.size() == 0) {
			return earliestUpdate;
		} else {
			DateTime firstArrival = new DateTime(prediction.predictions.get(0).arrival);
			return DateUtil.later(firstArrival, earliestUpdate);
		}
	}
}
