package com.verizon.vzmsgs.saverestore;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.verizon.mms.ui.RestoreConversationListActivity.ParsePreviewListener;



/**
 * This class/interface
 * 
 * @author Md Imthiaz
 * @Since Sep 4, 2012
 */
public class DataHandler extends DefaultHandler {

	private static ArrayList<String> COMPLEXTAGS;
	private BackUpMessage msg;
	private Stack<String> tags;
	private HashMap<String, String> smsItems;
	private HashMap<String, String> pduItems;
	private ArrayList<HashMap<String, String>> addrItems;
	private ArrayList<HashMap<String, String>> partsItems;
	private HashMap<String, String> childItems;
	private HashMap<String, String> addrValues;
	private boolean isPartsChild = false;
	private boolean current = false;
	private StringBuffer builder;
	//private ArrayList<BackUpMessage> msgs;
	private ArrayList<String> recipients;
	private boolean iterate;
	private boolean oldXml;
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

	public DataHandler(ParsePreviewListener mParsePreviewListener,
			ArrayList<String> recipients, boolean iterate) {
		this.parsePreviewListener = mParsePreviewListener;
		this.recipients = recipients;
		this.iterate = iterate;
	}
/*
	*//**
	 * Returns the data object
	 * 
	 * @return
	 *//*
	public ArrayList<BackUpMessage> getData() {
		return msgs;
	}*/

	/**
	 * This gets called when the xml document is first opened
	 * 
	 * @throws SAXException
	 */
	@Override
	public void startDocument() throws SAXException {
		//msgs = new ArrayList<BackUpMessage>();
		tags = new Stack<String>();
	}

	/**
	 * Called when it's finished handling the document
	 * 
	 * @throws SAXException
	 */
	@Override
	public void endDocument() throws SAXException {

	}

	/**
	 * This gets called at the start of an element. Here we're also setting the
	 * booleans to true if it's at that specific tag. (so we know where we are)
	 * 
	 * @param namespaceURI
	 * @param localName
	 * @param qName
	 * @param atts
	 * @throws SAXException
	 */
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {

		current = true;
		builder = new StringBuffer(10240);
		if (COMPLEXTAGS.contains(localName)) {
			tags.add(localName);
		}
		if (localName.equals("messages")) {
			//3.0 and above xml
			oldXml = false;
		} else if (localName.equals("file")) {
			//Old VZM xml
			oldXml = true;
		} else if (localName.equals("message")) {
			msg = new BackUpMessage();
			msg.setSms(true);
			smsItems = new HashMap<String, String>();
		} else if (localName.equals("sms")) {
			msg = new BackUpMessage();
			msg.setSms(true);
			msg.setRecipients(atts.getValue("recipients"));
			msg.setMsgIndex(Integer.valueOf(atts.getValue("id")));
			smsItems = new HashMap<String, String>();
		} else if (localName.equals("mms")) {
			msg = new BackUpMessage();
			msg.setSms(false);
			msg.setRecipients(atts.getValue("recipients"));
			msg.setMsgIndex(Integer.valueOf(atts.getValue("id")));
		} else if (localName.equals("item") && atts.getLength() > 0) {
			addrValues = new HashMap<String, String>();
			addrValues.put(atts.getLocalName(0), atts.getValue(0));
			addrValues.put(atts.getLocalName(1), atts.getValue(1));
			addrValues.put(atts.getLocalName(2), atts.getValue(2));
		} else if (localName.equals("pdu")) {
			pduItems = new HashMap<String, String>();
		} else if (localName.equals("parts")) {
			isPartsChild = true;
			partsItems = new ArrayList<HashMap<String, String>>();

		} else if (localName.equals("addr")) {
			addrItems = new ArrayList<HashMap<String, String>>();

		} else if (localName.equals("item")) {
			childItems = new HashMap<String, String>();

		}
	}

