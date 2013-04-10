package com.danielstiner.cyride;

import java.util.List;

import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;

public interface PredictionsListener extends Callback<List<StopPrediction>> {

}
