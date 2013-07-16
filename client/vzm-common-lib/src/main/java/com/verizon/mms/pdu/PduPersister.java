/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verizon.mms.pdu;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Addr;
import android.provider.Telephony.Mms.Part;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;


import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.ContentType;
import com.verizon.mms.InvalidHeaderValueException;
import com.verizon.mms.InvalidMessageException;
import com.verizon.mms.MmsException;
import com.verizon.mms.util.PduCache;
import com.verizon.mms.util.PduCacheEntry;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.mms.util.VZTelephony;

/**
 * This class is the high-level manager of PDU storage.
 */
public class PduPersister {
    private static final long DUMMY_THREAD_ID = Long.MAX_VALUE;

    /**
     * The uri of temporary drm objects.
     */
    public static final String TEMPORARY_DRM_OBJECT_URI = "content://" + VZUris.getMmsUri().getAuthority()
            + "/" + Long.MAX_VALUE + "/part";
    /**
     * Indicate that we transiently failed to process a MM.
     */
    public static final int PROC_STATUS_TRANSIENT_FAILURE = 1;
    /**
     * Indicate that we permanently failed to process a MM.
     */
    public static final int PROC_STATUS_PERMANENTLY_FAILURE = 2;
    /**
     * Indicate that we have successfully processed a MM.
     */
    public static final int PROC_STATUS_COMPLETED = 3;

    private static PduPersister sPersister;
    private static final PduCache PDU_CACHE_INSTANCE;

    private static final int[] ADDRESS_FIELDS = new int[] { PduHeaders.BCC, PduHeaders.CC, PduHeaders.FROM,
            PduHeaders.TO };

    private static String colPriority = Mms.PRIORITY; // try this value first
    private static final String COL_PRIORITY2 = "priority"; // fallback to this if above doesn't exist
    private static boolean fieldsInited;

    private static String[] PDU_PROJECTION = new String[] { Mms._ID, Mms.MESSAGE_BOX, Mms.THREAD_ID,
            Mms.RETRIEVE_TEXT, Mms.SUBJECT, Mms.CONTENT_LOCATION, Mms.CONTENT_TYPE, Mms.MESSAGE_CLASS,
            Mms.MESSAGE_ID, Mms.RESPONSE_TEXT, Mms.TRANSACTION_ID, Mms.CONTENT_CLASS, Mms.DELIVERY_REPORT,
            Mms.MESSAGE_TYPE, Mms.MMS_VERSION, colPriority, Mms.READ_REPORT, Mms.READ_STATUS,
            Mms.REPORT_ALLOWED, Mms.RETRIEVE_STATUS, Mms.STATUS, Mms.DATE, Mms.DELIVERY_TIME, Mms.EXPIRY,
            Mms.MESSAGE_SIZE, Mms.SUBJECT_CHARSET, Mms.RETRIEVE_TEXT_CHARSET, };

    private static final int PDU_COLUMN_ID = 0;
    private static final int PDU_COLUMN_MESSAGE_BOX = 1;
    private static final int PDU_COLUMN_THREAD_ID = 2;
    private static final int PDU_COLUMN_RETRIEVE_TEXT = 3;
    private static final int PDU_COLUMN_SUBJECT = 4;
    private static final int PDU_COLUMN_CONTENT_LOCATION = 5;
    private static final int PDU_COLUMN_CONTENT_TYPE = 6;
    private static final int PDU_COLUMN_MESSAGE_CLASS = 7;
    private static final int PDU_COLUMN_MESSAGE_ID = 8;
    private static final int PDU_COLUMN_RESPONSE_TEXT = 9;
    private static final int PDU_COLUMN_TRANSACTION_ID = 10;
    private static final int PDU_COLUMN_CONTENT_CLASS = 11;
    private static final int PDU_COLUMN_DELIVERY_REPORT = 12;
    private static final int PDU_COLUMN_MESSAGE_TYPE = 13;
    private static final int PDU_COLUMN_MMS_VERSION = 14;
    private static final int PDU_COLUMN_PRIORITY = 15;
    private static final int PDU_COLUMN_READ_REPORT = 16;
    private static final int PDU_COLUMN_READ_STATUS = 17;
    private static final int PDU_COLUMN_REPORT_ALLOWED = 18;
    private static final int PDU_COLUMN_RETRIEVE_STATUS = 19;
    private static final int PDU_COLUMN_STATUS = 20;
    private static final int PDU_COLUMN_DATE = 21;
    private static final int PDU_COLUMN_DELIVERY_TIME = 22;
    private static final int PDU_COLUMN_EXPIRY = 23;
    private static final int PDU_COLUMN_MESSAGE_SIZE = 24;
    private static final int PDU_COLUMN_SUBJECT_CHARSET = 25;
    private static final int PDU_COLUMN_RETRIEVE_TEXT_CHARSET = 26;

    private static final String[] PART_PROJECTION = new String[] { Part._ID, Part.CHARSET,
            Part.CONTENT_DISPOSITION, Part.CONTENT_ID, Part.CONTENT_LOCATION, Part.CONTENT_TYPE,
            Part.FILENAME, Part.NAME, Part.TEXT };

    private static final int PART_COLUMN_ID = 0;
    private static final int PART_COLUMN_CHARSET = 1;
    private static final int PART_COLUMN_CONTENT_DISPOSITION = 2;
    private static final int PART_COLUMN_CONTENT_ID = 3;
    private static final int PART_COLUMN_CONTENT_LOCATION = 4;
    private static final int PART_COLUMN_CONTENT_TYPE = 5;
    private static final int PART_COLUMN_FILENAME = 6;
    private static final int PART_COLUMN_NAME = 7;
    private static final int PART_COLUMN_TEXT = 8;

    private static final HashMap<Uri, Integer> MESSAGE_BOX_MAP;
    // These map are used for convenience in persist() and load().
    private static final HashMap<Integer, Integer> CHARSET_COLUMN_INDEX_MAP;
    private static final HashMap<Integer, Integer> ENCODED_STRING_COLUMN_INDEX_MAP;
    private static final HashMap<Integer, Integer> TEXT_STRING_COLUMN_INDEX_MAP;
    private static final HashMap<Integer, Integer> OCTET_COLUMN_INDEX_MAP;
    private static final HashMap<Integer, Integer> LONG_COLUMN_INDEX_MAP;
    private static final HashMap<Integer, String> CHARSET_COLUMN_NAME_MAP;
    private static final HashMap<Integer, String> ENCODED_STRING_COLUMN_NAME_MAP;
    private static final HashMap<Integer, String> TEXT_STRING_COLUMN_NAME_MAP;
    private static final HashMap<Integer, String> OCTET_COLUMN_NAME_MAP;
    private static final HashMap<Integer, String> LONG_COLUMN_NAME_MAP;

