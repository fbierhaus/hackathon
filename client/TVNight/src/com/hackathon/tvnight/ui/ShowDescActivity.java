package com.hackathon.tvnight.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.hackathon.tvnight.R;

public class ShowDescActivity extends Activity implements OnClickListener {

	private Button createWatchGroup;
	private Button saveButt;
	private ArrayList<String> selectedNums;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.show_desc_layout);
		createWatchGroup = (Button) findViewById(R.id.create_watch_group_butt);
		saveButt = (Button) findViewById(R.id.save_butt);
		saveButt.setOnClickListener(this);
		createWatchGroup.setOnClickListener(this);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
			selectedNums = data.getStringArrayListExtra("selected_nums");
			createWatchGroup.setText("Edit Watch Group ("+selectedNums.size()+")");
			saveButt.setVisibility(View.VISIBLE);
		}
		else {
			selectedNums = null;
			createWatchGroup.setText("Create a Watch Group");
			saveButt.setVisibility(View.GONE);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.create_watch_group_butt:
			Intent startContactSelect = new Intent(this, ContactListSelectorActivity.class);
			if (selectedNums != null) {
				startContactSelect.putStringArrayListExtra("selected_nums", selectedNums);
			}
			startActivityForResult(startContactSelect, 0);
			break;
		case R.id.save_butt:
			//TODO send request to server to send out text message to recips
			//recpis are in selectedNums
			break;
		}
	}
	
}
