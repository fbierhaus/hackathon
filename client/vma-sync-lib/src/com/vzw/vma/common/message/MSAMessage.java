/*
 * Copyright 2012 Verizon Wireless.  All rights reserved.
 * 
 * Modified by strumsoft.
 * 
 */
package com.vzw.vma.common.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.zip.CRC32;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.Telephony;
import android.webkit.MimeTypeMap;

import com.strumsoft.android.commons.logger.Logger;
import com.sun.mail.iap.Literal;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.mms.ContentType;
import com.verizon.mms.MmsException;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.PduPart;
import com.verizon.mms.pdu.PduPersister;



public class MSAMessage extends MimeMessage implements Literal {


	public final static String TEXT_SECTION = "Text-Section";
	public final static String ATTACHMENT_SECTION = "Attachment-Section";
	public final static String THUMBNAIL_SECTION = "Thumbnail-Section";
	public final static String SECTION_HEADER = "X-Section-ID";
	public final static String NEW_LINE = "\r\n";

	public final static String HEADER_DATE = "Date";
	public final static String HEADER_FROM = "From";
	public final static String HEADER_TO = "To";
	public final static String HEADER_CC = "Cc";
	public final static String HEADER_BCC = "Bcc";
	public final static String HEADER_SUBJECT = "Subject";
	public final static String HEADER_MESSAGE_ID = "Message-ID";
	public final static String HEADER_X_MSGID = "X-MSGID";
	public final static String HEADER_MESSAGE_TYPE = "Message-Type";
	public final static String HEADER_MESSAGE_SOURCE = "Message-Source";
	public final static String HEADER_CONVERSATION_ID = "Conversation-ID";
	public final static String HEADER_MIME_VERSION = "MIME-Version";

	public final static String HEADER_CONTENT_TYPE = "Content-Type";
	public final static String HEADER_CONTENT_LOCATION = "Content-Location";
	public final static String HEADER_CONTENT_ID = "Content-ID";
	public final static String HEADER_CONTENT_ENCODING = "Content-Transfer-Encoding";
	public final static String ENCODING_BINARY = "binary";

	public static final String MMS = "MMS";

	private String date;
	private String from;
	private String to;
	private String cc;
	private String bcc;
	private String subject;
	private String messageId;
	private String xmsgid;
	private String messageType;
	private String messageSource;
	private String conversationId;

	public final static String FLAG_DELETED = "\\Deleted";
	public final static String FLAG_SEEN = "\\Seen";
	public final static String FLAG_SENT = "$Sent";
	public final static String FLAG_THUMBNAIL = "$Thumbnail";

	public final static String MULTIPART_MIX = "multipart/mixed";

	private boolean deleted = false;
	private boolean seen = false;
	private boolean sent = false;
	private boolean thumbnail = false;
	private long localParticipantId;


	private ArrayList<MimeBodyPart> textSection;
	private ArrayList<MimeBodyPart> attachmentSection;
	private ArrayList<MimeBodyPart> thumbnailSection;
	private int size = 0;

	private HashMap<String, VMAAttachment> attachments;
	private String text;
//	private long modSeq;
	private long pMcr;
	private long sMcr;
	
	private HashMap<String,String> deliveryReports = new HashMap<String, String>();

	public HashMap<String,String> getDeliveryReports() {
		return deliveryReports;
	}
	
	public String getDateStr(){
		return date;
	}

//	public Date getDate(){
//		// RFC 2822 date-time format: http://tools.ietf.org/html/rfc2822#page-14
//		/*
//		 * date-time       =       [ day-of-week "," ] date FWS time [CFWS]
//			day-of-week     =       ([FWS] day-name) / obs-day-of-week
//			day-name        =       "Mon" / "Tue" / "Wed" / "Thu" /
//			                        "Fri" / "Sat" / "Sun"
//			date            =       day month year
//			year            =       4*DIGIT / obs-year
//			month           =       (FWS month-name FWS) / obs-month
//			month-name      =       "Jan" / "Feb" / "Mar" / "Apr" /
//			                        "May" / "Jun" / "Jul" / "Aug" /
//			                        "Sep" / "Oct" / "Nov" / "Dec"
//			day             =       ([FWS] 1*2DIGIT) / obs-day
//			time            =       time-of-day FWS zone
//			time-of-day     =       hour ":" minute [ ":" second ]
//			hour            =       2DIGIT / obs-hour
//			minute          =       2DIGIT / obs-minute
//			second          =       2DIGIT / obs-second
//			zone            =       (( "+" / "-" ) 4DIGIT) / obs-zone
//		 */
////	    Mon, 4 Mar 2013 21:01:51 +0000 (GMT+00:00)
//		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z" , Locale.US);
//		Date date = null;
//		try {
//			date =  sdf.parse(getDateStr());
//		} catch (ParseException e) {
//			Logger.error(getClass(),"Error parsing message date: " + e.getMessage());
//		}
//
//		return date;
//	}

	
	