    static {
        MESSAGE_BOX_MAP = new HashMap<Uri, Integer>();
        MESSAGE_BOX_MAP.put(VZUris.getMmsInboxUri(), Mms.MESSAGE_BOX_INBOX);
        MESSAGE_BOX_MAP.put(VZUris.getMmsSentUri(), Mms.MESSAGE_BOX_SENT);
        MESSAGE_BOX_MAP.put(VZUris.getMmsDraftsUri(), Mms.MESSAGE_BOX_DRAFTS);
        MESSAGE_BOX_MAP.put(VZUris.getMmsOutboxUri(), Mms.MESSAGE_BOX_OUTBOX);

        CHARSET_COLUMN_INDEX_MAP = new HashMap<Integer, Integer>();
        CHARSET_COLUMN_INDEX_MAP.put(PduHeaders.SUBJECT, PDU_COLUMN_SUBJECT_CHARSET);
        CHARSET_COLUMN_INDEX_MAP.put(PduHeaders.RETRIEVE_TEXT, PDU_COLUMN_RETRIEVE_TEXT_CHARSET);

        CHARSET_COLUMN_NAME_MAP = new HashMap<Integer, String>();
        CHARSET_COLUMN_NAME_MAP.put(PduHeaders.SUBJECT, Mms.SUBJECT_CHARSET);
        CHARSET_COLUMN_NAME_MAP.put(PduHeaders.RETRIEVE_TEXT, Mms.RETRIEVE_TEXT_CHARSET);

        // Encoded string field code -> column index/name map.
        ENCODED_STRING_COLUMN_INDEX_MAP = new HashMap<Integer, Integer>();
        ENCODED_STRING_COLUMN_INDEX_MAP.put(PduHeaders.RETRIEVE_TEXT, PDU_COLUMN_RETRIEVE_TEXT);
        ENCODED_STRING_COLUMN_INDEX_MAP.put(PduHeaders.SUBJECT, PDU_COLUMN_SUBJECT);

        ENCODED_STRING_COLUMN_NAME_MAP = new HashMap<Integer, String>();
        ENCODED_STRING_COLUMN_NAME_MAP.put(PduHeaders.RETRIEVE_TEXT, Mms.RETRIEVE_TEXT);
        ENCODED_STRING_COLUMN_NAME_MAP.put(PduHeaders.SUBJECT, Mms.SUBJECT);

        // Text string field code -> column index/name map.
        TEXT_STRING_COLUMN_INDEX_MAP = new HashMap<Integer, Integer>();
        TEXT_STRING_COLUMN_INDEX_MAP.put(PduHeaders.CONTENT_LOCATION, PDU_COLUMN_CONTENT_LOCATION);
        TEXT_STRING_COLUMN_INDEX_MAP.put(PduHeaders.CONTENT_TYPE, PDU_COLUMN_CONTENT_TYPE);
        TEXT_STRING_COLUMN_INDEX_MAP.put(PduHeaders.MESSAGE_CLASS, PDU_COLUMN_MESSAGE_CLASS);
        TEXT_STRING_COLUMN_INDEX_MAP.put(PduHeaders.MESSAGE_ID, PDU_COLUMN_MESSAGE_ID);
        TEXT_STRING_COLUMN_INDEX_MAP.put(PduHeaders.RESPONSE_TEXT, PDU_COLUMN_RESPONSE_TEXT);
        TEXT_STRING_COLUMN_INDEX_MAP.put(PduHeaders.TRANSACTION_ID, PDU_COLUMN_TRANSACTION_ID);

        TEXT_STRING_COLUMN_NAME_MAP = new HashMap<Integer, String>();
        TEXT_STRING_COLUMN_NAME_MAP.put(PduHeaders.CONTENT_LOCATION, Mms.CONTENT_LOCATION);
        TEXT_STRING_COLUMN_NAME_MAP.put(PduHeaders.CONTENT_TYPE, Mms.CONTENT_TYPE);
        TEXT_STRING_COLUMN_NAME_MAP.put(PduHeaders.MESSAGE_CLASS, Mms.MESSAGE_CLASS);
        TEXT_STRING_COLUMN_NAME_MAP.put(PduHeaders.MESSAGE_ID, Mms.MESSAGE_ID);
        TEXT_STRING_COLUMN_NAME_MAP.put(PduHeaders.RESPONSE_TEXT, Mms.RESPONSE_TEXT);
        TEXT_STRING_COLUMN_NAME_MAP.put(PduHeaders.TRANSACTION_ID, Mms.TRANSACTION_ID);

        // Octet field code -> column index/name map.
        OCTET_COLUMN_INDEX_MAP = new HashMap<Integer, Integer>();
        OCTET_COLUMN_INDEX_MAP.put(PduHeaders.CONTENT_CLASS, PDU_COLUMN_CONTENT_CLASS);
        OCTET_COLUMN_INDEX_MAP.put(PduHeaders.DELIVERY_REPORT, PDU_COLUMN_DELIVERY_REPORT);
        OCTET_COLUMN_INDEX_MAP.put(PduHeaders.MESSAGE_TYPE, PDU_COLUMN_MESSAGE_TYPE);
        OCTET_COLUMN_INDEX_MAP.put(PduHeaders.MMS_VERSION, PDU_COLUMN_MMS_VERSION);
        OCTET_COLUMN_INDEX_MAP.put(PduHeaders.PRIORITY, PDU_COLUMN_PRIORITY);
        OCTET_COLUMN_INDEX_MAP.put(PduHeaders.READ_REPORT, PDU_COLUMN_READ_REPORT);
        OCTET_COLUMN_INDEX_MAP.put(PduHeaders.READ_STATUS, PDU_COLUMN_READ_STATUS);
        OCTET_COLUMN_INDEX_MAP.put(PduHeaders.REPORT_ALLOWED, PDU_COLUMN_REPORT_ALLOWED);
        OCTET_COLUMN_INDEX_MAP.put(PduHeaders.RETRIEVE_STATUS, PDU_COLUMN_RETRIEVE_STATUS);
        OCTET_COLUMN_INDEX_MAP.put(PduHeaders.STATUS, PDU_COLUMN_STATUS);

        OCTET_COLUMN_NAME_MAP = new HashMap<Integer, String>();
        OCTET_COLUMN_NAME_MAP.put(PduHeaders.CONTENT_CLASS, Mms.CONTENT_CLASS);
        OCTET_COLUMN_NAME_MAP.put(PduHeaders.DELIVERY_REPORT, Mms.DELIVERY_REPORT);
        OCTET_COLUMN_NAME_MAP.put(PduHeaders.MESSAGE_TYPE, Mms.MESSAGE_TYPE);
        OCTET_COLUMN_NAME_MAP.put(PduHeaders.MMS_VERSION, Mms.MMS_VERSION);
        OCTET_COLUMN_NAME_MAP.put(PduHeaders.PRIORITY, colPriority);
        OCTET_COLUMN_NAME_MAP.put(PduHeaders.READ_REPORT, Mms.READ_REPORT);
        OCTET_COLUMN_NAME_MAP.put(PduHeaders.READ_STATUS, Mms.READ_STATUS);
        OCTET_COLUMN_NAME_MAP.put(PduHeaders.REPORT_ALLOWED, Mms.REPORT_ALLOWED);
        OCTET_COLUMN_NAME_MAP.put(PduHeaders.RETRIEVE_STATUS, Mms.RETRIEVE_STATUS);
        OCTET_COLUMN_NAME_MAP.put(PduHeaders.STATUS, Mms.STATUS);

        // Long field code -> column index/name map.
        LONG_COLUMN_INDEX_MAP = new HashMap<Integer, Integer>();
        LONG_COLUMN_INDEX_MAP.put(PduHeaders.DATE, PDU_COLUMN_DATE);
        LONG_COLUMN_INDEX_MAP.put(PduHeaders.DELIVERY_TIME, PDU_COLUMN_DELIVERY_TIME);
        LONG_COLUMN_INDEX_MAP.put(PduHeaders.EXPIRY, PDU_COLUMN_EXPIRY);
        LONG_COLUMN_INDEX_MAP.put(PduHeaders.MESSAGE_SIZE, PDU_COLUMN_MESSAGE_SIZE);

        LONG_COLUMN_NAME_MAP = new HashMap<Integer, String>();
        LONG_COLUMN_NAME_MAP.put(PduHeaders.DATE, Mms.DATE);
        LONG_COLUMN_NAME_MAP.put(PduHeaders.DELIVERY_TIME, Mms.DELIVERY_TIME);
        LONG_COLUMN_NAME_MAP.put(PduHeaders.EXPIRY, Mms.EXPIRY);
        LONG_COLUMN_NAME_MAP.put(PduHeaders.MESSAGE_SIZE, Mms.MESSAGE_SIZE);

        PDU_CACHE_INSTANCE = PduCache.getInstance();
    }

    private final Context mContext;
    private final ContentResolver mContentResolver;
    
    private PduPersister(Context context) {
        mContext = context;
        mContentResolver = context.getContentResolver();
    }

    /** Get(or create if not exist) an instance of PduPersister */
    public static PduPersister getPduPersister(Context context) {
        if ((sPersister == null) || !context.equals(sPersister.mContext)) {
            sPersister = new PduPersister(context);
        }

        return sPersister;
    }

    /**
     * Set proper values for fields with variant names.
     * 
     * @param uri
     *            Valid MMS message uri
     * @param returnCursor
     *            True if cursor is to be returned
     * @return Cursor with PDU_PROJECTION if returnCursor is true
     * @throws MmsException
     */
    private Cursor initFields(Uri uri, boolean returnCursor) throws MmsException {
        Cursor cursor = null;
        if (!fieldsInited || returnCursor) {
            fieldsInited = true;
            boolean first = true;
            while (true) {
                try {
                    cursor = SqliteWrapper.query(mContext, mContentResolver, uri, PDU_PROJECTION, null, null,
                            null);
                    if (!returnCursor && cursor != null) {
                        try {
                            cursor.close();
                            cursor = null;
                        } catch (Exception e) {
                        }
                    }
                    break;
                } catch (SQLiteException e) {
                    if (first) {
                        // try other priority field
                        colPriority = COL_PRIORITY2;
                        PDU_PROJECTION[PDU_COLUMN_PRIORITY] = COL_PRIORITY2;
                        OCTET_COLUMN_NAME_MAP.put(PduHeaders.PRIORITY, COL_PRIORITY2);
                        first = false;
                    } else {
                        throw new MmsException(e);
                    }
                }
            }
        }
        return cursor;
    }

