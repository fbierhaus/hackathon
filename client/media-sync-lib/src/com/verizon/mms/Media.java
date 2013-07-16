package com.verizon.mms;

import java.io.BufferedInputStream;

import com.verizon.common.VZUris;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

/**
 * MmsMedia Cache Object
 * 
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * 
 */
public class Media {

//    public static final String MMS_CT = "application/vnd.wap.multipart.related"; // application/vnd.wap.multipart.mixed
//    public static final String SMS_CT = "plain/text";
    public static final String M_LINK_CT = "text/link";
    public static final String M_EMAIL_CT = "text/mailto";
    public static final String M_PHONE_CT = "text/tel";
    public static final String M_LOCATION_CT = "text/x-vcard";
    public static final String M_LOCATION_CT2 = "text/x-vCard";

    // Thread Id
    final int threadId;
    // mms/sms id
    final int mId;
    /*
     * Message Part Id
     * 
     * MMS: partId SMS: just a counter, there is no relevance!
     */
    final int mPartId;

    final String address; // sender/receiver,
                          // depending
                          // upon mType

    // Message Content Type
    // MMS: application/vnd.wap.multipart.related
    // SMS: plain/text
    String mCt; // application/vnd.wap.multipart.related

    /*
     * Part Content Type
     * 
     * e.g. for image: image/jpeg or image/bmp or image/gif or image/jpg or image/png for URL: text/link
     */
    String mPartCt;

    // text/body?
    final String text;

    // Message Type: outgoing or incoming?
    // MMS ==> 132: received, 128: sent
    // SMS ==> 1: received, 2: sent
    final int mType; //
    // read?
    final int mRead;
    // date
    final long date;
    //width
     int width;
    //height
     int height;
    
    public Media(int threadId, int mId, int mPartId, String address, String mCt, String mPartCt, String text,
            int mType, int mRead, long date, int width, int height) {
        this.threadId = threadId;
        this.mId = mId;
        this.mPartId = mPartId;
        this.address = address;
        this.mCt = mCt;
        this.mPartCt = mPartCt;
        this.text = text;
        this.mType = mType;
        this.mRead = mRead;
        this.date = date;
        if (width != 0 || height != 0) {
            this.width = width;
            this.height = height;
        }
    }

    public int getmType() {
        return mType;
    }

    public int getmRead() {
        return mRead;
    }

    public int getThreadId() {
        return threadId;
    }

    public int getMId() {
        return mId;
    }

    public int getmPartId() {
        return mPartId;
    }

    public String getAddress() {
        return address;
    }

    public String getmCt() {
        return mCt;
    }

	public void setmCt(String mCt) {
		this.mCt = mCt;
	}

    public String getmPartCt() {
        return mPartCt;
    }

	public void setmPartCt(String mPartCt) {
		this.mPartCt = mPartCt;
	}

    public String getText() {
        return text;
    }

    public long getDate() {
        return date;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
    	this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
    	this.height = height;
    }

    public boolean isSms() {
        //TODO putting temp check for null need to find reasion behind it.
     return mCt != null ? mCt.equals("text/plain") : false;
    }

    public boolean isMms() {
        return !isSms();// mCt.equals("application/vnd.wap.multipart.related"); or
                        // application/vnd.wap.multipart.mixed
    }

    public boolean isOutgoing() {
        return isMms() ? (mType == 128) : (mType == 2);
    }

    public boolean isIncoing() {
        return isMms() ? (mType == 132) : (mType == 1);
    }

    public boolean isImage() {
        // quick check
        if (!mPartCt.startsWith("image/")) {
            return false;
        }
        // bug#3433: added ms-bmp,wbmp,svg file content types as well.
        return ("image/jpeg".equals(mPartCt) || "image/bmp".equals(mPartCt) || "image/gif".equals(mPartCt)
                || "image/jpg".equals(mPartCt) || "image/png".equals(mPartCt) || "image/x-ms-bmp".equals(mPartCt)
                || "image/vnd.wap.wbmp".equals(mPartCt) || "image/svg+xml".equals(mPartCt) );
    }

    public boolean isVideo() {
        // quick check
        if (!mPartCt.startsWith("video/")) {
            return false;
        }
        return ("video/3gpp".equals(mPartCt)
        		|| "video/h263".equals(mPartCt)        		
        		|| "video/*".equals(mPartCt)
        		|| "video/3gpp2".equals(mPartCt)
        		|| "video/h264".equals(mPartCt)
        		|| "video/mp4".equals(mPartCt)
        		|| "video/mpeg".equals(mPartCt)
        		|| "video/mp4v-es".equals(mPartCt)
                || "video/h263-2000".equals(mPartCt)
                || "video/quicktime".equals(mPartCt));
    }