	public void setDate(String date){
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("Date=" + date);
		}
		this.date = date;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("Text=" + text);
		}
		this.text = text;
	}

	public String getFromStr() {
		return from;
	}

	public void setFrom(String from) {
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("From=" + from);
		}
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("To=" + to);
		}
		this.to = to;
	}

	public String getCc() {
		return cc;
	}

	public void setCc(String cc) {
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("CC=" + cc);
		}
		this.cc = cc;
	}

	public String getBcc() {
		return bcc;
	}

	public void setBcc(String bcc) {
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("BCC=" + bcc);
		}
		this.bcc = bcc;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("Subject=" + subject);
		}
		this.subject = subject;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("messageId=" + messageId);
		}
		this.messageId = messageId;
	}

	public String getXmsgid() {
		return xmsgid;
	}

	public void setXmsgid(String xmsgid) {
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("XmessageId=" + xmsgid);
		}
		this.xmsgid = xmsgid;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("messageType=" + messageType);
		}
		this.messageType = messageType;
	}

	public String getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(String messageSource) {
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("messageSource=" + messageSource);
		}
		// TODO . Temp hack for release 
		if("AUTOREPLY".equalsIgnoreCase(messageSource)||"AUTOFORWARD".equalsIgnoreCase(messageSource)){
		    this.messageSource=MessageSourceEnum.WEB.name();
		}else{
		    this.messageSource = messageSource;
		}
	}

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("conversationId=" + conversationId);
		}
		this.conversationId = conversationId;
	}



	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public boolean isSeen() {
		return seen;
	}

	public void setSeen(boolean seen) {
		this.seen = seen;
	}

	public boolean isSent() {
		return sent;
	}

	public void setSent(boolean sent) {
		this.sent = sent;
	}

	public boolean isThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(boolean thumbnail) {
		this.thumbnail = thumbnail;
	}

	public boolean isMMS(){
		return getMessageType().equalsIgnoreCase(MMS);
	}

	public boolean isSMS(){
		return !isMMS();
	}

	public void saveChanges() throws MessagingException {


		MimeMultipart mmp = new MimeMultipart();

		if (textSection.size() > 1) {

			MimeMultipart mmPart = new MimeMultipart();
			for (MimeBodyPart part : textSection) {
				mmPart.addBodyPart(part);
			}

			MimeBodyPart mbPart = new MimeBodyPart();
			mbPart.setContent(mmPart);
			mbPart.setHeader(SECTION_HEADER, TEXT_SECTION);
			mbPart.setHeader("Content-Type", mmPart.getContentType());

			size += mbPart.getSize();
			mmp.addBodyPart(mbPart);

		} else if ((textSection.size() == 1)) {

			MimeBodyPart mbPart = textSection.get(0);
			mbPart.setHeader(SECTION_HEADER, TEXT_SECTION);

			size += mbPart.getSize();
			mmp.addBodyPart(mbPart);

		}

		// always use multipart/mixed for attachment (for attachment fetch)
		if (attachmentSection.size() > 1) {
			MimeMultipart mmPart = new MimeMultipart();
			for (MimeBodyPart part : attachmentSection) {
				mmPart.addBodyPart(part);
			}

			MimeBodyPart mbPart = new MimeBodyPart();
			mbPart.setContent(mmPart);
			mbPart.setHeader(SECTION_HEADER, ATTACHMENT_SECTION);
			mbPart.setHeader("Content-Type", mmPart.getContentType());

			size += mbPart.getSize();
			mmp.addBodyPart(mbPart);

			//		} else if ((attachmentSection.size() == 1)) {
			//
			//			MimeBodyPart mbPart = attachmentSection.get(0);
			//			mbPart.setHeader(SECTION_HEADER, ATTACHMENT_SECTION);
			//
			//			size += mbPart.getSize();
			//			mmp.addBodyPart(mbPart);

		}  else if ((attachmentSection.size() == 1)) {

			MimeBodyPart mbPart = attachmentSection.get(0);
			mbPart.setHeader(SECTION_HEADER, ATTACHMENT_SECTION);

			size += mbPart.getSize();
			mmp.addBodyPart(mbPart);

		}

		if (thumbnailSection.size() > 1) {
			MimeMultipart mmPart = new MimeMultipart();
			for (MimeBodyPart part : thumbnailSection) {
				mmPart.addBodyPart(part);
			}

			MimeBodyPart mbPart = new MimeBodyPart();
			mbPart.setContent(mmPart);
			mbPart.setHeader(SECTION_HEADER, THUMBNAIL_SECTION);
			mbPart.setHeader("Content-Type", mmPart.getContentType());

			size += mbPart.getSize();
			mmp.addBodyPart(mbPart);

		} else if ((thumbnailSection.size() == 1)) {

			MimeBodyPart mbPart = thumbnailSection.get(0);
			mbPart.setHeader(SECTION_HEADER, THUMBNAIL_SECTION);

			size += mbPart.getSize();
			mmp.addBodyPart(mbPart);

		}

		this.setContent(mmp);
		super.saveChanges();

		super.removeHeader(HEADER_MESSAGE_ID);
		super.removeHeader(HEADER_MIME_VERSION);

		if (from!=null) addHeader(HEADER_FROM, from);
		if (to!=null) addHeader(HEADER_TO, to);
		if (cc!=null) addHeader(HEADER_CC, cc);
		if (bcc!=null) addHeader(HEADER_BCC, bcc);
		if (subject!=null) addHeader(HEADER_SUBJECT, subject);
		if (messageId!=null) addHeader(HEADER_MESSAGE_ID, messageId);
		if (xmsgid!=null) addHeader(HEADER_X_MSGID, xmsgid);
		if (messageType!=null) addHeader(HEADER_MESSAGE_TYPE, messageType);
		if (messageSource!=null) addHeader(HEADER_MESSAGE_SOURCE, messageSource);
		if (conversationId!=null) addHeader(HEADER_CONVERSATION_ID, conversationId);
		if (date!=null) addHeader(HEADER_DATE, date);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		this.writeTo(bos);
		this.size = bos.size();

	}

	public int size() {
		return size;
	}

	public void writeTo(OutputStream os) {
		try {

			super.writeTo(os);

		} catch (Exception e) {
			Logger.error(getClass(), e);
		}
	}

	public MSAMessage() {
		super((Session) null);
		textSection = new ArrayList<MimeBodyPart>();
		attachmentSection = new ArrayList<MimeBodyPart>();
		thumbnailSection = new ArrayList<MimeBodyPart>();
	}

	public MSAMessage(VMAMessage vm) throws MessagingException {

		super((Session) null);
		textSection = new ArrayList<MimeBodyPart>();
		attachmentSection = new ArrayList<MimeBodyPart>();
		thumbnailSection = new ArrayList<MimeBodyPart>();

		setSentDate(vm.getMessageTime());
		setFrom(vm.getSourceAddr());
		setTo(vm.getToAddrs()==null?null:vm.getToAddrs().toString().replace(  "[", "").replace("]","").replace(" ", ""));
		setCc(vm.getCcAddrs()==null?null:vm.getCcAddrs().toString().replace(  "[", "").replace("]","").replace(" ", ""));
		setBcc(vm.getBccAddrs()==null?null:vm.getBccAddrs().toString().replace("[", "").replace("]","").replace(" ", ""));
		setXmsgid(vm.getXmsgId());
		setSubject(vm.getMessageSubject());
		setMessageType(vm.getMessageType().getDesc());
		setMessageSource(vm.getMessageSource().getDesc());

		// For MMS, set message id to mscID/xmsgId, which is from the MMSC
		// For SMS, set message id to yyyyMMddHHmmss-CRC32(participants+msg)
		if (MessageTypeEnum.SMS.equals(vm.getMessageType()) && MessageSourceEnum.PHONE.equals(vm.getMessageSource())) {
			CRC32 crc32 = new CRC32();
			crc32.update((getParticipants(vm.getMdn()) + vm.getMessageText()).getBytes());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			setMessageId(sdf.format(vm.getMessageTime())+"-"+crc32.getValue());
		} else if (MessageTypeEnum.MMS.equals(vm.getMessageType()) && MessageSourceEnum.PHONE.equals(vm.getMessageSource()) && StringUtils.isNotEmpty(vm.getXmsgId())) {
			setMessageId(vm.getXmsgId());
		} else {
			setMessageId(vm.getMessageId());
		}


		// attachments
		if (StringUtils.isNotEmpty(vm.getMessageText())) {
			MimeBodyPart mbPart = new MimeBodyPart();
			mbPart.addHeader(HEADER_CONTENT_TYPE, "text/plain;utf-8");
			mbPart.setHeader(HEADER_CONTENT_ID, "text.txt");
			mbPart.setHeader(HEADER_CONTENT_LOCATION, "text.txt");
			mbPart.setText(vm.getMessageText());
			mbPart.setDisposition(null);
			addText(mbPart);
		}

		if (vm.getAttachments()!=null) {
			int aid = 1;
			for (VMAAttachment attachment : vm.getAttachments()) {

				String attachmentId = StringUtils.isEmpty(attachment.getAttachmentId()) ? "attachment"+ aid : attachment.getAttachmentId();
				aid++;

				MimeBodyPart mbPart = new MimeBodyPart();

				//mbPart.setDataHandler(new DataHandler(new FileDataSource(attachment.getDataURI())));

				mbPart.setDataHandler(new DataHandler(new AndroidDataSource(attachment.getDataURI(), attachment.getAttachmentName(), attachment.getMimeType())));

				//mbPart.setDataHandler(new DataHandler(new FixedDataSource(attachment.getDataURI(), attachment.getAttachmentName(), attachment.getMimeType())));

				String mimeType = attachment.getMimeType();
				mbPart.addHeader(HEADER_CONTENT_TYPE, mimeType);
				mbPart.setHeader(HEADER_CONTENT_ID, attachmentId);
				mbPart.setHeader(HEADER_CONTENT_LOCATION, getAttachmentName(attachment.getAttachmentName(),attachment.getMimeType()));
				mbPart.setHeader(HEADER_CONTENT_ENCODING, ENCODING_BINARY);
				mbPart.setDisposition(null);
				//mbPart.setHeader("Content-Transfer-Encoding", "binary");
				addAttachment(mbPart);

				/*
				if (StringUtils.isNotEmpty(attachment.getThumbnailURI())) {
					mbPart = new MimeBodyPart();

					//mbPart.setDataHandler(new DataHandler(new FileDataSource(attachment.getThumbnailURI())));
                    mbPart.setDataHandler(new DataHandler(new AndroidDataSource(attachment.getThumbnailURI(), attachment.getAttachmentName(), attachment.getMimeType())));

					mbPart.addHeader(HEADER_CONTENT_TYPE, attachment.getMimeType());
					mbPart.setHeader(HEADER_CONTENT_ID, attachmentId);
					mbPart.setHeader(HEADER_CONTENT_LOCATION, new File(attachment.getThumbnailURI()).getName());
					mbPart.setHeader(HEADER_CONTENT_ENCODING, ENCODING_BINARY);
					mbPart.setDisposition(null);
					//mbPart.setHeader("Content-Transfer-Encoding", "binary");

					addThumbnail(mbPart);

				}
				 */
			}
		}
		saveChanges();


	}


	public MSAMessage(InputStream is) throws MessagingException {
		super((Session) null, is);
	}

	public void parse(byte[] bs) throws MessagingException {

		String line = null;
		int i=0;
		char[] cs = null;
		while (i < bs.length-1) {
			if (bs[i] =='\r' && bs[i+1] == '\n') {
				cs = new char[i];
				break;
			}
			i++;
		}

		if (cs!=null) {
			for (int j=0; j<cs.length; j++) {
				cs[j] = (char)bs[j];
			}

			line = String.valueOf(cs);
		}

		int modSeqIdx = line.indexOf("XMCR");
		if (modSeqIdx != -1) {
			int endOfModSeq = line.indexOf(")");
			String modSeqString = line.substring(modSeqIdx, endOfModSeq);
			modSeqString = modSeqString.replace("XMCR (", "");
			modSeqString = modSeqString.replace(")", "");
			String[] xmcrs =  modSeqString.split(" ", 2);
//			modSeq = Long.valueOf(xmcrs[0]);
			// MSA fail over changes 
			pMcr= Long.valueOf(xmcrs[0]);
			sMcr= Long.valueOf(xmcrs[1]);
			
			if(Logger.IS_DEBUG_ENABLED) {
				Logger.debug("XMCR : pMCR="+pMcr+" sMcr="+sMcr);
			}
		}

		int flagsIdx = line.indexOf("FLAGS");
		if (flagsIdx != -1) {
			line = line.substring(flagsIdx+5);
			int endOfFlagsIdx = line.indexOf(")");
			if (flagsIdx != -1) {
				line = line.substring(0, endOfFlagsIdx).replace("(", "");
			}
		}

		if (line!=null) {
			StringTokenizer st = new StringTokenizer(line);
			while (st.hasMoreTokens()) {
				String flag = st.nextToken();
				if (FLAG_SEEN.equalsIgnoreCase(flag)) {
					setSeen(true);
				} else if (FLAG_DELETED.equalsIgnoreCase(flag)) {
					setDeleted(true);
				} else if (FLAG_SENT.equalsIgnoreCase(flag)) {
					setSent(true);
				} else if (FLAG_THUMBNAIL.equalsIgnoreCase(flag)) {
					setThumbnail(true);
				}
			}
		}

		parse(new ByteArrayInputStream(bs));
	}

	protected String removeCr(String s) {
		String ret = s;
		if(s != null) {
			ret = s.replaceAll("\r", "");
		}
		return ret;
	}
	public void parse(InputStream is, boolean fetchattAttachment, Context context , HashSet<String> sendMsgIdCache) throws MessagingException {

		parseFirstLine(is);

		super.parse(is);

		Enumeration<Header> headers = getAllHeaders();
		while (headers.hasMoreElements()) {
			Header header = (Header)headers.nextElement();
			if (HEADER_FROM.equalsIgnoreCase(header.getName())) {
				setFrom(removeCr(header.getValue()));
			} else if (HEADER_TO.equalsIgnoreCase(header.getName())) {
				setTo(removeCr(header.getValue()));
			} else if (HEADER_CC.equalsIgnoreCase(header.getName())) {
				setCc(removeCr(header.getValue()));
			} else if (HEADER_BCC.equalsIgnoreCase(header.getName())) {
				setBcc(removeCr(header.getValue()));
			} else if (HEADER_SUBJECT.equalsIgnoreCase(header.getName())) {
				setSubject(removeCr(header.getValue()));
			} else if (HEADER_MESSAGE_ID.equalsIgnoreCase(header.getName())) {
				setMessageId(removeCr(header.getValue()));
			} else if (HEADER_X_MSGID.equalsIgnoreCase(header.getName())) {
				setXmsgid(removeCr(header.getValue()));
			} else if (HEADER_MESSAGE_TYPE.equalsIgnoreCase(header.getName())) {
				setMessageType(removeCr(header.getValue()));
			} else if (HEADER_MESSAGE_SOURCE.equalsIgnoreCase(header.getName())) {
				setMessageSource(removeCr(header.getValue()));
			} else if (HEADER_CONVERSATION_ID.equalsIgnoreCase(header.getName())) {
				setConversationId(removeCr(header.getValue()));
			} else if (HEADER_DATE.equalsIgnoreCase(header.getName())) {
				setDate(removeCr(header.getValue()));
			}
		}

		DataHandler data = getDataHandler();

		MimeMultipart multiPart = new MimeMultipart(data.getDataSource());

		attachments = new HashMap<String, VMAAttachment>();
		boolean persistAttachement=true;
		if(sendMsgIdCache.contains(getMessageId())){
		    if(Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(),"Already downloaded no need to save the attachement. msgId="+getMessageId());
            }
		    persistAttachement=false;
		}
		
		if(isDeleted()){
		    if(Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(),"Deleted message no need to save the attachement. msgId="+getMessageId());
            }
		    persistAttachement=false;
		}
		
		if(fetchattAttachment) {
			if(Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(),"Processing attachments count=" + multiPart.getCount());
			}
			text = "";		
			try {

				for (int i = 0; multiPart != null && i < multiPart.getCount(); i++) {
					BodyPart part = multiPart.getBodyPart(i);
					if(Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(),"Processing attachment type=" + part.isMimeType(MULTIPART_MIX));
					}
					if (part.isMimeType(MULTIPART_MIX) || part.isMimeType(MULTIPART_MIX + "\r")) {
						MimeMultipart multiPart2 = (MimeMultipart) part.getContent();
						for (int j = 0; multiPart2 != null && j < multiPart2.getCount(); j++) {
							BodyPart part2 = multiPart2.getBodyPart(j);
							part2.setHeader(SECTION_HEADER, ATTACHMENT_SECTION);
							saveAttachment(part2 ,context);
						}
					} else {
						part.setHeader(SECTION_HEADER, ATTACHMENT_SECTION);
						saveAttachment(part ,context);
					}
				}

			} catch (Exception e) {
			    if(Logger.IS_ERROR_ENABLED){
			        Logger.error(getClass(),"Failed to parse Attachements", e);
			    }
			}
		} else {

			String mytext = "";

			try {

				for (int i = 0; multiPart != null && i < multiPart.getCount(); i++) {
					BodyPart part = multiPart.getBodyPart(i);

					String section = null;
					if (part.getHeader(SECTION_HEADER)!=null) {
						section = part.getHeader(SECTION_HEADER)[0];
						section = removeCr(section);
					}

					if (part.getHeader(SECTION_HEADER)!=null && TEXT_SECTION.equalsIgnoreCase(section)) {
						if (part.isMimeType(MULTIPART_MIX)) {
							MimeMultipart multiPart2 = (MimeMultipart) part.getContent();
							for (int j = 0; multiPart2 != null && j < multiPart2.getCount(); j++) {
								BodyPart part2 = multiPart2.getBodyPart(j);
								mytext += part2.getContent();
							}
						} else {
							mytext += part.getContent();
						}
					} else if (persistAttachement && section!=null && (ATTACHMENT_SECTION.equalsIgnoreCase(section) || 
							THUMBNAIL_SECTION.equalsIgnoreCase(section)))  {
					        
						if (part.isMimeType(MULTIPART_MIX)) {
							MimeMultipart multiPart2 = (MimeMultipart) part.getContent();
							for (int j = 0; multiPart2 != null && j < multiPart2.getCount(); j++) {
								BodyPart part2 = multiPart2.getBodyPart(j);
								part2.setHeader(SECTION_HEADER, section);
								    saveAttachment(part2,context);
							}
						} else {
							saveAttachment(part,context);
						}
					} 

				}

			} catch (Exception e) {
				Logger.error(getClass(), e);
			}

			setText(mytext);
		}
	}

	private void parseFirstLine(InputStream is) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		int ch;
		boolean hasLine = false;
		while(true) {
			try {
				ch = is.read();
				if(ch == -1) {
					break;
				}
				hasLine = true;
				os.write(ch);
				if(ch == '\n') {
					break;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		}

		String line = null;
		if(hasLine) {
			byte[] lineBytes = os.toByteArray();
			line = new String(lineBytes);
//			if(Logger.IS_DEBUG_ENABLED) {
//				Logger.debug("\n\n\n\n\n\nGot firstline from parse = " + line);
//			}
			//System.exit(1);
		}

		int modSeqIdx = line.indexOf("XMCR");
		if (modSeqIdx != -1) {
			int endOfModSeq = line.indexOf(")");
			String modSeqString = line.substring(modSeqIdx, endOfModSeq);
			modSeqString = modSeqString.replace("XMCR (", "");
			modSeqString = modSeqString.replace(")", "");
			String[] xmcrs =  modSeqString.split(" ", 2);
//			modSeq = Long.valueOf(xmcrs[0]);
			// MSA fail over changes 
			pMcr= Long.valueOf(xmcrs[0]);
			sMcr= Long.valueOf(xmcrs[1]);
			
			if(Logger.IS_DEBUG_ENABLED) {
				Logger.debug("XMCR : pMCR="+pMcr+" sMcr="+sMcr);
			}

		}

		int flagsIdx = line.indexOf("FLAGS");
		String flagsLine = null;
		if (flagsIdx != -1) {
			flagsLine = line.substring(flagsIdx+5);
			int endOfFlagsIdx = flagsLine.indexOf(")");
			if (endOfFlagsIdx != -1) {
				flagsLine = flagsLine.substring(0, endOfFlagsIdx).replace("(", "");
			}
			if(Logger.IS_DEBUG_ENABLED) {
				Logger.debug("Found flags = " + flagsLine);
			}
		}

		if (flagsLine!=null) {
			StringTokenizer st = new StringTokenizer(flagsLine);
			while (st.hasMoreTokens()) {
				String flag = st.nextToken();
				if (FLAG_SEEN.equalsIgnoreCase(flag)) {
					setSeen(true);
				} else if (FLAG_DELETED.equalsIgnoreCase(flag)) {
					setDeleted(true);
				} else if (FLAG_SENT.equalsIgnoreCase(flag)) {
					setSent(true);
				} else if (FLAG_THUMBNAIL.equalsIgnoreCase(flag)) {
					setThumbnail(true);
				}
			}
		}

		//828442 FETCH (UID 4828442 XMCR (201) FLAGS ($Sent) RFC822.SI4321 Message-ID (23e54212s) XRECIPSTATUS (2222222222 NONE 3333333333DELIVERED 4444444444 FAILED 5555555555 READ)) 
		String recptLine = null;
		int recptIdx = line.indexOf("XRECIPSTATUS");
		if (recptIdx != -1) {
			recptLine = line.substring(recptIdx+12);
			int endOfRecptIdx = recptLine.indexOf(")");
			if (endOfRecptIdx != -1) {
				recptLine = recptLine.substring(0, endOfRecptIdx).replace("(", "");
			}
//			if(Logger.IS_DEBUG_ENABLED) {
//				Logger.debug("Found delivery receipts = " + recptLine);
//			}
		}

		if (recptLine!=null && !StringUtils.isEmpty(recptLine)) {
			StringTokenizer st = new StringTokenizer(recptLine);
			while (st.hasMoreTokens()) {
				String key = st.nextToken();
				String val = st.nextToken();
				if(Logger.IS_DEBUG_ENABLED) {
					Logger.debug("Adding delivery status = " + key + "=" + val);
				}
				deliveryReports.put(key, val);
			}
		}
	}

	/*
	 * 
	 * 90447542 FETCH (UID 90447542 XMCR (165 0) FLAGS () BODY[ATTACHMENTS] {600741}
Content-Type: multipart/mixed; 
	boundary="----=_Part_108_15673831.1347987884951"
X-Section-ID: Attachment-Section

------=_Part_108_15673831.1347987884951
Content-Type: image/jpeg;
 name="2010-03-05_15.41.09.jpg"
Content-Transfer-Encoding: binary
Content-ID: attachment-1
Content-Location: 2010-03-05_15.41.09.jpg.jpg
	 */


	public void parseAttachment(byte[] bs, String attachmentFilePrefix ,Context context) throws MessagingException {
		parseAttachment(new ByteArrayInputStream(bs), attachmentFilePrefix,context);
	}

	public void parseAttachment(InputStream is, String attachmentFilePrefix,Context context) throws MessagingException {

		messageId = attachmentFilePrefix;

		super.parse(is);

		DataHandler data = getDataHandler();

		MimeMultipart multiPart = new MimeMultipart(data.getDataSource());

		attachments = new HashMap<String, VMAAttachment>();
		text = "";

		try {

			for (int i = 0; multiPart != null && i < multiPart.getCount(); i++) {
				BodyPart part = multiPart.getBodyPart(i);
				if (part.isMimeType(MULTIPART_MIX)) {
					MimeMultipart multiPart2 = (MimeMultipart) part.getContent();
					for (int j = 0; multiPart2 != null && j < multiPart2.getCount(); j++) {
						BodyPart part2 = multiPart2.getBodyPart(j);
						part2.setHeader(SECTION_HEADER, ATTACHMENT_SECTION);
						saveAttachment(part2,context);
					}
				} else {
					part.setHeader(SECTION_HEADER, ATTACHMENT_SECTION);
					saveAttachment(part,context);
				}
			}

		} catch (Exception e) {
			Logger.error(getClass(), e);
		}

	}

	/**
	 * Parses the BODY.PEEK[ATTACHMENTS] response from MSA.  Does NOT write attachments to disk
	 * or populate the <code>attachments</code> Map.
	 * 
	 * @param bs
	 * @throws MessagingException
	 */
	public void parseAttachment(byte[] bs) throws MessagingException {
		parseAttachment(new ByteArrayInputStream(bs));
	}

	/**
	 * Parses the BODY.PEEK[ATTACHMENTS] response from MSA.  Does NOT write attachments to disk
	 * or populate the <code>attachments</code> Map.
	 * 
	 * @param is
	 * @throws MessagingException
	 */
	public void parseAttachment(InputStream is) throws MessagingException {

		super.parse(is);

		super.saveChanges();

		super.removeHeader(HEADER_MESSAGE_ID);
		super.removeHeader(HEADER_MIME_VERSION);

	}

	private Uri genSaveUri() {
		return Uri.parse("content://dummy/" + System.currentTimeMillis());
	}

	/**
	 * Writes attachments to disk so different components can access via <code>VMAAttachment</code> get*URI() methods.
	 * Also populates the <code>attachments</code> Map.
	 * 
	 * @param part
	 * @throws Exception
	 */
	private void saveAttachment(BodyPart part , Context context) throws Exception {
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(),"saveAttachments: called for section header: " + part.getHeader(SECTION_HEADER));
		}
		String fileName = null;
		if(part.getHeader(HEADER_CONTENT_LOCATION) != null) { 
			fileName = part.getHeader(HEADER_CONTENT_LOCATION)[0];
		} else {
			fileName = "fileName_" + System.currentTimeMillis(); 
		}
		
		String[] contentTypes = part.getHeader(HEADER_CONTENT_TYPE);
		//Content-Type: image/jpeg;
		//name="PART_1359205223065"
		String contentType = null;
		if(contentTypes != null && contentTypes.length > 0) {
			contentType = contentTypes[0];
			if(contentType.contains("\n")){
	           String[] mContentTypes=contentType.split("\n",2);
	           contentType=mContentTypes[0];
	        }
	        if(contentType.contains(";")){
	           String[] mContentTypes=contentType.split(";",2);
	           contentType=mContentTypes[0];
	        }
			contentType = contentType.replaceAll(";", "");
			contentType = contentType.replaceAll("\r", "");
			if(Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "Part content type=" + contentType);
			}
		}
		

		String contentId = part.getHeader(HEADER_CONTENT_ID)[0];
		if(contentId != null) {
			contentId = contentId.replaceAll("\r", "");
		}

		// gets the content
		Uri storeUri = genSaveUri();

		//String id = contentId.substring(contentId.indexOf("-"));
		VMAAttachment att = attachments.get(contentId);
		if (att == null) {
			att = new VMAAttachment();
		}
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(),"saveAttachments: Saving data for attach contentID=" + contentId + " using  attachment client to: " + storeUri);
		}

		String section = null;
		if (part.getHeader(SECTION_HEADER)!=null) {
			section = part.getHeader(SECTION_HEADER)[0];
			section = removeCr(section);
		}

		if (section!=null && ( ATTACHMENT_SECTION.equalsIgnoreCase(section) || 	    
								THUMBNAIL_SECTION.equalsIgnoreCase(section))) 
		{
			att.setAttachmentId(contentId);
			att.setAttachmentName(fileName);
			att.setMimeType(contentType);
			att.setDataURI(storeUri.toString());
			// Section value is mandatory to identify the attachment as thumbnail 
			att.setXSectionID(section);
		} else {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(),"saveAttachments 1 just setting thumbnail uri to: " + storeUri);
			}
			att.setThumbnailURI(storeUri.toString());
		}
		
		PduPersister p = PduPersister.getPduPersister(context);
        PduPart pduPart = new PduPart();
        pduPart.setContentType(contentType.getBytes());
        pduPart.setName(fileName.getBytes());
        pduPart.setFilename(fileName.getBytes());
        pduPart.setContentId(contentId.getBytes());
        pduPart.setContentTransferEncoding("binary".getBytes());
        // Section ID is used to identify the message is Thumbnail or Attachment.
        pduPart.setContentDisposition(section.getBytes());
        pduPart.setData(part.getInputStream());
        
        
        
        long msgId= System.currentTimeMillis();
        Uri uri = p.persistPartFromStream(pduPart, msgId);
        
        // 
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(),"saveAttachments: Saving data "+ uri+",dummy Mms Gid="+msgId);
        }
 		att.setTempPartUri(uri);	
		attachments.put(contentId, att);

	}
	//OOM Issue Fix
	private Uri persistPart(BodyPart part, long msgId, ContentValues values, String contentType) throws MmsException {

		OutputStream os = null;
		InputStream is = null;
		byte data[] = null;
		final Uri uri = VZUris.getMmsPartsUri(msgId);
		
		if (Logger.IS_DEBUG_ENABLED) {
	    	Logger.debug(getClass(),"Store to parts URI : "+ uri);
		}
		
		Uri res = null;
		try {
			res = ApplicationSettings.getInstance().getContentResolver().insert(uri, values);
			if (Logger.IS_DEBUG_ENABLED) {
		    	Logger.debug(getClass(),"Stored Values to parts  : "+ res);
			}

			if (ContentType.TEXT_PLAIN.equals(contentType)
					|| ContentType.APP_SMIL.equals(contentType)
					|| ContentType.TEXT_HTML.equals(contentType)) {
				
				if (Logger.IS_DEBUG_ENABLED) {
			    	Logger.debug(getClass(),"Part has plain Text  ");
				}
				ContentValues cv = new ContentValues();

				ByteArrayOutputStream toStream = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int length;
				is = part.getInputStream();
				while ((length = is.read(buffer)) > 0) {
					toStream.write(buffer, 0, length);
				}

				toStream.flush();
				toStream.close();
				data = toStream.toByteArray();
				cv.put(Telephony.Mms.Part.TEXT,
						new EncodedStringValue(data).getString());
				if (ApplicationSettings.getInstance().getContentResolver().update(uri, cv, null, null) != 1) {
					throw new MmsException("unable to update " + uri.toString());
				}
			} else {
				os = ApplicationSettings.getInstance().getContentResolver()
						.openOutputStream(res);
				if (data == null) {
					if (Logger.IS_DEBUG_ENABLED) {
				    	Logger.debug(getClass(),"Writing parts to Table : "+res);
					}
					is = part.getInputStream();
					byte[] buffer = new byte[256];
					for (int len = 0; (len = is.read(buffer)) != -1;) {
						os.write(buffer, 0, len);
					}
					
					if (Logger.IS_DEBUG_ENABLED) {
				    	Logger.debug(getClass(),"Writing parts to Table complete : "+ res);
					}
				}

			}
		} catch (Exception e) {
		    if(Logger.IS_DEBUG_ENABLED){
		        Logger.error(getClass(),"Exception while persisting parts"+ e);
		    }
		} finally {
			try {
				if (is != null) {
					is.close();
				}
				if (os != null) {
					os.close();
				}
			} catch (Exception e) {
				Logger.error(getClass(), e);
			}
		}

		return res;

	}

	public VMAMessage toVMAMessage(boolean attachmentsOnly) {
		VMAMessage vm = new VMAMessage();
		try {
			if(attachmentsOnly)  {
				if (attachments!=null && attachments.size()>0) {
					ArrayList<VMAAttachment> atts = new ArrayList<VMAAttachment>();
					for (VMAAttachment att : attachments.values()) {
						atts.add(att);
					}
					vm.setAttachments(atts);
				}
			} else {
				/*
				Address[] addresses = getFrom();
				*/
				ArrayList<String> fromList = new ArrayList<String>();
				/*
				for(Address address: addresses) {
					fromList.add(address.toString());
				}
				*/
				fromList.add(getFromStr());
				vm.setFromList(fromList);
//				vm.setDate(getDate());
				vm.setMessageTime(getSentDate());
				vm.setSourceAddr(getFromStr());
				String toAdrs =getTo();
				//vm.setConversationId(getTo()); // XXX: TO is the same as conversation id ?
				if (StringUtils.isNotEmpty(to)) {
				    ArrayList<String> list = new ArrayList<String>();
				    StringTokenizer st =null;
				    // Full Sync we are getting semicolon and partial sync we are getting comma
				    // Need to find the proper delimiter from server team.
				    if(toAdrs.contains(",")){
				        st=new StringTokenizer(getTo(), ",");
				    }else{
				        st = new StringTokenizer(getTo(), ";");
				    }
					while (st.hasMoreTokens()) {
						list.add(st.nextToken());
					}
					vm.setToAddrs(list);
				}
				String cc = getCc();
				if (StringUtils.isNotEmpty(cc)) {
					ArrayList<String> list = new ArrayList<String>();
                    StringTokenizer st = null;
                    if(cc.contains(",")){
                        st = new StringTokenizer(cc, ",");
                    }else{
                        st = new StringTokenizer(cc, ";");    
                    }
					while (st.hasMoreTokens()) {
						list.add(st.nextToken());
					}
					vm.setCcAddrs(list);
				}
				String bcc = getBcc();
				if (StringUtils.isNotEmpty(bcc)) {
					ArrayList<String> list = new ArrayList<String>();
					StringTokenizer st = null;
					if(bcc.contains(",")){
					    st = new StringTokenizer(bcc, ",");
					}else{
					    st = new StringTokenizer(bcc, ";");    
					}
					while (st.hasMoreTokens()) {
						list.add(st.nextToken());
					}
					vm.setBccAddrs(list);
				}

				// MDN should be set by the calling method, as we don't know here which
				// MDN to use to login to MSA.
				//			vm.setMdn(getFromStr());
				vm.setMdn(getFromStr());
				vm.setMessageId(getMessageId());
				vm.setXmsgId(getXmsgid());
				vm.setMessageSubject(getSubject());
				if (getMessageType()!=null) {
					vm.setMessageType(MessageTypeEnum.valueOf(getMessageType()));
				}
				if (getMessageSource()!=null) {
					vm.setMessageSource(MessageSourceEnum.valueOf(getMessageSource()));
				}
				vm.setMessageText(text);

				if (attachments!=null && attachments.size()>0) {
					ArrayList<VMAAttachment> atts = new ArrayList<VMAAttachment>();
					for (VMAAttachment att : attachments.values()) {
						atts.add(att);
					}
					vm.setAttachments(atts);
				}

				vm.setFlagSeen(isSeen());
				vm.setFlagSent(isSent());
				vm.setFlagDeleted(isDeleted());
			}

		} catch (MessagingException e) {
			Logger.error(getClass(), e);
		}

		return vm;
	}

	public HashMap<String, VMAAttachment> getAttachments() {
		return attachments;
	}

	public void addText(MimeBodyPart text) {
		textSection.add(text);
	}

	public void addAttachment(MimeBodyPart attachment) {
		attachmentSection.add(attachment);
	}

	public void addThumbnail(MimeBodyPart thumbnail) {
		thumbnailSection.add(thumbnail);
	}

	/** 
	 * Gets the content-type for the first attachment of this message.
	 * 
	 * TODO: Assumes only one attachment
	 * 
	 * @return the content-type of the attachment, <code>null</code> on error
	 */
	public String getAttachmentContentType(){
		String contentType = null;
		if ((attachmentSection != null) && (attachmentSection.size() > 0)) {
			// TODO assuming one attachment
			MimeBodyPart mbp = attachmentSection.get(1);
			try {
				contentType = mbp.getContentType();
			} catch (MessagingException e) {
				Logger.error(getClass(), e);
			}
		}else{
			// perhaps this is a message from a UID FETCH
			try {
				MimeMultipart mmp = (MimeMultipart) getContent();
				if (mmp != null) {
					BodyPart bp = mmp.getBodyPart(0);
					if (bp != null) {
						contentType = bp.getContentType();
					}
				}
			} catch (Exception e) {
				Logger.error(getClass(), e);
			}
		}

		return contentType;
	}

	/** 
	 * Gets the bytes from the first attachment of this message.
	 * 
	 * TODO: Assumes only one attachment
	 * 
	 * @return a <code>byte[]</code> containing the binary data of the attachment, <code>null</code> if there is an error
	 */
	public byte[] getAttachmentBytes(){
		byte[] bytes = null;
		if ((attachmentSection != null) && (attachmentSection.size() > 0)) {
			// TODO assuming one attachment
			MimeBodyPart mbp = attachmentSection.get(1);
			try {
				InputStream is = mbp.getInputStream();
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				byte[] b = new byte[1024];
				int len = 0;
				while((len=is.read(b, 0, 1024)) > 0) {
					bos.write(b, 0, len);
				}
				bytes = bos.toByteArray();
			} catch (Exception e) {
				Logger.error(getClass(), e);
			}
		}else{
			// perhaps this is a message from a UID FETCH
			try {
				MimeMultipart mmp = (MimeMultipart) getContent();
				if (mmp != null) {
					BodyPart bp = mmp.getBodyPart(0);
					if (bp != null) {
						InputStream is = bp.getInputStream();
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						byte[] b = new byte[1024];
						int len = 0;
						while((len=is.read(b, 0, 1024)) > 0) {
							bos.write(b, 0, len);
						}
						bytes = bos.toByteArray();					}
				}
			} catch (Exception e) {
				Logger.error(getClass(), e);
			}
		}

		return bytes;
	}


	/**
	 * Updates an existing attachment with a new transcoded one.
	 * 
	 * TODO: Assumes only one attachment
	 * 
	 * @param transcoded
	 */


	@Override
	public String toString() {
		return "MSAMessage2 [attachmentSection=" + attachmentSection + ", attachments=" + attachments + ", bcc=" + bcc + ", cc=" + cc + ", conversationId=" + conversationId + ", date=" + date
				+ ", deleted=" + deleted + ", from=" + from + ", messageId=" + messageId + ", messageSource=" + messageSource + ", messageType=" + messageType + ", seen=" + seen + ", sent=" + sent
				+ ", size=" + size + ", subject=" + subject + ", text=" + text + ", textSection=" + textSection + ", thumbnail=" + thumbnail + ", thumbnailSection=" + thumbnailSection + ", to=" + to
				+ ", xmsgid=" + xmsgid + "]";
	}


	public String getParticipants(String mdn) {
		String participants = null;
		TreeSet<String> set = new TreeSet<String>();
		set.add(from);
		if (StringUtils.isNotEmpty(to)) {
			StringTokenizer st = new StringTokenizer(to.replaceAll(";", ","), ",");
			while (st.hasMoreTokens()) {
				set.add(st.nextToken());
			}
		}
		if (StringUtils.isNotEmpty(cc)) {
			StringTokenizer st = new StringTokenizer(cc.replaceAll(";", ","), ",");
			while (st.hasMoreTokens()) {
				set.add(st.nextToken());
			}
		}
		if (StringUtils.isNotEmpty(bcc)) {
			StringTokenizer st = new StringTokenizer(bcc.replaceAll(";", ","), ",");
			while (st.hasMoreTokens()) {
				set.add(st.nextToken());
			}
		}
		set.remove(mdn);
		for (String addr : set) {
			participants = participants == null ? addr : participants + ";" + addr;
		}
		return participants;
	}

	public static String getAttachmentName(String name, String mimeType) {
		String attName = name;
		
		MimeTypeMap map  = MimeTypeMap.getSingleton();
		String suffix = map.getExtensionFromMimeType(mimeType);
		
		if (suffix !=null) {
			attName = name + "." + suffix;
		}

		return attName;
	}
