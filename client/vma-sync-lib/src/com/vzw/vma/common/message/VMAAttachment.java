package com.vzw.vma.common.message;

import java.io.Serializable;

import android.net.Uri;

import com.strumsoft.android.commons.logger.Logger;

public class VMAAttachment implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6622901264849841970L;
	private String attachmentId;
	private String attachmentName;
	private String mimeType; //TODO enum
	private String thumbnailURI;
	private String dataURI;
	private String xSectionId;
	private Uri partsUri = null;
	/*
	 * Optional when converting from MSA
	 */
	//private byte[] data;
	
	/*
	 * Optional when converting from MSA
	 */
//	public byte[] getData() {
//		return data;
//	}
	/*
	 * Optional when converting from MSA
	 */	
//	public void setData(byte[] data) {
//		this.data = data;
//	}
	public String getAttachmentId() {
		return attachmentId;
	}
	public void setAttachmentId(String attachmentId) {
		this.attachmentId = attachmentId;
	}
	public String getAttachmentName() {
		return attachmentName;
	}
	public void setAttachmentName(String attachmentName) {
		this.attachmentName = attachmentName;
	}
	public String getMimeType() {
		return mimeType;
	}
	public void setMimeType(String mimeType) {
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("VMAAttachment: Part content type = " + mimeType);
		}
		this.mimeType = mimeType;
	}
	public String getThumbnailURI() {
		return thumbnailURI;
	}
	public void setThumbnailURI(String thumbnailURI) {
		this.thumbnailURI = thumbnailURI;
	}
	public String getDataURI() {
		return dataURI;
	}
	public void setDataURI(String dataURI) {
		this.dataURI = dataURI;
	}
	@Override
	public String toString() {
		return "WebAttachment [attachmentId=" + attachmentId + ", attachmentName=" + attachmentName + ", dataURI=" + dataURI + ", mimeType=" + mimeType + ", thumbnailURI=" + thumbnailURI + "]";
	}
    /**
     * This Method 
     * @param xSectionId
     */
    public void setXSectionID(String sectionId) {
        this.xSectionId=sectionId;
    }
    /**
     * Returns the Value of the xSectionId
     * @return the  {@link String}
     */
    public String getXSectionId() {
        return xSectionId;
    }
	public Uri getTempPartUri() {
		return partsUri;
	}
	public void setTempPartUri(Uri partsUri) {
		this.partsUri = partsUri;
	}
	
}