    private void setEncodedStringValueToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn) {
        String s = c.getString(columnIndex);
        if ((s != null) && (s.length() > 0)) {
            int charsetColumnIndex = CHARSET_COLUMN_INDEX_MAP.get(mapColumn);
            int charset = c.getInt(charsetColumnIndex);
            EncodedStringValue value = new EncodedStringValue(charset, getBytes(s));
            headers.setEncodedStringValue(value, mapColumn);
        }
    }

    private void setTextStringToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn) {
        String s = c.getString(columnIndex);
        if (s != null) {
            headers.setTextString(getBytes(s), mapColumn);
        }
    }

    private void setOctetToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn)
            throws InvalidHeaderValueException {
        if (!c.isNull(columnIndex)) {
            int b = c.getInt(columnIndex);
            headers.setOctet(b, mapColumn);
        }
    }

    private void setLongToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn) {
        if (!c.isNull(columnIndex)) {
            long l = c.getLong(columnIndex);
            headers.setLongInteger(l, mapColumn);
        }
    }

    private Integer getIntegerFromPartColumn(Cursor c, int columnIndex) {
        if (!c.isNull(columnIndex)) {
            return c.getInt(columnIndex);
        }
        return null;
    }

    private byte[] getByteArrayFromPartColumn(Cursor c, int columnIndex) {
        if (!c.isNull(columnIndex)) {
            return getBytes(c.getString(columnIndex));
        }
        return null;
    }

    private PduPart[] loadParts(long msgId) throws MmsException {
        Cursor c = SqliteWrapper.query(mContext, mContentResolver,
                Uri.parse("content://" + VZUris.getMmsUri().getAuthority() + "/" + msgId + "/part"),
                PART_PROJECTION, null, null, null);

        PduPart[] parts = null;

        try {
            if ((c == null) || (c.getCount() == 0)) {
                if (Logger.IS_DEBUG_ENABLED) {
                	Logger.debug(getClass(), "loadParts(" + msgId + "): no part to load.");
                }
                return null;
            }

            int partCount = c.getCount();
            int partIdx = 0;
            parts = new PduPart[partCount];
            while (c.moveToNext()) {
                PduPart part = new PduPart();
                Integer charset = getIntegerFromPartColumn(c, PART_COLUMN_CHARSET);
                if (charset != null) {
                    part.setCharset(charset);
                }

                byte[] contentDisposition = getByteArrayFromPartColumn(c, PART_COLUMN_CONTENT_DISPOSITION);
                if (contentDisposition != null) {
                    part.setContentDisposition(contentDisposition);
                }

                byte[] contentId = getByteArrayFromPartColumn(c, PART_COLUMN_CONTENT_ID);
                if (contentId != null) {
                    part.setContentId(contentId);
                }

                byte[] contentLocation = getByteArrayFromPartColumn(c, PART_COLUMN_CONTENT_LOCATION);
                if (contentLocation != null) {
                    part.setContentLocation(contentLocation);
                }

                byte[] contentType = getByteArrayFromPartColumn(c, PART_COLUMN_CONTENT_TYPE);
                if (contentType != null) {
                    part.setContentType(contentType);
                } else {
                    throw new MmsException("Content-Type must be set.");
                }

                byte[] fileName = getByteArrayFromPartColumn(c, PART_COLUMN_FILENAME);
                if (fileName != null) {
                    part.setFilename(fileName);
                }

                byte[] name = getByteArrayFromPartColumn(c, PART_COLUMN_NAME);
                if (name != null) {
                    part.setName(name);
                }

                // Construct a Uri for this part.
                long partId = c.getLong(PART_COLUMN_ID);
                Uri partURI = Uri.parse("content://" + VZUris.getMmsUri().getAuthority() + "/part/" + partId);
                part.setDataUri(partURI);

                // For images/audio/video, we won't keep their data in Part
                // because their renderer accept Uri as source.
                String type = toIsoString(contentType);
                if (!ContentType.isImageType(type) && !ContentType.isAudioType(type)
                        && !ContentType.isVideoType(type)) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    InputStream is = null;
 
                    // Store simple string values directly in the database instead of an
                    // external file. This makes the text searchable and retrieval slightly
                    // faster.
                    if (ContentType.TEXT_PLAIN.equals(type) || ContentType.APP_SMIL.equals(type)
                            || ContentType.TEXT_HTML.equals(type)) {
                        String text = c.getString(PART_COLUMN_TEXT);
                        byte[] blob = new EncodedStringValue(text != null ? text : "").getTextString();
                        baos.write(blob, 0, blob.length);
                    } else {

                        try {
                            is = new BufferedInputStream(mContentResolver.openInputStream(partURI), 4096);
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = is.read(buffer)) >= 0) {
                                baos.write(buffer, 0, len);
                            }
                        } catch (IOException e) {
                        	Logger.error(getClass(), "Failed to load part data", e);
                            c.close();
                            throw new MmsException(e);
                        } finally {
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (IOException e) {
                                	Logger.error(getClass(), "Failed to close stream", e);
                                } // Ignore
                            }
                        }
                    }
                    part.setData(baos.toByteArray());
                }
                parts[partIdx++] = part;
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return parts;
    }

    private void loadAddress(long msgId, PduHeaders headers) {
        Cursor c = SqliteWrapper.query(mContext, mContentResolver,
                Uri.parse("content://" + VZUris.getMmsUri().getAuthority() + "/" + msgId + "/addr"),
                new String[] { Addr.ADDRESS, Addr.CHARSET, Addr.TYPE }, null, null, null);

        if (c != null) {
            try {
                while (c.moveToNext()) {
                    String addr = c.getString(0);
                    if (!TextUtils.isEmpty(addr)) {
                        int addrType = c.getInt(2);
                        switch (addrType) {
                        case PduHeaders.FROM:
                            headers.setEncodedStringValue(
                                    new EncodedStringValue(c.getInt(1), getBytes(addr)), addrType);
                            break;
                        case PduHeaders.TO:
                        case PduHeaders.CC:
                        case PduHeaders.BCC:
                            headers.appendEncodedStringValue(new EncodedStringValue(c.getInt(1),
                                    getBytes(addr)), addrType);
                            break;
                        default:
                        	Logger.error(getClass(), "Unknown address type: " + addrType);
                            break;
                        }
                    }
                }
            } finally {
                c.close();
            }
        }
    }

    /**
     * Load a PDU from storage by given Uri.
     * 
     * @param uri
     *            The Uri of the PDU to be loaded.
     * @return A generic PDU object, it may be cast to dedicated PDU.
     * @throws MmsException
     *             Failed to load some fields of a PDU.
     */
    public GenericPdu load(Uri uri) throws MmsException {
        PduCacheEntry cacheEntry = PDU_CACHE_INSTANCE.get(uri);
        if (cacheEntry != null) {
            return cacheEntry.getPdu();
        }

        Cursor c = initFields(uri, true);
        PduHeaders headers = new PduHeaders();
        Set<Entry<Integer, Integer>> set;
        long msgId = ContentUris.parseId(uri);
        int msgBox;
        long threadId;

        try {
            if ((c == null) || (c.getCount() != 1) || !c.moveToFirst()) {
            	if (c != null) {
            		c.close();
            		c = null;
            	}
                throw new InvalidMessageException("Failed to load uri: " + uri);
            }

            msgBox = c.getInt(PDU_COLUMN_MESSAGE_BOX);
            threadId = c.getLong(PDU_COLUMN_THREAD_ID);

            set = ENCODED_STRING_COLUMN_INDEX_MAP.entrySet();
            for (Entry<Integer, Integer> e : set) {
                setEncodedStringValueToHeaders(c, e.getValue(), headers, e.getKey());
            }

            set = TEXT_STRING_COLUMN_INDEX_MAP.entrySet();
            for (Entry<Integer, Integer> e : set) {
                setTextStringToHeaders(c, e.getValue(), headers, e.getKey());
            }

            set = OCTET_COLUMN_INDEX_MAP.entrySet();
            for (Entry<Integer, Integer> e : set) {
                setOctetToHeaders(c, e.getValue(), headers, e.getKey());
            }

            set = LONG_COLUMN_INDEX_MAP.entrySet();
            for (Entry<Integer, Integer> e : set) {
                setLongToHeaders(c, e.getValue(), headers, e.getKey());
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        // Check whether 'msgId' has been assigned a valid value.
        if (msgId == -1L) {
            throw new MmsException("Error! ID of the message: -1.");
        }

        // Load address information of the MM.
        loadAddress(msgId, headers);

        int msgType = headers.getOctet(PduHeaders.MESSAGE_TYPE);
        PduBody body = new PduBody();

        // For PDU which type is M_retrieve.conf or Send.req, we should
        // load multiparts and put them into the body of the PDU.
        if ((msgType == PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF)
                || (msgType == PduHeaders.MESSAGE_TYPE_SEND_REQ)
                || (msgType == PduHeaders.MESSAGE_TYPE_SEND_REQ_X)) {
            PduPart[] parts = loadParts(msgId);
            if (parts != null) {
                int partsNum = parts.length;
                for (int i = 0; i < partsNum; i++) {
                    body.addPart(parts[i]);
                }
            }
        }

        GenericPdu pdu = null;
        switch (msgType) {
        case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
        case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND_X:
            pdu = new NotificationInd(headers);
            break;
        case PduHeaders.MESSAGE_TYPE_DELIVERY_IND:
            pdu = new DeliveryInd(headers);
            break;
        case PduHeaders.MESSAGE_TYPE_READ_ORIG_IND:
            pdu = new ReadOrigInd(headers);
            break;
        case PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF:
            pdu = new RetrieveConf(headers, body);
            break;
        case PduHeaders.MESSAGE_TYPE_SEND_REQ:
        case PduHeaders.MESSAGE_TYPE_SEND_REQ_X:
            pdu = new SendReq(headers, body);
            break;
        case PduHeaders.MESSAGE_TYPE_ACKNOWLEDGE_IND:
            pdu = new AcknowledgeInd(headers);
            break;
        case PduHeaders.MESSAGE_TYPE_NOTIFYRESP_IND:
            pdu = new NotifyRespInd(headers);
            break;
        case PduHeaders.MESSAGE_TYPE_READ_REC_IND:
        case PduHeaders.MESSAGE_TYPE_READ_REC_IND_X:
            pdu = new ReadRecInd(headers);
            break;
        case PduHeaders.MESSAGE_TYPE_SEND_CONF:
        case PduHeaders.MESSAGE_TYPE_FORWARD_REQ:
        case PduHeaders.MESSAGE_TYPE_FORWARD_CONF:
        case PduHeaders.MESSAGE_TYPE_MBOX_STORE_REQ:
        case PduHeaders.MESSAGE_TYPE_MBOX_STORE_CONF:
        case PduHeaders.MESSAGE_TYPE_MBOX_VIEW_REQ:
        case PduHeaders.MESSAGE_TYPE_MBOX_VIEW_CONF:
        case PduHeaders.MESSAGE_TYPE_MBOX_UPLOAD_REQ:
        case PduHeaders.MESSAGE_TYPE_MBOX_UPLOAD_CONF:
        case PduHeaders.MESSAGE_TYPE_MBOX_DELETE_REQ:
        case PduHeaders.MESSAGE_TYPE_MBOX_DELETE_CONF:
        case PduHeaders.MESSAGE_TYPE_MBOX_DESCR:
        case PduHeaders.MESSAGE_TYPE_DELETE_REQ:
        case PduHeaders.MESSAGE_TYPE_DELETE_CONF:
        case PduHeaders.MESSAGE_TYPE_CANCEL_REQ:
        case PduHeaders.MESSAGE_TYPE_CANCEL_CONF:
            throw new MmsException("Unsupported PDU type: " + Integer.toHexString(msgType));

        default:
            throw new MmsException("Unrecognized PDU type: " + Integer.toHexString(msgType));
        }

        cacheEntry = new PduCacheEntry(pdu, msgBox, threadId);
        PDU_CACHE_INSTANCE.add(uri, cacheEntry);
        return pdu;
    }

    private void persistAddress(long msgId, int type, EncodedStringValue[] array) {
        ContentValues values = new ContentValues(3);

        for (EncodedStringValue addr : array) {
            values.clear(); // Clear all values first.
            values.put(Addr.ADDRESS, toIsoString(addr.getTextString()));
            values.put(Addr.CHARSET, addr.getCharacterSet());
            values.put(Addr.TYPE, type);

            final Uri uri = VZUris.getMmsAddrUri(msgId);
            SqliteWrapper.insert(mContext, mContentResolver, uri, values);
        }
    }

    public Uri persistPart(PduPart part, long msgId) throws MmsException {
        // OOM Issue Fix
        if (part.getPartsUri() != null) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "Updating the dummyId to message Id.oldMsgId=" + part.getPartsUri()
                        + ",newMsg=" + msgId);
            }
            // use the existing written parts just update the dummy id with the new ID
            ContentValues values = new ContentValues(1);
            values.put(Part.MSG_ID, msgId);
            int count = SqliteWrapper.update(mContext, mContentResolver, part.getPartsUri(), values, null, null);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "Parts updated. count= " + count);
            }
            return VZUris.getMmsPartsUri(msgId);

        } else {
            final Uri uri = VZUris.getMmsPartsUri(msgId);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "Persisting part");
            }

            ContentValues values = new ContentValues(8);

            int charset = part.getCharset();
            if (charset != 0) {
                values.put(Part.CHARSET, charset);
            }

            String contentType = null;
            if (part.getContentType() != null) {
                contentType = toIsoString(part.getContentType());
                values.put(Part.CONTENT_TYPE, contentType);
                // To ensure the SMIL part is always the first part.
                if (ContentType.APP_SMIL.equals(contentType)) {
                    values.put(Part.SEQ, -1);
                }
            } else {
                throw new MmsException("MIME type of the part must be set.");
            }

            if (part.getFilename() != null) {
                String fileName = new String(part.getFilename());
                values.put(Part.FILENAME, fileName);
            }

            if (part.getName() != null) {
                String name = new String(part.getName());
                values.put(Part.NAME, name);
            }

            Object value = null;
            if (part.getContentDisposition() != null) {
                value = toIsoString(part.getContentDisposition());
                values.put(Part.CONTENT_DISPOSITION, (String) value);
            }

            if (part.getContentId() != null) {
                value = toIsoString(part.getContentId());
                values.put(Part.CONTENT_ID, (String) value);
            }

            if (part.getContentLocation() != null) {
                value = toIsoString(part.getContentLocation());
                values.put(Part.CONTENT_LOCATION, (String) value);
            }

            Uri res = SqliteWrapper.insert(mContext, mContentResolver, uri, values);
            if (res == null) {
                throw new MmsException("Failed to persist part, return null.");
            }

            persistData(part, res, contentType);
            // After successfully store the data, we should update
            // the dataUri of the part.
            part.setDataUri(res);
            return res;

        }

    }

    /**
     * Save data of the part into storage. The source data may be given by a byte[] or a Uri. If it's a
     * byte[], directly save it into storage, otherwise load source data from the dataUri and then save it. If
     * the data is an image, we may scale down it according to user preference.
     * 
     * @param part
     *            The PDU part which contains data to be saved.
     * @param uri
     *            The URI of the part.
     * @param contentType
     *            The MIME type of the part.
     * @throws MmsException
     *             Cannot find source data or error occurred while saving the data.
     */
    private void persistData(PduPart part, Uri uri, String contentType) throws MmsException {
        OutputStream os = null;
        InputStream is = null;

        try {
            byte[] data = part.getData();
            if (ContentType.TEXT_PLAIN.equals(contentType) || ContentType.APP_SMIL.equals(contentType)
                    || ContentType.TEXT_HTML.equals(contentType)) {
                ContentValues cv = new ContentValues();
                cv.put(Telephony.Mms.Part.TEXT, new EncodedStringValue(data).getString());
                if (mContentResolver.update(uri, cv, null, null) != 1) {
                    throw new MmsException("unable to update " + uri.toString());
                }
            } else {
                os = mContentResolver.openOutputStream(uri);
                if (data == null) {
                    Uri dataUri = part.getDataUri();
                    if ((dataUri == null) || (dataUri == uri)) {
                    	Logger.warn(getClass(), "Can't find data for this part.");
                        return;
                    }
                    is = mContentResolver.openInputStream(dataUri);

                    if (Logger.IS_DEBUG_ENABLED) {
                    	Logger.debug(getClass(), "Saving data to: " + uri);
                    }

                    byte[] buffer = new byte[256];
                    for (int len = 0; (len = is.read(buffer)) != -1;) {
                        os.write(buffer, 0, len);
                    }
                } else {
                    if (Logger.IS_DEBUG_ENABLED) {
                    	Logger.debug(getClass(), "Saving data to: " + uri);
                    }
                    os.write(data);
                }
            }
        } catch (FileNotFoundException e) {
        	Logger.error(getClass(), "Failed to open Input/Output stream.", e);
            throw new MmsException(e);
        } catch (IOException e) {
        	Logger.error(getClass(), "Failed to read/write data.", e);
            throw new MmsException(e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                	Logger.error(getClass(), "IOException while closing: " + os, e);
                } // Ignore
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                	Logger.error(getClass(), "IOException while closing: " + is, e);
                } // Ignore
            }
        }
    }

    private void updateAddress(long msgId, int type, EncodedStringValue[] array) {
        // Delete old address information and then insert new ones.
        SqliteWrapper.delete(mContext, mContentResolver,
                Uri.parse("content://" + VZUris.getMmsUri().getAuthority() + "/" + msgId + "/addr"),
                Addr.TYPE + "=" + type, null);

        if (array != null) {
        	persistAddress(msgId, type, array);
        }
    }

    /**
     * Update headers of a SendReq.
     * 
     * @param uri
     *            The PDU which need to be updated.
     * @param pdu
     *            New headers.
     * @param fixPduType
     *            True if we should use MESSAGE_TYPE_SEND_REQ_X instead of MESSAGE_TYPE_SEND_REQ
     * @param overrideRecipients
     *            True if we should delete any previously saved recipients
     * @throws MmsException
     *             Bad URI or updating failed.
     */
    public void updateHeaders(Uri uri, SendReq sendReq, boolean fixPduType, boolean overrideRecipients) throws MmsException {
        PDU_CACHE_INSTANCE.purge(uri);

        initFields(uri, false);

        ContentValues values = new ContentValues(13);
        byte[] contentType = sendReq.getContentType();
        if (contentType != null) {
            values.put(Mms.CONTENT_TYPE, toIsoString(contentType));
        }

        long date = sendReq.getDate();
        if (date != -1) {
            values.put(Mms.DATE, date);
        }

        int deliveryReport = sendReq.getDeliveryReport();
        if (deliveryReport != 0) {
            values.put(Mms.DELIVERY_REPORT, deliveryReport);
        }

        long expiry = sendReq.getExpiry();
        if (expiry != -1) {
            values.put(Mms.EXPIRY, expiry);
        }

        byte[] msgClass = sendReq.getMessageClass();
        if (msgClass != null) {
            values.put(Mms.MESSAGE_CLASS, toIsoString(msgClass));
        }

        int priority = sendReq.getPriority();
        if (priority != 0) {
            values.put(colPriority, priority);
        }

        int readReport = sendReq.getReadReport();
        if (readReport != 0) {
            values.put(Mms.READ_REPORT, readReport);
        }

        byte[] transId = sendReq.getTransactionId();
        if (transId != null) {
            values.put(Mms.TRANSACTION_ID, toIsoString(transId));
        }

        EncodedStringValue subject = sendReq.getSubject();
        if (subject != null) {
            values.put(Mms.SUBJECT, toIsoString(subject.getTextString()));
            values.put(Mms.SUBJECT_CHARSET, subject.getCharacterSet());
        } else {
            values.put(Mms.SUBJECT, "");
        }

        long messageSize = sendReq.getMessageSize();
        if (messageSize > 0) {
            values.put(Mms.MESSAGE_SIZE, messageSize);
        }

        if (fixPduType) {
            values.put(Mms.MESSAGE_TYPE, PduHeaders.MESSAGE_TYPE_SEND_REQ_X);        	
        }

        final PduHeaders headers = sendReq.getPduHeaders();
        final HashSet<String> recipients = new HashSet<String>();
        final long msgId = ContentUris.parseId(uri);

        for (int addrType : ADDRESS_FIELDS) {
            EncodedStringValue[] array = null;
            if (addrType == PduHeaders.FROM) {
                EncodedStringValue v = headers.getEncodedStringValue(addrType);
                if (v != null) {
                    array = new EncodedStringValue[1];
                    array[0] = v;
                }
            } else {
                array = headers.getEncodedStringValues(addrType);
                if (array != null) {
	                for (EncodedStringValue v : array) {
	                    if (v != null) {
	                        recipients.add(v.getString());
	                    }
	                }
                }
            }

            if (array != null || (overrideRecipients && addrType != PduHeaders.FROM)) {
                updateAddress(msgId, addrType, array);
            }
        }

        long threadId = VZTelephony.getOrCreateThreadId(mContext, recipients);
        values.put(Mms.THREAD_ID, threadId);
        SqliteWrapper.update(mContext, mContentResolver, uri, values, null, null);
    }

    private void updatePart(Uri uri, PduPart part) throws MmsException {
        ContentValues values = new ContentValues(7);

        int charset = part.getCharset();
        if (charset != 0) {
            values.put(Part.CHARSET, charset);
        }

        String contentType = null;
        if (part.getContentType() != null) {
            contentType = toIsoString(part.getContentType());
            values.put(Part.CONTENT_TYPE, contentType);
        } else {
            throw new MmsException("MIME type of the part must be set.");
        }

        if (part.getFilename() != null) {
            String fileName = new String(part.getFilename());
            values.put(Part.FILENAME, fileName);
        }

        if (part.getName() != null) {
            String name = new String(part.getName());
            values.put(Part.NAME, name);
        }

        Object value = null;
        if (part.getContentDisposition() != null) {
            value = toIsoString(part.getContentDisposition());
            values.put(Part.CONTENT_DISPOSITION, (String) value);
        }

        if (part.getContentId() != null) {
            value = toIsoString(part.getContentId());
            values.put(Part.CONTENT_ID, (String) value);
        }

        if (part.getContentLocation() != null) {
            value = toIsoString(part.getContentLocation());
            values.put(Part.CONTENT_LOCATION, (String) value);
        }

        SqliteWrapper.update(mContext, mContentResolver, uri, values, null, null);

        // Only update the data when:
        // 1. New binary data supplied or
        // 2. The Uri of the part is different from the current one.
        if ((part.getData() != null) || (uri != part.getDataUri())) {
            persistData(part, uri, contentType);
        }
    }

    /**
     * Update all parts of a PDU.
     * 
     * @param uri
     *            The PDU which need to be updated.
     * @param body
     *            New message body of the PDU.
     * @throws MmsException
     *             Bad URI or updating failed.
     */
    public void updateParts(Uri uri, PduBody body) throws MmsException {
        PduCacheEntry cacheEntry = PDU_CACHE_INSTANCE.get(uri);
        if (cacheEntry != null) {
            ((MultimediaMessagePdu) cacheEntry.getPdu()).setBody(body);
        }

        ArrayList<PduPart> toBeCreated = new ArrayList<PduPart>();
        HashMap<Uri, PduPart> toBeUpdated = new HashMap<Uri, PduPart>();

        int partsNum = body.getPartsNum();
        StringBuilder filter = new StringBuilder().append('(');
        
        for (int i = 0; i < partsNum; i++) {
            PduPart part = body.getPart(i);
            Uri partUri = part.getDataUri();
            
            String auth = null;
            if (partUri != null) {
                auth = partUri.getAuthority();
            }
            
            if (auth == null || !auth.startsWith(VZUris.getMmsAuthority())) {  
                toBeCreated.add(part);
            } else {
                toBeUpdated.put(partUri, part);

                // Don't use 'i > 0' to determine whether we should append
                // 'AND' since 'i = 0' may be skipped in another branch.
                if (filter.length() > 1) {
                    filter.append(" AND ");
                }

                filter.append(Part._ID);
                filter.append("!=");
                DatabaseUtils.appendEscapedSQLString(filter, partUri.getLastPathSegment());
            }  
        }
        filter.append(')');

        long msgId = ContentUris.parseId(uri);

        // Remove the parts which doesn't exist anymore.
        SqliteWrapper.delete(mContext, mContentResolver,
                Uri.parse(VZUris.getMmsUri() + "/" + msgId + "/part"),
                filter.length() > 2 ? filter.toString() : null, null);

        // Create new parts which didn't exist before.
        for (PduPart part : toBeCreated) {
            persistPart(part, msgId);
        }

        // Update the modified parts.
        for (Map.Entry<Uri, PduPart> e : toBeUpdated.entrySet()) {
            updatePart(e.getKey(), e.getValue());
        }
    }

    public Uri persist(GenericPdu pdu, Uri uri ,boolean markAsRead) throws MmsException {
        return persist(pdu, uri, markAsRead,null);
    }
    
    public Uri persist(GenericPdu pdu, Uri uri) throws MmsException {
    	return persist(pdu, uri, false,null);
    }
    public Uri persist(GenericPdu pdu, Uri uri,String localMdn) throws MmsException {
        return persist(pdu, uri, false,localMdn);
    }
    
    /**
     * Persist a PDU object to specific location in the storage.
     * 
     * @param pdu
     *            The PDU object to be stored.
     * @param uri
     *            Where to store the given PDU object.
     * @param markAsRead
     *            if flag is true we mark the message as read
     * @return A Uri which can be used to access the stored PDU.
     */
    public Uri persist(GenericPdu pdu, Uri uri, boolean markAsRead, String localMdn) throws MmsException {
        if (uri == null) {
            throw new MmsException("Uri may not be null.");
        }

        initFields(uri, false);

        Integer msgBox = MESSAGE_BOX_MAP.get(uri);
        if (msgBox == null) {
            throw new MmsException("Bad destination, must be one of "
                    + "content://mms/inbox, content://mms/sent, "
                    + "content://mms/drafts, content://mms/outbox, " + "content://mms/temp.");
        }
        PDU_CACHE_INSTANCE.purge(uri);

        PduHeaders header = pdu.getPduHeaders();
        PduBody body = null;
        ContentValues values = new ContentValues();
        Set<Entry<Integer, String>> set;

        set = ENCODED_STRING_COLUMN_NAME_MAP.entrySet();
        for (Entry<Integer, String> e : set) {
            int field = e.getKey();
            EncodedStringValue encodedString = header.getEncodedStringValue(field);
            if (encodedString != null) {
                String charsetColumn = CHARSET_COLUMN_NAME_MAP.get(field);
                values.put(e.getValue(), toIsoString(encodedString.getTextString()));
                values.put(charsetColumn, encodedString.getCharacterSet());
            }
        }

        set = TEXT_STRING_COLUMN_NAME_MAP.entrySet();
        for (Entry<Integer, String> e : set) {
            byte[] text = header.getTextString(e.getKey());
            if (text != null) {
                values.put(e.getValue(), toIsoString(text));
            }
        }

        set = OCTET_COLUMN_NAME_MAP.entrySet();
        for (Entry<Integer, String> e : set) {
            int b = header.getOctet(e.getKey());
            if (b != 0) {
                values.put(e.getValue(), b);
            }
        }

        set = LONG_COLUMN_NAME_MAP.entrySet();
        for (Entry<Integer, String> e : set) {
            long l = header.getLongInteger(e.getKey());
            if (l != -1L) {
                values.put(e.getValue(), l);
            }
        }

        HashMap<Integer, EncodedStringValue[]> addressMap = new HashMap<Integer, EncodedStringValue[]>(
                ADDRESS_FIELDS.length);
        // Save address information.
        for (int addrType : ADDRESS_FIELDS) {
            EncodedStringValue[] array = null;
            if (addrType == PduHeaders.FROM) {
                EncodedStringValue v = header.getEncodedStringValue(addrType);
                if (v != null) {
                    array = new EncodedStringValue[1];
                    array[0] = v;
                }
            } else {
                array = header.getEncodedStringValues(addrType);
            }
            addressMap.put(addrType, array);
        }

        HashSet<String> recipients = new HashSet<String>();
        long threadId = (pdu.getThreadId() > 0)?pdu.getThreadId():DUMMY_THREAD_ID;
        
        int msgType = pdu.getMessageType();
        // Here we only allocate thread ID for M-Notification.ind,
        // M-Retrieve.conf and M-Send.req.
        // Some of other PDU types may be allocated a thread ID outside
        // this scope.
        if ((msgType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND)
                || (msgType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND_X)
                || (msgType == PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF)
                || (msgType == PduHeaders.MESSAGE_TYPE_SEND_REQ)
                || (msgType == PduHeaders.MESSAGE_TYPE_SEND_REQ_X)) {
            EncodedStringValue[] array = null;
            switch (msgType) {
            case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
            case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND_X:
            case PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF:
                // if there are multiple recipients then add the ones other than us
                final EncodedStringValue[] from = addressMap.get(PduHeaders.FROM);
                final EncodedStringValue[] to = addressMap.get(PduHeaders.TO);
                final EncodedStringValue[] cc = addressMap.get(PduHeaders.CC);
                final int tolen = (to == null ? 0 : to.length) + (cc == null ? 0 : cc.length);
                if (tolen > 1) {
                    array = new EncodedStringValue[tolen + (from == null ? 0 : from.length)];
                    int i = 0;
                    for (EncodedStringValue[] vals : new EncodedStringValue[][] { from, to, cc }) {
                        if (vals != null) {
                            final int len = vals.length;
                            for (int j = 0; j < len; ++j) {
                                // remove ourselves from the list
                                EncodedStringValue val = vals[j];
                                // XXX check emails as well?
//                                if (MessageUtils.isLocalNumber(val.getString())) {
                                if(localMdn!=null && PhoneNumberUtils.compare(val.getString(), localMdn)){
                                    // VMA tablet
                                    val = null;
                                }else if (isLocalNumber(val.getString())) {
                                    // Handset 
                                    val = null;
                                }
                                array[i++] = val;
                            }
                        }
                    }
                } else {
                    array = from;
                }

                if (array != null) {
                    for (EncodedStringValue v : array) {
                        if (v != null) {
                            recipients.add(v.getString());
                        }
                    }
                }
                break;

            case PduHeaders.MESSAGE_TYPE_SEND_REQ:
                for (int hdr : new int[] { PduHeaders.TO, PduHeaders.CC, PduHeaders.BCC }) {
                    array = addressMap.get(hdr);
                    if (array != null) {
                        for (EncodedStringValue v : array) {
                            if (v != null) {
                                recipients.add(v.getString());
                            }
                        }
                    }
                }
                break;
            }
            try {
            	//http://50.17.243.155/bugzilla/show_bug.cgi?id=1705
            	//jellybean crashes when forwarding an mms
            	if (Build.VERSION.SDK_INT >= 16  && recipients.size() == 0) {
            		threadId = -1;
            	} else {
            		threadId = VZTelephony.getOrCreateThreadId(mContext, recipients);
            	}
            } catch (Exception e) {
            	if ((msgType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) || (msgType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND_X)) {
					if (!(pdu instanceof MultimediaMessagePdu)) {
						/*try {
							SaveGetOrCreateThreadIDFailure.getInstance().saveReceivedMMSNotificationIND(values,	recipients, addressMap);
						} catch (IOException e1) {
							e1.printStackTrace();
						} catch (Exception e1) {
							e1.printStackTrace();
						}*/
					}
					return null;
				} else {
					throw new IllegalArgumentException(	"Unable to find or allocate a thread ID.", e);
				}

            }
            //  Reusing the threadId instead of Querying 
            if (threadId > 0) {
                pdu.setThreadId(threadId);
            }
        }
        // insert the threadId only if its value is greater than zero
        if (threadId > 0) {
        	values.put(Mms.THREAD_ID, threadId);
        }

        //set the read and seen flag of the message
        if (markAsRead || (msgType == PduHeaders.MESSAGE_TYPE_SEND_REQ)
                || (msgType == PduHeaders.MESSAGE_TYPE_SEND_REQ_X) ) {
        	values.put(Mms.READ, 1);
        	values.put(Mms.SEEN, 1);
        	
        	
        	values.put(OCTET_COLUMN_NAME_MAP.get(PduHeaders.READ_REPORT), PduHeaders.VALUE_NO);
        }
        // if this is a RetrieveConf or SendReq save the parts under a dummy ID so that
        // we can atomically attach them to the message below after it's created; we do
    	// this first since it can take a while, which shortens the timing hole until the
    	// message is fully persisted
        //
        int partsNum = 0;
        long dummyId = System.currentTimeMillis();
        if (pdu instanceof MultimediaMessagePdu) {
            body = ((MultimediaMessagePdu) pdu).getBody();
            if (body != null) {
                partsNum = body.getPartsNum();
                for (int i = 0; i < partsNum; i++) {
                    PduPart part = body.getPart(i);
                    persistPart(part, dummyId);
                }
            }
        }

		// create the message
		final Context context = mContext;
		final ContentResolver resolver = mContentResolver;
		final Uri msgUri = SqliteWrapper.insert(context, resolver, uri, values);
		if (msgUri == null) {
		    throw new MmsException("persist failed to insert the message");
		}
        final long msgId = ContentUris.parseId(msgUri);

        // save the addresses
        // since the message creation above triggers a notification to any cursor observers
        // they may load the message before/while we add the addresses, but they should handle
        // this condition relatively gracefully, and we will ensure that they are updated again
        // once the message is fully persisted
		//
        for (int addrType : ADDRESS_FIELDS) {
            EncodedStringValue[] array = addressMap.get(addrType);
            if (array != null) {
                persistAddress(msgId, addrType, array);
            }
        }

		// atomically update parts saved under the dummy ID with the real ID
        values = new ContentValues(1);
		if (partsNum != 0) {
	        values.put(Part.MSG_ID, msgId);
	        final Uri partsUri = VZUris.getMmsPartsUri(dummyId);
	        SqliteWrapper.update(context, resolver, partsUri, values, null, null);
		}

		// purge the message in case it was loaded before being completely persisted
		PDU_CACHE_INSTANCE.purge(msgUri);

		// force a cursor notification to ensure that observers update with the complete message
        resolver.notifyChange(VZUris.getMmsSmsUri(), null);

		// return the base uri appended with the new msg ID
		final Uri retUri = Uri.parse(uri + "/" + msgId);

		if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(getClass(), "persist: created " + retUri);
        }
        return retUri;
    }

    public void fixMessageTypeToMmsNotification(Uri uri) {
        ContentValues values = new ContentValues(1);
        values.put(Mms.MESSAGE_TYPE, PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND);
        int i = SqliteWrapper.update(mContext, mContentResolver,
                uri, values, null, null);
		if (Logger.IS_DEBUG_ENABLED)
			Logger.debug(PduPersister.class, "Fixed MessageType - of uri: " + uri + "updCount:" + i);
    }
    
    public void fixMessageTypeToMmsSend(Uri uri) {
        ContentValues values = new ContentValues(1);
        values.put(Mms.MESSAGE_TYPE, PduHeaders.MESSAGE_TYPE_SEND_REQ);
        int i = SqliteWrapper.update(mContext, mContentResolver,
                uri, values, null, null);
		if (Logger.IS_DEBUG_ENABLED)
			Logger.debug(PduPersister.class, "Fixed MessageType - of uri: " + uri + "updCount:" + i);
    }
    /**
     * Move a PDU object from one location to another.
     * 
     * @param from
     *            Specify the PDU object to be moved.
     * @param to
     *            The destination location, should be one of the following: "content://mms/inbox",
     *            "content://mms/sent", "content://mms/drafts", "content://mms/outbox", "content://mms/trash".
     * @return New Uri of the moved PDU.
     * @throws MmsException
     *             Error occurred while moving the message.
     */
    public Uri move(Uri from, Uri to) throws MmsException {
        // Check whether the 'msgId' has been assigned a valid value.
        long msgId = ContentUris.parseId(from);
        if (msgId == -1L) {
            throw new MmsException("Error! ID of the message: -1.");
        }

        // Get corresponding int value of destination box.
        Integer msgBox = MESSAGE_BOX_MAP.get(to);
        if (msgBox == null) {
            throw new MmsException("Bad destination, must be one of "
                    + "content://mms/inbox, content://mms/sent, "
                    + "content://mms/drafts, content://mms/outbox, " + "content://mms/temp.");
        }

        ContentValues values = new ContentValues(1);
        values.put(Mms.MESSAGE_BOX, msgBox);
        SqliteWrapper.update(mContext, mContentResolver, from, values, null, null);
        return ContentUris.withAppendedId(to, msgId);
    }

    /**
     * Wrap a byte[] into a String.
     */
    public static String toIsoString(byte[] bytes) {
        try {
            return new String(bytes, CharacterSets.MIMENAME_ISO_8859_1);
        } catch (UnsupportedEncodingException e) {
            // Impossible to reach here!
        	Logger.error(PduPersister.class, "ISO_8859_1 must be supported!", e);
            return "";
        }
    }

    /**
     * Unpack a given String into a byte[].
     */
    public static byte[] getBytes(String data) {
        try {
            return data.getBytes(CharacterSets.MIMENAME_ISO_8859_1);
        } catch (UnsupportedEncodingException e) {
            // Impossible to reach here!
        	Logger.error(PduPersister.class, "ISO_8859_1 must be supported!", e);
            return new byte[0];
        }
    }

    /**
     * Remove all objects in the temporary path.
     */
    public void release() {
        Uri uri = Uri.parse(TEMPORARY_DRM_OBJECT_URI);
        SqliteWrapper.delete(mContext, mContentResolver, uri, null, null);
    }

