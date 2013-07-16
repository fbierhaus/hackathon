package com.verizon.vzmsgs.saverestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xmlpull.v1.XmlSerializer;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Xml;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.model.SmilHelper;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.ui.RestoreConversationListActivity.ParsePreviewListener;
import com.verizon.mms.ui.SaveRestoreActivity;
import com.verizon.vzmsgs.saverestore.BackupManagerImpl.BackUpStatusListener;

/**
 * This class/interface
 * 
 * @author Md Imthiaz
 * @Since Sep 6, 2012
 */
public class XmlParser {

	private MessagesDaoImpl msgDAO;
	private Context mContext;
	private Integer restoreStatus;
	private File root;
	private File saveFile;
	private File csvFile;
	// XML Tag Variables
	private final String MESSAGES = "messages";
	private final String VERSION = "ver";
	private final String RECIPIENTS = "recipients";
	private final String ID = "id";
	private final String VERSIONNO = "3.0.1";
	private final String SMS = "sms";
	private final String MMS = "mms";
	private final String PDU = "pdu";
	private final String ADDR = "addr";
	private final String PARTS = "parts";
	private final String ITEM = "item";

	// Final Variables
	private static final int EMPTY_FILE = 0;
	private static final int XML_VERSION = 2;
	private static final int OTHER_VERSION = 3;
	private static final int SUCCEEDED = 4;
	private  boolean isSDCardMounted = true;

	public XmlParser(Context context) {
		mContext = context;
		msgDAO = new MessagesDaoImpl(mContext);
	}

