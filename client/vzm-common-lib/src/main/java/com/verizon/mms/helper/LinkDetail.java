package com.verizon.mms.helper;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holder class for Link Details
 * 
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * @Since Apr 10, 2012
 */
public class LinkDetail implements Parcelable {
    String link;
    String contentType;
    String title;
    String description;
//    String ogUrl;
    String linkImage;
    int    responseCode;
    String error;
    
    long lastUpdate = 0;
    long lastRead = 0;
    String imagePath = null;

    public LinkDetail() {
    };

    public LinkDetail(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(link);
        dest.writeString(contentType);
        dest.writeString(title);
        dest.writeString(description);
//        dest.writeString(ogUrl);
        dest.writeString(linkImage);
        dest.writeInt(responseCode);
        dest.writeString(error);
        
        dest.writeLong(lastUpdate);
        dest.writeLong(lastRead);
        dest.writeString(imagePath);
    }

    private void readFromParcel(Parcel in) {
        link = in.readString();
        contentType = in.readString();
        title = in.readString();
        description = in.readString();
//        ogUrl = in.readString();
        linkImage = in.readString();
        responseCode = in.readInt();
        error = in.readString();
        
        lastUpdate = in.readLong();
        lastRead = in.readLong();
        imagePath = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getLink() {
        return link;
    }

    public String getContentType() {
        return contentType;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

//    public String getOgUrl() {
//        return ogUrl;
//    }

    public String getLinkImage() {
        return linkImage;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        return "LinkDetail [link=" + link + ", contentType=" + contentType + ", title=" + title
                + ", description=" + description + /*", ogUrl=" + ogUrl +*/ ", linkImage=" + linkImage
                + ", responseCode=" + responseCode + ", error=" + error + "]";
    }

    // ==============================================================
    // do know where to put it, so put it here
    
    /**
     * Convert hostname to lowercase.
     * 
     * @param url
     * @return
     */
	public static String fixUrl(String url) {
		String newAddress = url;

		if (url != null) {
			if (url.contains("://") == false) {
				// default to http
				url = "http://" + url;
			}
			
			// there is no locator
			Uri uri = Uri.parse(url);

			// hostname is not case-sensitive, change it to lowercase
			String host = uri.getHost();
			if (host != null) {				
				String lowHost = host.toLowerCase();
				
				int start = url.indexOf(host);
				if (start >= 0) {
					newAddress = url.substring(0, start) + lowHost + url.substring(start + lowHost.length());
				}
			}

//			String userInfo = uri.getUserInfo();
//			if (userInfo != null) {
//				userInfo += "@";
//			}
//			else {
//				userInfo = "";
//			}
//			
//			// hostname is not case-sensitive, change it to lowercase
//			String host = uri.getHost();
//			if (host != null) {
//				host = host.toLowerCase();	
//			}
//			else {
//				host = "";
//			}
//			
//			String port;
//			int portNum = uri.getPort();
//			if (portNum == -1) {
//				port = "";
//			}
//			else {
//				port = ":" + portNum;
//			}
//			
//			String path = uri.getPath();
//			if (path == null) {
//				path = "";
//			}
//			
//			String query = uri.getEncodedQuery();
//			if (query == null) {
//				query = "";
//			}
//			else {
//				query = "?" + query;
//			}
//
//			newAddress = uri.getScheme() + "://" + userInfo + host + port + path + query;
		}

		return newAddress;
	}

	/**
	 * Convert a relative url to absolute url using the base url
	 * 
	 */
	public static String relativeToAbsoluteUrl(String address, String base) {
		String newAddress = address;

		if (address != null) {
			if (address.contains("://") == false) {
				// there is no locator
				Uri uri = Uri.parse(base);

				// construct a new url
				if (address.startsWith("/")) {
					// full path without host
					newAddress = uri.getScheme() + "://" + uri.getAuthority() + address;
				}
				else {
					// relative to the document
					newAddress = uri.getScheme() + "://" + uri.getAuthority() + uri.getPath() + "/" + address;
				}    		    		
			}
		}

		return newAddress;
	}
}
