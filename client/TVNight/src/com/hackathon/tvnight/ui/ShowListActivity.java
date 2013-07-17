package com.hackathon.tvnight.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.hackathon.tvnight.R;
import com.hackathon.tvnight.model.TVShow;
import com.hackathon.tvnight.model.TextEntry;
import com.hackathon.tvnight.task.GetShowListTask;

public class ShowListActivity extends Activity {
	private final static int MSG_SHOW_LIST = 1;

	private ListView showList;
	private TVShowAdapter showsAdapter;
	private GetShowListTask getShowListTask = null;
	
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
		
		getShowListTask = new GetShowListTask(handler, MSG_SHOW_LIST);
		getShowListTask.execute();
	}
	
	@Override
	public void onDestroy() {
		if (getShowListTask != null) {
			getShowListTask.cancelOperation();
		}
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
			TVShow show = shows.get(position);
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
			List<TextEntry> titleList = show.getTitle();
			if (titleList.size() > 0) {
				TextEntry entry = titleList.get(0);
				name = entry.getDefault();
			}				
			myViewData.title.setText(name);
			
			convertView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
//					Intent i = new Intent(ShowListActivity.this, ShowDescActivity.class);
////					i.putExtra(name, value);
//					startActivity(i);
				}
			});
			
			return convertView;
		}
		
	}

}
