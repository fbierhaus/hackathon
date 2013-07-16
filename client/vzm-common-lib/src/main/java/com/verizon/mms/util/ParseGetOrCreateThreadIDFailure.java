package com.verizon.mms.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentValues;
import android.content.Context;
import android.provider.Telephony.Sms.Inbox;

import com.strumsoft.android.commons.logger.Logger;

public class ParseGetOrCreateThreadIDFailure {
	File cacheDir ;
	ContentValues values ;
	 boolean current = false;
	static ParseGetOrCreateThreadIDFailure mParseGetOrCreateThreadIDFailure = null;;
	private ParseGetOrCreateThreadIDFailure(Context context) {
		cacheDir = new File(context.getCacheDir().toString()+"/FailedThreads");
	}
	public static ParseGetOrCreateThreadIDFailure getInstance(Context context) {
		if (mParseGetOrCreateThreadIDFailure == null) {
			mParseGetOrCreateThreadIDFailure = new ParseGetOrCreateThreadIDFailure(context);
		}
		return mParseGetOrCreateThreadIDFailure;
	}
	public ContentValues parseSMS(String fileName) {
		
		SAXParserFactory spf = null;
		File parseFile = null;
		FileInputStream xmlInputStream = null;
		SAXParser saxParser =  null;
		values = new ContentValues();
		try {
			spf = SAXParserFactory.newInstance();
			parseFile = new File(cacheDir,fileName);
		//	parseFile.setReadOnly();
		    xmlInputStream = new FileInputStream(parseFile);
			saxParser = spf.newSAXParser();
			saxParser.parse(xmlInputStream, new DefaultHandler() { 
				StringBuilder data = null;
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
					current = true;
					data = new StringBuilder(1024);
				}
				
				public void characters(char ch[], int start, int length) throws SAXException {
					if(current) {
						data.append(new String(ch, start, length));
					}
				}
				
				public void endElement(String uri, String localName,String qName) throws SAXException {
					current = false;
					if(localName.equals((Inbox.ADDRESS))) {
						values.put(Inbox.ADDRESS, data.toString());
					} else if (localName.equals((Inbox.DATE))) {
						values.put(Inbox.DATE,  data.toString());
					} else if (localName.equals((Inbox.PROTOCOL))) {
						values.put(Inbox.PROTOCOL,  data.toString());	
					} else if (localName.equals((Inbox.READ))) {
						values.put(Inbox.READ,  data.toString());
					} else if (localName.equals((Inbox.SEEN))) {
						values.put(Inbox.SEEN,  data.toString());
					} else if (localName.equals((Inbox.SUBJECT))) {
						values.put(Inbox.SUBJECT,  data.toString());
					} else if (localName.equals((Inbox.REPLY_PATH_PRESENT))) {
						values.put(Inbox.REPLY_PATH_PRESENT,  data.toString());
					} else if (localName.equals((Inbox.SERVICE_CENTER))) {
						values.put(Inbox.SERVICE_CENTER,  data.toString());
					} else if (localName.equals((Inbox.BODY))) {
						if(data.toString().startsWith("<![CDATA[")) {
							String body = data.toString().substring(9, data.toString().length()-3);
							values.put(Inbox.BODY,  body);
						}
					}
				}
			});
			
		}
		catch(Exception e) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.error(ParseGetOrCreateThreadIDFailure.class,e);
			}
		}
		finally {
			if(xmlInputStream != null)
				try {
					xmlInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return values;
	}
	public RestoreMMS parseMMS(String fileName) {
		
		SAXParserFactory spf = null;
		File parseFile = null;
		FileInputStream xmlInputStream = null;
		SAXParser saxParser =  null;
		values = new ContentValues();
		final RestoreMMS restoreMMS = new RestoreMMS();
		try {
			spf = SAXParserFactory.newInstance();
			parseFile = new File(cacheDir,fileName);
		//	parseFile.setReadOnly();
		    xmlInputStream = new FileInputStream(parseFile);
			saxParser = spf.newSAXParser();
			saxParser.parse(xmlInputStream, new DefaultHandler() { 
				StringBuilder data = null;
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
					current = true;
					data = new StringBuilder(1024);
					if (localName.equals("Cust_Address")) {
						ContentValues addressValues = new ContentValues();
						for (int i = 0;i < attributes.getLength(); i++) {
							String key = attributes.getQName(i);
							String value = attributes.getValue(i);
							addressValues.put(key, value);
						}
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(ParseGetOrCreateThreadIDFailure.class,"Address Values :"+addressValues);
						}
						restoreMMS.addAddressValues(addressValues);
					}
				}
				
				public void characters(char ch[], int start, int length) throws SAXException {
					if(current) {
						data.append(new String(ch, start, length));
					}
				}
				
				public void endElement(String uri, String localName,String qName) throws SAXException {
					current = false;
					if(!(localName.equals("Cust_Receipients")) &&!(localName.equals("Cust_Address")) && !(localName.equals("MMS_RECEIVED")) && !(localName.equals("MMS")) ) {
						if(localName.trim().equals("ct_l")) {
							String ct_l = data.toString().substring(9, data.toString().length()-3);
							values.put(localName, ct_l);
						} else { 
							values.put(localName, data.toString());
						}
					} else if (localName.equals("Cust_Receipients")) {
						HashSet<String> recipients = new HashSet<String>();
						String recip = data.toString();
						StringTokenizer tokenizer = new StringTokenizer(recip,"|");
						while(tokenizer.hasMoreTokens()) {
							String recipt = tokenizer.nextToken();
							recipients.add(recipt);
						}
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(ParseGetOrCreateThreadIDFailure.class,"Recipients :"+recipients);
						}
						restoreMMS.recipients = recipients;
					} 
				}
			});
			
		}
		catch(Exception e) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.error(ParseGetOrCreateThreadIDFailure.class,e);
			}
		}
		finally {
			if(xmlInputStream != null)
				try {
					xmlInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		restoreMMS.values = values;
		return restoreMMS;
	}
	
	
}
class RestoreMMS {
	 public ContentValues values = null;
	 public HashSet<String> recipients = null ;
	 public ArrayList<ContentValues> addressValues = new ArrayList<ContentValues>();

	public RestoreMMS() {
		
	}
	public void addAddressValues (ContentValues values){
		addressValues.add(values);
	}
	@Override
	public String toString() {
		String display = "values : "+values +"recipients : "+ recipients + "addressValues : " +addressValues;
		return display;
	}
}
