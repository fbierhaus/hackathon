package com.verizon.mms.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;

import com.rocketmobile.asimov.Asimov;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ui.widget.OnOffChooser;
import com.verizon.mms.ui.widget.OnOffChooser.OnStateChangedListener;

public class CustomizeBubblesActivity extends VZMActivity implements OnClickListener{
	
	public DragNDropListView mListView;
	private OnOffChooser mChooser;
	private int mOrientation;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.custom_bubble_order_screen);

		findViewById(R.id.btnSave).setOnClickListener(this);
		findViewById(R.id.btnCancel).setOnClickListener(this);

		mOrientation = getResources().getConfiguration().orientation;
		
		mListView = (DragNDropListView) findViewById(R.id.convList);
		mAdapter = new DragNDropAdapter(this, ConversationResHelper.getAllBubbles(), ConversationResHelper.fillBubble(), mListView);
		mListView.setAdapter(mAdapter);
		mListView.setBackgroundColor(ConversationResHelper.getBGColor());
		mListView.setDropListener(mDropListener);
		mListView.setRemoveListener(mRemoveListener);
		mListView.setDragListener(mDragListener);
				
		mChooser = (OnOffChooser)findViewById(R.id.group_chooser);
		mChooser.setGroupMode(ConversationResHelper.fillBubble());
		mChooser.setListener(new OnStateChangedListener() {
			@Override
			public void stateChanged(boolean groupMms) {
				mAdapter.fillBubbleColor(groupMms);
			}
		});
	}

	private DropListener mDropListener = 
			new DropListener() {
		public void onDrop(int from, int to) {
			mAdapter.onDrop(from, to);
			mListView.invalidateViews();
		}
	};

	@Override
	protected void onDestroy() {
		mListView.setAdapter(null);
		mListView = null;
		
		super.onDestroy();
	}
    

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		if (mOrientation != newConfig.orientation) {
			//reset the adapter since the seekBar doesnt seem to update the background 
			//gradient drawable
			mOrientation = newConfig.orientation;
			ArrayList<Integer> colors = mAdapter.getEditedColors();
			
			mAdapter = new DragNDropAdapter(this, colors, mChooser.getToggleState(), mListView);
			mListView.setAdapter(mAdapter);
		}
	}

	private RemoveListener mRemoveListener =
			new RemoveListener() {
		public void onRemove(int which) {
			mAdapter.onRemove(which);
			mListView.invalidateViews();
		}
	};

	protected DragNDropAdapter mAdapter;

	private DragListener mDragListener =
			new DragListener() {

		public void onDrag(int x, int y, ListView listView) {
		}

		public void onStartDrag(View itemView, int pos) {
			mAdapter.onStartDrag(itemView, pos);
		}

		public void onStopDrag(View itemView) {
			mAdapter.onStopDrag();
		}
	};

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.btnSave:
			Editor edit = PreferenceManager.getDefaultSharedPreferences(CustomizeBubblesActivity.this).edit();
			ArrayList<Integer> colors = mAdapter.getEditedColors();
			edit.putInt(ConversationResHelper.CONV_RIGHT_BUBBLE_COLOR, colors.get(0));
			edit.putString(ConversationResHelper.CONV_LEFT_BUBBLE_COLORS, 
					convertColorstoString(colors));
			edit.putBoolean(ConversationResHelper.CONV_FILL_BUBBLE_COLOR, mChooser.getToggleState());
			edit.commit();

			ConversationResHelper.refresh(CustomizeBubblesActivity.this);
			finish();
			break;
		case R.id.btnCancel:
			finish();
			break;
		}
	}
	
	private String convertColorstoString(ArrayList<Integer> ImageList){
		if (ImageList != null)
		{
			StringBuilder sb = new StringBuilder();
			sb.setLength(0);
			
			//color at 0 position is for right bubble so ignore it and save only left bubble colors
			sb.append(ImageList.get(1));
			
			for (int i = 2; i < ImageList.size(); i++)
			{
				sb.append(",").append((ImageList.get(i)));
			}
			return sb.toString();
		}
		return null;
	}
}
