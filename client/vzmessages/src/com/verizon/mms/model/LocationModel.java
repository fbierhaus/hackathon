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
import android.graphics.Rect;
import android.net.Uri;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MmsException;
import com.verizon.mms.drm.DrmWrapper;
import com.verizon.mms.pdu.CharacterSets;
import com.verizon.mms.ui.UriVcard;
import com.verizon.vcard.android.provider.VCardContacts;
import com.verizon.vcard.android.syncml.pim.VDataBuilder;
import com.verizon.vcard.android.syncml.pim.VNode;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct.ContactMethod;
import com.verizon.vcard.android.syncml.pim.vcard.VCardException;
import com.verizon.vcard.android.syncml.pim.vcard.VCardParser;

/**
 * This class/interface   
 * @author Essack
 * @Since  May 14, 2012
 */
public class LocationModel extends RegionMediaModel {
    
    private String mFormattedMsg = null;
    private ContactStruct mContactStruct = null;
    
    //set to true when there is an error parsing the VCard
    private boolean mVcardLoadError = false;
    private boolean vcardLoaded = false;
    
    private final int mCharset;
    
    public LocationModel(Context context, Uri uri, RegionModel region)
            throws MmsException {
        super(context, SmilHelper.ELEMENT_TAG_LOCATION, uri, region);
        
        initModelFromUri(uri);
        
        mCharset = CharacterSets.UTF_8;
    }
    
    public LocationModel(Context context, String contentType, String src, int charset,
            Uri uri, RegionModel region) throws DrmException, MmsException {
        super(context, SmilHelper.ELEMENT_TAG_LOCATION,
                contentType, src, uri, region);
        
        if (charset == CharacterSets.ANY_CHARSET) {
            // By default, we use ISO_8859_1 to decode the data
            // which character set wasn't set.
            charset = CharacterSets.ISO_8859_1;
        }
        mCharset = charset;
    }

    public LocationModel(Context context, String contentType, String src, int charset, 
            DrmWrapper wrapper, RegionModel regionModel) throws IOException {
        super(context, SmilHelper.ELEMENT_TAG_LOCATION, contentType, src,
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
            Logger.debug(getClass(), "New LocationdModel created:" + " mSrc=" + mSrc + " mContentType="
                    + mContentType + " mUri=" + uri);
        }
    }

    private void initVCardFields(Uri uri) {
        InputStream input = null;
        vcardLoaded = true;
        
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), " initVcardFields uri: " + uri);
        }

        try {
            VCardParser parser = new VCardParser();
            VDataBuilder builder = new VDataBuilder();

            input = mContext.getContentResolver().openInputStream(uri); 
            byte[] buffer = new byte[1024];
            StringBuilder vcard = new StringBuilder();

            int bytesRead;
            while((bytesRead = input.read(buffer)) > 0){
                vcard.append(new String(buffer, 0, bytesRead));
            }

            if (input != null) {
                input.close();
            }

            //parse the string
            boolean parsed = parser.parse(vcard.toString(), CharacterSets.getMimeName(mCharset), builder);
            if (!parsed) {
                return;
            }

            vcard.delete(0, vcard.length());
            vcard = null;

            //get all parsed contacts
            List<VNode> pimContacts = builder.vNodeList;

            if (pimContacts.size() > 0) {
                ContactStruct contact = ContactStruct.constructContactFromVNode(pimContacts.get(0), 0);

                if (contact != null) {
                    mContactStruct = contact;
                    mFormattedMsg = getFormattedMsg(contact);
                    mMemorySize = contact.getMemorySize();
                    if (mFormattedMsg != null) {
                    	mMemorySize += mFormattedMsg.length() * 2;
                    }

                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "initVcardFields: formattedMsg = " + mFormattedMsg);
                    }
                }
            }
        } //TODO handle the exceptions properly perhaps show a toast 
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

    /**
     * This Method 
     * @param contact
     */
    private String getFormattedMsg(ContactStruct contact) {
        String msg = null;
        
        List<ContactMethod> contactMethods = contact.getContactMethodsList();
        if (contactMethods != null) {
            for(ContactMethod contactMethod : contactMethods)
            {
                if (contactMethod.kind == VCardContacts.KIND_POSTAL) {
                    msg = contactMethod.data;
                }
            }
        }
        
        if (msg == null) {
            msg = contact.getGeoCoord();
        }
        
        return msg;
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
            initVCardFields(getUri());
        }
        
        if (mFormattedMsg == null) {
            mFormattedMsg = mContext.getString(R.string.location_parse_error);
        }
        
        return mFormattedMsg;
    }

    /**
     * This Method 
     * @return
     */
    public ContactStruct getContactStruct() {
        if (mContactStruct == null && !mVcardLoadError) {
            initVCardFields(getUri());
        }
        return mContactStruct;
    }
    
    public int getCharset() {
        return mCharset;
    }
    
    public void setLocationImageUri() {
        
    }

	@Override
	public boolean isLoaded(Rect dims) {
		return vcardLoaded;
	}
}
