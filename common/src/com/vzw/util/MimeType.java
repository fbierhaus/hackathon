/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author hud
 */
public enum MimeType {
	OTHERS("5009", "application/octet-stream")
,	PNG("32", "image/png", MimeSubType.image, "png")
,	JPG("30", "image/jpeg", MimeSubType.image, "jpg", "jpe", "jpeg", "jpz")
,	GIF("29", "image/gif", MimeSubType.image, "gif")
,	BMP("5001", "image/bmp", MimeSubType.image, "bmp")
,	DOC("5009", "application/msword", "doc")
,   SMIL("5000", "application/smil", "smil")
,   TEXT ("3", "text/plain", "txt")
,	VCARD("7", "text/x-vcard", "vcf")
,	THREEGPP_VIDEO("5020", "video/3gpp", MimeSubType.video, "3gp", "3gpp")
,	THREEGPP_AUDIO("5020", "audio/3gpp", MimeSubType.audio, "3gp", "3gpp")
,	THREEGPP2_VIDEO("5021", "video/3gpp2", MimeSubType.video, "3g2", "3gpp2")
,	THREEGPP2_AUDIO("5021", "audio/3gpp2", MimeSubType.audio, "3g2", "3gpp2")
,	AMR("6020", "audio/amr", MimeSubType.audio, "amr")
,	MPTHREE("6021", "audio/mp3", MimeSubType.audio, "mp3")
,	MPEGAUDIO("6027", "audio/mpeg", MimeSubType.audio, "mpga")
,	FLV("6022", "video/x-flv", MimeSubType.video, "flv")
,	MOV("6023", "video/quicktime", MimeSubType.video, "mov")
,   WAV("6024", "audio/wav", MimeSubType.audio, "wav")
,   AAC("6025", "audio/aac", MimeSubType.audio, "aac")
,	AVI("10000", "video/avi", MimeSubType.video, "avi")
,	MP4VIDEO("6026", "video/mp4", MimeSubType.video, "mp4")

//,	AAC("10001", "audio/x-aac", "aac")



