package com.verizon.mms.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Telephony.Sms;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.ui.MessageItem.DeliveryStatus;
import com.verizon.mms.ui.MessageItem.ReportStatus;
import com.verizon.mms.util.SqliteWrapper;


public class DeliveryReport extends Dialog {
	private Activity mActivity;
	private MessageItem mMsgItem;
	private Conversation mConv;
	private LinearLayout mListLayout;
	private ViewGroup mInfo;
	private Button mCloseButton;
	private Button mResendButton;
	private LinearLayout mResendToAllLayout;
	private TextView mResendToAllTextView;
	private View mProgress;
	private boolean mFailed;
	private boolean mGlobalFailed;
	private Typeface robotoRegular;
	private Typeface robotoBold;
	private Handler resendHandler;
	private List<DeliveryReportItem> items;
	private static int dividerHeight;
	private static int dividerMargin;
	private static int labelMargin;

	private static final String FONT_ROBOTO_REGULAR = "fonts/roboto/Roboto-Regular.ttf";
	private static final String FONT_ROBOTO_BOLD = "fonts/roboto/Roboto-Bold.ttf";

	private static final int INFO_TEXT_COLOR = 0xff333333;
	private static final float INFO_LABEL_TEXT_SIZE = 17.784f;
	private static final float INFO_LABEL_TEXT_SIZE_LARGE = 21.784f;
	private static final float INFO_VALUE_TEXT_SIZE = 14f;
	private static final float INFO_VALUE_TEXT_SIZE_LARGE = 18f;
	private static final float INFO_LABEL_MARGIN = 4f;
	private static final float DIVIDER_HEIGHT = 1f;
	private static final float DIVIDER_MARGIN = 6.66f;

	private static final String[] SMS_REPORT_STATUS_PROJECTION = new String[] {
		Sms.ADDRESS, //0
		Sms.STATUS,  //1
		Sms.TYPE     //2
	};

	// These indices must sync up with the projections above.
	private static final int COLUMN_RECIPIENT = 0;
	private static final int COLUMN_DELIVERY_STATUS = 1;
	private static final int COLUMN_SMS_MBOX = 2;
	private boolean showStatus = false;

	static {
		final DisplayMetrics metrics = new DisplayMetrics();
		metrics.setToDefaults();
		dividerHeight = Math.round(DIVIDER_HEIGHT * metrics.density);
		if (dividerHeight <= 0) {
			dividerHeight = 1;
		}
		dividerMargin = Math.round(DIVIDER_MARGIN * metrics.density);
		labelMargin = Math.round(INFO_LABEL_MARGIN * metrics.density);
	}


	public DeliveryReport(Activity activity, MessageItem msgItem, Conversation conv, Handler resendHandler) {
		super(activity, android.R.style.Theme_Translucent_NoTitleBar);

		mActivity = activity;
		mMsgItem = msgItem;
		mConv = conv;
		this.resendHandler = resendHandler;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.message_details);
		mResendToAllLayout = (LinearLayout)findViewById(R.id.message_info_resendall_layout);
		mResendButton = (Button)findViewById(R.id.message_resend_button);
		mResendToAllTextView = (TextView)findViewById(R.id.message_resendall_textview);
		mCloseButton = (Button)findViewById(R.id.message_close_button);
		mCloseButton.setOnClickListener(mCloseButtonClick);
		mInfo = (ViewGroup)findViewById(R.id.message_info_container_layout);
		mListLayout = (LinearLayout)findViewById(R.id.delivery_status_container_layout);
		mProgress = findViewById(R.id.progress);
		mProgress.setVisibility(View.VISIBLE);
		setCancelable(true);

