package com.verizon.mms.transaction;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class ToastHandler extends Thread{
	public ToastHandler() {
		start();
	}
	Handler toastHandler =null;
	public void run () {
		Looper.prepare(); 
		toastHandler = new Handler();
		Looper.loop();
	}
	public void queueToast(final Context context, final CharSequence message, final long timeMillis) {
		toastHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context, message, (int) timeMillis).show();
			}
		});
	}

}
