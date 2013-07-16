package com.verizon.mms.transaction;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsMessage;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.Media;
import com.verizon.mms.SmsScanner;
import com.verizon.mms.helper.BitmapManager;
import com.verizon.mms.helper.BitmapManager.BitmapEntry;
import com.verizon.mms.helper.HrefManager;
import com.verizon.mms.helper.LinkDetail;
import com.verizon.mms.model.SlideshowModel;
import com.verizon.mms.pdu.GenericPdu;
import com.verizon.mms.pdu.MultimediaMessagePdu;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.ui.MessageItem.MMSData;

/**
 * Prefetch link details and image preview from SMS or MMS.
 */
public class PreviewPrefetcher extends Handler {
	private static final String TAG = PreviewPrefetcher.class.getSimpleName();
	
	private Context mContext;

	private static final int MSG_LINK_DETAIL = 1;
//	private static final int MSG_IMAGE_DONE = 2;	// we don't need to know if image is done

	public PreviewPrefetcher(Context context) {
		super();
		
		mContext = context;
	}
	
	/**
	 * Extract links from SMS and prefetch details.
	 * 
	 * @param sms
	 */
    public void processLinksInSms(SmsMessage sms) {
    	try {
    		String msg = sms.getMessageBody();
    		if (msg != null) {
    			processLinksInText(msg);
    		}
    	}
    	catch (Exception e) {
    		Logger.warn(TAG+":processLinksInSms failed", e);
    	}
    }

    /**
     * Extract links from MMS and prefetch details.
     * 
     * @param uri
     */
    public void processLinksInMms(Uri uri) {
    	try {
    		PduPersister p = PduPersister.getPduPersister(mContext);
    		GenericPdu msg = p.load(uri);
    		if (!(msg instanceof  MultimediaMessagePdu)) {
    			return;
    		}
    		MultimediaMessagePdu mmsMsg = (MultimediaMessagePdu)msg;
    		MMSData mmsData = SlideshowModel.getAttachmentFromBody(mContext, mmsMsg.getBody());

    		String body = mmsData.mBody;
    		if (body != null) {
    			if (Logger.IS_DEBUG_ENABLED) {
    				Logger.info(TAG + ":MMSBody = " + body);
    			}
    			processLinksInText(body);
    		}
    	}
    	catch (Exception e) {
    		Logger.warn(TAG+":processLinksInMms failed", e);
    	}    	
    }

    /**
     * Extracts links from text string and prefetch details.
     * 
     * @param msg
     */
    private void processLinksInText(String msg) {
    	Map<String, String> map = SmsScanner.extractUris(msg);
    	
    	Set<Entry<String,String>> set = map.entrySet();
    	HrefManager href = HrefManager.INSTANCE;
    			
    	for (Entry<String, String> entry : set) {
    		String url = entry.getKey();
    		String type = entry.getValue();
    		if (type.equals(Media.M_LINK_CT)) {
    	        if (Logger.IS_DEBUG_ENABLED) {
    	        	Logger.debug(getClass(), "Prefetching Link: " + url);
    	        }
    	        
				// check global cache
				LinkDetail linkDetail = href.getLink(mContext, url);
				if (linkDetail == null) {
					href.loadLink(mContext, url, this, MSG_LINK_DETAIL, false);
				}
			}
    	}
    }
    
    /**
     * Handle callback message from HrefManager to continue fetching the image.
     */
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_LINK_DETAIL: {
			LinkDetail detail = (LinkDetail)msg.obj;
			if (detail != null) {
				String image = detail.getLinkImage();
				if (image != null) {
					if(Logger.IS_DEBUG_ENABLED){
					Logger.debug(getClass(), "Prefetching Image: " + image);
					}

					// now prefetech image
					BitmapManager mgr = BitmapManager.INSTANCE;
					int minPreviewImage = (int)mContext.getResources().getDimension(R.dimen.linkPreviewMinImageSizeConversation);

					int pixel = BitmapManager.dipToPixel(mContext, R.dimen.linkPreviewImageSize);
					BitmapEntry result = mgr.getBitmap(mContext, image, pixel, pixel); // minPreviewImage, null, 0);

					// if not in the cache then try to download it
					if (result == null) {
						mgr.loadBitmap(mContext, image, pixel, pixel, minPreviewImage, null, 0);
					}
				}
			}
			break;
		}

		default:
			break;
		}
	}
}
