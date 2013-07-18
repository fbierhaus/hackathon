package com.hackathon.tvnight.ui;

import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hackathon.tvnight.R;
import com.hackathon.tvnight.model.ShowImage;
import com.hackathon.tvnight.model.TVShow;
import com.hackathon.tvnight.task.GetShowImageTask;
import com.hackathon.tvnight.task.GetShowListTask;
import com.koushikdutta.urlimageviewhelper.UrlImageViewCallback;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

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
	private GetShowImageTask myImageTask = null;
	private EditText searchTerm;
	private ProgressBar progressSpinner;
	private Button submit;

	private Handler imageHandler = new Handler() {
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			String roviId = data.getString(TVShow.ID_KEY_ROVI);
			List<ShowImage> list = (List<ShowImage>) msg.obj;
			for (TVShow show : showsAdapter.getShows()) {
				if (show == null || show.getId(TVShow.ID_KEY_ROVI) == null || list == null || list.get(0) == null || list.get(0).getUrl() == null) {
					continue;
				}
				if (show.getId(TVShow.ID_KEY_ROVI).equalsIgnoreCase(roviId)) {
					show.setImageUrl(list.get(0).getUrl());
				}
			}
			showsAdapter.notifyDataSetChanged();
		}
	};
	
	private Handler handler = new Handler() {
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MSG_SHOW_LIST) {

				progressSpinner.setVisibility(View.GONE);

				List<TVShow> shows = (List<TVShow>)msg.obj;
				if (shows == null) {
					// error
				}
				else {
					showsAdapter = new TVShowAdapter(shows);
					for (TVShow show : shows) {
						if (new Random().nextInt(3) == 1 || show.getDefaultTitle().startsWith("Big Bang Theory")) {
							show.setSimulatePaid(true);
						}
						else {
							show.setSimulatePaid(false);
						}
						
					}
					
					showList.setAdapter(showsAdapter);
					showList.setVisibility(View.VISIBLE);
					String[] roviIds = new String[shows.size()];
					for (int i = 0; i < shows.size(); i++) {
						roviIds[i] = shows.get(i).getId(TVShow.ID_KEY_ROVI);
					}
					if (myImageTask != null) {
						myImageTask.cancelOperation();
					}
					myImageTask = new GetShowImageTask(imageHandler, 1);
					myImageTask.execute(roviIds);
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
		
		progressSpinner = (ProgressBar) findViewById(R.id.progress_spinner);
		
		getShowListTask = new GetShowListTask(handler, MSG_SHOW_LIST, "super-man");
		getShowListTask.execute(10);	// get 10 at a time
	}
	
	@Override
	public void onDestroy() {
		if (getShowListTask != null) {
			getShowListTask.cancelOperation();
		}
		if (myImageTask != null) {
			myImageTask.cancelOperation();
		}
		super.onDestroy();
	}
	
	static class ShowViewHolder {
		private TextView title;
		private ImageView icon;
		private TextView episodeTitle;
		private ProgressBar loading;
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
		
		public List<TVShow> getShows() {
			return shows;
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
				holder.loading = (ProgressBar) convertView.findViewById(R.id.loading);
				convertView.setTag(holder);
			}
			
			final ShowViewHolder myViewData = (ShowViewHolder) convertView.getTag();
			
			myViewData.title.setText(show.getDefaultTitle());
			
			if (show.getSimulatePaid()) {
				myViewData.episodeTitle.setText("First Episode -- $$$");
			}
			else {
				myViewData.episodeTitle.setText("First Episode");
			}
			
			if (show.getImageUrl() != null) {
				myViewData.icon.setVisibility(View.GONE);
				myViewData.loading.setVisibility(View.VISIBLE);
				UrlImageViewHelper.setUrlDrawable(myViewData.icon, show.getImageUrl(), new UrlImageViewCallback() {
					@Override
					public void onLoaded(ImageView arg0, Bitmap arg1, String arg2, boolean arg3) {
						myViewData.icon.setVisibility(View.VISIBLE);
						myViewData.loading.setVisibility(View.GONE);
					}
				});
			}
			else {
				myViewData.icon.setImageResource(R.drawable.ic_missing_thumbnail_video);
				myViewData.icon.setVisibility(View.VISIBLE);
				myViewData.loading.setVisibility(View.GONE);
			}
			
			convertView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent i = new Intent(ShowListActivity.this, ShowDescActivity.class);
					i.putExtra("name", show.getDefaultTitle());
					i.putExtra("desc", show.getDefaultDescription());
					i.putExtra("simpaid", show.getSimulatePaid());
					i.putExtra("imgurl", show.getImageUrl());
					i.putExtra("show_id", show.getId(TVShow.ID_KEY_MERLIN));
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
		progressSpinner.setVisibility(View.VISIBLE);
		showList.setVisibility(View.GONE);
		
		if (getShowListTask != null) {
			getShowListTask.cancelOperation();
		}
		if (myImageTask != null) {
			myImageTask.cancelOperation();
		}
		
		getShowListTask = new GetShowListTask(handler, MSG_SHOW_LIST, search);
		getShowListTask.execute(10);
	}

}
