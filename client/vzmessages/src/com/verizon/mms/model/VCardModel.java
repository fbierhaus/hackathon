/**
 * VCardModel.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.w3c.dom.events.Event;

import android.content.Context;
import android.drm.mobile1.DrmException;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MmsException;
import com.verizon.mms.drm.DrmWrapper;
import com.verizon.mms.pdu.CharacterSets;
import com.verizon.mms.ui.UriVcard;
import com.verizon.vcard.android.syncml.pim.VDataBuilder;
import com.verizon.vcard.android.syncml.pim.VNode;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct.ContactMethod;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct.OrganizationData;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct.PhoneData;
import com.verizon.vcard.android.syncml.pim.vcard.VCardException;
import com.verizon.vcard.android.syncml.pim.vcard.VCardParser;

/**
 * This class/interface   
 * @author Essack
 * @Since  May 14, 2012
 */
public class VCardModel extends RegionMediaModel {
    private String mFormattedMsg;
    private ContactStruct mContactStruct;
    private final int mCharset;
    private boolean mVcardLoadError = false;
    
    private final int PHONE_NUM_LIMIT = 3;

    private boolean vcardLoaded = false;
    
    public VCardModel(Context context, Uri uri, RegionModel region)
            throws MmsException {
        super(context, SmilHelper.ELEMENT_TAG_VCARD, uri, region);

        initModelFromUri(uri);

        mCharset = CharacterSets.UTF_8;
    }

    public VCardModel(Context context, String contentType, String src, int charset,
            Uri uri, RegionModel region) throws DrmException, MmsException {
        super(context, SmilHelper.ELEMENT_TAG_VCARD,
                contentType, src, uri, region);

        if (charset == CharacterSets.ANY_CHARSET) {
            // By default, we use ISO_8859_1 to decode the data
            // which character set wasn't set.
            charset = CharacterSets.ISO_8859_1;
        }
        mCharset = charset;
    }

    public VCardModel(Context context, String contentType, String src, int charset, 
            DrmWrapper wrapper, RegionModel regionModel) throws IOException {
        super(context, SmilHelper.ELEMENT_TAG_VCARD, contentType, src,
                wrapper, regionModel);

        if (charset == CharacterSets.ANY_CHARSET) {
            // By default, we use ISO_8859_1 to decode the data
            // which character set wasn't set.
            charset = CharacterSets.ISO_8859_1;
        }
        mCharset = charset;
    }

    /**
     * This Method 
     * @param uri
     * @throws MmsException 
     */
    private void initModelFromUri(Uri uri) throws MmsException {
        UriVcard uriVcard = new UriVcard(mContext, uri);

        mContentType = uriVcard.getContentType();
        if (TextUtils.isEmpty(mContentType)) {
            throw new MmsException("Type of media is unknown.");
        }
        mSrc = uriVcard.getSrc();

        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(getClass(), "New VCardModel created:"
                    + " mSrc=" + mSrc
                    + " mContentType=" + mContentType
                    + " mUri=" + uri);
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

        if (contact.getContactMethodsList() != null && contact.getContactMethodsList().size() > 0) {
            ContactMethod data = contact.getContactMethodsList().get(0);
            formatMsg.append("\n" + data.data);
        }

        return formatMsg.toString();
    }

    /* Overriding method 
     * (non-Javadoc)
     * @see org.w3c.dom.events.EventListener#handleEvent(org.w3c.dom.events.Event)
     */
    @Override
    public void handleEvent(Event evt) {
        // TODO Auto-generated method stub

    }

    public String getFormattedMsg() {
        if (mFormattedMsg == null && !mVcardLoadError) {
            loadVcard();
        }
        
        if (mFormattedMsg == null) {
            mFormattedMsg = mContext.getString(R.string.name_card_parse_error);
        }
        
        return mFormattedMsg;
    }

    public Bitmap getContactPicture() {
        if (mContactStruct == null && !mVcardLoadError) {
            loadVcard();
        }

        if (mContactStruct != null) {
            return mContactStruct.getContactPicture();
        }

        return null;
    }

    public ContactStruct getContactStruct() {
        if (mContactStruct == null && !mVcardLoadError) {
            loadVcard();
        }
        return mContactStruct;
    }

    public void loadVcard() {
        Uri uri = getUri();
        InputStream input = null;
        
        vcardLoaded = true;
        
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "VcardModel.loadVcard " + uri);
        }
        try {
            VCardParser parser = new VCardParser();
            VDataBuilder builder = new VDataBuilder();

            input = mContext.getContentResolver().openInputStream(uri);
            byte[] buffer = new byte[1024];
            StringBuilder vcard = new StringBuilder();

            int bytesRead;
            while ((bytesRead = input.read(buffer)) > 0) {
                vcard.append(new String(buffer, 0, bytesRead));
            }

            if (input != null) {
                input.close();
            }
            // parse the string
            boolean parsed = parser.parse(vcard.toString(), CharacterSets.getMimeName(mCharset), builder);
            if (!parsed) {
                return;
            }

            vcard.delete(0, vcard.length());
            vcard = null;

            // get all parsed contacts
            List<VNode> pimContacts = builder.vNodeList;

            if (pimContacts.size() > 0) {
                ContactStruct contact = ContactStruct.constructContactFromVNode(pimContacts.get(0), 0);

                if (contact != null) {
                    mContactStruct = contact;
                    mFormattedMsg = getFormattedMsg(contact);
                    mMemorySize = contact.getMemorySize() + mFormattedMsg.length() * 2;
                }
             }
        } // TODO handle the exceptions properly perhaps show a toast
        catch (IOException e) {
            Logger.error(e);
            mVcardLoadError = true;
        } catch (VCardException e) {
        	Logger.error(e);
        	mVcardLoadError = true;
        } catch (Exception e) {
            Logger.error(e);
            mVcardLoadError = true;
        }
    }

    public int getCharset() {
        return mCharset;
    }

	@Override
	public boolean isLoaded(Rect rect) {
		return vcardLoaded;
	}
}
