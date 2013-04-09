package com.danielstiner.cyride;

import android.R;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;

public class Service extends android.app.Service {
	
	private static final String AGENCY = "cyride";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		return super.onStartCommand(intent, flags, startId);
	}
	
//	private void showNearbyNotification(){
//		Intent notificationIntent = new Intent(ctx, YourClass.class);
//		PendingIntent contentIntent = PendingIntent.getActivity(ctx,
//		        YOUR_PI_REQ_CODE, notificationIntent,
//		        PendingIntent.FLAG_CANCEL_CURRENT);
//
//		NotificationManager nm = (NotificationManager) ctx
//		        .getSystemService(Context.NOTIFICATION_SERVICE);
//
//		Resources res = ctx.getResources();
//		Notification.Builder builder = new Notification.Builder(ctx);
//
//		builder.setContentIntent(contentIntent)
//		            .setSmallIcon(R.drawable.arrow_down_float)
//		            .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.alert_dark_frame))
//		            .setTicker(res.getString(R.string.nearby_ticker))
//		            .setWhen(System.currentTimeMillis())
//		            .setAutoCancel(true)
//		            .setContentTitle(res.getString(R.string.your_notif_title))
//		            .setContentText(res.getString(R.string.your_notif_text));
//		Notification n = builder.build();
//
//		nm.notify(YOUR_NOTIF_ID, n);
//	}
	
	//"http://webservices.nextbus.com/service/publicXMLFeed?command=routeList&a=cyride"
	
	//"http://webservices.nextbus.com/service/publicXMLFeed?command=routeConfig&a=cyride&terse=true"

}
