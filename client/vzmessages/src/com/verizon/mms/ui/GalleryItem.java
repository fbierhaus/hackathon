package com.verizon.mms.ui;

import java.util.HashMap;

import com.verizon.mms.ContentType;
import com.verizon.mms.Media;

class GalleryItem {
	long id;
	int type;
	Media media;
	int weight;
	private static final HashMap<String, Integer> typeMap;

    public static final int TYPE_UNDEFINED = 0;
    public static final int TYPE_IMAGE     = 1 << 0;
    public static final int TYPE_VIDEO     = 1 << 1;
    public static final int TYPE_LINK      = 1 << 2;
    public static final int TYPE_LOCATION  = 1 << 3;
    public static final int TYPE_CONTACT   = 1 << 4;
    public static final int TYPE_ALL       = TYPE_IMAGE | TYPE_VIDEO | TYPE_LINK | TYPE_LOCATION | TYPE_CONTACT;


    static {
		typeMap = new HashMap<String, Integer>(23);
		typeMap.put("text/mailto", TYPE_CONTACT);
		typeMap.put("text/namecard", TYPE_CONTACT);
		typeMap.put("text/tel", TYPE_CONTACT);
		typeMap.put(Media.M_LINK_CT, TYPE_LINK);
		typeMap.put(Media.M_LOCATION_CT, TYPE_LOCATION);
		typeMap.put(Media.M_LOCATION_CT2, TYPE_LOCATION);
		typeMap.put(ContentType.IMAGE_JPEG, TYPE_IMAGE);
		typeMap.put(ContentType.IMAGE_JPG, TYPE_IMAGE);
		typeMap.put(ContentType.IMAGE_PNG, TYPE_IMAGE);
		typeMap.put(ContentType.IMAGE_GIF, TYPE_IMAGE);
		typeMap.put(ContentType.IMAGE_BMP, TYPE_IMAGE);
		
		// bug#3433: added ms-bmp,svg file content types as well.
		typeMap.put(ContentType.IMAGE_MBMP, TYPE_IMAGE);
		typeMap.put(ContentType.IMAGE_SVG_XML, TYPE_IMAGE);
		//commented as these format are not supported in in Media.java
		
		// bug#3433:uncommented WBMP .
		typeMap.put(ContentType.IMAGE_WBMP, TYPE_IMAGE);
//		typeMap.put(ContentType.IMAGE_SVG, TYPE_IMAGE);
//		typeMap.put(ContentType.IMAGE_UNSPECIFIED, TYPE_IMAGE);
		typeMap.put(ContentType.VIDEO_MP4ES, TYPE_VIDEO);
		typeMap.put(ContentType.VIDEO_MPEG, TYPE_VIDEO);
		typeMap.put(ContentType.VIDEO_MP4, TYPE_VIDEO);
		typeMap.put(ContentType.VIDEO_3G2, TYPE_VIDEO);
		typeMap.put(ContentType.VIDEO_H2632000, TYPE_VIDEO);
		typeMap.put(ContentType.VIDEO_H264, TYPE_VIDEO);
		typeMap.put(ContentType.VIDEO_H263, TYPE_VIDEO);
		typeMap.put(ContentType.VIDEO_UNSPECIFIED, TYPE_VIDEO);
		typeMap.put(ContentType.VIDEO_3GPP, TYPE_VIDEO);
		typeMap.put(ContentType.VIDEO_QUICKTIME, TYPE_VIDEO);
    }


    GalleryItem(long id, Media media) {
		this.id = id;
		this.type = getType(media.getmPartCt());
		this.media = media;
	}

	private int getType(String mimeType) {
		final Integer type = typeMap.get(mimeType);
		return type != null ? type : TYPE_UNDEFINED;
	}

	public boolean equal(Object o) {
		if (o instanceof GalleryItem) {
			final GalleryItem other = (GalleryItem)o;
			return id == other.id && type == other.type && media.getmPartId() == other.media.getmPartId();
		}
		return false;
	}

	@Override
	public String toString() {
		return super.toString() + ": id = " + id + ", type = " + type +
			", weight = " + weight + ", media = " + media;
	}
}