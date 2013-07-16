package com.verizon.mms.ui;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.ArrayListCursor;


public class ContactSearchTask extends AsyncTask<String, Void, Cursor> {
    private static final String   TAG                  = "ContactSearchTask";

    private Context               mContext;
    private ContactSearchListener mSearchListener;

    public static final int       INDEX                = 0;
    public static final int       CONTACT_ID           = 1;
    public static final int       CONTACT_DISPLAY_NAME = 2;
    public static final int       CONTACT_DATA         = 3;
    public static final int       MIME_TYPE            = 4;
    public static final int       PHONE_TYPE           = 5;
    public static final int       PHONE_LABEL          = 6;
    public String                 mSearchString;
    
    private boolean isAutoTerminate = false;
    public final static String CONTACT_SEPARATOR = ";";//Contact Separator  

    private static final String[] PROJECTION_PHONE     = new String[] { ContactsContract.Contacts._ID,
            ContactsContract.Data.CONTACT_ID, ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Data.DATA1, ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.LABEL,"display_name_alt" };

    public interface ContactSearchListener {
        public void updateContactList(Cursor cursor, String searchString ,boolean isAutoTerminate );
    }

    public ContactSearchTask(Context context, ContactSearchListener listener) {
        mContext = context;
        mSearchListener = listener;
    }

    protected void onPreExecute() {

    };
   
    @Override
    protected Cursor doInBackground(String... params) {
        String search = params[0];
        /*
         * if the user trying to enter multiple contacts with the help of a separator ";" 
         *  eg(080982536;98765678)
         */
        if (search.length() > CONTACT_SEPARATOR.length() && search.endsWith(CONTACT_SEPARATOR)){
        	isAutoTerminate = true; 
        	search = search.substring(0, search.indexOf(CONTACT_SEPARATOR));
        }
        else {        	
        	isAutoTerminate = false;
        }
        return getMatchingAttendeesFromPB(search.trim());
    }

    @Override
    protected void onPostExecute(Cursor matchingContacts) {
    	if (Logger.IS_DEBUG_ENABLED) {
    		Logger.debug(getClass(), "onPostExecute: cur = " + matchingContacts);
    	}
    	if (matchingContacts != null) {
    		mSearchListener.updateContactList(matchingContacts, mSearchString, isAutoTerminate);
    	}
    }

