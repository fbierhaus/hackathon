package com.verizon.vzmsgs.saverestore;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.ui.RestoreConversationListActivity.ParsePreviewListener;

public class ConversationHandler extends DefaultHandler {

	private BackUpMessage msg;
	private static ArrayList<String> COMPLEXTAGS;
	private Stack<String> tags;
	private boolean current = false;
	private StringBuilder builder;
	private HashMap<String, String> smsItems;
	private HashMap<String, String> pduItems;
	private ParsePreviewListener parsePreviewListener;

	static {
		COMPLEXTAGS = new ArrayList<String>();
		COMPLEXTAGS.add("sms");
		COMPLEXTAGS.add("mms");
		COMPLEXTAGS.add("pdu");
		COMPLEXTAGS.add("parts");
		COMPLEXTAGS.add("addr");
		COMPLEXTAGS.add("item");
		COMPLEXTAGS.add("message");
	}

	public ConversationHandler(ParsePreviewListener mParsePreviewListener) {

		this.parsePreviewListener = mParsePreviewListener;
	}
	
	/**
	 * Returns the data object
	 * 
	 * @return
	 */
	public BackUpMessage getData() {
		return msg;
	}

	@Override
	public void startDocument() throws SAXException {
		// data = new ArrayList<Object>();
		tags = new Stack<String>();
		super.startDocument();
	}

	@Override
	public void endDocument() throws SAXException {
		// TODO Auto-generated method stub
		super.endDocument();
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		current = true;
		builder = new StringBuilder(1024);
		if (COMPLEXTAGS.contains(localName)) {
			tags.add(localName);
		}
		if (localName.equals("message")) {
			msg = new BackUpMessage();
			smsItems = new HashMap<String, String>();
		} else if (localName.equals("sms")) {
			msg = new BackUpMessage();
			msg.setRecipients(atts.getValue("recipients"));

			msg.setMsgIndex(Integer.valueOf(atts.getValue("id")));
			smsItems = new HashMap<String, String>();
		} else if (localName.equals("mms")) {
			msg = new BackUpMessage();
			msg.setRecipients(atts.getValue("recipients"));

			msg.setMsgIndex(Integer.valueOf(atts.getValue("id")));
		} else if (localName.equals("pdu")) {
			pduItems = new HashMap<String, String>();
		}
		super.startElement(namespaceURI, localName, qName, atts);
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		current = false;
		if (COMPLEXTAGS.contains(localName)) {
			String removedElement = tags.pop();
			if (removedElement.equals("sms")) {
				msg.setPduData(smsItems);
				parsePreviewListener.updatePreviewListArrayList(msg);
			} else if (removedElement.equals("mms")) {
				parsePreviewListener.updatePreviewListArrayList(msg);
			} else if (removedElement.equals("message")) {
				msg.setPduData(smsItems);
				parsePreviewListener.updatePreviewListArrayList(msg);
			} else if (removedElement.equals("pdu")) {
				msg.setPduData(pduItems);
			}
		} else {
			if (!tags.empty()) {
				String topElement = tags.lastElement();
				if (topElement.equals("sms")) {
					if (localName.equals("date")) {
						smsItems.put(localName, builder.toString());
					}

				} else if (topElement.equals("pdu")) {
					if (localName.equals("date")) {
						pduItems.put(localName, builder.toString());
					}

				} else if (topElement.equals("message")) {
					if (localName.equals("date")) {
						smsItems.put(localName, builder.toString());
					}
					if (localName.equals("address")) {
						msg.setRecipients(URLDecoder.decode(builder.toString()));

					}
				}

			}
		}
		if (parsePreviewListener.isCancelled()) {
			if(Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(),"Stopped the XML Parsing");
			}
				throw new SAXException();
		}
		super.endElement(namespaceURI, localName, qName);
	}

	/**
	 * Calling when we're within an element. Here we're checking to see if there
	 * is any content in the tags that we're interested in and populating it in
	 * the Config object.
	 * 
	 * @param ch
	 * @param start
	 * @param length
	 */
	@Override
	public void characters(char ch[], int start, int length) {
		if (current) {
			builder.append(new String(ch, start, length));
		}

	}
}
