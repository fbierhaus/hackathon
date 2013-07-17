package com.hackathon.tvnight.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.hackathon.tvnight.R;

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
		whosWatching = (TextView) findViewById(R.id.watch_list);
		acceptInvite = (Button) findViewById(R.id.accept_inv);
		declineInvite = (Button) findViewById(R.id.decline_inv);
		super.onCreate(savedInstanceState);
	}
	
}
