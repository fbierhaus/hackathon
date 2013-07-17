package com.hackathon.tvnight.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.hackathon.tvnight.R;

public class ShowDescActivity extends Activity implements OnClickListener {

	private Button createWatchGroup;
	private TextView showTitle;
	private TextView showDesc;
	private Button saveButt;
	private ArrayList<String> selectedNums;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.show_desc_layout);
		createWatchGroup = (Button) findViewById(R.id.create_watch_group_butt);
		showTitle = (TextView) findViewById(R.id.show_title);
		showDesc = (TextView) findViewById(R.id.show_desc);
		showTitle.setText(getIntent().getStringExtra("name"));
		showDesc.setText(getIntent().getStringExtra("desc"));
		saveButt = (Button) findViewById(R.id.save_butt);
		if (getIntent().getBooleanExtra("simpaid", false)) {
			findViewById(R.id.purchase_butt).setVisibility(View.VISIBLE);
		}
		saveButt.setOnClickListener(this);
		createWatchGroup.setOnClickListener(this);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
			selectedNums = data.getStringArrayListExtra("selected_nums");
			createWatchGroup.setText("Edit Watch Group ("+selectedNums.size()+")");
			findViewById(R.id.save_layout).setVisibility(View.VISIBLE);
		}
		else {
			selectedNums = null;
			createWatchGroup.setText("Create a Watch Group");
			findViewById(R.id.save_layout).setVisibility(View.GONE);
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
