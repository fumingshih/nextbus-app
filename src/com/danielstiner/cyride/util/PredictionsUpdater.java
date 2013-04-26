package com.danielstiner.cyride.util;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;

import android.os.Handler;
import de.akquinet.android.androlog.Log;

public class PredictionsUpdater {

	private final Runnable mTask;

	private final Task<DateTime> mPredictionTimeUpdateCalculator;

	private Handler mHandler;

	private ReadableInstant nextScheduledUpdate;

	public PredictionsUpdater(Task<DateTime> nextUpdateTimeCalculator,
			Runnable task) {
		this.mPredictionTimeUpdateCalculator = nextUpdateTimeCalculator;
		this.mTask = task;
	}

	private void scheduleUpdate() {
		DateTime updateAt = mPredictionTimeUpdateCalculator.get();

		if (this.nextScheduledUpdate != null
				&& updateAt.isAfter(this.nextScheduledUpdate))
			return;

		this.nextScheduledUpdate = updateAt;

		long delayMillis = Math.max(0, updateAt.getMillis() - DateTime.now().getMillis());

		Log.v(this, "Scheduling predictions update in: " + delayMillis/1000 + "s");

		if (null != nextScheduledUpdate)
			stop();
		mHandler.postDelayed(mTask, delayMillis);
	}

	public void schedule(Handler handler) {
		this.mHandler = handler;
		scheduleUpdate();
	}

	public void stop() {
		mHandler.removeCallbacks(mTask);
		this.nextScheduledUpdate = null;
	}
}