//	@Deprecated
//	public long getModSeq() {
//		return modSeq;
//	}
	
	
	public long getPrimaryMCR() {
		return pMcr;
	}

	public long getSecondaryMCR() {
		return sMcr;
	}

	public void setLocalParticipantId(long threadId) {
		localParticipantId = threadId;	
	}

	public long getLocalParticipantId() {
		return localParticipantId;	
	}

	/*
	 * Created for debugging purposes
	 * 
	protected class FixedDataSource implements DataSource {

		protected String uri;
		protected String name;
		protected String type;
		protected ByteArrayInputStream bis = new ByteArrayInputStream("Hello World".getBytes());
		//protected InputStream in;

		public FixedDataSource(String uri, String name, String type) {
			this.uri = uri;
			this.name = name;
			this.type = type;
		}

		@Override
		public String getContentType() {
			return type;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			ByteArrayInputStream bis = new ByteArrayInputStream("Hello World".getBytes());
			return bis;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			if(Logger.IS_DEBUG_ENABLED) {
				Logger.debug("VMA: Returning outputstream = null "  + " for uri " + uri);
			}
			return null;
		}	
	}
	 */

	protected class AndroidDataSource implements DataSource {

		protected String uri;
		protected String name;
		protected String type;
		//protected InputStream in;

		public AndroidDataSource(String uri, String name, String type) {
			this.uri = uri;
			this.name = name;
			this.type = type;
		}

		@Override
		public String getContentType() {
			return type;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			InputStream in = null;
			synchronized(this) {
				if(in == null) {
					ContentResolver resolver = ApplicationSettings.getInstance().getContentResolver();
					Uri pUri = Uri.parse(uri);
					in = resolver.openInputStream(pUri);
				}
			}
			if(Logger.IS_DEBUG_ENABLED) {
				Logger.debug("VMA: Returning inputstream = " + in + " for uri " + uri);
			}
			return in;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			if(Logger.IS_DEBUG_ENABLED) {
				Logger.debug("VMA: Returning outputstream = null "  + " for uri " + uri);
			}
			return null;
		}	
	}
}
