package com.verizon.vzmsgs.saverestore;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Part;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.ContentType;
import com.verizon.mms.MmsException;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.model.SmilHelper;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.ui.MessageListAdapter;
import com.verizon.mms.ui.SaveRestoreActivity;


/**
 * This class/interface
 * 
 * @author Md Imthiaz
 * @Since Sep 1, 2012
 */
public class MessagesDaoImpl {

    private Context mContext;
    private final List<String> supportedSMSColumns;
    private final List<String> supportedPDUColumns;
    private final List<String> supportedPartsColumns;
    private static String msgSelectionString = "address = ? AND body = ? AND date = ?";
    private static String[] ID_MSG_TYPE_PROJECTON = new String[] { MmsSms._ID,
            MmsSms.TYPE_DISCRIMINATOR_COLUMN };
	
    public MessagesDaoImpl(Context context) {
        mContext = context;
        supportedSMSColumns = getSupportedColumns(Sms.CONTENT_URI);
        // Need to verify the supported columns on device

        supportedPDUColumns = getSupportedColumns(Mms.CONTENT_URI);
        supportedPartsColumns = getSupportedColumns(Uri.parse("content://" + Mms.CONTENT_URI.getAuthority()
                + "/1/part"));
    }

    /**
     * This Method to get the support columns in the given URI,
     * 
     * @param contentUri
     * @return {@link List}
     */
    private List<String> getSupportedColumns(Uri contentUri) {
        Cursor cursor = null;
        String[] columns = null;
        try {
            cursor = mContext.getContentResolver().query(contentUri, null, null, null, null);
            if (cursor != null) {
                columns = cursor.getColumnNames();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return Arrays.asList(columns);
    }

    /**
     * This Method returns JSONString holding MMS Contents for specific msgId
     * 
     * @param msg
     */
    public JSONMessage loadSMS(long msgId) {
        String where = Sms._ID + "=" + msgId;
        JSONMessage msg = new JSONMessage();
        Cursor c = null;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "MessageID ::" + msgId + "loading SMS...");
        }
        try {

            c = mContext.getContentResolver().query(Sms.CONTENT_URI, null, where, null, null);

            if (c != null && c.moveToFirst()) {
                String[] columnNames = c.getColumnNames();
                for (String column : columnNames) {
                    if (column.equalsIgnoreCase(Sms._ID)) {
                        continue;
                    }
                    if (column.equalsIgnoreCase(Sms.THREAD_ID)) {
                        // Creating conversation Id
                        String recipients = getRecipients(c.getLong(c.getColumnIndex(column)));
                        msg.setRecipients(recipients);
                        continue;
                    }
                    try {
                        msg.getPdu().put(column, c.getString(c.getColumnIndex(column)));
                    } catch (JSONException e) {

                    }
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "MessageID ::" + msgId + "loaded Successfully...");
            }
        }

        return msg;
    }

