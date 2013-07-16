package com.verizon.mms.ui.widget;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.data.RecipContact;
import com.verizon.mms.ui.ComposeMessageFragment;
import com.verizon.mms.ui.MessageUtils;
import com.verizon.mms.ui.RecipientListActivity;
import com.verizon.mms.util.Util;

/*
 * Scroll view to handle adding and removing the contacts present in 
 * the AddressBook
 */

//TODO: use better datastructure to store the contacts instead of 
//geting it from the layout and get all the related code here itself
public class RecipientEditor extends HorizontalScrollView {
    private final int               ACTION_MENU_REMOVE_RECIP        = 1;
    private final int               ACTION_MENU_CALL_RECIP          = 2;
    private final int               ACTION_MENU_ADD_CONTACT         = 3;
    private final int               ACTION_MENU_CONTACT_DETAIL      = 4;
    private final int               ACTION_MENU_CONVERSATION_DETAIL = 5;
    
    static public final int NO_LAST_CONTACT = 0;
	static public final int BLANK_CONTACT = 1;
	static public final int CONTACT_ADDED = 2;
	static public final int BLANK_CONTACT_ONLY = 3;
	
	
	private LinearLayout mLinearLayout = null;
	private EditText	 mEditText = null;
	private RecipientsStateListener mRecipStateListener = null;
	private String mContactKey;

	private Activity mActivity;
	private ComposeMessageFragment mFragment;

	//used to send the update when user removes a recipient
	public interface RecipientsStateListener {
		public void updateState();
		
		/*
		 * Notify the listener that the entered recipient is invalid
		 */
		public void notifyInvalidRecip(RecipContact recip);
		
		/**
		 * This function checks if the working message is mms or not
		 */
		public boolean isMMS();
		
		/**
         * This function asks to load the message without any worthsaving item
         */
        public void nativeIntentCalled();
	}

