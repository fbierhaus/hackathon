package com.verizon.mms.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.LinearGradient;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.rocketmobile.asimov.Asimov;
import com.verizon.messaging.vzmsgs.R;

public class CustomizeBackgroundActivity extends VZMActivity implements OnClickListener{

	private int mStartSeekbarValue = 1;


	private SeekBar mSeekBar;
	private ShapeDrawable mShapePotrait;
	private ShapeDrawable mShapeLandscape;

	private SharedPreferences mPrefs;

	protected int mBackgroundColor;

	private ListView mListView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.custom_bg_screen);

		findViewById(R.id.btnSave).setOnClickListener(this);
		findViewById(R.id.btnCancel).setOnClickListener(this);

		mListView = (ListView) findViewById(R.id.convList);
		mListView.setAdapter(new CustomAdapter(this, R.layout.customize_bubble_item, R.id.timestamp));

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mBackgroundColor = ConversationResHelper.getBGColor();

		initSeekBar();

		applyBackground();
	}

	private void initSeekBar() {
		mSeekBar = (SeekBar)findViewById(R.id.seekbar);
		mSeekBar.setMax(ConversationResHelper.COLORS.length-1);

		LinearGradient linearGradientPotrait;
		LinearGradient linearGradientLandscape;

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			linearGradientPotrait = new LinearGradient(0, 0, getWindowManager().getDefaultDisplay().getWidth() - 30, 0, ConversationResHelper.COLORS ,
					null, Shader.TileMode.CLAMP);
			linearGradientLandscape = new LinearGradient(0, 0, getWindowManager().getDefaultDisplay().getHeight() - 30, 0, ConversationResHelper.COLORS ,
					null, Shader.TileMode.CLAMP);

		}
		else
		{
			linearGradientPotrait = new LinearGradient(0, 0, getWindowManager().getDefaultDisplay().getHeight() - 30, 0, ConversationResHelper.COLORS ,
					null, Shader.TileMode.CLAMP);
			linearGradientLandscape = new LinearGradient(0, 0, getWindowManager().getDefaultDisplay().getWidth() - 30, 0, ConversationResHelper.COLORS ,
					null, Shader.TileMode.CLAMP);
		}



		mShapePotrait = new ShapeDrawable(new RectShape());
		mShapePotrait.getPaint().setShader(linearGradientPotrait);

		mShapeLandscape = new ShapeDrawable(new RectShape());
		mShapeLandscape.getPaint().setShader(linearGradientLandscape);

		mStartSeekbarValue = 1;
		for (int i = 1; i < ConversationResHelper.COLORS.length; i++) {
			if (mBackgroundColor == ConversationResHelper.COLORS[i]) {
				mStartSeekbarValue = i;
				break;
			}
		}
		mSeekBar.setProgress(mStartSeekbarValue);
		
		createColorGradient(getResources().getConfiguration());
		setSeekBarMethods();
	}

	private void createColorGradient(Configuration _config)
	{
		if (_config.orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			mSeekBar.setProgressDrawable( (Drawable)mShapePotrait );
			mSeekBar.invalidate();
		}
		else
		{
			mSeekBar.setProgressDrawable( (Drawable)mShapeLandscape );
			mSeekBar.invalidate();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		createColorGradient(newConfig);
	}

	private void applyBackground(){
		int colorOffset = mSeekBar.getProgress();

		mBackgroundColor = ConversationResHelper.COLORS[colorOffset];
		mListView.setBackgroundColor(mBackgroundColor);
		mListView.invalidate();
		CustomAdapter ad =  (CustomAdapter)mListView.getAdapter();
		ad.notifyDataSetChanged();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.btnSave:
			Editor edit = mPrefs.edit();
			edit.putInt("seekbardef", mStartSeekbarValue);
			edit.putInt(ConversationResHelper.CONV_BG_COLOR, mBackgroundColor);
			edit.commit();

			ConversationResHelper.refresh(CustomizeBackgroundActivity.this);
			finish();
			break;
		case R.id.btnCancel:
			finish();
			break;
		}
	}

	private void setSeekBarMethods(){
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar,
					int progress, boolean fromUser) {
				mStartSeekbarValue = seekBar.getProgress();
				if (fromUser)
				{
					applyBackground();
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				applyBackground();

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}   
		});
	}


	public class CustomAdapter extends ArrayAdapter<Integer> {
		ArrayList<Integer> bubbles;
		Context context = null;
		LayoutInflater inflater;

		Drawable rightBGDrawable = null;
		Drawable leftBGDrawable[] = new Drawable[21];

		private String Bubble; 
		private String Recipient; 
		private String OutBubble;

		class Tag {
			View bubble;
			View bubbleFill;
			TextView text;
			TextView recipient;
		}

		public CustomAdapter(Context context, int resource,
				int textViewResourceId) {
			super(context, resource, textViewResourceId);
			// TODO Auto-generated constructor stub
			bubbles = ConversationResHelper.getAllBubbles();
			this.context = context;
			inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);

			Resources res = context.getResources();
			OutBubble = res.getString(R.string.out_message_bubble);
			Bubble = res.getString(R.string.message_bubble) + " ";
			Recipient = res.getString(R.string.recipient) + " ";
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			Tag tag = null;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.customize_bubble_item, parent, false);

				tag = new Tag();
				tag.bubble = convertView.findViewById(R.id.bubble);
				tag.bubbleFill = convertView.findViewById(R.id.highlightBackground);
				tag.recipient = (TextView)convertView.findViewById(R.id.sender);
				tag.text = (TextView)convertView.findViewById(R.id.text);

				convertView.setTag(tag);
			} else {
				tag = (Tag)convertView.getTag();
			}

			Drawable draw = null;
			int color = 0;

			if (position == 0) {
				color = bubbles.get(0);
				if (rightBGDrawable == null) {
					rightBGDrawable = getResources().getDrawable(R.drawable.chat_bubble_adi_right_gradient);
					rightBGDrawable.setFilterBitmap(true);
					rightBGDrawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
				}
				draw = rightBGDrawable;
			} else {
				draw = leftBGDrawable[position - 1];
				color = bubbles.get(position);

				if (draw == null) {
					draw = getResources().getDrawable(R.drawable.chat_bubble_adi_left_gradient);
					draw.setFilterBitmap(true);
					draw.setColorFilter(color, PorterDuff.Mode.MULTIPLY);

					leftBGDrawable[position - 1] = draw;
				}
			}

			if (!ConversationResHelper.fillBubble()) {
				if (position == 0) {
					tag.bubbleFill.setBackgroundResource(R.drawable.chat_bubble_white_right);
				} else {
					tag.bubbleFill.setBackgroundResource(R.drawable.chat_bubble_white_left);
				}
			} else {
				tag.text.setTextColor(ConversationResHelper.getBubbleTextColor(color));
				tag.recipient.setTextColor(ConversationResHelper.getTimeStampColor(color));
			}

			TextView tv = (TextView)convertView.findViewById(R.id.timestamp);
			tv.setTextColor(ConversationResHelper.getTimeStampColor(mBackgroundColor));

			tag.bubble.setBackgroundDrawable(draw);
			if (position == 0) {
				tag.text.setText(OutBubble);
				tag.recipient.setText("");
			} else {
				tag.text.setText(Bubble + position);
				tag.recipient.setText(Recipient + position);
			}

			return convertView;
		}

		@Override
		public int getCount() {
			return bubbles.size();
		}
	}
}
