package com.verizon.mms.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.LinearGradient;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.verizon.messaging.vzmsgs.R;

public final class DragNDropAdapter extends BaseAdapter implements RemoveListener, DropListener{

	private LayoutInflater mInflater;
	private ArrayList<Item> mContent;

	private String mRecipient;
	private String mOutBubble;
	private final Resources mRes;
	private boolean mFillBubbleColor = false;
	private int mDragPos = -1;
	private ListView mListView;
	private int mSeekBarHeight;
	private int mDragIconId;
	private int mDividerColor;
	
	class Item {
		public Item(int color) {
			this.color = color;
			seekBarVisible = false;
			seekBarValue = -1;
			
			for (int i = 0; i < ConversationResHelper.COLORS.length; i++) {
				if (color == ConversationResHelper.COLORS[i]) {
					seekBarValue = i;
					break;
				}
			}
			
			if (seekBarValue == -1) {
                //default color see if we can find approx value
				for (int i = 0; i < ConversationResHelper.COLORS.length; i++) {
					if (i > 0 && color > ConversationResHelper.COLORS[i-1]) {
						if (color < ConversationResHelper.COLORS[i]) {
							seekBarValue = i;
							break;
						}
					}
				}
			}
			
			if (seekBarValue < 1) {
				seekBarValue = 1;
			}
		}
		
		int color;
		boolean seekBarVisible;
		int seekBarValue;
	}
	
	class Tag {
		View bubble;
		View bubbleFill;
		View horizontalDivider;
		View verticaDivider;
		TextView text;
		TextView recipient;
		View colorChoser;
		SeekBar seekbar;
		int mPosition;
		Item item;
	}

	private Context mContext;
	
	public DragNDropAdapter(Context context, ArrayList<Integer> content, boolean fillBubble, ListView listView) {
		mContext = context;
		mRes = context.getResources();
		mInflater = LayoutInflater.from(context);
		mContent = new ArrayList<Item>(content.size());
		
		for (int color : content) {
			mContent.add(new Item(color));
		}
		
		mRecipient = mRes.getString(R.string.recipient) + " ";
		mOutBubble = mRes.getString(R.string.out_message_bubble);
		mFillBubbleColor = fillBubble;
		
		mListView = listView;
		mSeekBarHeight = mRes.getDimensionPixelSize(R.dimen.colorseekbarLayoutHeight);
		
		if (ConversationResHelper.isBrightColor(ConversationResHelper.getBGColor())) {
			mDragIconId = R.drawable.ico_reorder_darkgray;
			mDividerColor = mRes.getColor(R.color.divider_dark);
		} else {
			mDragIconId = R.drawable.ico_reorder_white;
			mDividerColor = mRes.getColor(R.color.divider_light);
		}
	}