	public void createXMLFromData(ArrayList<Long[]> msgIDs, String filePath,
			BackUpStatusListener listener) throws FileNotFoundException,
			IOException, JSONException, SDCardUnmountException,Exception {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "Saving conversations...");
		} 
		saveFile = new File(filePath);
		saveFile.createNewFile();
		FileOutputStream fos = new FileOutputStream(saveFile);
		XmlSerializer serializer = Xml.newSerializer();
		serializer.setOutput(fos, "UTF-8");
		serializer.startDocument("UTF-8", true);
		serializer.startTag("", MESSAGES.trim());
		serializer.attribute("", VERSION.trim(), VERSIONNO.trim());
		int totalCount = msgIDs.size();
		int progressCount = 0;
		for (int index = 0; index < totalCount; index++) {
           if(isSDCardMounted) {
        		if (listener.isTaskCancelled()) {
					fos.close();
					saveFile.delete();
					return;
				}
				listener.updateStatus(++progressCount, totalCount);
				boolean isSMS = msgIDs.get(index)[1] == 1 ? true : false;
				saveData(serializer, msgIDs.get(index)[0], isSMS, index);
           } else {
        	   	if (Logger.IS_DEBUG_ENABLED) {
        	   		Logger.debug(getClass(), "Saving conversations stopped as SDCard Unmounted...");
       			}
        		throw new SDCardUnmountException();
          }
		}
		serializer.endTag("", MESSAGES.trim());
		serializer.endDocument();
		serializer.flush();
		fos.close();
	}
   

	/*public void createCSVFileData(ArrayList<Long[]> msgIDs, String filePath,
			BackUpStatusListener listener) throws FileNotFoundException,
			IOException, JSONException, SDCardUnmountException,Exception {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "CSV..Saving conversations...");
		} 
		csvFile = new File(filePath);
		csvFile.createNewFile();
		FileWriter writeFile = new FileWriter(csvFile);
        CSVWriter csvWrite = new CSVWriter(writeFile);
        String[] csvValues  = new String[] { Sms.ADDRESS, Sms.BODY , Sms.DATE , Sms.TYPE, Mms.SUBJECT }; 
        //TODO : Need to add more MMS Columns Additionally if required
        csvWrite.writeNext(csvValues);
		int totalCount = msgIDs.size();
		int progressCount = 0;
		for (int index = 0; index < totalCount; index++) {
           if(isSDCardMounted) {
        		if (listener.isTaskCancelled()) {
					saveFile.delete();
					return;
				}
				listener.updateStatus(++progressCount, totalCount);
				boolean isSMS = msgIDs.get(index)[1] == 1 ? true : false;
				saveCSVFile(csvWrite, msgIDs.get(index)[0], isSMS, index);
           } else {
        	   	if (Logger.IS_DEBUG_ENABLED) {
        	   		Logger.debug(getClass(), "Saving conversations stopped as SDCard Unmounted...");
       			}
        		throw new SDCardUnmountException();
          }
		}
		csvWrite.close();
		
	}*/
	
	
	public void createXMLFromData(long msgID, boolean isSMS, String filePath,
			BackUpStatusListener listener) throws FileNotFoundException,
			IOException, JSONException,SDCardUnmountException, Exception {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "Saving messages...");
		}
		if (isSDCardMounted) {
			saveFile = new File(filePath);
			saveFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(saveFile);
			XmlSerializer serializer = Xml.newSerializer();
			serializer.setOutput(fos, "UTF-8");
			serializer.startDocument("UTF-8", true);
			serializer.startTag("", MESSAGES.trim());
			serializer.attribute("", VERSION.trim(), VERSIONNO.trim());
			listener.updateStatus(1, 1);
			saveData(serializer, msgID, isSMS, 0);
			serializer.endTag("", MESSAGES.trim());
			serializer.endDocument();
			serializer.flush();
			fos.close();
		} else {
			throw new SDCardUnmountException();
		}
	}

	public ArrayList<BackUpMessage> parseXML(String filePath,
			boolean fromConversations, ArrayList<String> recipients,
			ParsePreviewListener mParsePreviewListener, boolean iterate) throws 
				ParserConfigurationException, SAXException, IOException, OutOfMemoryError, Exception {
		ArrayList<BackUpMessage> msgs = new ArrayList<BackUpMessage>();

		File parseFile = new File(filePath);
		parseFile.setReadOnly();
		if (parseFile.length() <= 0) {
			restoreStatus = EMPTY_FILE;
			return msgs;
		}
		FileInputStream fis = null;
		// SAX PARSER
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			ConversationHandler conversationHandler = null;
			DataHandler dataHandler = null;
			XMLReader xr = sp.getXMLReader();
			if (fromConversations) {
				conversationHandler = new ConversationHandler(
						mParsePreviewListener);
				xr.setContentHandler(conversationHandler);
			} else {
				dataHandler = new DataHandler(mParsePreviewListener,
						recipients, iterate);
				xr.setContentHandler(dataHandler);

			}
			fis = new FileInputStream(parseFile);
			if (fis.getChannel() == null) {
				restoreStatus = OTHER_VERSION;
				return msgs;
			}
			InputSource is = new InputSource(fis);
			is.setEncoding("UTF-8");
			xr.parse(is);
			if (iterate) {
				if (dataHandler != null) {
					//msgs = dataHandler.getData();
				}
			}
			if (conversationHandler != null) {
				BackUpMessage msg = conversationHandler.getData();
				if (msg.getRecipients() == null) {
					restoreStatus = EMPTY_FILE;
				} else {
					restoreStatus = SUCCEEDED;
				}
			}
		} 
			
		 finally {
			if (fis != null) {
				fis.close();
			}
		}

		return msgs;
	}
	
	//TODO : Vz3.2 Release
	
   /* private void saveCSVFile(CSVWriter csvWrite, long msgID, boolean isSMS, int index) throws FileNotFoundException, IOException, JSONException, Exception {
    	
    	if (isSMS) {
    		if (Logger.IS_DEBUG_ENABLED) {
    			Logger.debug(getClass(), "Saving CSV for SMS of MSGID :: " + msgID);
    		}
    		HashMap<String, String> colValues = msgDAO.getSMS(msgID);
    		String[] csvValues = new String[5];
            Set<String> keyset = colValues.keySet();
            Iterator<String> itr = keyset.iterator();
            while(itr.hasNext()) {
            	String key = itr.next();
            	if (key.equalsIgnoreCase(Sms.ADDRESS)) {
            		csvValues[0] = colValues.get(key);
            	} else if (key.equalsIgnoreCase(Sms.BODY)) {
            		csvValues[1] = colValues.get(key);
            	}  else if (key.equalsIgnoreCase(Sms.DATE)) {
            		csvValues[2] = colValues.get(key);
            	}  else if (key.equalsIgnoreCase(Sms.TYPE)) {
            		csvValues[3] = colValues.get(key);
            	}
            }
            csvWrite.writeNext(csvValues);
            


            
	
    		
    		
    	} else {
    		//MMS Only text part to be saved
    		if (Logger.IS_DEBUG_ENABLED) {
    			Logger.debug(getClass(), "Saving CSV for MMS of MSGID :: " + msgID);
    		}
    	}
    }*/
	
    private void saveData(XmlSerializer serializer, long msgID, boolean isSMS,
			int index) throws FileNotFoundException, IOException, JSONException, Exception{
		JSONMessage message = null;
		// Saving SMS
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "Checking ::" + msgID + "is MMS or SMS");
		}
		if (isSMS) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "MessageID ::" + msgID
						+ "is saved as SMS");
			}
			message = msgDAO.loadSMS(msgID);
			if (message.getRecipients() == null) {
				return; // Handled if message is deleted from conversation
			}
			serializer.startTag("", SMS.trim());
			serializer
					.attribute("", RECIPIENTS.trim(), message.getRecipients());
			serializer.attribute("", ID.trim(), String.valueOf(index));
			ContentValues values = msgDAO.toContentValues(message.getPdu());
			addDataInXML(values, serializer);
			serializer.endTag("", SMS.trim());
		} else {
			// Saving MMS
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "MessageID ::" + msgID
						+ "is saved as MMS");
			}
			message = msgDAO.loadMMS(msgID);
			if (message.getRecipients() == null) {
				return;
			}
			// Check if MMS downloaded or delivery msg
			String mTypeString = (String) message.getPdu().get("m_type");
			int mType = Integer.valueOf(mTypeString);
			if (mType == PduHeaders.MESSAGE_TYPE_SEND_REQ
					|| mType == PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF) {

				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "Started saving as MMS ::" + msgID);
				}
				serializer.startTag("", MMS.trim());
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(),
							"Adding recipients :: " + message.getRecipients());
				}
				// Recipients
				serializer.attribute("", RECIPIENTS.trim(),
						message.getRecipients());
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "Adding index :: " + index);
				}
				serializer.attribute("", ID.trim(), String.valueOf(index));
				// PDU
				serializer.startTag("", PDU.trim());
				ContentValues values = msgDAO.toContentValues(message.getPdu());
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(),
							"Adding PDU :: " + values.toString());
				}
				addDataInXML(values, serializer);
				serializer.endTag("", PDU.trim());
				// Address
				serializer.startTag("", ADDR.trim());
				for (JSONObject adr : message.getAddresses()) {
					ContentValues addressValues = msgDAO.toContentValues(adr);
					serializer.startTag("", ITEM.trim());
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "Adding Address :: "
								+ addressValues.toString());
					}
					addAddressInXML(addressValues, serializer);
					serializer.endTag("", ITEM.trim());
				}
				serializer.endTag("", ADDR.trim());
				// Parts
				serializer.startTag("", PARTS.trim());
				for (JSONObject parts : message.getParts()) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "Adding Parts");
					}
					ContentValues partValues = msgDAO.toContentValues(parts);
					serializer.startTag("", "item");
					addDataInXML(partValues, serializer);
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "Added Parts successfully");
					}
					serializer.endTag("", "item");
				}
				serializer.endTag("", PARTS.trim());
				serializer.endTag("", MMS.trim());
			}
		}
	}

	private void addAddressInXML(ContentValues values, XmlSerializer serializer)
			throws IOException {
		Set<Entry<String, Object>> entries = null;
		Iterator itr = null;
		entries = values.valueSet();
		itr = entries.iterator();
		while (itr.hasNext()) {
			Map.Entry item = (Map.Entry) itr.next();
			String key = item.getKey().toString();
			String value = (String) item.getValue();
			if (key.equals("msg_id")) {
				continue;
			}
			serializer.attribute("", key, value);
		}
	}
  
	/* private void addDataInCSV(ContentValues values) {
    	Set<Entry<String, Object>> entries = null;
		Iterator itr = null;
		entries = values.valueSet();
		itr = entries.iterator();
		while (itr.hasNext()) { 
			Map.Entry item = (Map.Entry) itr.next();
			String key = item.getKey().toString();
			String value = (String) item.getValue();
			
		}
    }*/
	
	private void addDataInXML(ContentValues values, XmlSerializer serializer)
			throws FileNotFoundException, IOException, Exception{
		Set<Entry<String, Object>> entries = null;
		Iterator itr = null;
		entries = values.valueSet();
		itr = entries.iterator();
		while (itr.hasNext()) {
			Map.Entry item = (Map.Entry) itr.next();
			String key = item.getKey().toString();
			String value = (String) item.getValue();
			serializer.startTag("", key);
			if (key.equals("_data")) {
				String dirName = saveFile.getName();
				dirName = dirName.substring(0, dirName.length() - 4);
				
				/* root = new File(saveFile.getParent(), dirName + "-parts"); */
				root = new File(SaveRestoreActivity.rootFile, dirName + "-parts");
				if (!root.exists()) {
					root.mkdir();
				}
				String partName = String.valueOf(System.currentTimeMillis());
				String partsFileName = root + "/" + partName;
				File backUpFile = new File(partsFileName);

				InputStream fis = null;
				FileOutputStream fos = null;
				try {

					fis = mContext.getContentResolver().openInputStream(
							Uri.parse(value));
					fos = new FileOutputStream(backUpFile);
					byte[] buffer = new byte[1024];
					int bytesRead;

					while ((bytesRead = fis.read(buffer)) != -1) {
						fos.write(buffer, 0, bytesRead);
					}
				} catch (FileNotFoundException e) {
					throw e;
				} catch (IOException e) {
					throw e;
				}
				catch(Exception e) {
					throw e;
				}
				finally {
					if (fis != null) {
						fis.close();
					}
					if (fos != null) {
						fos.close();
					}
				}
				/*
				 * serializer.text(getCDataString("/"+ dirName +"/" + dirName +
				 * "-parts" + "/" + partName));
				 */
				serializer.text(getCDataString("/" + dirName + "-parts" + "/"
						+ partName));
			} else if (key.equals("cid")) {
				serializer.text(getCDataString(value));
			} else if (key.equals("body") || key.equals("text")) {
				String outputString = "";
				outputString = SmilHelper.escapeXML(value);
				try {
					serializer.text(outputString);
				} catch (IllegalArgumentException e) {

					serializer.text("UnKnown");
					e.printStackTrace();
				}

			} else {
				serializer.text(value);
			}
			serializer.endTag("", key);

		}

	}

	public Integer getParseStatus() {
		if (restoreStatus != null) {
			return restoreStatus;
		} else {
			return XML_VERSION;
		}

	}

	private String getCDataString(String value) {
		return "<![CDATA[" + value + "]]>";
	}
	public void setSDCardMounted (boolean status) {
		isSDCardMounted = status;
	}

}
