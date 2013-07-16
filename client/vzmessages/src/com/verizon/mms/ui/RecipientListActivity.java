package com.verizon.mms.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.QuickContact;
import android.provider.Telephony.Mms;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.rocketmobile.asimov.ConversationListActivity;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.Contact.UpdateListener;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.data.Conversation.OutgoingMessageData;
import com.verizon.mms.ui.GroupModeChooser.OnGroupModeChangedListener;
import com.verizon.mms.ui.MessageItem.GroupMode;
import com.verizon.mms.ui.widget.ActionItem;
import com.verizon.mms.ui.widget.QuickAction;
import com.verizon.mms.ui.widget.TextButton;

public class RecipientListActivity extends VZMListActivity implements UpdateListener {
    private TextButton   	  mCreateButton          = null;
    private RecipientsAdapter mAdapter               = null;
    private ListView          mListView              = null;
    private ContactList       mContactList;
    private Contact			  mReloadContact;
    private final int         MENU_CALL_RECIP        = 1;
    private final int         MENU_ADD_CONTACT       = 2;
    private final int         MENU_CONTACT_DETAIL    = 3;
    private final int         MENU_MESSAGE_RECIPIENT = 4;
    private final int		  REQUEST_CODE_CHECK_CONTACT = 444;
    private final int		  REQUEST_CODE_RELOAD_CONTACT = 445;
    private int               mPosition              = -1;
	private GroupModeChooser  groupChooser;
	private View              groupDialog;
    public static final String THREAD_ID              = "thread_id";
    public static final String RECIPIENTS             = "recipients";
    public static final String GROUP_MODE             = "group";
	private final Handler mRecipientListHandler = new Handler();

	static final int GROUP_UNCHANGED = 1;
	static final int GROUP_GROUP     = 2;
	static final int GROUP_SENDER    = 3;

