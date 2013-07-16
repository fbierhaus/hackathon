package com.verizon.mms.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.rocketmobile.asimov.Asimov;

public class MMSReadReportReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
	//	long id = intent.getExtras().getLong("READ_REPORT_ID");6
		long threadID = -1;
		long msgID = -1;
		int status = intent.getExtras().getInt("READ_REPORT_STATUS");
		threadID = intent.getExtras().getLong("READ_REPORT_THREAD_ID") ;
		threadID = threadID == 0 ? -1 : threadID; 
		msgID = intent.getExtras().getLong("READ_REPORT_ID");
		MMSReadReport.handleReadReport(Asimov.getApplication(), msgID, threadID, status);
		
	} 
	
}
