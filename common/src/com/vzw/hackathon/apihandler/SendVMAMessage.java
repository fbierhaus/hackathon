package com.vzw.hackathon.apihandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.vzw.vmp.client.CampaignSender;
import com.vzw.vmp.schema.mm7.SubmitRspType;

public class SendVMAMessage extends Thread {
	private static Logger logger = Logger.getLogger(SendVMAMessage.class);


	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SendVMAMessage.sendMMS("9257089093", "9255888998,9253248817", "Jeff changed channel");
	}
	
	public static boolean sendMMS(String from, String to, String message) {
		
		boolean success = false;
		
		try {
	        CampaignSender sender = CampaignSender.getInstance();
	        
	        long ts = System.currentTimeMillis();
	        String dir = "/tmp/mms-"+ts;
	        String filename = dir + "/" + "text.txt";
	        new File(dir).mkdirs();
	        FileWriter file = new FileWriter(filename);
	        
	        file.write(message);
	        file.flush();
	        file.close();
	        

	        
	        StringTokenizer st = new StringTokenizer(to, ",");
	        ArrayList<String> list = new ArrayList<String>();
	        while (st.hasMoreTokens()) {
	        	list.add(st.nextToken());
	        }
	        SubmitRspType rsp = sender.submit(list, dir, false, true, null, "To", from);
	        
	        String result = "from=" + from + ", to=" + list + ", status = " + rsp.getStatus().getStatusCode() + "-" + rsp.getStatus().getStatusText() + "-" + rsp.getStatus().getDetails() + ", messageId = " + rsp.getMessageID();

	        logger.info(result);
	        
	        new File(filename).delete();
	        new File(dir).delete();
        } catch (IOException e) {
	        logger.error("error", e);
        }
		
		return success;
	}
	
}