    public boolean isAudio() {
        // quick check
        if (!mPartCt.startsWith("audio/")) {
            return false;
        }
        return ("audio/evrc".equals(mPartCt) || "audio/midi".equals(mPartCt) || "audio/3gpp".equals(mPartCt)
                || "audio/3gpp2".equals(mPartCt) || "audio/amr".equals(mPartCt)
                || "audio/mp4a-latm".equals(mPartCt) || "audio/wav".equals(mPartCt)
                || "audio/mpeg".equals(mPartCt) || "audio/mp3".equals(mPartCt)
                || "audio/qcelp".equals(mPartCt) || "audio/vnd.qcelp".equals(mPartCt));
    }

    public boolean isLink() {
        return M_LINK_CT.equals(mPartCt);
    }
    
    public boolean isNameCard() {
        return "text/namecard".equals(mPartCt);
    }

    public boolean isEmail() {
        return M_EMAIL_CT.equals(mPartCt);
    }

    public boolean isPhone() {
        return M_PHONE_CT.equals(mPartCt);
    }

    public boolean isLocation() {
    	return (M_LOCATION_CT.equalsIgnoreCase(mPartCt) || M_LOCATION_CT2.equalsIgnoreCase(mPartCt));
    }

    public String getNameCardUri() {
    	if (!isNameCard()) {
             throw new IllegalStateException(String.format("mid=%s, thread_id=%s, part_id=%s is not a namecard",
                     mId, threadId, mPartId));
        }
        return "content://"+VZUris.getMmsUri().getAuthority()+"/part/" + mPartId;
    }
    
    public String getImageUri() {
        if (!isImage()) {
            throw new IllegalStateException(String.format("mid=%s, thread_id=%s, part_id=%s is not an image",
                    mId, threadId, mPartId));
        }
        return "content://"+VZUris.getMmsUri().getAuthority()+"/part/" + mPartId;
    }

    public String getVideoUri() {
        if (!isVideo()) {
            throw new IllegalStateException(String.format("mid=%s, thread_id=%s, part_id=%s is not a video",
                    mId, threadId, mPartId));
        }
        return "content://"+VZUris.getMmsUri().getAuthority()+"/part/" + mPartId;
    }

    public String getLocationUri() {
    	if (!isLocation()) {
            throw new IllegalStateException(String.format("mid=%s, thread_id=%s, part_id=%s is not a location",
                    mId, threadId, mPartId));
        }
        return "content://"+VZUris.getMmsUri().getAuthority()+"/part/" + mPartId;
    }
    
    @Override
    public String toString() {
        return super.toString() + ": threadId=" + threadId + ", mId=" + mId + ", mPartId=" + mPartId
                + ", address=" + address + ", mCt=" + mCt + ", mPartCt=" + mPartCt + ", text=" + text
                + ", mType=" + mType + ", mRead=" + mRead + ", date=" + date + ", size=" + width + "x" + height;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mCt == null) ? 0 : mCt.hashCode());
        result = prime * result + mId;
        result = prime * result + mPartId;
        result = prime * result + threadId;
        return result;
    }
    
    /**
	 * 
	 * This Method checks to see if the VZW_LOCATION is present in the vcard 
	 * @param context
	 * @param uri
	 * @return
	 */
	public boolean hasLocation(Context context) {
		Uri uri = Uri.parse("content://"+VZUris.getMmsUri().getAuthority()+"/part/" + mPartId);
		boolean hasLocation = false;

		BufferedInputStream is = null;
		try {
			final ContentResolver res = context.getContentResolver();
			is = new BufferedInputStream(res.openInputStream(uri), 4096);
			int buflen = 8 * 1024;
			do {
				final byte[] buf = new byte[buflen];
				final int read = is.read(buf);
				if (read < buflen) {
					final String vcard = new String(buf, 0, read);
					hasLocation = vcard.contains("X-VZW-NGM-LOC");
					break;
				}
				buflen *= 2;
			} while (buflen <= 512 * 1024);
			if (buflen > 512 * 1024) {
//				log.e("hasLocation: hit size limit for " + uri);
			}
		}
		catch (Exception e) {
//			log.error("hasLocation: uri = " + uri, e);
		}
		finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (Exception e) {
				}
			}
		}

		return hasLocation;
	}

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Media other = (Media) obj;
        if (mCt == null) {
            if (other.mCt != null)
                return false;
        } else if (!mCt.equals(other.mCt))
            return false;
        if (mId != other.mId)
            return false;
        if (mPartId != other.mPartId)
            return false;
        if (threadId != other.threadId)
            return false;
        return true;
    }
}