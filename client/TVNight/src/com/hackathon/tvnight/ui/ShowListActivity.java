package com.hackathon.tvnight.ui;

import java.util.List;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import com.hackathon.tvnight.R;
import com.hackathon.tvnight.model.TVShow;

public class ShowListActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_list_layout);
		new AsyncTask<Void, Void, List<TVShow>>() {
			@Override
			protected List<TVShow> doInBackground(Void... params) {
				//TODO add http request here and return array of model object to show to UI
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				//hardcoding for now
				
				return null;
			}
			
			protected void onPostExecute(Void result) {
				findViewById(R.id.progress_spinner).setVisibility(View.GONE);
				findViewById(R.id.show_list).setVisibility(View.VISIBLE);
			};
			
		}.execute();
	}

}