	/**
	 * Called at the end of the element. Setting the booleans to false, so we
	 * know that we've just left that tag.
	 * 
	 * @param namespaceURI
	 * @param localName
	 * @param qName
	 * @throws SAXException
	 */
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {

		current = false;

		if (COMPLEXTAGS.contains(localName)) {
			String removedElement = tags.pop();
			if (removedElement.equals("sms")) {
				msg.setPduData(smsItems);
				if (recipients != null) {
					if (!iterate) {

						if (msg.getRecipients().equalsIgnoreCase(
								recipients.get(0))) {
							if (parsePreviewListener != null) {
								parsePreviewListener
										.updatePreviewMessageList(msg);
							}

						}
					} else {
						for (String recipient : recipients) {

							if (msg.getRecipients().equalsIgnoreCase(recipient)) {
								if (parsePreviewListener != null) {
									parsePreviewListener
											.updatePreviewMessageList(msg);
								}
								//msgs.add(msg);
							}
						}
					}

				} else {
					if (parsePreviewListener != null) {
						parsePreviewListener
								.updatePreviewMessageList(msg);
					}
					//msgs.add(msg);
				}

				
			} else if (removedElement.equals("mms")) {
				if (recipients != null) {
					if (!iterate) {

						if (msg.getRecipients().equalsIgnoreCase(
								recipients.get(0))) {

							if (parsePreviewListener != null) {
								parsePreviewListener
										.updatePreviewMessageList(msg);
							}
						}
					} else {
						for (String recipient : recipients) {

							if (msg.getRecipients().equalsIgnoreCase(recipient)) {
								if (parsePreviewListener != null) {
									parsePreviewListener
											.updatePreviewMessageList(msg);
								}
								//msgs.add(msg);
							}
						}
					}
				} else {
					if (parsePreviewListener != null) {
						parsePreviewListener
								.updatePreviewMessageList(msg);
					}
					//msgs.add(msg);
				}
			} else if (removedElement.equals("message")) {
				msg.setPduData(smsItems);
				if (recipients != null) {
					if (!iterate) {

						if (msg.getRecipients().equalsIgnoreCase(
								recipients.get(0))) {
							if (parsePreviewListener != null) {
								parsePreviewListener
										.updatePreviewMessageList(msg);
							}

						}
					} else {
						for (String recipient : recipients) {

							if (msg.getRecipients().equalsIgnoreCase(recipient)) {
								if (parsePreviewListener != null) {
									parsePreviewListener
											.updatePreviewMessageList(msg);
								}
								//msgs.add(msg);
							}
						}
					}
				} else {
					if (parsePreviewListener != null) {
						parsePreviewListener
								.updatePreviewMessageList(msg);
					}
					//msgs.add(msg);
				}
			} else if (removedElement.equals("pdu")) {
				msg.setPduData(pduItems);
			} else if (removedElement.equals("addr")) {
				msg.setAddressData(addrItems);
			} else if (removedElement.equals("parts")) {
				isPartsChild = false;
				msg.setPartsData(partsItems);
			} else if (removedElement.equals("item")) {
				if (tags.lastElement().equals("addr")) {
					addrItems.add(addrValues);
				} else if (tags.lastElement().equals("parts")) {
					partsItems.add(childItems);

				}

			}

		} else {
			if (!tags.empty()) {
				String topElement = tags.lastElement();
				if (topElement.equals("sms")) {
					smsItems.put(localName, builder.toString());
				} else if (topElement.equals("pdu")) {
					pduItems.put(localName, builder.toString());
				} else if (topElement.equals("message")) {
					if (localName.equals("body")) {
						smsItems.put(localName, URLDecoder.decode(builder.toString()));
					} else {
						smsItems.put(localName, builder.toString());	
					}
					if (localName.equals("address")) {
						msg.setRecipients(URLDecoder.decode(builder.toString()));
					}
				} else if (topElement.equals("item")) {
					if (isPartsChild) {
						String bufferin = builder.toString();
						childItems.put(localName, bufferin);
					}

				}
			}

		}

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
