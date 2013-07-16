package com.verizon.mms.ui;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ui.widget.MultilineCheckedTextView;


public class GroupModeChooser extends RelativeLayout {
	private View showDialog;
	private TextView senderBtn;
	private TextView groupBtn;
	private ImageView groupButtons;
	private View dialog;
	private MultilineCheckedTextView dialogGroupBtn;
	private MultilineCheckedTextView dialogSenderBtn;
	private Button closeDialog;
	private boolean groupMms;
	private View parent;
	private OnGroupModeChangedListener listener;
	private Context mContext = null;
	private TextView recipientsReply;

	public interface OnGroupModeChangedListener {
		public void groupModeChanged(boolean groupMms);
	}

	public GroupModeChooser(Context context) {
		super(context);
		init(context);
	}

	public GroupModeChooser(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public GroupModeChooser(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {

		// Initialize context
		mContext = context;
		// add content to layout
		parent = LayoutInflater.from(context).inflate(R.layout.group_chooser, this);

		showDialog = findViewById(R.id.group_show_dialog);
		showDialog.setOnClickListener(clickListener);
		groupButtons = (ImageView)findViewById(R.id.group_buttons);
		senderBtn = (TextView)findViewById(R.id.group_sender_btn);
		senderBtn.setOnClickListener(clickListener);
		groupBtn = (TextView)findViewById(R.id.group_group_btn);
		groupBtn.setOnClickListener(clickListener);
		dialog = findViewById(R.id.group_dialog);
		dialogGroupBtn = (MultilineCheckedTextView)findViewById(R.id.group_dialog_group);
		((View)dialogGroupBtn).setOnClickListener(clickListener);
		dialogSenderBtn = (MultilineCheckedTextView)findViewById(R.id.group_dialog_sender);
		((View)dialogSenderBtn).setOnClickListener(clickListener);
		closeDialog = (Button)findViewById(R.id.group_dialog_close);
		closeDialog.setOnClickListener(clickListener);
		recipientsReply = (TextView)findViewById(R.id.recipients_replyTo);

		final GestureDetector gestureDetector = new GestureDetector(new XButtonFlipDetector());
		final OnTouchListener gestureListener = new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};
		findViewById(R.id.group_btn_layout).setOnTouchListener(gestureListener);
		groupBtn.setOnTouchListener(gestureListener);
		senderBtn.setOnTouchListener(gestureListener);
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
							setGroupMode(false, false, true);
							return true;
						}
						else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE) {
							setGroupMode(true, false, true);
							return true;
						}
					}
				}
			}
			catch (Exception e) {
				Logger.error(getClass(),e);
			}
			return false;
		}
	}

	private void setGroupMode(boolean groupMms, boolean force, boolean callListener) {
		if (force || this.groupMms != groupMms) {
			this.groupMms = groupMms;
			if(groupMms){
				groupBtn.setTextColor(Color.WHITE);
				senderBtn.setTextColor(Color.GRAY);
			}
			else
			{
				groupBtn.setTextColor(Color.GRAY);
				senderBtn.setTextColor(Color.WHITE);
			}
			groupButtons.setImageResource(groupMms ? R.drawable.group_buttons_group : R.drawable.group_buttons_sender);
			dialogSenderBtn.setChecked(!groupMms);
			dialogGroupBtn.setChecked(groupMms);
			if (callListener && listener != null) {
				listener.groupModeChanged(groupMms);
			}
		}
	}

	private View.OnClickListener clickListener = new View.OnClickListener() {
		public void onClick(View v) {
			// dialog buttons act like radio buttons
			if (v == dialogGroupBtn) {
				if (!dialogGroupBtn.isChecked()) {
					setGroupMode(true, false, true);
				}
			}

			else if (v == dialogSenderBtn) {
				if (!dialogSenderBtn.isChecked()) {
					setGroupMode(false, false, true);
				}
			}

			else if (v == closeDialog) {
				dialog.setVisibility(GONE);
				LinearLayout.LayoutParams lp1 = null;
				RelativeLayout.LayoutParams lp2 = null;
				try {
					 lp1 = (LinearLayout.LayoutParams) parent.getLayoutParams();
				} catch (Exception e) {
					 lp2 = (RelativeLayout.LayoutParams) parent.getLayoutParams();
				}
				if (lp1 != null) {
					lp1.height = LayoutParams.WRAP_CONTENT;
					parent.setLayoutParams(lp1);
				}
				else {
					lp2.height = LayoutParams.WRAP_CONTENT;
					parent.setLayoutParams(lp2);
				}
			}

			else if (v == showDialog) {
				dialog.setVisibility(dialog.getVisibility() == GONE ? VISIBLE : GONE);
				InputMethodManager imm = (InputMethodManager) mContext
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(getApplicationWindowToken(), 0);
				v.setFocusableInTouchMode(true);
				if (dialog.getVisibility() == View.VISIBLE) {
					LinearLayout.LayoutParams lp1 = null;
					RelativeLayout.LayoutParams lp2 = null;
					try {
						 lp1 = (LinearLayout.LayoutParams) parent.getLayoutParams();
					} catch (Exception e) {
						 lp2 = (RelativeLayout.LayoutParams) parent.getLayoutParams();
					}
					if (lp1 != null) {
						lp1.height = LayoutParams.FILL_PARENT;
						parent.setLayoutParams(lp1);
					}
					else {
						lp2.height = LayoutParams.FILL_PARENT;
						parent.setLayoutParams(lp2);
					}
				}
				else {
					LinearLayout.LayoutParams lp1 = null;
					RelativeLayout.LayoutParams lp2 = null;
					try {
						 lp1 = (LinearLayout.LayoutParams) parent.getLayoutParams();
					} catch (Exception e) {
						 lp2 = (RelativeLayout.LayoutParams) parent.getLayoutParams();
					}
					if (lp1 != null) {
						lp1.height = LayoutParams.WRAP_CONTENT;
						parent.setLayoutParams(lp1);
					}
					else {
						lp2.height = LayoutParams.WRAP_CONTENT;
						parent.setLayoutParams(lp2);
					}
				}
			}

			else {
				// one of the header radio buttons
				final boolean groupMms = v == groupBtn;
				setGroupMode(groupMms, false, true);
			}
		}
	};

	public void setGroupMode(boolean groupMms) {
		setGroupMode(groupMms, true, false);
	}

	public void setListener(OnGroupModeChangedListener listener) {
		this.listener = listener;
	}

	public boolean getGroupMms() {
		return groupMms;
	}
	
	/*
	 * Function called to reflect change in the resources
	 */
	public void refresh() {
		groupBtn.setText(R.string.group);
		senderBtn.setText(R.string.just_me);
		recipientsReply.setText(R.string.recipients_reply);
		dialogGroupBtn.setText(R.string.group_prompt_group_desc);
		dialogSenderBtn.setText(R.string.group_prompt_sender_desc);
		TextView disclaimer = (TextView)findViewById(R.id.disclaimer);
		disclaimer.setText(R.string.group_prompt_disclaimer);
		closeDialog.setText(R.string.close);
	}
	
	//This is added to accommodate Just Me string which is an large string in French language.
	public void setMassTextPadding(){
		senderBtn.setPadding(1, 0, 0, 0);
		senderBtn.setGravity(Gravity.CENTER_VERTICAL);
	}
	
	public void changeMassTextPadding(){
		senderBtn.setPadding(7, 0, 0, 0);
		senderBtn.setGravity(Gravity.CENTER_VERTICAL);
		recipientsReply.setGravity(Gravity.CENTER_VERTICAL);
	}
}
