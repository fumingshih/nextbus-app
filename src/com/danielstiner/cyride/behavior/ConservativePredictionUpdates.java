package com.danielstiner.cyride.behavior;

import org.joda.time.DateTime;

import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.DateUtil;
import com.danielstiner.nextbus.NextBusAPI.StopPrediction;

public class ConservativePredictionUpdates extends AbstractPredictionUpdates
		implements IPredictionUpdateStrategy {

	@Override
	public DateTime nextPredictionUpdate(StopPrediction prediction) {

		DateTime earliestUpdate = new DateTime(prediction.timestamp)
				.plus(Constants.SHORTEST_UPDATE_TIME);

		if (prediction.predictions.size() == 0) {
			return DateUtil.later(new DateTime(), earliestUpdate);
		} else {
			DateTime lastArrival = new DateTime(
					prediction.predictions.get(prediction.predictions.size() - 1).arrival);
			if (lastArrival.isBeforeNow()) {
				return DateUtil.later(lastArrival, earliestUpdate);
			}
		}

		return earliestUpdate;
	}

}
