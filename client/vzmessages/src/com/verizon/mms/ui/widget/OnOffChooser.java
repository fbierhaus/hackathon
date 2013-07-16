package com.verizon.mms.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.verizon.messaging.vzmsgs.R;


public class OnOffChooser extends RelativeLayout {
	private TextView offBtn;
	private TextView onBtn;
	private ImageView toggleButtons;
	private boolean isOn;
	private View parent;
	private OnStateChangedListener listener;

	public interface OnStateChangedListener {
		public void stateChanged(boolean isOn);
	}

	public OnOffChooser(Context context) {
		super(context);
		init(context);
	}

	public OnOffChooser(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public OnOffChooser(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {

		// add content to layout
		parent = LayoutInflater.from(context).inflate(R.layout.fill_bubble_chooser, this);

		toggleButtons = (ImageView)findViewById(R.id.group_buttons);
		offBtn = (TextView)findViewById(R.id.group_sender_btn);
		offBtn.setOnClickListener(clickListener);
		onBtn = (TextView)findViewById(R.id.group_group_btn);
		onBtn.setOnClickListener(clickListener);

		final GestureDetector gestureDetector = new GestureDetector(new XButtonFlipDetector());
		final OnTouchListener gestureListener = new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};
		findViewById(R.id.group_btn_layout).setOnTouchListener(gestureListener);
		onBtn.setOnTouchListener(gestureListener);
		offBtn.setOnTouchListener(gestureListener);
	}

	class XButtonFlipDetector extends SimpleOnGestureListener {
		private static final int SWIPE_MIN_DISTANCE = 40;
		private static final int SWIPE_MAX_OFF_PATH = 100;
		private static final int SWIPE_THRESHOLD_VELOCITY = 25;

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			try {
				if (Math.abs(e1.getY() - e2.getY()) <= SWIPE_MAX_OFF_PATH) {
					if (Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
						if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE) {
							setToggleState(false, false, true);
							return true;
						}
						else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE) {
							setToggleState(true, false, true);
							return true;
						}
					}
				}
			}
			catch (Exception e) {
				//Logger.error(getClass(),e);
			}
			return false;
		}
	}

	private void setToggleState(boolean isOn, boolean force, boolean callListener) {
		if (force || this.isOn != isOn) {
			this.isOn = isOn;
			toggleButtons.setImageResource(isOn ? R.drawable.group_buttons_group : R.drawable.group_buttons_sender);
			if (callListener && listener != null) {
				listener.stateChanged(isOn);
			}
		}
	}

	public void setGroupMode(boolean groupMms) {
		setToggleState(groupMms, true, false);
	}

	public void setListener(OnStateChangedListener listener) {
		this.listener = listener;
	}

	public boolean getToggleState() {
		return isOn;
	}
	
	/*
	 * Function called to reflect change in the resources
	 */
	public void refresh() {
		onBtn.setText("ON");
		offBtn.setText("OFF");
	}
	
	private OnClickListener clickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			final boolean isOn = v == onBtn;
			setToggleState(isOn, true, true);
		}
	};
}