		init();
	}

	private void init() {
		// fetch data on a background thread
		new Thread() {
			public void run() {
				loadFonts();

				final MessageItem msgItem = mMsgItem;
				final boolean outgoing = msgItem.isOutboundMessage();
				final boolean sms = msgItem.isSms();

				final Contact fromContact = outgoing ? null : Contact.get(msgItem.mAddress, true, true);

				// get recipients and delivery status
				final boolean failed = mFailed = mMsgItem.isFailedMessage(true);
				if (failed) {
					// get the global message failure status
					mGlobalFailed = mMsgItem.isFailedMmsMessage();
				}
				final View recipients = getRecipients(sms, outgoing);

				// update on main thread
				mActivity.runOnUiThread(new Runnable() {
					public void run() {
					    initFonts();

					    // add the recepients to the layout
						if (recipients != null) {
							mListLayout.addView(recipients);
						}
						else {
							mListLayout.setVisibility(View.GONE);
						}

						final Context context = mActivity;
						final int timeLabel;
						if (outgoing) {
							((TextView)findViewById(R.id.delivery_status_textview)).setText(R.string.delivery_status);
							timeLabel = R.string.sent;

							if (failed) {
								// show resend text and button
								mResendButton.setVisibility(View.VISIBLE);
								mResendButton.setOnClickListener(mResendButtonClick);
								mResendToAllLayout.setVisibility(View.VISIBLE);
								final int msg = sms || items.size() <= 1 ? R.string.resend_to_some : R.string.resend_to_all;
								mResendToAllTextView.setText(msg);
							}
						}
						else {
							// from address
							if (fromContact != null) {
								addInfo(context.getText(R.string.from), fromContact.getName());
							}

							if(showStatus){
								((TextView)findViewById(R.id.delivery_status_textview)).setText(R.string.sent_to);
							}
							
							timeLabel = R.string.received;
						}

						// timestamp
						addInfo(context.getString(timeLabel), msgItem.getTimestamp());

						// message type
						final int type;
						if (sms) {
							type = R.string.text_message;
						}
						else if (msgItem.isDownloaded()) {
							type = R.string.multimedia_message;
						}
						else {
							type = R.string.multimedia_notification;
						}
						addInfo(context.getString(R.string.type), context.getString(type));

						// size
						addInfo(context.getString(R.string.size), MessageUtils.getSizeString(context, msgItem.mMessageSize));

						// Message source 
						addInfo(context.getString(R.string.vma_msg_source),toSourceString(msgItem.getMessageSource()));

						// debug
						addInfo("Debug", "tid " + mConv.getThreadId() + ", msg " + msgItem.mMsgId);
						String[] vma = msgItem.getVMAUidAndMessageId();
						addInfo("Uid", vma[0]);
                        if (Logger.IS_DEBUG_ENABLED) {
                            addInfo("msg-id", vma[1]);
                            addInfo("Date", new Date(msgItem.mMsgTime).toString());
                        }

						mProgress.setVisibility(View.GONE);
					}
				});
			}
		}.start();
	}

	private void addInfo(CharSequence label, CharSequence value) {
		// create info element layout with label and value and add to info layout
		final Context context = mActivity;
		if (mInfo.getChildCount() > 0) {
			// add divider
			final View divider = new View(context);
			divider.setBackgroundResource(R.drawable.message_info_divider);
			final LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, dividerHeight);
			params.bottomMargin = params.topMargin = dividerMargin;
			divider.setLayoutParams(params);
			mInfo.addView(divider);
		}

		 TextView labelView = new TextView(context);
		 if (PreferenceManager.getDefaultSharedPreferences(mActivity)
					.getString(MessagingPreferenceActivity.APP_FONT_SUPPORT, MessagingPreferenceActivity.APP_FONT_SUPPORT_DEFAULT).equals(MessagingPreferenceActivity.APP_FONT_SUPPORT_DEFAULT)) {
				if(android.provider.Settings.System.getFloat(
						mActivity.getContentResolver(),
						android.provider.Settings.System.FONT_SCALE,
						(float) 1.0) > 1.0) {
					labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, INFO_VALUE_TEXT_SIZE_LARGE);
				}else{
					labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, INFO_VALUE_TEXT_SIZE);
				}

			} else {
				if (PreferenceManager
						.getDefaultSharedPreferences(mActivity)
						.getString(MessagingPreferenceActivity.APP_FONT_SUPPORT, MessagingPreferenceActivity.APP_FONT_SUPPORT_NORMAL)
						.equals(MessagingPreferenceActivity.APP_FONT_SUPPORT_LARGE)) {
					labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, INFO_VALUE_TEXT_SIZE_LARGE);
				}else{
					labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, INFO_VALUE_TEXT_SIZE);
				}

			}
		labelView.setTypeface(robotoRegular);
		labelView.setTextColor(INFO_TEXT_COLOR);
		
		labelView.setText(label);
		labelView.setSingleLine(true);
		labelView.setEllipsize(null);
		labelView.setGravity(Gravity.LEFT | Gravity.BOTTOM);
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0);
		params.gravity = Gravity.LEFT | Gravity.BOTTOM;
		params.rightMargin = labelMargin;
		labelView.setLayoutParams(params);

		 TextView valueView = new TextView(context);
		 if (PreferenceManager.getDefaultSharedPreferences(mActivity)
					.getString(MessagingPreferenceActivity.APP_FONT_SUPPORT,MessagingPreferenceActivity.APP_FONT_SUPPORT_DEFAULT ).equals(MessagingPreferenceActivity.APP_FONT_SUPPORT_DEFAULT)) {
				if(android.provider.Settings.System.getFloat(
						mActivity.getContentResolver(),
						android.provider.Settings.System.FONT_SCALE,
						(float) 1.0) > 1.0) {
					valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, INFO_VALUE_TEXT_SIZE_LARGE);
				}else{
					valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, INFO_VALUE_TEXT_SIZE);
				}

			} else {
				if (PreferenceManager
						.getDefaultSharedPreferences(mActivity)
						.getString(MessagingPreferenceActivity.APP_FONT_SUPPORT, MessagingPreferenceActivity.APP_FONT_SUPPORT_NORMAL)
						.equals(MessagingPreferenceActivity.APP_FONT_SUPPORT_LARGE)) {
					valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, INFO_VALUE_TEXT_SIZE_LARGE);
				}else{
					valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, INFO_VALUE_TEXT_SIZE);
				}

			}
		valueView.setTypeface(robotoRegular);
		valueView.setTextColor(INFO_TEXT_COLOR);
		valueView.setText(value);
		valueView.setSingleLine(true);
		valueView.setEllipsize(TruncateAt.END);
		valueView.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
		params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1);
		params.gravity = Gravity.RIGHT | Gravity.BOTTOM;
		valueView.setLayoutParams(params);

		final LinearLayout info = new LinearLayout(context);
		info.addView(labelView);
		info.addView(valueView);
		params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		info.setLayoutParams(params);

		mInfo.addView(info);
	}

	private void loadFonts() {
		try {
			final AssetManager mgr = mActivity.getAssets();
			robotoRegular = Typeface.createFromAsset(mgr, FONT_ROBOTO_REGULAR);
			robotoBold = Typeface.createFromAsset(mgr, FONT_ROBOTO_BOLD);
		}
		catch (Exception e) {
			if (Logger.IS_ERROR_ENABLED) {
				Logger.error(e);
			}
		}
	}

	private void initFonts() {
		final Typeface robotoRegular = this.robotoRegular;
		final Typeface robotoBold = this.robotoBold;
		if (robotoRegular != null && robotoBold != null) {
			mResendButton.setTypeface(robotoBold);
			mCloseButton.setTypeface(robotoBold);
			((TextView)findViewById(R.id.message_info_text)).setTypeface(robotoBold);
			((TextView)findViewById(R.id.delivery_status_textview)).setTypeface(robotoBold);
			((TextView)findViewById(R.id.message_resendall_textview)).setTypeface(robotoRegular);
		}
	}

	private View.OnClickListener mCloseButtonClick = new View.OnClickListener() {
		public void onClick(View v) {
			dismiss();
		}
	};

	private View.OnClickListener mResendButtonClick = new View.OnClickListener() {
		public void onClick(View v) {
			final StringBuilder sb = new StringBuilder();
			final int num = items.size();
			for (int i = 0; i < num; ++i) {
				final DeliveryReportItem item = items.get(i);
				final Contact contact = item.recipient;
				if (contact != null) {
					if (i > 0) {
						sb.append(';');
					}
					sb.append(contact.getNumber());
				}
			}
			if (resendHandler != null) {
				resendHandler.sendMessage(Message.obtain(resendHandler, 0, sb.toString()));
			}
			mProgress.setVisibility(View.VISIBLE);  // it can take a while to start the compose activity
			dismiss();
		}
	};

	private LinearLayout getRecipients(boolean sms, boolean outgoing) {
		LinearLayout layout = null;

		final List<DeliveryReportItem> items;
		if (sms) {
			if (outgoing) {
				items = getSmsReportItems();
			}
			else {
				items = new ArrayList<DeliveryReportItem>(1);
				final Contact contact = Contact.get(MessageUtils.getLocalNumber(), true, true);
				if (contact != null) {
					items.add(new DeliveryReportItem(contact, null, false));
				}
			}
		}
		else {
			items = getMmsReportItems(outgoing);
		}

		if (items != null) {
			this.items = items;
			final LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			layout = new LinearLayout(mActivity);
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.setLayoutParams(params);
			final int size = items.size();
			try {
				final LayoutInflater inflater = mActivity.getLayoutInflater();
				for (int i = 0; i < size; i++) {
					final DeliveryReportItem item = items.get(i);
					final Contact contact = item.recipient;
					if (contact != null) {
					final View view = inflater.inflate(R.layout.delivery_status_list, null);
						view.setLayoutParams(params);
						final DeliveryReportListItem listItem = new DeliveryReportListItem(view, robotoRegular);
						if (!showStatus && TextUtils.isEmpty(contact.getNumber())) {
							showStatus = false;
						}
						else {
							showStatus = true;
						}
						view.setTag(listItem);
						listItem.bind(listItem, contact, outgoing ? item.status : "", item.failed, mActivity);
						if (i == size - 1) {
							view.findViewById(R.id.delivery_status_row_divider).setVisibility(View.GONE);
						}
						layout.addView(view);
					}
				}
			}
			catch (Exception e) {
				Logger.error(e);
			}

			if (layout.getChildCount() == 0) {
				layout = null;
			}
		}
		
		Logger.debug("deliveryitem info ", items);

		return layout;
	}

	private List<DeliveryReportItem> getSmsReportItems() {
		final String selection = "_id = ?";
		final String[] args = { Long.toString(mMsgItem.mMsgId) };
		Cursor c = SqliteWrapper.query(mActivity.getApplicationContext(), mActivity.getContentResolver(), VZUris.getSmsUri(),
				SMS_REPORT_STATUS_PROJECTION, selection, args, null);

		if (c == null) {
			return null;
		}
		try {
			if (c.getCount() <= 0) {
				return null;
			}

			final List<DeliveryReportItem> items = new ArrayList<DeliveryReportItem>();
			final Context context = mActivity;

			while (c.moveToNext()) {
				final DeliveryStatus stat;
				if (mFailed) {
					stat = DeliveryStatus.FAILED;
				}
				else {
					stat = MessageItem.getSmsStatus(c.getInt(COLUMN_SMS_MBOX), c.getInt(COLUMN_DELIVERY_STATUS));
				}
				final String status = context.getString(stat.getStringId());
				final Contact contact = Contact.get(c.getString(COLUMN_RECIPIENT), true, true);
				items.add(new DeliveryReportItem(contact, status, stat == DeliveryStatus.FAILED));
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "getSmsReportItems: " + c.getString(COLUMN_RECIPIENT) + ": mbox = " + c.getInt(COLUMN_SMS_MBOX)
							+ ", dstat = " + c.getInt(COLUMN_DELIVERY_STATUS) + ", stat = " + stat);
				}
			}
			return items;
		}
		finally {
			c.close();
		}
	}

	private DeliveryStatus getMmsReportStatus(ReportStatus rstat) {
		DeliveryStatus stat = null;

		// if there was a global error then all recipients are considered failed
		if (mGlobalFailed) {
			stat = DeliveryStatus.FAILED;
		}
		else if (rstat != null) {
			// try to get status from the report
			if (rstat.read) {
				stat = DeliveryStatus.READ;
			}
			else if (rstat.delivered) {
				stat = DeliveryStatus.DELIVERED;
			}
			else if (rstat.failed) {
				stat = DeliveryStatus.FAILED;
			}
		}

		if (stat == null) {
			// get status based on the message box
			stat = MessageItem.getStatusFromBox(false, mMsgItem.mBoxId, false);
		}

		return stat;
	}

	private List<DeliveryReportItem> getMmsReportItems(boolean outgoing) {
		List<DeliveryReportItem> items = null;

		if (outgoing) {
			final HashMap<String, ReportStatus> rstat = mMsgItem.getMmsReportStatus();
			items = new ArrayList<DeliveryReportItem>(rstat.size());
			for (String addr : rstat.keySet()) {
				final DeliveryStatus stat = getMmsReportStatus(rstat.get(addr));
				final String statusText = mMsgItem.getStatusText(mActivity, stat);
				final Contact contact = Contact.get(addr, true, true);
				items.add(new DeliveryReportItem(contact, statusText, stat == DeliveryStatus.FAILED));
			}
		}
		else {
			final List<String> addrs = mMsgItem.getMmsRecipients();
			if (addrs.size() != 0) {
				items = new ArrayList<DeliveryReportItem>(addrs.size());
				final String statusText = mActivity.getString(DeliveryStatus.NONE.getStringId());
				for (String addr : addrs) {
					final Contact contact = Contact.get(addr, true, true);
					items.add(new DeliveryReportItem(contact, statusText, false));
				}
			}
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "getMmsReportItems: items = " + items);
		}

		return items;
	}
	
	 private String toSourceString(int messageSourceid) {
	        String messageSource = ""; 
	        if (messageSourceid== -1) {
	            return messageSource;
	        }else if (messageSourceid == 0) {
	            //messageSource = "Web";
	            messageSource=mActivity.getString(R.string.web);
	        } else if (messageSourceid == 1) {
	           // messageSource = "Connected Device";
	            messageSource=mActivity.getString(R.string.connected_device);
	        } else if (messageSourceid == 2) {
	           // messageSource = "Phone";
	            messageSource=mActivity.getString(R.string.phone);
	        }
	        return messageSource;
	    }
}
