package com.hackathon.tvnight.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.hackathon.tvnight.R;
import com.hackathon.tvnight.model.TVShow;

public class ShowListActivity extends Activity {

	private ListView showList;
	private TVShowAdapter showsAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_list_layout);
		showList = (ListView) findViewById(R.id.show_list);
		new AsyncTask<Void, Void, List<TVShow>>() {
			@Override
			protected List<TVShow> doInBackground(Void... params) {
				//TODO add http request here and return array of model object to show to UI
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				//hardcoding for now
				ArrayList<TVShow> shows = new ArrayList<TVShow>();
				shows.add(new TVShow("Mad Men"));
				shows.add(new TVShow("Suits"));
				shows.add(new TVShow("LOST"));
				shows.add(new TVShow("True Blood"));
				shows.add(new TVShow("Dexter"));
				shows.add(new TVShow("How I Met Your Mother"));
				return shows;
			}
			
			protected void onPostExecute(List<TVShow> result) {
				findViewById(R.id.progress_spinner).setVisibility(View.GONE);
				showsAdapter = new TVShowAdapter(result);
				showList.setAdapter(showsAdapter);
				showList.setVisibility(View.VISIBLE);
			};
			
		}.execute();
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
			
			myViewData.title.setText(show.getName());
			
			convertView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent i = new Intent(ShowListActivity.this, ShowDescActivity.class);
//					i.putExtra(name, value);
					startActivity(i);
				}
			});
			
			return convertView;
		}
		
	}

}
