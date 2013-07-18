package com.hackathon.tvnight.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.hackathon.tvnight.R;
import com.hackathon.tvnight.model.ShowingResult;
import com.hackathon.tvnight.sms.SMSReceiver;

public class InviteProposeActivity extends Activity {

	private TextView invitationFrom;
	private TextView showName;
	private TextView showDesc;
	private TextView showTime;
	private CheckBox remindMe;
	private TextView whosWatching;
	private Button acceptInvite;
	private Button declineInvite;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.invite_layout);
		invitationFrom = (TextView) findViewById(R.id.invite_from_header);
		showName = (TextView) findViewById(R.id.show_title);
		showDesc = (TextView) findViewById(R.id.show_desc);
		showTime = (TextView) findViewById(R.id.show_date_and_time);
		remindMe = (CheckBox) findViewById(R.id.remind_me_checkbox);
		// whosWatching = (TextView) findViewById(R.id.watch_list);
		acceptInvite = (Button) findViewById(R.id.accept_inv);
		declineInvite = (Button) findViewById(R.id.decline_inv);
		acceptInvite.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (remindMe.isChecked()) {
					Toast.makeText(InviteProposeActivity.this, "Invite Accepted! You will get a reminder 1 minute before the show begins!", Toast.LENGTH_LONG).show();					
				}
				else {
					Toast.makeText(InviteProposeActivity.this, "Invite Accepted! You will not get a reminder for this show!", Toast.LENGTH_LONG).show();
				}
				finish();
			}
		});
		declineInvite.setEnabled(false); //disabled for demo
		String sender = getIntent().getStringExtra(SMSReceiver.EXTRA_SENDER);
		invitationFrom.setText("Invite From: " + sender);

		showTime.setText(ShowingResult.convertTime(System.currentTimeMillis()
				+ (3 * 60 * 1000)));

		super.onCreate(savedInstanceState);
	}

}
