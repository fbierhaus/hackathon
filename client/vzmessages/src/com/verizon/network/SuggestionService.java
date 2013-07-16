package com.verizon.network;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.DeviceConfig;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.ui.SendCommentsToDeveloper;
import com.verizon.mms.util.SendCommentsToDeveloperHttpClient;

/**
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * 
 */
public class SuggestionService extends AbstractNetworkConnectionService {

	private static final int MAX_RETRY_COUNT = 3;

	@Override
	public boolean preExecute(int startId, Bundle bundle) {
		return true;
	}

	@Override
	public boolean execute(int startId, Bundle bundle) throws Exception {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(SuggestionService.class, "send data in execute()==>"
					+ bundle);
		}
		HttpResponse response = null;
		HttpPost httpPost = new HttpPost(getApiUrl());
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(SuggestionService.class, "==>> [request] POST => url="
					+ httpPost.getURI());
		}

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("subject", bundle.getString("subject"));
			jsonObject.put("comments", bundle.getString("comment"));
			httpPost.setHeader("Content-type", "application/json;charset=UTF-8");
			httpPost.setEntity(new StringEntity(jsonObject.toString()));
			response = SendCommentsToDeveloperHttpClient.execute(httpPost);
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(SuggestionService.class, "http response ==>"
						+ response);
			}
		} catch (JSONException e) {
			Logger.debug(SuggestionService.class,
					"JSONException caught while creating JSON object ==>" + e);

		}
		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			return true;
		}

		return false;
	}

	@Override
	public void onExecuteFailure(ErrorType type, Throwable th, int startId,
			Bundle bundle) {
		int counter = bundle.getInt("retry_counter");
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(SuggestionService.class, "execute failure ==>");
			Logger.debug(SuggestionService.class, "error type ==> " + type);
			Logger.debug(SuggestionService.class, "th ==> "+ th);
			Logger.debug(SuggestionService.class, "bundle ==> " + bundle);
			Logger.debug(SuggestionService.class, "counter for retry ==> "
					+ counter);
		}	
		if (counter < MAX_RETRY_COUNT) {
			bundle.putInt("retry_counter", counter + 1);
			start(startId, bundle);
		}else{
			showNotification(
					ctx.getString(R.string.send_comments_notification_failure_title),
					ctx.getString(R.string.send_comments_notification_failure_text),
					new Intent(this, SendCommentsToDeveloper.class));
		}

	}

	@Override
	public void onExecuteSuccess(int startId, Bundle bundle) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(SuggestionService.class,
					"onExecuteSuccess bundle ==> " + bundle);
		}
		showNotification(
				ctx.getString(R.string.send_comments_notification_success_title),
				ctx.getString(R.string.send_comments_notification_success_text),
				new Intent(this, ConversationListActivity.class));
	}
	
	public void showNotification(String title, String text, Intent intent){
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(SuggestionService.class,
					"inside showNotification ==> ");
		}
		Notification notification = new Notification(
				R.drawable.launcher_home_icon, title, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
		notification.setLatestEventInfo(this, title,text, contentIntent);
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.notify(1, notification);
	}

	@Override
	public String getPhoneFeature() {
		//return Phone.FEATURE_ENABLE_MMS;
		return MmsConfig.getPhoneFeatureMms();
	}

	@Override
	public int getNetworkType() {
		//return ConnectivityManager.TYPE_MOBILE_MMS;
		return MmsConfig.getNetworkConnectivityMode();
	}

	private String getModel() {
		return DeviceConfig.OEM.deviceModel;
	}

	private String getAppVersion() {
		return MmsConfig.BUILD_VERSION;
	}

	private String getApiUrl() throws UnsupportedEncodingException {
		// ?make={1}&amp;model={2}&amp;mdn={3}&amp;version={4}
		String fmt = (String) getResources().getText(
				R.string.send_comments_to_dev_url);
		String uri = MessageFormat.format(fmt,
				URLEncoder.encode(getModel(), "UTF-8"), // model
				URLEncoder.encode(getAppVersion(), "UTF-8") // version
				);
		return uri;
	}
}