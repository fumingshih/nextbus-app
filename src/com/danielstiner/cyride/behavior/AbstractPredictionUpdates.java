package com.danielstiner.cyride.behavior;

import org.joda.time.DateTime;

import com.danielstiner.cyride.service.IPredictions.NearbyStopPredictions;
import com.danielstiner.cyride.util.Constants;
import com.danielstiner.cyride.util.DateUtil;
import com.danielstiner.nextbus.NextBusAPI.StopPrediction;

public abstract class AbstractPredictionUpdates implements
		IPredictionUpdateStrategy {

	private int backoff_factor = 0;
	private static final int BACKOFF_MULTIPLIER = 1;

	@Override
	public DateTime nextPredictionUpdate(NearbyStopPredictions predictions) {

		if (predictions == null) {
			int factor = backoff_factor;
			backoff_factor *= factor + 1;
			return new DateTime().plusSeconds(factor * BACKOFF_MULTIPLIER);
		} else {
			backoff_factor /= 2;
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
