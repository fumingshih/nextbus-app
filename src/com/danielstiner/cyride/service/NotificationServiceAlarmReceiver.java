package com.danielstiner.cyride.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationServiceAlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		NotificationService.onAlarm(context, intent);
	}
	
}