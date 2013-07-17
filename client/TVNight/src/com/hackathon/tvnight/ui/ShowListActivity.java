package com.hackathon.tvnight.ui;

import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hackathon.tvnight.R;
import com.hackathon.tvnight.model.TVShow;
import com.hackathon.tvnight.task.GetShowListTask;

public class ShowListActivity extends Activity implements OnClickListener {
	private final static int MSG_SHOW_LIST = 1;

	public static final boolean TEST_INVITE_ACTIVITY = false;
	
	private ListView showList;
	private TVShowAdapter showsAdapter;
	
	/**
	 * Task to get show list. You can keep calling this task if the list has not ended yet.
	 * It returns an empty list if no more show or null if failed.
	 * Results returned in Message.obj 
	 */
	private GetShowListTask getShowListTask = null;
	private EditText searchTerm;
	private Button submit;
	
	private Handler handler = new Handler() {
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MSG_SHOW_LIST) {

				findViewById(R.id.progress_spinner).setVisibility(View.GONE);

				List<TVShow> shows = (List<TVShow>)msg.obj;
				if (shows == null) {
					// error
				}
				else {
					showsAdapter = new TVShowAdapter(shows);
					for (TVShow show : shows) {
						if (new Random().nextInt(3) == 1) {
							show.setSimulatePaid(true);
						}
						else {
							show.setSimulatePaid(false);
						}
						
					}
					showList.setAdapter(showsAdapter);
					showList.setVisibility(View.VISIBLE);
				}
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_list_layout);
		showList = (ListView) findViewById(R.id.show_list);
		searchTerm = (EditText) findViewById(R.id.search_term);
		submit = (Button) findViewById(R.id.submit);
		submit.setOnClickListener(this);
				
		if (getIntent().getBooleanExtra("fromnotif", false) || TEST_INVITE_ACTIVITY) {
			Intent i = new Intent(this, ReminderActivity.class);
			startActivity(i);
		}
		
		getShowListTask = new GetShowListTask(handler, MSG_SHOW_LIST, "super-man");
		getShowListTask.execute(10);	// get 10 at a time
	}
	
	@Override
	public void onDestroy() {
		if (getShowListTask != null) {
			getShowListTask.cancelOperation();
		}
		super.onDestroy();
	}
	
	static class ShowViewHolder {
		private TextView title;
		private ImageView icon;
		private TextView episodeTitle;
	}
	
	class TVShowAdapter extends BaseAdapter {

		private List<TVShow> shows;
		
		public TVShowAdapter(List<TVShow> shows) {
			this.shows = shows;
		}
		
		@Override
		public int getCount() {
			return shows.size();
		}

		@Override
		public Object getItem(int position) {
			return shows.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final TVShow show = shows.get(position);
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.show_item, null);
				ShowViewHolder holder = new ShowViewHolder();
				holder.episodeTitle = (TextView) convertView.findViewById(R.id.episode_title);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				holder.title = (TextView) convertView.findViewById(R.id.title);
				convertView.setTag(holder);
			}
			
			ShowViewHolder myViewData = (ShowViewHolder) convertView.getTag();
			
			myViewData.title.setText(show.getDefaultTitle());
			
			if (show.getSimulatePaid()) {
				myViewData.episodeTitle.setText("First Episode -- $$$");
			}
			else {
				myViewData.episodeTitle.setText("First Episode");
			}
						
			convertView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent i = new Intent(ShowListActivity.this, ShowDescActivity.class);
					i.putExtra("name", show.getDefaultTitle());
					i.putExtra("desc", show.getDefaultDescription());
					i.putExtra("simpaid", show.getSimulatePaid());
					startActivity(i);
				}
			});
			
			return convertView;
		}
		
	}

	@Override
	public void onClick(View v) {
		if (getShowListTask != null) {
			getShowListTask.cancelOperation();
		}
		String search = searchTerm.getText().toString();
		if (search.trim().length() < 1) {
			Toast.makeText(this, "Please enter a valid search term.", Toast.LENGTH_SHORT).show();
			return;
		}
		showsAdapter = null;
		findViewById(R.id.progress_spinner).setVisibility(View.VISIBLE);
		showList.setVisibility(View.GONE);
		
		if (getShowListTask != null) {
			getShowListTask.cancelOperation();
		}
		
		getShowListTask = new GetShowListTask(handler, MSG_SHOW_LIST, search);
		getShowListTask.execute(10);
	}

}