	private static final float RECIPIENT_NAME_TEXT_SIZE = 16.66f;
	private static final float RECIPIENT_NAME_TEXT_SIZE_LARGE = 20.66f;
	private static final float RECIPIENT_PHONE_NUMBER_TEXT_SIZE = 14.81f;
	private static final float RECIPIENT_PHONE_NUMBER_TEXT_SIZE_LARGE = 18.81f;
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.recipient_list);
        initializeList();
    }
      
	@Override
	protected void onPause() {
		Contact.removeListener(this);
		super.onPause();
	}

	@Override
	protected void onResume() {
		Contact.addListener(this);
		super.onResume();
	}
	
    private void initializeList() {
        Intent intent = getIntent();
        long threadId = intent.getLongExtra(THREAD_ID, 0L);
       
        if (threadId != 0) {
            mContactList = Conversation.get(this, threadId, false).getRecipients();
        }
        else{
            String recipientList = intent.getStringExtra(RECIPIENTS);
            if(recipientList!=null){
                mContactList =  ContactList.getByNumbers(recipientList, false, false);
            }
        }
        if(mContactList == null || mContactList.isEmpty()){
        	final AppAlignedDialog d = new AppAlignedDialog(this,
        			R.drawable.dialog_alert, 
        			R.string.blank_recipient, 
        			R.string.visit_shortly);
        	Button canelButton = (Button) d.findViewById(R.id.positive_button);
        	canelButton.setText(R.string.ok);
        	canelButton.setOnClickListener(new View.OnClickListener() {
        		@Override
        		public void onClick(View v) {
        			d.dismiss();
        			finish();
        		}
        	});				
        	d.show();
        }
        else
        {    
            mAdapter = new RecipientsAdapter(this, mContactList);
            mListView = getListView();
            mListView.setAdapter(mAdapter);
            
            // make group delivery mode buttons visible if this is a group message
            final int vis = mContactList.size() > 1 ? View.VISIBLE : View.GONE;
            groupChooser = (GroupModeChooser)findViewById(R.id.group_chooser);
            groupChooser.setVisibility(vis);
            groupChooser.setListener(groupListener);
            initGroupMode(intent, threadId);
    		groupDialog = findViewById(R.id.group_dialog);
			setResult(GROUP_UNCHANGED);
        }
        mCreateButton = (TextButton) findViewById(R.id.create_message_button);
        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String contactNumbers = ContactList.getDelimSeperatedNumbers(mContactList, ";");
                
                Intent intent = ConversationListActivity.getIntentFromParent(RecipientListActivity.this, 0, true);
                intent.putExtra(ComposeMessageActivity.SEND_RECIPIENT, true);
        		intent.putExtra("showTextEditor", true);
                intent.putExtra(ComposeMessageActivity.PREPOPULATED_ADDRESS, contactNumbers);
                intent.putExtra(ComposeMessageActivity.GROUP_MODE, groupChooser.getGroupMms());
                finish();
                startActivity(intent);
            }
		});
    }

	private void initGroupMode(Intent intent, long threadId) {
		// see if the caller has specifed the group mode
		final boolean groupMms;
		if (intent.hasExtra(GROUP_MODE)) {
			groupMms = intent.getBooleanExtra(GROUP_MODE, ComposeMessageFragment.GROUP_MMS_DEFAULT);
		}
		else {
			// need to get it from the last outgoing message in the thread
			final OutgoingMessageData data = Conversation.getLastOutgoingData(this, threadId, true);
			if (data != null) {
				groupMms = data.groupMode == GroupMode.GROUP;
			}
			else {
				groupMms = ComposeMessageFragment.GROUP_MMS_DEFAULT;
			}
		}
		groupChooser.setGroupMode(groupMms);
	}


	private OnGroupModeChangedListener groupListener = new OnGroupModeChangedListener() {
		public void groupModeChanged(boolean groupMms) {
			setResult(groupMms ? GROUP_GROUP : GROUP_SENDER);
		}
	};

	private class RecipientsAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private ContactList    contactList;

        public RecipientsAdapter(Context context, ContactList contList) {
            contactList = contList;
            inflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return contactList.size();
        }

        public Object getItem(int position) {
            return contactList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {

            final ViewHolder holder;
            if (convertView == null) {   	
                convertView = inflater.inflate(R.layout.recipient_list_item, null);
                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.member_name);
                holder.avatar = (ContactImage) convertView.findViewById(R.id.photo);
                holder.phoneNo = (TextView) convertView.findViewById(R.id.phoneNumber);
                holder.ico_phone=(ImageView)convertView.findViewById(R.id.ico_phone) ;
                holder.ico_mail=(ImageView)convertView.findViewById(R.id.ico_mail);
                holder.ico_message=(ImageView)convertView.findViewById(R.id.ico_message);
                holder.seperator=(View)convertView.findViewById(R.id.seperator);
                final Contact recipient = (Contact) getItem(position);
                if (!MmsConfig.isTabletDevice()) {
                	if (Mms.isEmailAddress(recipient.getNumber())) {
                		holder.ico_mail.setVisibility(View.VISIBLE);
                	} else {
                		holder.ico_phone.setVisibility(View.VISIBLE);   
                	}
                } else {
                	if (Mms.isEmailAddress(recipient.getNumber())) {
                		holder.ico_mail.setVisibility(View.VISIBLE);
                	} else {
                		holder.seperator.setVisibility(View.INVISIBLE); 
                		holder.ico_phone.setVisibility(View.INVISIBLE);   
                	}
                }
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final Contact recipient = (Contact) getItem(position);
            Drawable defAvatar = getApplicationContext().getResources().getDrawable(
                    R.drawable.ic_contact_picture);
            holder.name.setText(recipient.getName());
            holder.avatar.setImage(recipient.getAvatar(getApplicationContext(), defAvatar));
            
            if (recipient.getName().equals(recipient.getNumber())) {
                holder.phoneNo.setText("");
            } else {
                holder.phoneNo.setText(recipient.getPrefix() + " : " + recipient.getNumber());
                
            }
            //avatar click Listener initialization
            AvatorClickListener listener = new AvatorClickListener(recipient);
            holder.avatar.setOnClickListener(listener);
            final Contact contact = mContactList.get(position);
            
            convertView.setOnClickListener(new View.OnClickListener() {
            	@Override
            	public void onClick(View v) {
            		if (contact.existsInDatabase()){
            			showContactDetails(String.valueOf(contact.getContactId()));
            		} else {
            			addContact(recipient.getNumber(), recipient.isEmail());
            		}
            	}
            });
         
            holder.ico_mail.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
				  Intent intentMail=new Intent(Intent.ACTION_VIEW,  Uri.parse("mailto:" + contact.getName()));
				  intentMail.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				  startActivity(intentMail);	
				  //messageRecipient(contact.getName());
				}
			});	
          
        	holder.ico_message.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					messageRecipient(contact.getNumber());
				}
			});
        
        	holder.ico_phone.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					callPhone(contact.getNumber());
				}
			});
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					setRecipientListMenu(position);
					return true;
				}
			});
            
            
            
            return convertView;
        }

        class ViewHolder {
            TextView     name;
            TextView     phoneNo;
            ContactImage avatar;
            ImageView   ico_message;
            ImageView   ico_phone;
            ImageView   ico_mail;
            View        seperator;
        }
    }

   public void setRecipientListMenu(int position){
        mPosition = position;
        final Contact contact = mContactList.get(mPosition);
        QuickAction mRecipientMenu = new QuickAction(RecipientListActivity.this);
        mRecipientMenu.setTitle(contact.getName());
        if (!MmsConfig.isTabletDevice()	&& !Mms.isEmailAddress(contact.getNumber()))
        {	
        	String callBackString = getString(R.string.menu_call_back).replace("%s", contact.getName());      
        	mRecipientMenu.addActionItem(new ActionItem(MENU_CALL_RECIP, callBackString, 0));
        }
        if (contact.existsInDatabase()) {
        	mRecipientMenu.addActionItem(new ActionItem(MENU_CONTACT_DETAIL, R.string.menu_view_contact, 0));
        } else {
        	mRecipientMenu.addActionItem(new ActionItem(MENU_ADD_CONTACT, R.string.menu_add_to_contacts, 0));
        }
        mRecipientMenu.addActionItem(new ActionItem(MENU_MESSAGE_RECIPIENT, getString(R.string.menu_msg)+" "+ contact.getName(), 0));       
        mRecipientMenu.setOnActionItemClickListener(mRecipientListMenuClickListener);
        mRecipientMenu.show(null, null, false);
    }

    
    QuickAction.OnActionItemClickListener  mRecipientListMenuClickListener  = new QuickAction.OnActionItemClickListener() {
    	
		@Override
		public void onItemClick(QuickAction source, int pos, int actionId) {
			final Contact contact = mContactList.get(mPosition);
			switch (actionId) {
			case MENU_CALL_RECIP: {
                callPhone(contact.getNumber());
                break;
            }

            case MENU_CONTACT_DETAIL: {
                showContactDetails(String.valueOf(contact.getContactId()));
                break;
            }

            case MENU_ADD_CONTACT: {
                addContact(contact.getNumber(), contact.isEmail());
                break;
            }
            case MENU_MESSAGE_RECIPIENT: {
                messageRecipient(contact.getNumber());
                break;
            }

			}
		}
		
	};

    /**
     * This Method will launch Unified Composer with the name prepopulated in address field with keyboard
     * displayed. If there is existing conversation, it will be added to that otherwise new Conversation will
     * start
     * 
     * @param phoneNumber
     *            Phone Number of The Message Recipient
     */
    public void messageRecipient(String phoneNumber) {
        Intent intent =  ConversationListActivity.getIntentFromParent(RecipientListActivity.this, 0, true);
        
        intent.putExtra(ComposeMessageActivity.SEND_RECIPIENT, true);
        intent.putExtra("showTextEditor", true);
        intent.putExtra(ComposeMessageActivity.PREPOPULATED_ADDRESS, phoneNumber);
        if(MmsConfig.isTabletDevice()){
        	finish();
        }
        startActivity(intent);
    }

    private void addContact(String phString, boolean isEmail) {
        if (phString != null && phString.length() > 0) {
            Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
            intent.setType(Contacts.CONTENT_ITEM_TYPE);
            
            if (isEmail) {
            	intent.putExtra(ContactsContract.Intents.Insert.EMAIL, phString);
            } else {
            	intent.putExtra(ContactsContract.Intents.Insert.PHONE, phString);
            }
            startActivityForResult(intent,MENU_ADD_CONTACT);
        }
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_RELOAD_CONTACT) {
			if (mReloadContact != null) {
				mReloadContact.reload();
				mReloadContact = null;
			}
		} else if (mPosition >= 0) {
			if (resultCode == RESULT_OK && data != null && requestCode == MENU_ADD_CONTACT) {
				mContactList.get(mPosition).reload();		
			}

			if(requestCode == REQUEST_CODE_CHECK_CONTACT){			
				mContactList.get(mPosition).reload();
			}
		}
	}

    protected void showContactDetails(String contactId) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_URI, "" + contactId));
        startActivityForResult(intent, REQUEST_CODE_CHECK_CONTACT);
    }

    private void callPhone(String phString) {
        if (phString != null && phString.length() > 0) {
            String Numb = "tel:" + phString;
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(Numb));
            startActivity(intent);
        }
    }
    
    public class AvatorClickListener implements View.OnClickListener {
        private Contact mContact;

        public AvatorClickListener(Contact contact) {
            this.mContact = contact;
        }

        @Override
        public void onClick(View view) {
            final String lookUpKey = mContact.getLookUpKey();
            if (lookUpKey != null) {
                Uri lookUpUri = Contacts.getLookupUri(mContact.getContactId(), lookUpKey);
                QuickContact.showQuickContact(RecipientListActivity.this, view, lookUpUri, ContactsContract.QuickContact.MODE_MEDIUM,
                        new String[] { Contacts.CONTENT_ITEM_TYPE });
            }
            else {
                final Uri createUri;
                if (Mms.isEmailAddress(mContact.getNumber())) {
                    createUri = Uri.fromParts("mailto", mContact.getNumber(), null);
                }
                else {
                    createUri = Uri.fromParts("tel", mContact.getNumber(), null);
                }
                final Intent intent = new Intent(Intents.SHOW_OR_CREATE_CONTACT, createUri);
                mReloadContact = mContact;
                startActivityForResult(intent, REQUEST_CODE_RELOAD_CONTACT);
            }
        }
    }

    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && groupDialog.getVisibility() == View.VISIBLE) {
			groupDialog.findViewById(R.id.group_dialog_close).performClick();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}


	@Override
	public void onUpdate(Contact updated, Object cookie) {
		if (mContactList == null || containsContact(mContactList, updated)) {
			mRecipientListHandler.post(new Runnable() {
				@Override
				public void run() {
					if (mAdapter != null) {
						mAdapter.notifyDataSetChanged();
					}
				}
			});
		}
	}

	private boolean containsContact(ContactList recipients, Contact updated) {
		for (Contact contact : recipients) {
			if (updated == contact) {
				return true;
			}
		}
		return false;
	}
}