	private void createColorGradient(Configuration _config, SeekBar seekBar, boolean requestLayout)
	{
		LinearGradient gradient;
        if (mRes.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
        {
        	gradient = new LinearGradient(0, 0, ((Activity)mContext).getWindowManager().getDefaultDisplay().getWidth() - 30, 0, ConversationResHelper.COLORS ,
    	        null, Shader.TileMode.CLAMP);
        } else
        {
        	gradient = new LinearGradient(0, 0, ((Activity)mContext).getWindowManager().getDefaultDisplay().getWidth() - 30, 0, ConversationResHelper.COLORS ,
            	        null, Shader.TileMode.CLAMP);
        }
		
		ShapeDrawable shape = new ShapeDrawable(new RectShape());
    	shape.getPaint().setShader(gradient);
    	
    	seekBar.setProgressDrawable( (Drawable)shape);
    	
    	if (requestLayout) {
    		seekBar.requestLayout();
    	}
    }
	
	private void setSeekBarMethods(SeekBar seekBar){
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar,
            		int progress, boolean fromUser) {
            	Tag tag = (Tag)seekBar.getTag();
            	Item item = tag.item;

            	int oldSeekBarValue = item.seekBarValue;
            	item.seekBarValue = seekBar.getProgress();
            	if (fromUser)
            	{
            		if (oldSeekBarValue != item.seekBarValue) {
            			applyBubbleColor(tag);
            		}
            	}
            }
            
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				applyBubbleColor((Tag)seekBar.getTag());
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				
			}   
        });
	}
	
	private void applyBubbleColor(Tag tag){
    	Item item = tag.item;
    	item.color = ConversationResHelper.COLORS[item.seekBarValue];
    	
    	setBubbleColor(tag);
    }
	/**
	 * The number of items in the list
	 * @see android.widget.ListAdapter#getCount()
	 */
	public int getCount() {
		return mContent.size();
	}

	/**
	 * Since the data comes from an array, just returning the index is
	 * sufficient to get at the data. If we were using a more complex data
	 * structure, we would return whatever object represents one row in the
	 * list.
	 *
	 * @see android.widget.ListAdapter#getItem(int)
	 */
	public Integer getItem(int position) {
		return position;
	}

	/**
	 * Use the array index as a unique id.
	 * @see android.widget.ListAdapter#getItemId(int)
	 */
	public long getItemId(int position) {
		return position;
	}

	/**
	 * Make a view to hold each row.
	 *
	 * @see android.widget.ListAdapter#getView(int, android.view.View,
	 *      android.view.ViewGroup)
	 */
	public View getView(int position, View convertView, ViewGroup parent) {
		// A ViewHolder keeps references to children views to avoid unneccessary calls
		// to findViewById() on each row.
		Tag tag;

		// When convertView is not null, we can reuse it directly, there is no need
		// to reinflate it. We only inflate a new View when the convertView supplied
		// by ListView is null.
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.customize_bubble_color_item, null);

			tag = new Tag();
			tag.bubble = convertView.findViewById(R.id.bubble);
			tag.bubbleFill = convertView.findViewById(R.id.highlightBackground);
			tag.recipient = (TextView)convertView.findViewById(R.id.sender);
			tag.text = (TextView)convertView.findViewById(R.id.text);
			tag.colorChoser = convertView.findViewById(R.id.colorchanger);
			tag.horizontalDivider = convertView.findViewById(R.id.horizontal_divider);
			tag.horizontalDivider.setBackgroundColor(mDividerColor);
			tag.verticaDivider = convertView.findViewById(R.id.vertical_divider);
			tag.verticaDivider.setBackgroundColor(mDividerColor);
			tag.seekbar = (SeekBar)convertView.findViewById(R.id.seekbar);
			
			tag.seekbar.setTag(tag);
			tag.bubble.setTag(tag);
			Item item = mContent.get(position);
			tag.mPosition = position;
			tag.item = item;
			
			ImageView iv = (ImageView)convertView.findViewById(R.id.dragView);
			iv.setImageResource(mDragIconId);
			convertView.findViewById(R.id.dragView).setVisibility(View.VISIBLE);
			
			createColorGradient(mRes.getConfiguration(), tag.seekbar, true);
			tag.seekbar.setMax(ConversationResHelper.COLORS.length-1);
			setSeekBarMethods(tag.seekbar);
			convertView.setTag(tag);
			
			tag.bubble.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					Tag tag = (Tag)arg0.getTag();
					int visibility = tag.colorChoser.getVisibility();
					
					if (visibility == View.VISIBLE) {
						tag.colorChoser.setVisibility(View.GONE);
						tag.item.seekBarVisible = false;
					} else {
						//only one seekbar will be visible at a time
						for (int i = 0; i < mContent.size(); i++) {
							if (i != tag.mPosition) {
								mContent.get(i).seekBarVisible = false;
							}
						}
						tag.colorChoser.setVisibility(View.VISIBLE);
						tag.seekbar.setProgress(tag.item.seekBarValue);
						tag.item.seekBarVisible = true;
						tag.seekbar.invalidate();
						
						if (tag.mPosition == mListView.getLastVisiblePosition()) {
							mListView.smoothScrollToPosition(tag.mPosition);
						}
						notifyDataSetChanged();
					}
				}
			});
		} else {
			tag = (Tag)convertView.getTag();
		}
		
		Item item = mContent.get(position);
		tag.mPosition = position;
		tag.item = item;

		if (mDragPos == position) {
			convertView.setVisibility(View.INVISIBLE);
		} else {
			convertView.setVisibility(View.VISIBLE);

			if (item.seekBarVisible) {
				if (tag.colorChoser.getVisibility() == View.GONE) {
					tag.seekbar.setProgress(tag.item.seekBarValue);
					tag.seekbar.requestLayout();
				}
				tag.colorChoser.setVisibility(View.VISIBLE);
			} else {
				tag.colorChoser.setVisibility(View.GONE);
			}
			
			setBubbleColor(tag);
			
			if (position == 0) {
				tag.text.setText(mOutBubble);
				tag.horizontalDivider.setVisibility(View.INVISIBLE);
			} else {
				tag.horizontalDivider.setVisibility(View.VISIBLE);
				tag.text.setText(mRecipient + position);
			}
		}
		return convertView;
	}

	private void setBubbleColor(Tag tag) {
		Drawable draw = null;
		int position = tag.mPosition;
		int color = mContent.get(position).color;
		
		if (position == 0) {
			draw = mRes.getDrawable(R.drawable.chat_bubble_adi_right_gradient);
		} else {
			draw = mRes.getDrawable(R.drawable.chat_bubble_adi_left_gradient);
		}
		draw.setFilterBitmap(true);
		draw.setColorFilter(color, PorterDuff.Mode.MULTIPLY);

		if (!mFillBubbleColor) {
			if (position == 0) {
				tag.bubbleFill.setBackgroundResource(R.drawable.chat_bubble_white_right);
			} else {
				tag.bubbleFill.setBackgroundResource(R.drawable.chat_bubble_white_left);
			}
			tag.text.setTextColor(mRes.getColor(R.color.black));
			tag.recipient.setTextColor(mRes.getColor(R.color.light_grey));
		} else {
			tag.text.setTextColor(ConversationResHelper.getBubbleTextColor(color));
			tag.recipient.setTextColor(ConversationResHelper.getTimeStampColor(color));
			tag.bubbleFill.setBackgroundResource(0);
		}

		tag.bubble.setBackgroundDrawable(draw);
	}

	public ArrayList<Integer> getEditedColors() {
		ArrayList<Integer> colors = new ArrayList<Integer>(mContent.size());
		
		synchronized (mContent) {
			for (Item item : mContent) {
				colors.add(item.color);
			}
		}
		return colors;
	}
	
	public void fillBubbleColor(boolean fill) {
		if (mFillBubbleColor != fill) {
			mFillBubbleColor = fill;
			notifyDataSetChanged();
		}
	}

	public void onRemove(int which) {
		if (which < 0 || which > mContent.size()) return;	
		
		synchronized (mContent) {
			mContent.remove(which);
		}
	}

	public void onDrop(int from, int to) {
		
		synchronized (mContent) {
			Item temp = mContent.get(from);
			mContent.remove(from);
			mContent.add(to,temp);
		}		
		mDragPos = -1;
		
		notifyDataSetChanged();
	}

	public void onStartDrag(View itemView, int pos) {
		Tag tag = (Tag)itemView.getTag();
		if (tag != null) {
			tag.colorChoser.setVisibility(View.GONE);
			tag.item.seekBarVisible = false;
		}
		
		mDragPos = pos;
		notifyDataSetChanged();
	}

	public void onStopDrag() {
		mDragPos = -1;
		notifyDataSetChanged();
	}
}