//    public void fixDueTimestampOfPendingMessage() {
//    	if (Logger.IS_DEBUG_ENABLED) {
//            Logger.debug(TransactionService.class, "Fixing timestamp - of all pending from PDU");
//    	}
//        Uri.Builder uriBuilder = VZUris.getNativeMmsSmsPendingUri().buildUpon();
//        uriBuilder.appendQueryParameter("protocol", "mms");
//
//        long dueTime = System.currentTimeMillis();
//            
//        String selection = PendingMessages.ERROR_TYPE + " < ?" + " AND " + PendingMessages.DUE_TIME + " <= ?";
//
//        String[] selectionArgs = new String[] { String.valueOf(MmsSms.ERR_TYPE_GENERIC_PERMANENT),
//                String.valueOf(dueTime) };
//
//        Cursor cursor = SqliteWrapper.query(mContext, mContentResolver, uriBuilder.build(), null, selection,
//                    selectionArgs, PendingMessages.DUE_TIME);
//            
//        ContentValues values = new ContentValues(1);
//        values.put(PendingMessages.DUE_TIME, dueTime+TransactionService.DELTA_TIME_TO_PREVENT_NATIVE);
//            
//        if (cursor != null) {
//        	if (Logger.IS_DEBUG_ENABLED) {
//        		Logger.debug(TransactionService.class, "Fixing timestamp - count = " + cursor.getCount());
//        	}
//        	try {
//        		while (cursor.moveToNext()) {
//        			int columnIndex = cursor.getColumnIndexOrThrow(PendingMessages._ID);
//        			long id = cursor.getLong(columnIndex);
//    				long msgDueTime = cursor.getLong(cursor.getColumnIndexOrThrow(PendingMessages.DUE_TIME));
//        	        int i = SqliteWrapper.update(mContext, mContentResolver, VZUris.getNativeMmsSmsPendingUri(),
//        					values, PendingMessages._ID + "=" + id, null);
//        			if (Logger.IS_DEBUG_ENABLED) {
//        				int columnIndexOfMsgId = cursor.getColumnIndexOrThrow(PendingMessages.MSG_ID);
//        				int columnIndexOfMsgType = cursor.getColumnIndexOrThrow(PendingMessages.MSG_TYPE);
//        				int msgType = cursor.getInt(columnIndexOfMsgType);					
//        				long msgId = cursor.getLong(columnIndexOfMsgId);
//        				String msgProto = cursor.getString(cursor.getColumnIndexOrThrow(PendingMessages.PROTO_TYPE));
//        				String msgErrType = cursor.getString(cursor.getColumnIndexOrThrow(PendingMessages.ERROR_TYPE));
//        				String msgErrCode = cursor.getString(cursor.getColumnIndexOrThrow(PendingMessages.ERROR_CODE));
//        				String msgRetryIndex = cursor.getString(cursor.getColumnIndexOrThrow(PendingMessages.RETRY_INDEX));
//
//        				Logger.debug(TransactionService.class, "Fixed pending row: msgId:" + msgId + " msgType:" + msgType 
//        						+ " rowId:" + id + " dueTime:" +  msgDueTime + " proto:" + msgProto + " errType:" + msgErrType 
//        						+ " errCode:" + msgErrCode + " retryIndex:" + msgRetryIndex + " updCount" + i);
//        			}
//        		}
//        	} catch (Exception e) {
//        		Logger.error(TransactionService.class, "SQLiteException in fixDueTimestamp", e);
//        	} finally {
//        		cursor.close();
//        	}
//        }
//    }

    /**
     * Find all messages to be sent or downloaded before certain time.
     */
    public Cursor getPendingMessages(long dueTime) {
        Uri.Builder uriBuilder = VZUris.getMmsSmsPendingUri().buildUpon();
        uriBuilder.appendQueryParameter("protocol", "mms");

//        String selection = PendingMessages.ERROR_TYPE + " < ?";
//
//        String[] selectionArgs = new String[] { String.valueOf(MmsSms.ERR_TYPE_GENERIC_PERMANENT),
//                };
        String selection = PendingMessages.ERROR_TYPE + " < ?" + " AND " + PendingMessages.DUE_TIME + " <= ?";
        
        String[] selectionArgs = new String[] { String.valueOf(MmsSms.ERR_TYPE_GENERIC_PERMANENT),
                String.valueOf(dueTime) };

        return SqliteWrapper.query(mContext, mContentResolver, uriBuilder.build(), null, selection,
                selectionArgs, PendingMessages.DUE_TIME);
    }

    /**
     * Class used to store the contentType of the media attachment and body of the mms PDU
     * @author admin
     *
     */
    public static class PduDetail {
    	public String contentType;
    	public String body;
    	public Uri uri;
    	
    	public PduDetail(String contentType, String body, Uri uri) {
    		this.contentType = contentType;
    		this.body = body;
    		this.uri = uri;
    		
    	}
    }
    
    /**
     * Fetch the media's contenttype and the body of the mms in one of the two ways: 
     * 1) if the pdu is already present in the cache use that pdu and fetch it
     * 2) load only the required parts from the parts table and use it
     * @param uri
     *            The Uri of the PDU to be loaded.
     * @return A generic PDU object, it may be cast to dedicated PDU.
     * @throws MmsException
     *             Failed to load some fields of a PDU.
     */
    public PduDetail getPduDetail(Uri uri) throws MmsException {
    	PduDetail mmsLastMsg = null;
    	PduCacheEntry cacheEntry = PDU_CACHE_INSTANCE.get(uri);
    	
    	if (cacheEntry != null) {
    		if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug("getPduDeail uri = " + uri + " found cacheEntry");
        	}
            mmsLastMsg = loadLastMsgFromPduBody(cacheEntry.getPdu());
        } else {
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug("getPduDeail uri = " + uri + " cacheEntry not found fetching req data from parts");
        	}
        	mmsLastMsg = loadReqMsgParts(uri);
        }
        
        return mmsLastMsg;
    }
    
    /**
     * Fetches the media type and the body of the mms message
     * @param uri
     * @return
     * @throws MmsException
     */
    private PduDetail loadReqMsgParts(Uri uri) throws MmsException {
    	final String[] LAST_MSG_PART_PROJECTION = new String[] {Part._ID, Part.CHARSET, Part.CONTENT_TYPE,
            Part.TEXT};
    	
    	long msgId = ContentUris.parseId(uri);
    	
    	Cursor c = SqliteWrapper.query(mContext, mContentResolver,
                Uri.parse("content://" + VZUris.getMmsUri().getAuthority() + "/" + msgId + "/part"),
                LAST_MSG_PART_PROJECTION, null, null, null);

    	final int PART_COLUMN_ID = 0;
    	final int PART_COLUMN_CHARSET = 1;
        final int PART_COLUMN_CONTENT_TYPE = 2;
        final int PART_COLUMN_TEXT = 3;
        
        PduDetail lastMsg = new PduDetail(null, null, null);
        
        try {
            if ((c == null) || (c.getCount() == 0)) {
                if (Logger.IS_DEBUG_ENABLED) {
                	Logger.debug(getClass(), "loadReqMsgParts(" + msgId + "): no part to load.");
                }
                return lastMsg;
            }

            while (c.moveToNext()) {
                String contentType = c.getString(PART_COLUMN_CONTENT_TYPE);
                
                if (Logger.IS_DEBUG_ENABLED) {
                	Logger.debug(getClass(), "loadReqMsgParts(" + msgId + "): contentType =" + contentType);
                }
                
                if (contentType.equals(ContentType.APP_SMIL)) {
        			continue;
        		}
                
                if (lastMsg.body == null && (ContentType.isPlainTextType(contentType) || 
        				ContentType.isHtmlTextType(contentType))) {
                	lastMsg.body = c.getString(PART_COLUMN_TEXT);
                } else if (lastMsg.contentType == null) {
                	Uri partURI = Uri.parse("content://" + VZUris.getMmsUri().getAuthority() + "/part/" + c.getLong(PART_COLUMN_ID));
                	lastMsg.uri = partURI;
                	lastMsg.contentType = contentType;
                }
                if (lastMsg.contentType != null && lastMsg.body != null) {
                	break;
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return lastMsg;
	}

    private PduDetail loadLastMsgFromPduBody(GenericPdu pdu) {
    	PduBody partBody = ((MultimediaMessagePdu)pdu).getBody();
    	PduDetail lastMsg = new PduDetail(null, null, null);
    	int num = partBody.getPartsNum();

    	PduPart pduPart;
    	for (int i = 0; i < num; i++) {
    		pduPart = partBody.getPart(i);

    		byte[] content = pduPart.getContentType();
    		if (content != null) {
    			String contentType = PduPersister.toIsoString(pduPart.getContentType());

    			if (Logger.IS_DEBUG_ENABLED) {
    				Logger.debug("loadLastMsgFromPduBody contentType = " + contentType);
    			}
    			
    			if (contentType.equals(ContentType.APP_SMIL)) {
    				continue;
    			}

    			if (lastMsg.body == null && (ContentType.isPlainTextType(contentType) || 
    					ContentType.isHtmlTextType(contentType))) {
    				int charSet = pduPart.getCharset();
    				if (charSet == CharacterSets.ANY_CHARSET) {
    					// By default, we use ISO_8859_1 to decode the data
    					// which character set wasn't set.
    					charSet = CharacterSets.ISO_8859_1;
    				}
    				lastMsg.body = extractTextFromData(pduPart.getData(), charSet).toString();
    			} else if (lastMsg.contentType == null) {
    				lastMsg.contentType = contentType;
    				lastMsg.uri = pduPart.getDataUri();
    			}

    			if (lastMsg.body != null && lastMsg.contentType != null) {
    				break;
    			}
    		}
    	}
    	return lastMsg;
    }
    
	/**
	 * Find all messages to be sent in future.
	 */
	public Cursor getAllPendingMessages() {
	    Uri.Builder uriBuilder = VZUris.getMmsSmsPendingUri().buildUpon();
	    uriBuilder.appendQueryParameter("protocol", "mms");
	
	//    String selection = PendingMessages.ERROR_TYPE + " < ?" ;
	//
	//    String[] selectionArgs = new String[] { String.valueOf(MmsSms.ERR_TYPE_GENERIC_PERMANENT)};
	    String selection = PendingMessages.ERROR_TYPE + " < ?";
	    
	    String[] selectionArgs = new String[] { String.valueOf(MmsSms.ERR_TYPE_GENERIC_PERMANENT) };
	
	    return SqliteWrapper.query(mContext, mContentResolver, uriBuilder.build(), null, selection,
	            selectionArgs, PendingMessages.DUE_TIME);
	}
	
	
	 public boolean isLocalNumber(String number) {
	        if (number == null) {
	            return false;
	        }
	    
	        // we don't use Mms.isEmailAddress() because it is too strict for comparing addresses like
	        // "foo+caf_=6505551212=tmomail.net@gmail.com", which is the 'from' address from a forwarded email
	        // message from Gmail. We don't want to treat "foo+caf_=6505551212=tmomail.net@gmail.com" and
	        // "6505551212" to be the same.
	        if (number.indexOf('@') >= 0) {
	            return false;
	        }
	        TelephonyManager tMgr =(TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
	        String phoneNumber = tMgr.getLine1Number();
	        return PhoneNumberUtils.compare(number, tMgr.getLine1Number());
	    }
	 
	  public  CharSequence extractTextFromData(byte[] data, int charset) {
	        if (data != null) {
	            try {
	                if (CharacterSets.ANY_CHARSET == charset) {
	                    return new String(data); // system default encoding.
	                } else {
	                    String name = CharacterSets.getMimeName(charset);
	                    return new String(data, name);
	                }
	            } catch (UnsupportedEncodingException e) {
	                Logger.error(PduPersister.class, "Unsupported encoding: " + charset, e);
	                return new String(data); // system default encoding.
	            }
	        }
	        return "";
	    }

    /**
     * This Method 
     * @param pduPart
     * @param msgId
     * @return
     * @throws MmsException 
     */
    public Uri persistPartFromStream(PduPart part, long msgId) throws MmsException {
        final Uri uri = VZUris.getMmsPartsUri(msgId);
        ContentValues values = new ContentValues(8);

        int charset = part.getCharset();
        if (charset != 0) {
            values.put(Part.CHARSET, charset);
        }

        String contentType = null;
        if (part.getContentType() != null) {
            contentType = toIsoString(part.getContentType());
            values.put(Part.CONTENT_TYPE, contentType);
            // To ensure the SMIL part is always the first part.
            if (ContentType.APP_SMIL.equals(contentType)) {
                values.put(Part.SEQ, -1);
            }
        } else {
            throw new MmsException("MIME type of the part must be set.");
        }

        if (part.getFilename() != null) {
            String fileName = new String(part.getFilename());
            values.put(Part.FILENAME, fileName);
        }

        if (part.getName() != null) {
            String name = new String(part.getName());
            values.put(Part.NAME, name);
        }

        Object value = null;
        if (part.getContentDisposition() != null) {
            value = toIsoString(part.getContentDisposition());
            values.put(Part.CONTENT_DISPOSITION, (String) value);
        }

        if (part.getContentId() != null) {
            value = toIsoString(part.getContentId());
            values.put(Part.CONTENT_ID, (String) value);
        }

        if (part.getContentLocation() != null) {
            value = toIsoString(part.getContentLocation());
            values.put(Part.CONTENT_LOCATION, (String) value);
        }

        Uri res = SqliteWrapper.insert(mContext, mContentResolver, uri, values);
        if (res == null) {
            throw new MmsException("Failed to persist part, return null.");
        }

        persistStreamData(part, res, contentType);
        
        
        
        
        // After successfully store the data, we should update
        // the dataUri of the part.
        part.setDataUri(res);
        return res;
    }

    /**
     * This Method 
     * @param part
     * @param res
     * @param contentType
     * @throws MmsException 
     */
    private void persistStreamData(PduPart part, Uri uri, String contentType) throws MmsException {
        OutputStream os = null;
        InputStream is = null;

        try {
            if (part.getInputStream() != null) {
                os = mContentResolver.openOutputStream(uri);
                is = part.getInputStream();
                byte[] buffer = new byte[1024];
                for (int len = 0; (len = is.read(buffer)) != -1;) {
                    os.write(buffer, 0, len);
                }
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "Saved stream data to: " + uri);
                }
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "No stream found : " + uri);
                }
            }

        } catch (FileNotFoundException e) {
            Logger.error(getClass(), "Failed to open Input/Output stream.", e);
            throw new MmsException(e);
        } catch (IOException e) {
            Logger.error(getClass(), "Failed to read/write data.", e);
            throw new MmsException(e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Logger.error(getClass(), "IOException while closing: " + os, e);
                } // Ignore
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Logger.error(getClass(), "IOException while closing: " + is, e);
                } // Ignore
            }
        }
    }

}
