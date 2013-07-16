package com.hackathon.tvnight.ui;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.hackathon.tvnight.R;

public class ShowDescActivity extends Activity implements OnClickListener {

	private Button createWatchGroup;
	private List<String> selectedNums;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.show_desc_layout);
		createWatchGroup = (Button) findViewById(R.id.create_watch_group_butt);
		createWatchGroup.setOnClickListener(this);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
			
		}
		else {
			
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void onClick(View v) {
		startActivityForResult(new Intent(this, ContactListSelectorActivity.class), 0);
	}
	
}
