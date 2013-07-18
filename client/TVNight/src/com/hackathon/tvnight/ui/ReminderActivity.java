package com.hackathon.tvnight.ui;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.hackathon.tvnight.R;
import com.hackathon.tvnight.model.ShowingResult;

public class ReminderActivity extends Activity implements OnClickListener {
	
	private String recipients;
	private Button enterGroupChat;
	private ReminderTimer myTimer;
	private TextView time;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.reminder_layout);
		myTimer = new ReminderTimer(60000, 1000);
		myTimer.start();
		time = (TextView) findViewById(R.id.date_and_time);
		time.setText(ShowingResult.convertTime(System.currentTimeMillis()+(60000)));
		recipients = getIntent().getStringExtra("recips");
		enterGroupChat = (Button) findViewById(R.id.enter_group_chat);
		enterGroupChat.setOnClickListener(this);
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onClick(View v) {
		 Intent intent = new Intent();
		 intent.setAction("com.verizon.messaging.vzmsgs.GROUPMMSTO");
		 intent.putExtra("prepopulated_addresses", recipients);
         startActivity(intent);
	}
	
	private String formatTime(long time) {
		return String.format(Locale.US, "%02d:%02d",
				TimeUnit.MILLISECONDS.toMinutes(time), TimeUnit.MILLISECONDS.toSeconds(time));
	}
	
	class ReminderTimer extends CountDownTimer {

		public ReminderTimer(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}

		@Override
		public void onFinish() {
			time.setText("Your show is starting! Server is attempting to change your channel to the designated show!");
		}

		@Override
		public void onTick(long millisUntilFinished) {
			time.setText(formatTime(millisUntilFinished));
		}
		
	}

}
