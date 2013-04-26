package com.danielstiner.cyride.behavior;

import org.joda.time.DateTime;

import com.danielstiner.cyride.service.IPredictions.NearbyStopPredictions;
import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.DateUtil;
import com.danielstiner.nextbus.NextBusAPI.StopPrediction;

public abstract class AbstractPredictionUpdates implements
		IPredictionUpdateStrategy {

	@Override
	public DateTime nextPredictionUpdate(NearbyStopPredictions predictions) {

		if (predictions == null) {
			return DateTime.now();
		} else {
			DateTime updateAt = DateTime.now().plus(
					Constants.LONGEST_UPDATE_TIME);
			if (predictions != null) {
				for (StopPrediction p : predictions.predictions) {
					updateAt = DateUtil.earlier(updateAt,
							nextPredictionUpdate(p));

				}
			}

			return updateAt;
		}
	}
}