    /**
     * This Method returns JSONString holding MMS Contents for specific msgId
     * 
     * @param msg
     */
    public JSONMessage loadMMS(long msgId) {
        JSONMessage msg = new JSONMessage();

        String where = Mms._ID + "=" + msgId;
        Cursor c = null;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "MessageID ::" + msgId + "loading MMS...");
        }
        c = mContext.getContentResolver().query(VZUris.getMmsUri(), null, where, null, null);

        if (c != null) {
            while (c.moveToNext()) {
                String[] columnNames = c.getColumnNames();
                for (String column : columnNames) {
                    try {
                        if (column.equalsIgnoreCase(Mms._ID)) {
                            continue;
                        }
                        if (column.equalsIgnoreCase(Mms.THREAD_ID)) {
                            // Creating conversation Id
                            String convId = getRecipients(c.getLong(c.getColumnIndex(column)));
                            msg.setRecipients(convId);
                            continue;
                        }
                        msg.getPdu().put(column, c.getString(c.getColumnIndex(column)));
                    } catch (JSONException e) {

                    }
                }
            }
            c.close();
        }

        // Addr
        Uri uri = Uri.parse("content://" + VZUris.getMmsAuthority() + "/" + msgId + "/addr");
        c = mContext.getContentResolver().query(uri, null, null, null, null);
        if (c != null) {
            JSONObject adr = null;
            while (c.moveToNext()) {
                adr = new JSONObject();
                String[] columnNames = c.getColumnNames();
                for (String column : columnNames) {
                    try {
                        if (column.equalsIgnoreCase(Mms._ID) || column.equalsIgnoreCase(Mms.Addr.CONTACT_ID)) {
                            continue;
                        }
                        adr.put(column, c.getString(c.getColumnIndex(column)));
                    } catch (JSONException e) {

                    }
                }
                msg.getAddresses().add(adr);
            }
            c.close();
        }

        // parts
        uri = Uri.parse("content://" + VZUris.getMmsAuthority() + "/" + msgId + "/part");
        c = mContext.getContentResolver().query(uri, null, null, null, null);
        if (c != null) {
            JSONObject part = null;
            while (c.moveToNext()) {
                // Loading Parts .....................
                part = new JSONObject();
                String[] columnNames = c.getColumnNames();
                for (String column : columnNames) {
                    try {
                        if (column.equalsIgnoreCase(Mms.Part._ID)) {
                            continue;
                        }
                        String value = c.getString(c.getColumnIndex(column));
                        part.put(column, value);

                        if (column.equalsIgnoreCase(Mms.Part._DATA) && value != null) {
                            // Loading Parts data
                            long partId = c.getLong(c.getColumnIndex(Mms.Part._ID));
                            Uri partURI = Uri.parse("content://" + Mms.CONTENT_URI.getAuthority() + "/part/"
                                    + partId);
                            part.put("_data", partURI);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                msg.getParts().add(part);
            }
            c.close();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "MessageID ::" + msgId + "loaded MMS Successfully...");
            }
        }
        return msg;
    }

    public long addSMS(BackUpMessage message, long threadId) {

        ContentValues values = getValues(message.getPduData(), supportedSMSColumns);
        values.put(Sms.THREAD_ID, threadId);
        return insert(VZUris.getSmsUri(), values);

    }

    /**
     * This Method checks the existence of message in native database
     * 
     * @return value
     */
    public boolean doesSMSExists(BackUpMessage message) {
        HashMap<String, String> pduValues = message.getPduData();
        String address = pduValues.get("address");
        String bodyString = pduValues.get("body");
        String body = SmilHelper.removeEscapeChar(bodyString);
        long date = Long.parseLong(pduValues.get("date"));
        boolean flag = false;
        String selectionArgs[] = { address, body, (new StringBuilder()).append("").append(date).toString() };
        try {
            Cursor cursor = mContext.getContentResolver().query(VZUris.getSmsUri(), null, msgSelectionString,
                    selectionArgs, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    flag = true;
                }
                cursor.close();
            }
        } catch (Exception e) {

        }
        return flag;
    }

	public boolean doesMMSExists(BackUpMessage message) {

		String value = null;
		String[] projection = null;
		String selection = null;
		String[] selectionArgs = null;
		Cursor cursor = null;
		// Check if MMS downloaded or delivery msg
		String mTypeString = (String) message.getPduData()
				.get(Mms.MESSAGE_TYPE);
		int mType = Integer.valueOf(mTypeString);
		if (mType == PduHeaders.MESSAGE_TYPE_SEND_REQ) {
			value = message.getPduData().get(Mms.TRANSACTION_ID);
			if (value != null) {
				projection = new String[] { Mms._ID, Mms.TRANSACTION_ID };
				selection = Mms.TRANSACTION_ID + " =? ";	
				selectionArgs = new String[] { value };
				try {
					cursor = mContext.getContentResolver().query(VZUris.getMmsUri(),
							projection, selection, selectionArgs, null);
					if (cursor != null) {
						if (cursor.getCount() != 0) {
							value = message.getPduData().get(Mms.DATE);
							projection = new String[] { Mms._ID, Mms.DATE };
							selection = Mms.DATE + " =? ";
							selectionArgs = new String[] { value };
							cursor = mContext.getContentResolver().query(VZUris.getMmsUri(),
									projection, selection, selectionArgs, null);
							if (cursor != null) {
								if (cursor.getCount() != 0) {
									return true;
								} else {
									return false;
								}
							} else {
								return false;
							}
							
						} else {
							return false;
						}
					}
				} catch (Exception e) {
					if (Logger.IS_ERROR_ENABLED) {
						Logger.error(e);
					}
				} finally {
					if (cursor != null) {
						cursor.close();
					}
				}
			}
		} else if (mType == PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF) {
			value = message.getPduData().get(Mms.MESSAGE_ID);
			if (value != null) {
				projection = new String[] { Mms._ID, Mms.MESSAGE_ID };
				selection = Mms.MESSAGE_ID + " =? ";
				selectionArgs = new String[] { value };
				try {
					cursor = mContext.getContentResolver().query(
							VZUris.getMmsUri(), projection, selection,
							selectionArgs, null);
					if (cursor != null) {
						if (cursor.getCount() != 0) {
							return true;
						} else {
							return false;
						}
					}
				} catch (Exception e) {
					if (Logger.IS_ERROR_ENABLED) {
						Logger.error(e);
					}
				} finally {
					if (cursor != null) {
						cursor.close();
					}
				}
			}
		}
		return false;
	}
    
   
    public long addMMS(BackUpMessage message, long threadId) throws SQLException, Exception {
        ContentValues values = getValues(message.getPduData(), supportedPDUColumns);
        values.put(Mms.THREAD_ID, threadId);
        long msgID = insert(Mms.CONTENT_URI, values);
        addMMSAddr(message, msgID);
        addMMSParts(message, msgID);
        return msgID;
    }
   
    private long insert(Uri uri, ContentValues values) {
		return ContentUris.parseId(mContext.getContentResolver().insert(uri,
				values));
	}
    
    private void addMMSParts(BackUpMessage message, long msgID) throws SQLException, Exception {
        ContentValues values = new ContentValues();
        String url = "content://" + Mms.CONTENT_URI.getAuthority() + "/" + msgID + "/part";
        ArrayList<HashMap<String, String>> partsList = message.getPartsData();

        for (HashMap<String, String> part : partsList) {
            values.clear();
            values.putAll(getValues(part, supportedPartsColumns));
            String backUpUri = null;
            if (values.containsKey("_data")) {
                backUpUri = values.getAsString("_data");
                values.remove("_data");
            }
            Uri partUri = mContext.getContentResolver().insert(Uri.parse(url), values);
            if (backUpUri != null) {

                try {
                    String contentType = values.getAsString(Part.CT_TYPE);
                    persistPartData(SaveRestoreActivity.rootFile + backUpUri, partUri, contentType);
                } catch (SQLException e) {
                    throw e;

                } catch (Exception e) {
                    throw e;
                } finally {

                }

            }
        }
    }

    /**
     * This Method
     * 
     * @throws MmsException
     */
    private void persistPartData(String backupUri, Uri uri, String contentType) throws SQLException,
            Exception {
        OutputStream os = null;
        InputStream is = null;
        File sourceFile = new File(backupUri);
        try {
            if (ContentType.TEXT_PLAIN.equals(contentType) || ContentType.APP_SMIL.equals(contentType)
                    || ContentType.TEXT_HTML.equals(contentType)) {
                ContentValues cv = new ContentValues();
                DataInputStream dis = null;
                FileInputStream fis = null;
                int size = (int) sourceFile.length();
                byte[] bytes = new byte[size];
                try {
                    fis = new FileInputStream(sourceFile);
                    dis = new DataInputStream(fis);
                    int read = 0;
                    int numRead = 0;
                    while (read < bytes.length && (numRead = dis.read(bytes, read, bytes.length - read)) >= 0) {
                        read = read + numRead;
                    }
                } catch (Exception e) {

                } finally {
                    if (dis != null) {
                        dis.close();
                    }
                    if (fis != null) {
                        fis.close();
                    }
                }

                if (bytes.length > 0) {
                    cv.put(Part.TEXT, new EncodedStringValue(bytes).getString());
                    if (mContext.getContentResolver().update(uri, cv, null, null) != 1) {
                        throw new IllegalArgumentException("unable to update " + uri.toString());
                    }
                }
            } else {
                if (os instanceof FileOutputStream) {
                    Logger.debug("**********************write to file *****************");
                }
                FileInputStream fis = null;
                try {
                    os = mContext.getContentResolver().openOutputStream(uri);
                    if (backupUri == null) {
                        Uri dataUri = uri;
                        if ((dataUri == null) || (dataUri == uri)) {
                            System.out.println("Can't find data for this part.");
                            return;
                        }
                        is = mContext.getContentResolver().openInputStream(dataUri);

                        byte[] buffer = new byte[256];
                        for (int len = 0; (len = is.read(buffer)) != -1;) {
                            os.write(buffer, 0, len);
                        }
                    } else {

                        fis = new FileInputStream(sourceFile);

                        byte[] buffer = new byte[1024];
                        int bytesRead;

                        while ((bytesRead = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }

                    }
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                    if (os != null) {
                        os.close();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Failed to open Input/Output stream." + e);
            throw new SQLException(e.getMessage());
        } catch (IOException e) {
            System.out.println("Failed to read/write data." + e);
            throw new SQLException(e.getMessage());
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    System.out.println("IOException while closing: " + os + e);
                } // Ignore
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    System.out.println("IOException while closing: " + is);
                } // Ignore
            }
        }
    }

    private void addMMSAddr(BackUpMessage message, long msgID) {
        ArrayList<HashMap<String, String>> addressList = message.getAddressData();
        String url = "content://" + Mms.CONTENT_URI.getAuthority() + "/" + msgID + "/addr";
        for (HashMap<String, String> adr : addressList) {
            // adding address
            mContext.getContentResolver().insert(Uri.parse(url), getValues(adr));
        }
    }

    private ContentValues getValues(HashMap<String, String> item, List<String> filter) {
        ContentValues values = new ContentValues();

        Set<String> keySet = item.keySet();
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (filter != null && !filter.contains(key)) {
                System.out.print("Ignoring Unsupported columns:" + key);
                continue;
            }
            if (key.equals("cid") || key.equals("_data")) {
                String token = item.get(key);
                String unTaggedString = token.substring(9, token.length() - 3);
                values.put(key, unTaggedString);
            } else if (key.equals("body") || key.equals("text")) {
                String outputString = "";

                String token = item.get(key);
                outputString = SmilHelper.removeEscapeChar(token);

                values.put(key, outputString);
            } else {
                values.put(key, item.get(key));
            }

        }
        return values;

    }

    private ContentValues getValues(HashMap<String, String> item) {
        ContentValues values = new ContentValues();
        Set<String> keySet = item.keySet();
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            values.put(key, item.get(key));
        }
        return values;
    }


    public String getRecipients(long threadId) {
        String[] projection = new String[] { Threads.RECIPIENT_IDS };
        String where = Threads._ID + "=" + threadId;
        String recipientsNumbers = null;
        Cursor c = mContext.getContentResolver().query(VZUris.getThreadsUri(), projection, where, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                recipientsNumbers = getCanonicalAddresses(c.getString(0));
            }
            c.close();
        }
        return recipientsNumbers;
    }

    /**
     * This Method
     * 
     * @param recipientIds
     * @return
     */
    private String getCanonicalAddresses(String recipientIds) {
        String[] recipients = recipientIds.split(" ");
        Uri uri = null;
        String recipientsMDN = "";
        for (String recipient : recipients) {
            if (!TextUtils.isEmpty(recipient)) {
                uri = Uri.withAppendedPath(VZUris.getMmsSmsCanonical(), "/" + recipient);
                Cursor c = mContext.getContentResolver().query(uri, null, null, null, null);
                if (c != null) {
                    while (c.moveToNext()) {
                        recipientsMDN += c.getString(c.getColumnIndex(Mms.Addr.ADDRESS)) + ";";
                    }
                    c.close();
                }
            }
        }
        return (recipientsMDN.length() > 0) ? recipientsMDN : null;
    }

    public long getThreadId(String recep) {
        if (recep != null) {
            String[] recipientsMDN = recep.split(";");
            HashSet<String> recipients = new HashSet<String>();
            for (String recipient : recipientsMDN) {
                recipients.add(recipient);
            }
            return Threads.getOrCreateThreadId(mContext, recipients);
        }
        return 0;
    }
    
    private boolean isGroupMessage(String recipients) {
    	boolean isGroup = false;
    	if (recipients != null) {
            String[] recipientsMDN = recipients.split(";");
            if (recipientsMDN.length > 1) {
               isGroup = true;
               return isGroup;	
            } 
        }
    	return isGroup; 
    }

    protected byte[] getPartsInBytes(Uri part) throws IOException {
        InputStream in = mContext.getContentResolver().openInputStream(part);
        byte[] buffer = new byte[1024];
        int bytesRead;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        in.close();
        out.flush();
        out.close();
        return out.toByteArray();
    }

    public static ArrayList<MessageInfo> getMessageIDs(Context context, Long threadID) {

        Cursor msgCursor = context.getContentResolver().query(Conversation.getUri(threadID),
                MessageListAdapter.PROJECTION, null, null, null);

        ArrayList<MessageInfo> messageInfo = new ArrayList<MessageInfo>();
        if (msgCursor != null) {
            if (msgCursor.getCount() > 0) {
                int noOfMsgs = msgCursor.getCount();
                msgCursor.moveToFirst();
                for (int j = 0; j < noOfMsgs; j++) {
                    String msgType = msgCursor.getString(msgCursor
                            .getColumnIndex(MmsSms.TYPE_DISCRIMINATOR_COLUMN));
                    boolean isSMS = false;
                    if (msgType.contains("sms")) {
                        isSMS = true;
                    }
                    messageInfo.add(new MessageInfo(msgCursor.getLong(1), isSMS));
                    msgCursor.moveToNext();

                }
            }
        }
        msgCursor.close();
        return messageInfo;
    }

    public ContentValues toContentValues(JSONObject data, List<String> filter) throws JSONException {
        ContentValues v = new ContentValues();
        @SuppressWarnings("unchecked")
        Iterator<String> iterator = data.keys();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            // IGNORING the unavailable columns
            if (filter != null && !filter.contains(key)) {
                System.out.print("Ignoring Unsupported columns:" + key);
                continue;
            }
            v.put(key, data.getString(key));
        }
        return v;
    }

    public ContentValues toContentValues(JSONObject data) throws JSONException {
        ContentValues v = new ContentValues();
        @SuppressWarnings("unchecked")
        Iterator<String> iterator = data.keys();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            v.put(key, data.getString(key));
        }
        return v;
    }
}