package com.hackathon.tvnight.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.hackathon.tvnight.R;
import com.hackathon.tvnight.model.ContactItem;

public class ContactListSelectorActivity extends Activity implements OnClickListener {
	
	private ListView contactsList;
	private ContactAdapter contactsAdapter;
	private Button confirm;
	private Button cancel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.select_contacts_layout);
		confirm = (Button) findViewById(R.id.confirm_butt);
		cancel = (Button) findViewById(R.id.cancel_butt);
		confirm.setOnClickListener(this);
		cancel.setOnClickListener(this);
		contactsList = (ListView) findViewById(R.id.contact_list);
		new AsyncTask<Void, Void, List<ContactItem>>() {

			@Override
			protected List<ContactItem> doInBackground(Void... params) {
				Cursor cursor = getContentResolver().query(
						ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
				List<ContactItem> contacts = new ArrayList<ContactItem>();
				while (cursor.moveToNext()) {
					String contactId = cursor.getString(cursor
							.getColumnIndex(ContactsContract.Contacts._ID));
					String hasPhone = cursor
							.getString(cursor
									.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
					String name = cursor.getString(cursor
							.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if ("1".equals(hasPhone) || Boolean.parseBoolean(hasPhone)) {
						ContactItem contact = new ContactItem();
						Cursor phones = getContentResolver().query(
								ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
								null,
								ContactsContract.CommonDataKinds.Phone.CONTACT_ID
										+ " = " + contactId, null, null);
						if (phones != null && phones.moveToFirst()) {
							String phoneNumber = phones
									.getString(phones
											.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
							int itype = phones
									.getInt(phones
											.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
							String type = (String) Phone.getTypeLabel(getResources(),
									itype, "");
							Bitmap b = BitmapFactory.decodeStream(openPhoto(Long.parseLong(contactId)));
							contact.setIcon(b);
							contact.setNumberType(type);
							contact.setName(name);
							contact.setNumber(phoneNumber);
							contacts.add(contact);
						}
						phones.close();
					}			
				}
				return contacts;
			}
			
			protected void onPostExecute(List<ContactItem> result) {
				findViewById(R.id.progress_bar).setVisibility(View.GONE);
				contactsList.setVisibility(View.VISIBLE);
				findViewById(R.id.bottom_layout).setVisibility(View.VISIBLE);
				contactsAdapter = new ContactAdapter(result);
				contactsList.setAdapter(contactsAdapter);
			};
			
			
		}.execute();
		
		super.onCreate(savedInstanceState);
	}

	public InputStream openPhoto(long contactId) {
	     Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
	     Uri photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);
	     Cursor cursor = getContentResolver().query(photoUri,
	          new String[] {Contacts.Photo.PHOTO}, null, null, null);
	     if (cursor == null) {
	         return null;
	     }
	     try {
	         if (cursor.moveToFirst()) {
	             byte[] data = cursor.getBlob(0);
	             if (data != null) {
	                 return new ByteArrayInputStream(data);
	             }
	         }
	     } finally {
	         cursor.close();
	     }
	     return null;
	 }

	static class ContactViewHolder {
		private ImageView icon;
		private TextView name;
		private TextView numAndType;
	}

	class ContactAdapter extends BaseAdapter {

		private List<ContactItem> contacts;

		public ContactAdapter(List<ContactItem> contacts) {
			this.contacts = contacts;
		}

		@Override
		public int getCount() {
			return contacts.size();
		}

		@Override
		public Object getItem(int position) {
			return contacts.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			ContactItem contact = contacts.get(position);
			
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.contact_item, null);
				ContactViewHolder contactViewHolder = new ContactViewHolder();
				contactViewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
				contactViewHolder.name = (TextView) convertView.findViewById(R.id.contact_name);
				contactViewHolder.numAndType = (TextView) convertView.findViewById(R.id.contact_num_and_type);
				convertView.setTag(contactViewHolder);
			}
			
			ContactViewHolder holder = (ContactViewHolder) convertView.getTag();
			
			Bitmap bit = contact.getIcon();
			if (bit != null) {
				holder.icon.setImageBitmap(bit);
			}
			else {
				holder.icon.setImageResource(R.drawable.ic_contact_picture);
			}
			holder.name.setText(contact.getName());
			holder.numAndType.setText(contact.getNumberType()+": "+contact.getNumber());
			
			return convertView;
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.cancel_butt:
			setResult(Activity.RESULT_CANCELED);
			finish();
			break;
		case R.id.confirm_butt:
			
			break;
		}
	}

}
