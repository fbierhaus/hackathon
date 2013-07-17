package com.hackathon.tvnight.ui;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
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
import com.hackathon.tvnight.model.TextEntry;
import com.hackathon.tvnight.task.GetShowListTask;

public class ShowListActivity extends Activity implements OnClickListener {
	private final static int MSG_SHOW_LIST = 1;

	public static final boolean TEST_INVITE_ACTIVITY = false;
	
	private ListView showList;
	private TVShowAdapter showsAdapter;
	private GetShowListTask getShowListTask = null;
	private EditText searchTerm;
	private Button submit;
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MSG_SHOW_LIST) {

				List<TVShow> shows = getShowListTask.getShowList();

				findViewById(R.id.progress_spinner).setVisibility(View.GONE);
				showsAdapter = new TVShowAdapter(shows);
				showList.setAdapter(showsAdapter);
				showList.setVisibility(View.VISIBLE);
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
		
		getShowListTask = new GetShowListTask(handler, MSG_SHOW_LIST, null);
		getShowListTask.execute();
	}
	
	@Override
	protected void onResume() {
		if (getIntent().getBooleanExtra("fromnotif", false) || TEST_INVITE_ACTIVITY) {
			Intent i = new Intent(this, ReminderActivity.class);
			startActivity(i);
		}
		super.onResume();
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
			
			String name = "Unknown";
			final String finalName, finalDesc;
			List<TextEntry> titleList = show.getTitle();
			if (titleList.size() > 0) {
				TextEntry entry = titleList.get(0);
				name = entry.getDefault();
			}				
			myViewData.title.setText(name);
			finalName = name;
			
			String desc = "Unknown";
			List<TextEntry> descList = show.getDescription();
			if (descList.size() > 0) {
				TextEntry descEntry = descList.get(0);
				desc = descEntry.getDefault();
			}
			finalDesc = desc;
			
			convertView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent i = new Intent(ShowListActivity.this, ShowDescActivity.class);
					i.putExtra("name", finalName);
					i.putExtra("desc", finalDesc);
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
		
		getShowListTask = new GetShowListTask(handler, MSG_SHOW_LIST, search);
		getShowListTask.execute();
	}

}