	// other irregular mimetypes , the index starts with 90000
,	DOCX("90000", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx")
,	XLS("90001", "application/vnd.ms-excel", "xls")
,	XLSX("90002", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx")
,	PPT("90003", "application/vnd.ms-powerpoint", "ppt")
,	PPTX("90004", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx")


	;

	private static final Map<String, Map<String, MimeType>>		mimeMapByExt = new HashMap<String, Map<String, MimeType>>();
	private static final Map<String, MimeType>					mimeMapByValue = new HashMap<String, MimeType>();
	
	private final String						index;
	private final String						value;
	private final String[]						exts;
	private MimeSubType							subType = MimeSubType.def;		// default value
	
	
	static {
		for (MimeType mime : values()) {
			for (String ext : mime.getExts()) {
				Map<String, MimeType> map1 = mimeMapByExt.get(ext);
				if (map1 == null) {
					map1 = new HashMap<String, MimeType>();
					mimeMapByExt.put(ext, map1);
				}
				map1.put(mime.getValue(), mime);
			}
			mimeMapByValue.put(mime.getValue(), mime);
		}
	}
	
	private MimeType(String index, String value, String...exts) {
		this.index = index;
		this.value = value;
		this.exts = exts;
	}
	
	private MimeType(String index, String value, MimeSubType subType, String...exts) {
		this.index = index;
		this.value = value;
		this.exts = exts;
		this.subType = subType;
	}

	public String getIndex() {
		return index;
	}
	
	public String getValue() {
		return value;
	}
	
	public String[] getExts() {
		return exts;
	}

	public MimeSubType getSubType() {
		return subType;
	}
	
	
	
	/**
	 * Get the first extension
	 * @return 
	 */
	public String getExt() {
		return CollectionUtil.isEmpty(exts) ? "" : exts[0];
	}
	
	public static Collection<MimeType> typesFromExt(String ext) {
		if (ext == null) {
			return null;
		}
		
		Map<String, MimeType> map1 = mimeMapByExt.get(ext.toLowerCase());
		if (map1 == null) {
			return null;
		}
		
		return map1.values();
	}
	
	/**
	 * 
	 * @param name
	 * @return 
	 */
	public static Collection<MimeType> typesFromName(String name) {
		return typesFromExt(FilenameUtils.getExtension(name));
	}

	/**
	 * 
	 * @param ext
	 * @return 
	 */
	public static MimeType fromExt(String ext) {
		return fromExt(ext, MimeSubType.def, null);
	}
	
	/**
	 * 
	 * @param ext
	 * @param subType
	 * @return 
	 */
	public static MimeType fromExt(String ext, MimeSubType subType) {
		return fromExt(ext, subType, null);
	}

	/**
	 * 
	 * @param ext
	 * @param valuePattern
	 *		does not have to be a full matched value, but a searchable regex
	 *		always return the first matched mimetype. So make sure the valuePattern
	 *		is unique
	 * @return 
	 */
	public static MimeType fromExt(String ext_, MimeSubType subType, String valuePattern) {
		
		MimeType ret = null;
		
		Collection<MimeType> candidates = null;
		
		String ext;
		if (ext_ != null) {
			ext = ext_.toLowerCase();
			
			Map<String, MimeType> map1 = mimeMapByExt.get(ext.toLowerCase());
			if (map1 != null) {
				if (valuePattern == null) {
					// take the first one
					candidates = map1.values();
				}
				else {
					// go over the map
					Pattern p = Pattern.compile(valuePattern);
					for (Map.Entry<String, MimeType> entry : map1.entrySet()) {
						Matcher m = p.matcher(entry.getKey());
						if (m.find()) {
							if (candidates == null) {
								candidates = new ArrayList<MimeType>();
							}
							candidates.add(entry.getValue());
						}
					}
				}
			}
		}
		
		// now go over the candidates based on sub type
		if (!CollectionUtils.isEmpty(candidates)) {
			if (candidates.size() == 1) {
				ret = candidates.iterator().next();	// just pick it
			}
			else {
				// there are more than one, find a match
				if (subType == MimeSubType.def) {
					// for def, need to get a real one
					// always pick video
					for (MimeType mt : candidates) {
						switch (mt) {
							case THREEGPP_VIDEO:
								ret = mt;
								break;
								
							case THREEGPP2_VIDEO:
								ret = mt;
								break;
						}
						
						if (ret != null) {
							break;
						}
					}
					
				}
				else {
					// do simple match
					for (MimeType mt : candidates) {
						if (mt.getSubType() == subType) {
							ret = mt;
							break;
						}
					}
					
				}
				
				// if not found, use the first one found
				if (ret == null) {
					// just pick the first one
					ret = candidates.iterator().next();	// just pick it
				}
			}
		}
		else {
			ret = OTHERS;
		}
		
		return ret;
	}
	
	
	/**
	 * 
	 * @return 
	 */
	public boolean isVideo() {
		return subType == MimeSubType.video;
				 
	}
	
	
	/**
	 * Determines if the provided MimeType is a Video type.
	 * 
	 * @param someType 
	 * @return true if the type is one of the VMA supported Video types, false otherwise
	 */
	public static boolean isVideo(String someType){
		MimeType mt = MimeType.fromValue(someType);
		return isVideo(mt);
	}

	/**
	 * Determines if the provided MimeType is a Video type.
	 * 
	 * @param someType 
	 * @return true if the type is one of the VMA supported Video types, false otherwise
	 */
	public static boolean isVideo(MimeType someType){
		if (someType != null) {
			return someType.isVideo();
		}
		else {
			return false;
		}
	}
	
	
	
	/**
	 * 
	 * @return 
	 */
	public boolean isImage() {
		return subType == MimeSubType.image;
	}

	/**
	 * Determines if the provided MimeType is a Video type.
	 * 
	 * @param someType 
	 * @return true if the type is one of the VMA supported Video types, false otherwise
	 */
	public static boolean isImage(String someType){
		MimeType mt = MimeType.fromValue(someType);
		return isImage(mt);
	}
	
	/**
	 * Determines if the provided MimeType is an Image type.
	 * 
	 * @param someType 
	 * @return true if the type is one of the VMA supported Image types, false otherwise
	 */	
	public static boolean isImage(MimeType someType){
		if (someType != null) {
			return someType.isImage();
		}
		else {
			return false;
		}
	}
	
	
	/**
	 * 
	 * @param value
	 * @return 
	 */
	public static boolean isAudio(String value) {
		MimeType mt = fromValue(value);
		if (mt != null) {
			return mt.isAudio();
		}
		else {
			return false;
		}
	}
	
	
	public boolean isAudio() {
		return subType == MimeSubType.audio;
	}
	
	
	/**
	 * 
	 * @param fileName
	 * @return 
	 */
	public static MimeType fromName(String fileName) {
		return fromName(fileName.toLowerCase(), MimeSubType.def);
	}
	
	/**
	 * 
	 * @param fileName
	 * @param subType
	 * @return 
	 */
	public static MimeType fromName(String fileName, MimeSubType subType) {
		return fromExt(FilenameUtils.getExtension(fileName.toLowerCase()), subType);
	}
	
	/**
	 * 
	 * @param value
	 *		<standard_value>[;other properties]
	 *		We need to strip any other properties here
	 * @return 
	 */
	public static MimeType fromValue(String value_) {
		if (value_ == null) {
			return OTHERS;
		}
		String value = value_.toLowerCase();
		
		int i = value.indexOf(';');
		String _value;
		if (i >= 0) {
			_value = value.substring(0, i).trim().toLowerCase();
		}
		else {
			_value = value.trim().toLowerCase();
		}
		
		MimeType mime = mimeMapByValue.get(_value);
		return mime == null ? OTHERS : mime;
	}
	
	
	/**
	 * 
	 * @param subType
	 * @return 
	 */
	public static List<MimeType> mimeTypesOfSubTypes(MimeSubType subType) {
		List<MimeType> ret = new ArrayList<MimeType>();
		
		for (MimeType mt : values()) {
			if (mt.getSubType() == subType) {
				ret.add(mt);
			}
		}
		
		return ret;
		
	}
	
	/**
	 * 
	 * @return 
	 */
	public static List<MimeType> videoMimeTypes() {
		return mimeTypesOfSubTypes(MimeSubType.video);
	}

	/**
	 * 
	 * @return 
	 */
	public static List<MimeType> audioMimeTypes() {
		return mimeTypesOfSubTypes(MimeSubType.audio);
	}
	
	/**
	 * 
	 * @return 
	 */
	public static List<MimeType> imageMimeTypes() {
		return mimeTypesOfSubTypes(MimeSubType.image);
	}
}
