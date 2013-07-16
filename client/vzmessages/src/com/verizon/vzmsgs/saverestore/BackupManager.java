package com.verizon.vzmsgs.saverestore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.xml.sax.SAXException;

import android.content.OperationApplicationException;
import android.os.RemoteException;

import com.verizon.mms.ui.RestoreConversationListActivity.ParsePreviewListener;
import com.verizon.vzmsgs.saverestore.BackupManagerImpl.BackUpStatusListener;

/**
 * This class/interface
 * 
 * @author Md Imthiaz
 * @Since  Sep 5, 2012
 */
public interface BackupManager {

	public void saveMessage(long msgID, long threadID, boolean isSMS, String filePath, BackUpStatusListener listener) throws FileNotFoundException, IOException, JSONException, Exception;

	public void saveConversations(ArrayList<Long[]> messageIDs, String filePath, BackUpStatusListener listener, boolean supportMMS) throws FileNotFoundException, IOException, JSONException, Exception;

	public void getConversations(String filePath, ParsePreviewListener mListener, String recipients, boolean fromCovnversation) throws OutOfMemoryError, ParserConfigurationException, SAXException, IOException, Exception;
	
	public ArrayList<BackUpMessage> getMessages(String recipients);
	
	public void restoreConversation(ArrayList<String> recipients,  BackUpStatusListener listener, String filePath) throws OutOfMemoryError, ParserConfigurationException, SAXException, IOException, Exception;
	
	public void restoreMessages(ArrayList<BackUpMessage> messages, BackUpStatusListener listener) throws RemoteException, JSONException, OperationApplicationException, Exception;
	
	public void restoreAll(String filePath, BackUpStatusListener listener) throws ParserConfigurationException, SAXException, IOException, OutOfMemoryError, Exception;

}
