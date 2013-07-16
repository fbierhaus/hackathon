package com.vzw.vma.sync.util;

import java.util.ArrayList;
import java.util.List;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.ContentType;
import com.vzw.vma.common.message.VMAAttachment;

public class MMSUtil {
	public static final String LOCATION_IMG_PREFIX = "location";
	public static final String LOCATION_VCARD_PREFIX = "VCARD_";

	public static class MMSMessage {
		private ArrayList<VMAAttachment> attachments = new ArrayList<VMAAttachment>();

		public void addAttachment(VMAAttachment attachment) {
			attachments.add(attachment);
		}
		
		public ArrayList<VMAAttachment> getAttachment() {
			return attachments;
		}
	}

	/*
	 * Splits the attachemnts into a list of MMS messages
	 * Makes sure that each message has one of these attachemnts
	 * 1) One media + one text part if present
	 * 2) One location vcard + location image if present + location text if found
	 */
	public static List<MMSMessage> splitMessages(List<VMAAttachment> attacments) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MMSUtil.class, "splitMessages");
		}

		List<MMSMessage> msgs = new ArrayList<MMSMessage>();
		List<VMAAttachment> pb = reorderPartBody(attacments);

		boolean hasText = false;
		boolean hasMedia = false;
		boolean hasLocation = false;
		int partsNum = pb.size();
		MMSMessage par = null;
		for (int i = 0; i < partsNum; i++) {
			// Create new <par> element.
			if ((par == null) || (hasMedia && hasText)) {
				par = addPar(msgs);
				hasText = false;
				hasMedia = false;
				hasLocation = false;
			}

			VMAAttachment part = pb.get(i);
			String contentType = new String(part.getMimeType());

			if (contentType.equals(ContentType.TEXT_PLAIN)
					|| contentType.equalsIgnoreCase(ContentType.APP_WAP_XHTML)
					|| contentType.equals(ContentType.TEXT_HTML)) {
				par.addAttachment(part);
				hasText = true;
			} else if (ContentType.isImageType(contentType)) {
				if (hasMedia && !isLocationImage(hasLocation, part.getAttachmentName())) {
					par = addPar(msgs);
					hasText = hasMedia = hasLocation = false;
				}
				par.addAttachment(part);
				hasMedia = true;
			} else if (ContentType.isVideoType(contentType)) {
				if (hasMedia || hasLocation) {
					par = addPar(msgs);
					hasText = hasMedia = hasLocation = false;
				}
				par.addAttachment(part);
				hasMedia = true;
			} else if (ContentType.isAudioType(contentType)) {
				if (hasMedia || hasLocation) {
					par = addPar(msgs);
					hasText = hasMedia = hasLocation = false;
				}
				par.addAttachment(part);
				hasMedia = true;
			} else if (ContentType.isVcardTextType(contentType)) {
				if (hasMedia || hasLocation) {
					par = addPar(msgs);
					hasText = hasMedia = hasLocation = false;
				}

				//Uri vcardUri = part.getDataUri();
				boolean hasLocationField = isLocationVcard(part.getAttachmentName());
				/*if (vcardUri == null) {
	               	hasLocationField = MessageUtils.hasLocation(context, part.getData());
	               } else {
	               	hasLocationField = MessageUtils.hasLocation(context, vcardUri);
	               }*/
	               if (hasLocationField) {
	            	   par.addAttachment(part);
	            	   hasLocation = true;
	               } else {
	            	   par.addAttachment(part);
	            	   //treat vcard as a media
	            	   hasMedia = true;
	               }
			} else if (!contentType.equals(ContentType.APP_SMIL)) {
				Logger.error(MMSUtil.class, "Unsupported media type " + contentType);
			}
		}
		
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MMSUtil.class, "splitMessages number of messages " + msgs.size());
		}

		return msgs;
	}

	private static MMSMessage addPar(List<MMSMessage> msgs) {
		MMSMessage msg = new MMSMessage();
		msgs.add(msg);

		return msg;
	}

	/*
	    * This method reorders the PduBody to make sure that the location is associated with the 
	    * appropriate image i.e the location image follows the corresponding location vcard
	    */
	private static List<VMAAttachment> reorderPartBody(List<VMAAttachment> pb) {
		int size = pb.size();
		VMAAttachment part;
		String contentType;
		List<VMAAttachment> orderedBody = new ArrayList<VMAAttachment>();

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MMSUtil.class, "reorderPartBody");
		}

		for (int i = 0; i < size; i++) {
			part = pb.get(i);
			contentType = part.getMimeType();

			if (ContentType.isVcardTextType(contentType)) {
				String location = part.getAttachmentName();
				boolean hasLocationField = isLocationVcard(location);
				/*if (vcardUri == null) {
	               	hasLocationField = MessageUtils.hasLocation(context, part.getData());
	               } else {
	               	hasLocationField = MessageUtils.hasLocation(context, vcardUri);
	               }*/

				if (hasLocationField) {
					if (location != null && location.startsWith(LOCATION_VCARD_PREFIX)) {
						int start = LOCATION_VCARD_PREFIX.length();
						int end = location.lastIndexOf(".");
						String id = location.substring(start);
						if (end > 0) {
							id = location.substring(start, end);
						}
						try {
							VMAAttachment par = getLocationImagePart(pb, id);

							//image corresponding to the location is found
							if (par != null) {
								//remove the old image part if already added to the orderedBody
								orderedBody.remove(par);

								//add the location part followed by location image
								orderedBody.add(part);
								orderedBody.add(par);
								continue;
							}
						} catch (Exception e) {
							Logger.error(MMSUtil.class, "reorderPartBody could not reorder the parts returning the unordered parts ", e);
							return pb;
						}
					}
				}
			} else if (ContentType.isImageType(contentType) && orderedBody.contains(part)) {
				continue;
			}
			orderedBody.add(part);
		}
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MMSUtil.class, " old parts " + pb + " ordered parts " + orderedBody);
		}

		return orderedBody;
	}
	
	//returns the location image corresponding to the location vcard
	private static VMAAttachment getLocationImagePart(List<VMAAttachment> pb, String locationId) {
		int size = pb.size();
		VMAAttachment part;
		String contentType;

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("MMSUtil.getLocationImagePart");
		}

		for (int i = 0; i < size; i++) {
			part = pb.get(i);
			contentType = part.getMimeType();

			if (ContentType.isImageType(contentType) && isLocationImageOfPart(locationId, part)) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug("MMSUtil.getLocationImagePart found image corresponding to the location");
				}

				return part;
			}
		}
		return null;
	}

	/**
	 * This Method checks if the image is associated with location
	 * message
	 * @param hasLocation
	 * @param bs
	 * @return
	 */
	private static boolean isLocationImage(boolean hasLocation, String location) {
		if (hasLocation) {
			if (location != null && location.startsWith(LOCATION_IMG_PREFIX)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This Method checks if the image is associated with proper location
	 * message
	 * @param hasLocation
	 * @param bs
	 * @return
	 */
	private static boolean isLocationImageOfPart(String locationId, VMAAttachment part) {
		String contentLocation = part.getAttachmentName();
		if (locationId != null && contentLocation != null && contentLocation.startsWith(LOCATION_IMG_PREFIX + locationId)) {
			return true;
		}
		return false;
	}
	
	//TODO: check if there is a need to open the vcard file to verify if it is an location attachment
	private static boolean isLocationVcard(String locationName) {
		if (locationName != null && locationName.startsWith(LOCATION_VCARD_PREFIX)) {
			return true;
		}
		return false;
	}
}
