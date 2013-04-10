package com.danielstiner.cyride;

import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;

public interface ILocalService extends IPredictions {

	public abstract void showNotification(StopPrediction predictions);

}