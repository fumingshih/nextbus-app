package com.danielstiner.cyride.service;

import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;

public interface INotifications {

	public abstract void showNotification(StopPrediction predictions);

}