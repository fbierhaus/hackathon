package com.verizon.mms.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.Media;
import com.verizon.mms.MediaProvider;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.MmsException;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.model.IModelChangedObserver;
import com.verizon.mms.model.Model;
import com.verizon.mms.model.RegionModel;
import com.verizon.mms.model.VCardModel;
import com.verizon.mms.ui.ComposeMessageActivity;
import com.verizon.mms.ui.ConversationListFragment;
import com.verizon.mms.ui.MessageUtils;
import com.verizon.mms.ui.widget.ActionItem;
import com.verizon.mms.ui.widget.QuickAction;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct.OrganizationData;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct.PhoneData;

public class ContactInfoAdapter extends CursorAdapter {

    private static final int MENU_CONTACT_DETAILS = 0;
    private static final int MENU_CALL = 0;
    private static final int MENU_MESSAGE = 0;
    private static final int MENU_ADD_TO_CONTACTS = 0;
    private static final int MENU_SEND_EMAIL = 0;
    private static final int PHONE_NUM_LIMIT =3;
    private final String parseError;
    private Context c;
    
    public ContactInfoAdapter(Context context, Cursor c) {
        super(context, c);
        this.c = context;
        
        parseError = context.getString(R.string.name_card_parse_error);
    }

    public static class ViewHolder {
        private TextView sender;
    }


    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder vh = (ViewHolder)view.getTag();
        final Media mms = MediaProvider.Helper.fromCursorCurentPosition(cursor);
        if (mms.isOutgoing()) {
            vh.sender.setText(R.string.me);
        }
        else {
            vh.sender.setText(getAddress(mms));
        }

        ((TextView) view.findViewById(R.id.date)).setText(MessageUtils.formatTimeStampString(mms.getDate(), false));
        