    /**
     * 
     * This Method searches for all the contacts containing the search string in name and phone number 
     * @param search
     * @return
     */
    protected Cursor getMatchingAttendeesFromPB(String search) {
        Uri uri = ContactsContract.Data.CONTENT_URI;
        String searchFirstName =  search + "%";
        String searchLasttName =  search + "%";
        String searchDisplayName = "%"+ search + "%";
        String selection;
        String sortOrder = ContactsContract.Contacts.DISPLAY_NAME  + " COLLATE LOCALIZED ASC";
        boolean hasOnlyNumbers = false;
        hasOnlyNumbers = Pattern.matches("[0-9,),(,-]+", search.replaceAll("\\s+", ""));
        String searchNumber = searchDisplayName;
        if (hasOnlyNumbers) {
            searchNumber = "%" + search.replaceAll("\\D+", "") + "%";
        }

//        	selection = "(" + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "') AND (" +
//            		"REPLACE("+ContactsContract.Data.DATA1 + ",'-','')"+" LIKE ? OR "+
//            		"display_name_alt LIKE  ? OR "+
//            		ContactsContract.Data.DISPLAY_NAME + " LIKE ?  )";
//        	 sortOrder = ContactsContract.Contacts.DISPLAY_NAME + ", display_name_alt COLLATE LOCALIZED DESC";


   /* Bug_id_408 Merging 4 cursors togather to create search order. Display has a order of starts With first Name or 
       Starts with Last name or the match found any where in the contact . To achieve this we have made individual query 
       and merged the cursor.
   */   
         	   
          // Search for the Phone number in contact. The Phone (960-914-1234) has "-" in the Data1 . so we have replaced the "-" with null
    	   String selectionForNos = "(" + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "') AND (" +
    			   "REPLACE(REPLACE(REPLACE(REPLACE("+ContactsContract.Data.DATA1+ ", '-', ''), ' ', ''), ')', ''), '(', '')  LIKE ? )AND (" + ContactsContract.Data.DATA1 +" not null)";
    			   
//    			"REPLACE("+ContactsContract.Data.DATA1 + ",'-','')"+" LIKE ? )" +" OR (" +
//        		"REPLACE("+ContactsContract.Data.DATA1 + ",'(','')"+" LIKE ? )" +" OR (" +
//        		"REPLACE("+ContactsContract.Data.DATA1 + ",') ','')"+" LIKE ? )" +" OR (" +
//        	    "REPLACE("+ ContactsContract.Data.DATA1 + ",' ','')" +" LIKE ? )";
    	   //if not number then compare full text
    	   selection = "(" + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "') AND (" +
                   ContactsContract.Data.DATA1 +" LIKE ? ) AND (" + ContactsContract.Data.DATA1 +" not null)";
           Cursor pCur0 = mContext.getContentResolver().query(uri, PROJECTION_PHONE, hasOnlyNumbers ? selectionForNos : selection,hasOnlyNumbers ? new String[] { searchNumber } : new String[] { searchDisplayName }, sortOrder);
           
           //Search The display Name if starts with 
           selection = "(" + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "') AND (" +
     			   ContactsContract.Data.DISPLAY_NAME + " LIKE ? ) AND (" + ContactsContract.Data.DATA1 +" not null)";
           Cursor pCur1 = mContext.getContentResolver().query(uri, PROJECTION_PHONE, selection,new String[] { searchFirstName }, sortOrder);
          //Search the last name.As we don't have last name we go by display_name_alt . which stores LastName+FirstnName
          selection = "(" + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "') AND (" +
        		 " display_name_alt LIKE  ?  AND "+ ContactsContract.Data.DISPLAY_NAME+" NOT LIKE ? ) AND (" + ContactsContract.Data.DATA1 +" not null)";
           Cursor pCur2 = mContext.getContentResolver().query(uri, PROJECTION_PHONE, selection, new String[] { searchLasttName , searchLasttName }, sortOrder);
          // Search the display name if the string is present any where in the display_name.
          selection = "(" + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "') AND (" +
 			   ContactsContract.Data.DISPLAY_NAME + " LIKE ? AND "+ContactsContract.Data.DISPLAY_NAME+" NOT LIKE ? AND display_name_alt NOT LIKE ? ) AND (" + ContactsContract.Data.DATA1 +" not null)";
          Cursor pCur3 = mContext.getContentResolver().query(uri, PROJECTION_PHONE, selection, new String[] { searchDisplayName,searchFirstName,searchLasttName }, sortOrder);
       //Merge all the cursor to get the order as we retrieved .
          MergeCursor pCur = new MergeCursor(new Cursor[]{pCur0,pCur1,pCur2,pCur3}); 
         

        mSearchString = search;
        // show the typed value when the search returns 0
        if (pCur.getCount() == 0) {
        	pCur.close();
            ArrayList<Object> result = new ArrayList<Object>();
            result.add(Integer.valueOf(-1)); // ID
            result.add(Long.valueOf(-1)); // CONTACT_ID
            result.add(search); // NAME
            result.add(search); // DATA
            result.add(""); // MIMETYPE
            result.add(""); // PHONE_TYPE
            result.add(""); // PHONE_LABEL

            ArrayList<ArrayList<Object>> wrap = new ArrayList<ArrayList<Object>>();
            wrap.add(result);

            ArrayListCursor translated = new ArrayListCursor(PROJECTION_PHONE, wrap);

            return translated;
        }
        return pCur;
    }
}
