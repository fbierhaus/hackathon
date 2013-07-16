package com.verizon.mms.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.Telephony.Sms;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;


public class TestData {

	private static final String ACCOUNT_NAME = "VZM";
	private static final String CONTACT_PREFIX = "VZM Test ";
	private static final String MESSAGE_PREFIX = "VZM Test Message ";
	private static final String CONTACT_SEL = RawContacts.ACCOUNT_NAME + " = '" + ACCOUNT_NAME + "'";
	private static final String MESSAGE_SEL = Sms.BODY + " LIKE '" + MESSAGE_PREFIX + "%'";
	private static final String BASE_NUMBER = "1-555-555-0000";  // assumes <= 10k contacts

	private static final int MSG_COMPLETE = 1;
	private static final int MSG_UPDATE = 2;


	/**
	 * Create test contacts and messages for them.  Must be called on the main thread.
	 * 
	 * @param context
	 * @param numContacts The number of contacts to create
	 * @param messagesPerContact The number of messages per contact to create (average number if randomize is true)
	 * @param randomize If true create a random number of messages per contact, averaging messagesPerContact
	 */
	public static void create(final Context context, final int numContacts, final int messagesPerContact, final boolean randomize) {
		final ProgressDialog pd = new ProgressDialog(context);
		pd.setMessage("Getting existing data");
		pd.show();

		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				final int what = msg.what;
				if (what == MSG_UPDATE) {
					pd.setMessage((String)msg.obj);
				}
				else if (what == MSG_COMPLETE) {
					pd.dismiss();
				}
			}
		};

		new Thread() {
			public void run() {
				try {
					final ContentResolver res = context.getContentResolver();

					// get count and max id of existing contacts
					String[] prevCols = new String[] { RawContacts._ID };
					Cursor prevCur = res.query(RawContacts.CONTENT_URI, prevCols, CONTACT_SEL, null, RawContacts._ID + " DESC");
					if (prevCur != null) {
						int contactBase = prevCur.getCount();
						final int maxContactId;
						if (contactBase == 0) {
							maxContactId = 0;
						}
						else {
							prevCur.moveToFirst();
							maxContactId = prevCur.getInt(0);
						}

						// get count of any previously created messages
						prevCols = new String[] { Sms._ID };
						prevCur = res.query(VZUris.getSmsUri(), prevCols, MESSAGE_SEL, null, null);
						if (prevCur != null) {
							int msgBase = prevCur.getCount();

							// create contacts
							final ContentValues[] contactList = new ContentValues[numContacts];
							for (int i = 0; i < numContacts; ++i) {
								final ContentValues values = new ContentValues(3);
								values.put(RawContacts.ACCOUNT_NAME, ACCOUNT_NAME);
								values.put(RawContacts.ACCOUNT_TYPE, ACCOUNT_NAME);
								values.put(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED);  // don't want to aggregate these
								contactList[i] = values;
							}
							final Uri contactUri = RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
							int inserted = doInsert(res, contactUri, contactList, 10, handler, "Creating contacts");

							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(getClass(), "TestMessage.create: created " + inserted + " contacts");
							}
				
							if (inserted == numContacts) {
								// get raw contact ids of newly created contacts
								final String[] cols = new String[] { RawContacts._ID };
								final String where = CONTACT_SEL + " AND " + RawContacts._ID + " > " + maxContactId;
								final Cursor cur = res.query(RawContacts.CONTENT_URI, cols, where, null, RawContacts._ID);
								final int numCreated = cur == null ? -1 : cur.getCount();

								if (numCreated == numContacts) {
									// create data and messages for each contact
									final int numDataRecords = numContacts * 2;
									final ContentValues[] contactValues = new ContentValues[numDataRecords];
									final int numMessages = numContacts * messagesPerContact;
									final ArrayList<ContentValues> messageValues = new ArrayList<ContentValues>((int)(numMessages * 1.5));
									final Random rand = new Random();
									final int maxNumMessages = messagesPerContact * 2;
									int numMessagesPerContact = messagesPerContact;
									int recordNum = 0;
									final int lastContact = numContacts - 1;
									final int numLength = BASE_NUMBER.length();
			
									for (int i = 0; i <= lastContact; ++i, ++contactBase) {
										if (cur.moveToPosition(i)) {
											final long id = cur.getLong(0);
					
											// name
											ContentValues values = new ContentValues(3);
											final String num = Integer.toString(contactBase);
											final String name = CONTACT_PREFIX + num;
											values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
											values.put(StructuredName.DISPLAY_NAME, name);
											values.put(Data.RAW_CONTACT_ID, id);
											contactValues[recordNum++] = values;
					
											// phone number
											values = new ContentValues(4);
											final String number = BASE_NUMBER.substring(0, numLength - num.length()) + num;
											values.put(Phone.NUMBER, number);
											values.put(Phone.TYPE, Phone.TYPE_MOBILE);
											values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
											values.put(Data.RAW_CONTACT_ID, id);
											contactValues[recordNum++] = values;
					
											// TODO photo
					
											// messages
											if (randomize) {
												numMessagesPerContact = rand.nextInt(maxNumMessages) + 1;
											}
											for (int j = 0; j < numMessagesPerContact; ++j, ++msgBase) {
												values = new ContentValues(3);
								            	final long time = System.currentTimeMillis() + j;
								            	final String text = MESSAGE_PREFIX + msgBase;
								                values.put(Sms.ADDRESS, number);
							                    values.put(Sms.DATE, time);
								                values.put(Sms.BODY, text);
												messageValues.add(values);
											}
										}
										else {
											Logger.error(getClass(),"TestMessage.create: error moving to position " + i);
											return;
										}
									}
					
									// insert the data records
									inserted = doInsert(res, Data.CONTENT_URI, contactValues, 10, handler, "Creating contact records");
					
									String msg = "TestMessage.create: inserted " + inserted + " contact records";
									if (inserted == numDataRecords) {
										if (Logger.IS_DEBUG_ENABLED) {
											Logger.debug(getClass(), msg);
										}
									}
									else {
										Logger.error(getClass(),msg + " instead of " + numDataRecords);
									}

									// insert the messages into the user's inbox
									final ContentValues[] vals = messageValues.toArray(new ContentValues[messageValues.size()]);
									inserted = doInsert(res, VZUris.getSmsInboxUri(), vals, 10, handler, "Creating messages");
                                    if (Logger.IS_DEBUG_ENABLED) {
                                        Logger.debug(getClass(), "TestMessage.create: created " + inserted
                                                + " messages");
                                    }
                                }

								else {
									Logger.error(getClass(),"TestMessage.create: found " + numCreated + " contacts instead of " + numContacts);
								}
							}
							else {
								Logger.error(getClass(),"TestMessage.create: created " + inserted + " contacts instead of " + numContacts);
							}
						}
						else {
							Logger.error(getClass(),"TestMessage.create: no message results");
						}
					}
					else {
						Logger.error(getClass(),"TestMessage.create: no count results");
					}
				}
				catch (Exception e) {
					Logger.error(getClass(),e);
				}

				handler.sendEmptyMessage(MSG_COMPLETE);
			}
		}.start();
	}

	private static int doInsert(ContentResolver res, Uri uri, ContentValues[] vals, int chunk, Handler handler, String msg) {
		int inserted = 0;

		final List<ContentValues> list = Arrays.asList(vals);
		final int len = vals.length;
		int end;
		for (int i = 0; i < len; i = end) {
			end = i + chunk;
			if (end > len) {
				end = len;
			}
			String message;
			final ContentValues[] values = new ContentValues[end - i];
			list.subList(i, end).toArray(values);
			try {
				inserted += res.bulkInsert(uri, values);
				message = msg + ": " + inserted;
			}
			catch (Exception e) {
				Logger.error(TestData.class,e);
				message = "Error inserting records: " + e;
				break;
			}
			handler.sendMessage(Message.obtain(handler, MSG_UPDATE, message));
		}

		return inserted;
	}

	/**
	 * Delete any existing test data.  Must be called on the main thread.
	 */
	public static void delete(final Context context) {
		final ProgressDialog pd = new ProgressDialog(context);
		pd.setMessage("Deleting test data");
		pd.show();

		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				pd.dismiss();
			}
		};

		new Thread() {
			public void run() {
				try {
					final ContentResolver res = context.getContentResolver();
			
					final Uri contactUri = RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
					int numDeleted = res.delete(contactUri, CONTACT_SEL, null);
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "TestMessage.create: deleted " + numDeleted + " contacts");
                    }
					numDeleted = res.delete(VZUris.getSmsUri(), MESSAGE_SEL, null);
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "TestMessage.create: deleted " + numDeleted + " messages");
                    }
                }
				catch (Exception e) {
					Logger.error(getClass(),e);
				}

				handler.sendEmptyMessage(0);
			}
		}.start();
	}
}