        if (!mms.isNameCard() && (mms.isPhone() || mms.isEmail())) {
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mms.isEmail()) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse("mailto:"+mms.getText()));
                        c.startActivity(Intent.createChooser(i, c.getString(R.string.complete_action_using)));
                    }
                    if (mms.isPhone()) {
                        Intent i = new Intent(Intent.ACTION_DIAL);
                        i.setData(Uri.parse("tel:"+mms.getText()));
                        c.startActivity(i);
                    }
                }
            });
            TextView mainInfo = ((TextView) view.findViewById(R.id.contact));

            mainInfo.setText(mms.getText());
            view.setOnLongClickListener(new View.OnLongClickListener() {
                
                @Override
                public boolean onLongClick(View v) {
                    QuickAction mContactInfoMenu = new QuickAction(c);
                    mContactInfoMenu.setTitle(mms.getText());
                    boolean isContact;
                    ActionItem actionItem;
                    if (mms.isPhone()) {
                        if(!MmsConfig.isTabletDevice()) {
                            String callString = c.getString(R.string.menu_call_back, mms.getText());
                            Intent call = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + mms.getText()));
                            actionItem = new ActionItem(MENU_CALL, callString, 0);
                            actionItem.setTag(call);
                            mContactInfoMenu.addActionItem(actionItem);                         
                        }
                        String textString = c.getString(R.string.menu_msg) + " " + mms.getText();
                        Intent messageIntent = ConversationListActivity.getIntentFromParent(c, 0, true);
                        messageIntent.putExtra(ComposeMessageActivity.SEND_RECIPIENT, true);
                        messageIntent.putExtra(ComposeMessageActivity.PREPOPULATED_ADDRESS, mms.getText());
                        actionItem = new ActionItem(MENU_MESSAGE, textString, 0);
                        actionItem.setTag(messageIntent);
                        mContactInfoMenu.addActionItem(actionItem);
                        
                        Contact contact = Contact.get(mms.getText(), true);
                        if (contact.existsInDatabase()) {
                            Uri contactUri = contact.getUri();
                            Intent viewContact = new Intent(Intent.ACTION_VIEW, contactUri);
                            actionItem = new ActionItem(MENU_CONTACT_DETAILS, R.string.menu_view_contact, 0);
                            actionItem.setTag(viewContact);
                            mContactInfoMenu.addActionItem(actionItem);
                        }
                        else {
                            Intent conIntent = ConversationListFragment.createAddContactIntent(mms.getText());
                            String addContactString = c.getString(R.string.menu_add_address_to_contacts).replace("%s",
                                    mms.getText());
                            actionItem = new ActionItem(MENU_ADD_TO_CONTACTS, addContactString, 0);
                            actionItem.setTag(conIntent);
                            mContactInfoMenu.addActionItem(actionItem);
                        }
                        //this contact might change so mark it as stale
                        contact.markAsStale();
                    }
                    if (mms.isEmail()) {
                        String sendEmailString = c.getString(R.string.menu_send_email, mms.getText());
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + mms.getText()));
                        actionItem = new ActionItem(MENU_SEND_EMAIL, sendEmailString, 0);
                        actionItem.setTag(intent);
                        mContactInfoMenu.addActionItem(actionItem);
                        isContact = haveEmailContact(mms.getText());
                        if (!isContact) {
                            Intent conIntent = ConversationListFragment.createAddContactIntent(mms.getText());
                            String addContactString = c.getString(R.string.menu_add_address_to_contacts).replace("%s",
                                    mms.getText());
                            actionItem = new ActionItem(MENU_ADD_TO_CONTACTS, addContactString, 0);
                            actionItem.setTag(conIntent);
                            mContactInfoMenu.addActionItem(actionItem);
                        }
                    }
                    mContactInfoMenu.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
                        
                        @Override
                        public void onItemClick(QuickAction source, int pos, int actionId) {
                            ActionItem item = source.getActionItem(pos);
                            c.startActivity((Intent)item.getTag());
                        }
                    });
                    mContactInfoMenu.show(null, null, false);
                    return true;
                }
            });
        }
        else {
            final ImageView icon = (ImageView) view.findViewById(R.id.namecardico);
            TextView nameView = (TextView) view.findViewById(R.id.name);
            TextView phoneView = (TextView) view.findViewById(R.id.phone);
            TextView emailView = (TextView) view.findViewById(R.id.email);
            icon.setImageDrawable(c.getResources().getDrawable(R.drawable.list_namecard));
            VCardModel vcm  = null;
            try {
                vcm = new VCardModel(c, Uri.parse(mms.getNameCardUri()), new RegionModel(null, 0, 0, 0, 0));
            } catch (MmsException e) {
                Logger.error(e);
                
                nameView.setText(R.string.name_card_parse_error);
                emailView.setVisibility(View.GONE);
                phoneView.setVisibility(View.GONE);
                view.setOnClickListener(null);//some error occured set it back to null to avoid wrong click event
                return;
            }
            if (vcm.getContactPicture() != null) {
                icon.setImageBitmap(vcm.getContactPicture());
            }
            else {
                final IModelChangedObserver mVCardModelChangedObserver =
                        new IModelChangedObserver() {
                            public void onModelChanged(final Model model,final  boolean dataChanged) {
                                VCardModel vcm = (VCardModel) model;
                                if (vcm.getContactPicture() != null) {
                                    icon.setImageBitmap(vcm.getContactPicture());
                                }
                            }
                        };
                vcm.registerModelChangedObserver(mVCardModelChangedObserver);
            }
            
            ContactStruct contactStruct = vcm.getContactStruct();
            
            final String number ;
            if (contactStruct != null) {
                final String name = contactStruct.getName();
                number = getFormattedMsg(contactStruct);                      
                final String email = contactStruct.getFirstEmail();
                if (name != null) {
                    nameView.setText(name);
                    emailView.setVisibility(View.GONE);
                    phoneView.setVisibility(View.GONE);
                }
                if (email != null) {
                    emailView.setText(email);
                    emailView.setVisibility(View.VISIBLE);
                    phoneView.setVisibility(View.GONE);
                }
                if (number != null) {
                    phoneView.setText(number);
                    phoneView.setVisibility(View.VISIBLE);
                }
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Contact contact = Contact.get(number, true);
                        if (contact.existsInDatabase()) {
                            Uri contactUri = contact.getUri();
                            Intent viewContact = new Intent(Intent.ACTION_VIEW, contactUri);
                            viewContact.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            c.startActivity(viewContact);
                                                                                    
                        }
                        else {
                            Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                            intent.setType(Contacts.CONTENT_ITEM_TYPE);
                            if (email != null) {
                                intent.putExtra(ContactsContract.Intents.Insert.EMAIL, email);
                            }
                            if (name != null) {
                                intent.putExtra(ContactsContract.Intents.Insert.NAME, name);
                            }
                            if (number != null) {
                                intent.putExtra(ContactsContract.Intents.Insert.PHONE, number);
                                intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
                                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
                            }
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                            c.startActivity(intent);
                        }
                        //this contact may change so mark it as stale
                        contact.markAsStale();
                    }
                });
            } else {
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = ContactInfoAdapter.this.c;
                        Toast.makeText(context, parseError, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
        
    }

    /**
     * This Method 
     * @param contact
     */
    private String getFormattedMsg(ContactStruct contact) {
        StringBuilder formatMsg = new StringBuilder();

        formatMsg.append(contact.getName());

        if (contact.getOrganizationalData() != null && contact.getOrganizationalData().size() > 0) {
            OrganizationData data = contact.getOrganizationalData().get(0);
            formatMsg.append("\n" + data.companyName + "\n" + data.positionName);
        }

        if (contact.getPhoneList() != null) {
            int i = 0;
            for (PhoneData data : contact.getPhoneList()) {
                formatMsg.append("\n" + data.data);
                i++;

                //add first three phone numbers
                if (i == PHONE_NUM_LIMIT) {
                    break;
                }
            }
        }
                      // Commented because its adding unnecessary duplicate email address with 3 phone numbers
        
//        if (contact.getContactMethodsList() != null && contact.getContactMethodsList().size() > 0) {
//            ContactMethod data = contact.getContactMethodsList().get(0);
//            formatMsg.append("\n" + data.data);
//        }

        return formatMsg.toString();
    }
    
        
    private String getAddress(Media mms) {
        String ret = "";
        final String addr = mms.getAddress();
        if (addr != null) {
            final ContactList list = ContactList.getByNumbers(addr, false, false);
            if (list.size() != 0) {
                ret = list.get(0).getName();
            }
            else {
                Logger.error(getClass(), "empty address for " + mms);
            }
        }
        else {
            Logger.error(getClass(), "null address for " + mms);
        }
        return ret;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        Media mms = MediaProvider.Helper.fromCursorCurentPosition(cursor);
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final ViewHolder vh = new ViewHolder();
        final View v;
        if (!mms.isNameCard()) {
            v = vi.inflate(R.layout.contactinfolistitem, null);
        }
        else {
            v = vi.inflate(R.layout.namecarditem, null);
        }
        vh.sender = (TextView)v.findViewById(R.id.sender);
        v.setTag(vh);
        return v;
    }
    
    private int getItemViewType(Cursor cursor) {
        Media mms = MediaProvider.Helper.fromCursorCurentPosition(cursor);
        if (!mms.isNameCard()) {
            return 0;
        }
        else {
            return 1;
        }
    }
    
    @Override
    public int getItemViewType(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return getItemViewType(cursor);
    }
    
    @Override
    public int getViewTypeCount() {
        return 2;
    }
    
    private boolean haveEmailContact(String emailAddress) {
        Cursor cursor = SqliteWrapper.query((Activity) c, c.getContentResolver(),
                Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(emailAddress)),
                new String[] { Contacts.DISPLAY_NAME }, null, null, null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    if (!TextUtils.isEmpty(name)) {
                        return true;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }
    
}
//-------------
//ContactInfoAdapter.java