package com.verizon.mms.ui;

import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.model.VCardModel;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct.ContactMethod;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct.PhoneData;

public class VcardReport extends Dialog implements OnClickListener{
	private Activity mActivity;
	private LinearLayout mListLayout;
	private Button mCloseButton;
	private ImageView avatar;
	private TextView vcardNameTextView;
	private Typeface robotoRegular;
	private Typeface robotoBold;
	private VCardModel vcm;
	private LinearLayout rowLayout;
	private LayoutParams params;
	private LinearLayout layout = null;

	private static final String FONT_ROBOTO_REGULAR = "fonts/roboto/Roboto-Regular.ttf";
	private static final String FONT_ROBOTO_BOLD = "fonts/roboto/Roboto-Bold.ttf";

	public VcardReport(Context activity, VCardModel vcmodel) {
		super(activity, android.R.style.Theme_Translucent_NoTitleBar);

		mActivity = (Activity) activity;
		vcm = vcmodel;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.vcard_details);
		avatar = (ImageView) findViewById(R.id.tablet_contact_icon);
		vcardNameTextView = (TextView) findViewById(R.id.contact_name);
		mCloseButton = (Button) findViewById(R.id.vcard_close_button);
		mCloseButton.setOnClickListener(mCloseButtonClick);
		mListLayout = (LinearLayout) findViewById(R.id.vcard_details_layout);

		init();
	}

	private void init() {
		loadFonts();
		initFonts();
		if (vcm.getContactPicture() != null) {
			avatar.setImageBitmap(vcm.getContactPicture());
			avatar.setVisibility(View.VISIBLE);
		}
		vcardNameTextView.setText(vcm.getContactStruct().getName());
		
		params = new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		layout = new LinearLayout(mActivity);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setLayoutParams(params);
		
		List<ContactMethod> emailData = vcm.getContactStruct().getContactMethodsList();		
		List<PhoneData> phoneData = vcm.getContactStruct().getPhoneList();
		if(phoneData != null && emailData != null)
		{
			rowLayout = getPhoneList(phoneData);
			rowLayout = getEmailList(emailData);
			mListLayout.addView(rowLayout);
		} else if(phoneData == null && emailData != null) {
			rowLayout = getEmailList(emailData);
			mListLayout.addView(rowLayout);
		} else if(phoneData != null && emailData == null) {
			rowLayout = getPhoneList(phoneData);
			mListLayout.addView(rowLayout);
		}
	}

	private LinearLayout getEmailList(List<ContactMethod> emailData) {

		int size = emailData.size();
		try {
			LayoutInflater inflater = mActivity.getLayoutInflater();
			for (int i = 0; i < size; i++) {
				ContactMethod item = emailData.get(i);
				if (item != null) {
					View view = inflater.inflate(R.layout.contact_list,
							null);
					view.setLayoutParams(params);
					((TextView) view
							.findViewById(R.id.contact_type_recipient_number_textview))
							.setText("Email");
					((TextView) view.findViewById(R.id.contact_number_textview))
							.setText(item.data);
					if (i == size - 1) {
						view.findViewById(R.id.row_divider).setVisibility(
								View.GONE);
					}
					layout.addView(view);
					view.setOnClickListener(this);
				}
			}
		} catch (Exception e) {
			Logger.error(e);
		}
		if (layout.getChildCount() == 0) {
			layout = null;
		}
		return layout;
	}

	private void loadFonts() {
		try {
			final AssetManager mgr = mActivity.getAssets();
			robotoRegular = Typeface.createFromAsset(mgr, FONT_ROBOTO_REGULAR);
			robotoBold = Typeface.createFromAsset(mgr, FONT_ROBOTO_BOLD);
		} catch (Exception e) {
			if (Logger.IS_ERROR_ENABLED) {
				Logger.error(e);
			}
		}
	}

	private void initFonts() {
		final Typeface robotoRegular = this.robotoRegular;
		final Typeface robotoBold = this.robotoBold;
		if (robotoRegular != null && robotoBold != null) {
			mCloseButton.setTypeface(robotoBold);
			((TextView) findViewById(R.id.contact_name))
					.setTypeface(robotoBold);
			((TextView) findViewById(R.id.vcard_info_textview))
					.setTypeface(robotoBold);

		}
	}

	private View.OnClickListener mCloseButtonClick = new View.OnClickListener() {
		public void onClick(View v) {
			dismiss();
		}
	};

	private LinearLayout getPhoneList(List<PhoneData> mDatas) {

		int size = mDatas.size();
		try {
			LayoutInflater inflater = mActivity.getLayoutInflater();
			for (int i = 0; i < size; i++) {
				PhoneData item = mDatas.get(i);
				if (item != null) {
					View view = inflater.inflate(R.layout.contact_list,
							null);
					view.setLayoutParams(params);
					((TextView) view
							.findViewById(R.id.contact_type_recipient_number_textview))
							.setText(getPhoneType(item.type));
					((TextView) view.findViewById(R.id.contact_number_textview))
							.setText(item.data);
					if (i == size - 1) {
						view.findViewById(R.id.row_divider).setVisibility(
								View.GONE);
					}
					layout.addView(view);
					view.setOnClickListener(this);
				}
			}
		} catch (Exception e) {
			Logger.error(e);
		}
		if (layout.getChildCount() == 0) {
			layout = null;
		}
		return layout;
	}

	private String getPhoneType(int type) {
		String cType = null;

		switch (type) {
		case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
			cType = mActivity.getString(R.string.type_home);
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
			cType = mActivity.getString(R.string.type_mobile);
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
			cType = mActivity.getString(R.string.type_work);
			break;
		default:
			cType = mActivity.getString(R.string.type_others);
			break;
		}

		return cType;
	}

    @Override
    public void onClick(View v) {
    	if (v instanceof LinearLayout) {

    		String type = ((TextView) v.findViewById(R.id.contact_type_recipient_number_textview)).getText().toString();
    		
    		if(type != null && type.equalsIgnoreCase("Email")) {
    			String email = ((TextView) v.findViewById(R.id.contact_number_textview)).getText().toString();
    			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND); 
    			emailIntent.setType("plain/text"); 
    			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{email}); 
    			emailIntent.putExtra(android.content.Intent.EXTRA_CC, ""); 
    			emailIntent.putExtra(android.content.Intent.EXTRA_BCC, ""); 
    			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, ""); 
    			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, ""); 
    			getContext().startActivity(Intent.createChooser(emailIntent, "Complete action using..."));
    		} else {
    			if(!MmsConfig.isTabletDevice()) {
    				String phString = ((TextView) v.findViewById(R.id.contact_number_textview)).getText().toString();    		
    				if (phString != null && phString.length() > 0) {
    					String Numb = "tel:" + phString;
    					Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(Numb));
    					getContext().startActivity(intent);
    				}
    			}
    		}
    		dismiss();
    	}
    }
}