	public RecipientEditor(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void init(Activity activity ,final ComposeMessageFragment mComposeMessageFragment) {
	    mActivity = activity;
	    mFragment = mComposeMessageFragment;
		mLinearLayout = (LinearLayout)findViewById(R.id.linearLayout);
		mEditText = (EditText)findViewById(R.id.curRecip);
		
		//below IME options are set to HANDLE "Next" button event...
		mEditText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mEditText.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    mComposeMessageFragment.gainNextFieldFocus();
                    return true;
                }
                return false;
            }
        });
		mEditText.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_DEL) {
					EditText et = (EditText)v;

					if (et.getText().length() == 0) {
						if (event.getAction() == KeyEvent.ACTION_DOWN) {
							removeLastContact();
						}
						return true;
					}
				}
				else if ((keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) && mComposeMessageFragment.isEnterKeyHandled(keyCode, event)) {
					EditText mRecipent = (EditText)getEditControl();
	 				String searchString = mRecipent.getText().toString().trim();
	 				if(searchString.length() == 0) {
	 					mRecipent.setText("");
	 				}
	 				
	 				if (mRecipent != null && mRecipent.length() != 0 && searchString.length() > 0) {
	 				    	mComposeMessageFragment.autoSelect(searchString);
	 		            return true;
	 				}
	 			}
			
			return false;
		}
		});
		
		final GestureDetector gestureDetect = new GestureDetector(new RecipGestureDetector());
		setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                try {
                    return gestureDetect.onTouchEvent(event);
                } catch (Exception e) {
                    //due to bug in some version of android OS we get an exception here
                    return false;
                }
            }
        });
		//mContactList = new ArrayList<RecipContact>();
	}
	public void setFocusListener(OnFocusChangeListener mfocusChangeListener)
	{
		mEditText.setOnFocusChangeListener(mfocusChangeListener);
	}

	public void addTextWatcher(TextWatcher watcher) {
	    mEditText.addTextChangedListener(watcher);
	}

	public void registerStateListener(RecipientsStateListener listener) {
		mRecipStateListener = listener;
	}
	
	public boolean addContact(final RecipContact contact, final Activity activity) {
		return addContact(contact, activity, false);
	}
	
	/**
	 * This function adds RecipContact to the recipient editor if valid contact is passed
	 * 
	 * @param contact: contact to be added
	 * @param activity
	 * @return true if the contact was added
	 */
	public boolean addContact(final RecipContact contact, final Activity activity, boolean notify) {
	    if (Logger.IS_DEBUG_ENABLED) {
	        Logger.debug(getClass(), "Adding contact " + contact.getName());
	    }

		mEditText.setText("");
		
		//dont add invalid recipient
		if (!isValidAddress(contact.getKey(), true)) {
			if (mRecipStateListener != null && notify) {
				mRecipStateListener.notifyInvalidRecip(contact);
			}
			return false;
		}
		
		mEditText.setHint("");
		
		LayoutInflater li = (LayoutInflater)getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		View view = li.inflate(R.layout.recepient_txtbox, null);
		view.setTag(contact);

		TextView tv = (TextView)view.findViewById(R.id.textView);
		tv.setVisibility(View.VISIBLE);
		tv.setText(contact.getName());
		
		view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showRecipActionMenu(v).show(v, mEditText, false);
                return true;
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditText.requestFocus();
                Util.showKeyboard(activity,mEditText);
            }
        });

		//mContactList.add(contact);
		//Place the new TextView before the EditText
		mLinearLayout.addView(view, mLinearLayout.getChildCount()-1);
		
		return true;
	}
	
	public void showEmptyContactAlert(RecipContact contact)
	{
		Util.forceHideKeyboard(mActivity, mEditText);
		if (mRecipStateListener != null) {
			mRecipStateListener.notifyInvalidRecip(contact);
		}
	}
	

	public void populate(ContactList list, Activity activity) {
		/*if (mContactList.size() > 0) {
			mContactList.clear();
		}*/
		if (list != null) {
			resetRecipContacts();
			for (Contact contact : list) {
				RecipContact recipContact = new RecipContact(contact.getName(), contact.getNumber(), 
						contact.getContactId(), contact.getPrefix());
				
				addContact(recipContact, activity);
			}
		}
	}

	private QuickAction showRecipActionMenu(final View view) {
		final RecipContact contact = (RecipContact)view.getTag();
		final QuickAction quickAction = new QuickAction(getContext());
		quickAction.setTitle(contact.getName());
		//add action items into QuickAction
		ActionItem actionItem = new ActionItem(ACTION_MENU_REMOVE_RECIP, R.string.menu_remove_recip, 0);
		quickAction.addActionItem(actionItem);

		if (!MmsConfig.isTabletDevice()	&& !Mms.isEmailAddress(contact.getKey())) 
		{
			actionItem = new ActionItem(ACTION_MENU_CALL_RECIP,R.string.menu_call, 0);
			quickAction.addActionItem(actionItem);
		}
        
        if(!contact.isInAddressBook())
        {
            actionItem = new ActionItem(ACTION_MENU_ADD_CONTACT, R.string.menu_add_to_contacts, 0);
            quickAction.addActionItem(actionItem);
        }
        else
        {
            actionItem = new ActionItem(ACTION_MENU_CONTACT_DETAIL, R.string.menu_view_contact, 0);
            quickAction.addActionItem(actionItem);
        }
         
        final List<String> numbers = getNumbers();
        if(numbers.size()>1){
            actionItem = new ActionItem(ACTION_MENU_CONVERSATION_DETAIL, R.string.menu_conversation_detail, 0);
            quickAction.addActionItem(actionItem);
        }
        
		//Set listener for action item clicked
		quickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {			
			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {				
				//here we can filter which action item was clicked with pos or actionId parameter
				switch (actionId) {
				case ACTION_MENU_REMOVE_RECIP:
					mLinearLayout.removeView(view);
					//mContactList.remove(view.getTag());
					
					if (mLinearLayout.getChildCount() == 1) {
					    mEditText.setHint(R.string.to_hint);
					}
					if (mRecipStateListener != null) {
						mRecipStateListener.updateState();
					}
					break;
				case ACTION_MENU_CALL_RECIP:
				    callPhone(contact.getKey());
				    break;
				case ACTION_MENU_ADD_CONTACT:
				    addContact(contact.getKey());
					break;
				case ACTION_MENU_CONTACT_DETAIL:
				    showContactDetails(String.valueOf(contact.getContactId()));
				    break;
				case ACTION_MENU_CONVERSATION_DETAIL:    
				    showRecipientList(numbers);
				    break;
				}
			}
		});

		return quickAction;
	}
    
	private void addContact(String key) {
		mRecipStateListener.nativeIntentCalled();
		
		if (key != null && key.length() > 0) {
			mContactKey = key;
			Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
			intent.setType(Contacts.CONTENT_ITEM_TYPE);
			if(Mms.isEmailAddress(key)){
				intent.putExtra(ContactsContract.Intents.Insert.EMAIL, key);
			} else {	
				intent.putExtra(ContactsContract.Intents.Insert.PHONE, key);
			}
			mActivity.startActivityForResult(intent, ComposeMessageFragment.REQUEST_CODE_ADD_CONTACT_FROM_RECIPIENT_EDITOR);
		}
		else {
			Toast.makeText(getContext(), R.string.no_phone, Toast.LENGTH_SHORT).show();
		}
	}
     
    protected void showContactDetails(String contactId) {
        mRecipStateListener.nativeIntentCalled();
       Intent intent = new Intent(Intent.ACTION_VIEW, 
               Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, ""+contactId));
       getContext().startActivity(intent);
       
    }

    private void callPhone(String phString) {
         mRecipStateListener.nativeIntentCalled();
        if (phString != null && phString.length() > 0){
            String Numb = "tel:" + phString;
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(Numb));
            getContext().startActivity(intent);
        }
        else {
            Toast.makeText(getContext(), R.string.no_phone, Toast.LENGTH_SHORT).show();
        }
    }

   
	public int getRecipientCount() {
		return getNumbers(null);
	}

	 /**
     * This Method will show all recipients for Multiple Conversation
     */
    public void showRecipientList(List<String> numbers) {
        String numberList = "";
        for(int i=0;i<numbers.size();i++){
            if(i!=0){
                numberList += ";"; 
            }
            numberList += numbers.get(i);
        }
        Intent intent = new Intent(mActivity, RecipientListActivity.class);
        intent.putExtra(RecipientListActivity.THREAD_ID, 0L);
        intent.putExtra(RecipientListActivity.RECIPIENTS, numberList);
		intent.putExtra(RecipientListActivity.GROUP_MODE, mFragment.getGroupMms());
        getContext().startActivity(intent);
    }
	
	public List<String> getNumbers() {
		final List<String> numbers = new ArrayList<String>(mLinearLayout.getChildCount());
		getNumbers(numbers);
		return numbers;
	}

	private int getNumbers(final List<String> numbers) {
		final int count = mLinearLayout.getChildCount();
		int num = 0;
		
		//setting isMms to true by default
		//since there is no reason to pass isMms as false since the type of recipient address i.e phone number 
		//or email adress also determines if it is an email
		boolean isMms = true;
		
		if (mRecipStateListener != null) {
			isMms = mRecipStateListener.isMMS();
		}

		for (int i = 0; i < count; i++) {
			final View vi = mLinearLayout.getChildAt(i);
			if (vi instanceof EditText){
				final String number = ((TextView)vi).getText().toString();
				//consider the number in EditText only if it is valid
				if (number.length() > 0 && isValidAddress(number, isMms)) {
					++num;
					if (numbers != null) {
						numbers.add(number);
					}
				}
			}
			else if (vi instanceof RelativeLayout) {
				++num;
				if (numbers != null) {
					final RecipContact contact1 = (RecipContact)vi.getTag();
					numbers.add(contact1.getKey());
				}
			}
		}

		return num;
	}

	public boolean containsEmail() {
		int count = mLinearLayout.getChildCount();
		for (int i = 0; i < count; i++) {
			View vi = mLinearLayout.getChildAt(i);

			if (vi instanceof EditText){
				String number = ((TextView)vi).getText().toString();

				if (number.length() > 0) {
					if (Mms.isEmailAddress(number)) {
						return true;
					}
				}
			} else if (vi instanceof RelativeLayout) {
				RecipContact contact1 = (RecipContact)vi.getTag();
				if (Mms.isEmailAddress(contact1.getKey())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isValidAddress(String number, boolean isMms) {
		return MessageUtils.isValidMmsAddress(number);
	}

	public boolean hasValidRecipient(boolean isMms) {
		int count = mLinearLayout.getChildCount();

		for (int i = 0; i < count; i++) {
			View vi = mLinearLayout.getChildAt(i);

			if (vi instanceof EditText){
				String number = ((TextView)vi).getText().toString();

				if (number.length() > 0) {
					if (isValidAddress(number, isMms)) {
						return true;
					}
				}
			} else if (vi instanceof RelativeLayout) {
				RecipContact contact = (RecipContact)vi.getTag();
				if (isValidAddress(contact.getKey(), isMms))
					return true;
			}
		}
		return false;
	}

	public boolean hasInvalidRecipient(boolean isMms) {
		int count = mLinearLayout.getChildCount();
		String number = null;
		View vi = null;
		
		for (int i = 0; i < count; i++) {
			vi = mLinearLayout.getChildAt(i);
			
			if (vi instanceof EditText){
				number = ((TextView)vi).getText().toString();
			} else if (vi instanceof RelativeLayout) {
				RecipContact contact = (RecipContact)vi.getTag();
				number = contact.getKey();
			}
			
			if (number != null && number.length() > 0) {
				if (!isValidAddress(number, isMms)) {
					if (MmsConfig.getEmailGateway() == null) {
						return true;
					} else if (!MessageUtils.isAlias(number)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public String formatInvalidNumbers(boolean isMms) {
		StringBuilder sb = new StringBuilder();
		int count = mLinearLayout.getChildCount();
		String number = null;
		View vi = null;
		
		for (int i = 0; i < count; i++) {
			vi = mLinearLayout.getChildAt(i);
			
			if (vi instanceof EditText){
				number = ((TextView)vi).getText().toString();
			} else if (vi instanceof RelativeLayout) {
				RecipContact contact = (RecipContact)vi.getTag();
				number = contact.getKey();
			}
			
			if (number != null && number.length() > 0) {
				if (!isValidAddress(number, isMms)) {
					if (sb.length() != 0) {
						sb.append(", ");
					}
					sb.append(number);
				}
			}
		}
			
		return sb.toString();
	}

	public String getText() {
		StringBuilder sb = new StringBuilder();
		int count = mLinearLayout.getChildCount();
		String number = null;
		View vi = null;
		
		for (int i = 0; i < count; i++) {
			vi = mLinearLayout.getChildAt(i);
			
			if (vi instanceof EditText){
				number = ((TextView)vi).getText().toString();
			} else if (vi instanceof RelativeLayout) {
				RecipContact contact = (RecipContact)vi.getTag();
				number = contact.getKey();
			}
			
			if (number != null && number.length() > 0) {
				sb.append(number + " ");
			}
		}
		return sb.toString();
	}

	/*
	 * Removes the contact at the end
	 * in response to user pressing the delete button in the keyboard
	 */
	private void removeLastContact() {
		int count = mLinearLayout.getChildCount();

		if (count > 1) {
			try {
				mLinearLayout.removeViewAt(count - 2);
				
				if (count == 2) {
				    //since there is no contacts left in recipient editor set the hint message
				    mEditText.setHint(R.string.to_hint);
				}
			}catch (Exception e) {
				Logger.error(e);
			}
		}
		
		if (mRecipStateListener != null) {
            mRecipStateListener.updateState();
        }
	}

	public View getEditControl() {
		return mEditText;
	}

	public ContactList constructContactsFromInput() {
	    return constructContactsFromInput(true);
	}

	public ContactList constructContactsFromInput(boolean update) {
		ContactList list = new ContactList();
		int count = mLinearLayout.getChildCount();

		for (int i = 0; i < count; i++) {
			View vi = mLinearLayout.getChildAt(i);

			if (vi instanceof EditText){
				String number = ((TextView)vi).getText().toString();

				if (number.length() > 0) {
					Contact contact = Contact.get(number, false, update);
					contact.setNumber(number);
					list.add(contact);
				}
			} else if (vi instanceof RelativeLayout) {
				RecipContact recipContact = (RecipContact)vi.getTag();
				Contact contact = Contact.get(recipContact.getKey(), false, update);
				contact.setNumber(recipContact.getKey());
				list.add(contact);
			}
		}

		return list;
	}

	public void removeTextWatcher(TextWatcher mRecipientsWatcher) {
		mEditText.removeTextChangedListener(mRecipientsWatcher);
	}

	//clear out all the recipients entries
	private void resetRecipContacts() {
		int count = mLinearLayout.getChildCount();

		for (int i = 0; i < count; i++) {
			View vi = mLinearLayout.getChildAt(0);

			if (vi instanceof EditText){
				((EditText)vi).setText("");
			} else if (vi instanceof RelativeLayout) {
				mLinearLayout.removeViewAt(0);
			}
		}
	}
	
	/**
	 * Clear the edit text field and validate any existing input, adding a new contact if valid. 
	 * @param mms True if message is MMS
	 * @return True if a contact was added
	 */
	public int flush(boolean mms) {
		int added = NO_LAST_CONTACT;
		final String text = mEditText.getText().toString();
		if (text.length() != 0) { //last moment contact
			// parse the entered number(s) and validate them
			final StringBuilder sb = new StringBuilder();
			String delim = null;
			final String[] nums = text.split("\\s*;\\s*");
			for (final String num : nums) {
				if (isValidAddress(num, mms)) {
					// add as a contact
					final RecipContact contact = new RecipContact(num, num, 0, null);
		        	if (contact.getKey().trim().length() == 0 && mLinearLayout.getChildCount() == 1) {
						added = BLANK_CONTACT_ONLY;
						showEmptyContactAlert(contact);
					} else if (contact.getKey().trim().length() == 0 &&  mLinearLayout.getChildCount() > 1){
						added = BLANK_CONTACT;
					}else {
					   	addContact(contact, mActivity);
		        		added = CONTACT_ADDED;
					}
				} else {
					// add it back to the edit text
					if (delim != null) {
						sb.append(delim);
					}
					else {
						delim = ";";
					}
					sb.append(num);
				}
			}
    		mEditText.setText(sb.toString());
		}
		return added;
	}

	public String getContactKey() {
		return mContactKey;
	}
	
	/*
	 * Class used to simulate click and longclick on EditText 
	 */
	class RecipGestureDetector extends SimpleOnGestureListener {
	    @Override
        public void onLongPress(MotionEvent e) {
            if (!mEditText.hasFocus()) {
                mEditText.requestFocus();
            }
            mEditText.performLongClick();
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Util.forceShowKeyboard(mActivity, mEditText);
            mEditText.requestFocus();
            RecipientEditor.this.fullScroll(FOCUS_RIGHT);
            return true;
        }
        
        @Override
        public boolean onDown(MotionEvent e) {
            //We have to return true here or else in some devices
            //like Samsung Stealth onLongPress will be triggered even
            //for single click event
            super.onDown(e);
            return true;
        }
	}
	
	/*
	 * Function called when there is a change in the resources
	 */
	public void refresh() {
	    if (mLinearLayout.getChildCount() == 1) {
	        mEditText.setHint(R.string.to_hint);
	    }
	}

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            return super.onTouchEvent(ev);
        } catch (Exception e) {
            //due to bug in some version of android OS we get an exception here
            return false;
        }
    }
}
