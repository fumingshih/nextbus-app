package com.danielstiner.cyride.service;

import java.util.LinkedList;
import java.util.Queue;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.danielstiner.cyride.util.Callback;
import com.danielstiner.cyride.util.Functor1;

public class ServiceConnector<ServiceInterface> {

	private ServiceInterface mBoundService;

	private Functor1<IBinder, ServiceInterface> mBoundServiceGetter;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mBoundService = mBoundServiceGetter.apply(service);

			while (!mScheduledCallbacks.isEmpty())
				mScheduledCallbacks.poll().run(mBoundService);
		}

		public void onServiceDisconnected(ComponentName className) {
			mBoundService = null;
		}
	};
	private boolean mIsBound = false;
	private Queue<Callback<ServiceInterface>> mScheduledCallbacks = new LinkedList<Callback<ServiceInterface>>();

	private Class<? extends Service> serviceClass;

	private ServiceConnector(Class<? extends Service> serviceClass,
			Functor1<IBinder, ServiceInterface> getServiceFromBinder) {
		this.serviceClass = serviceClass;
		this.mBoundServiceGetter = getServiceFromBinder;
	}

	public void bind(Context context) {
		if (!mIsBound)
			context.bindService(new Intent(context, serviceClass), mConnection,
					Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	public void schedule(Callback<ServiceInterface> callback) {
		if (mIsBound && null != mBoundService) {
			callback.run(mBoundService);
		} else {
			mScheduledCallbacks.add(callback);
		}
	}

	public void unbind(Context context) {
		if (mIsBound) {
			// Detach our existing connection.
			context.unbindService(mConnection);
			mIsBound = false;
		}
	}

	public static <ServiceInterface> ServiceConnector<ServiceInterface> createConnection(
			Class<? extends Service> serviceClass,
			Functor1<IBinder, ServiceInterface> getServiceFromBinder) {
		return new ServiceConnector<ServiceInterface>(serviceClass,
				getServiceFromBinder);
	}

	public void maybeNow(Callback<ServiceInterface> callback) {
		if (mIsBound && null != mBoundService) {
			callback.run(mBoundService);
		}
	}

}
