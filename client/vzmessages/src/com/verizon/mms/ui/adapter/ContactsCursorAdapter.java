package com.verizon.mms.ui.adapter;

import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ui.ContactSearchTask;
import com.verizon.mms.ui.ListDataWorker;
import com.verizon.mms.ui.ListDataWorker.ListDataJob;
import com.verizon.mms.util.MemoryCacheMap;
import com.verizon.mms.util.Util;

public class ContactsCursorAdapter extends SimpleCursorAdapter {

    private Context                  context       = null;

    private ListDataWorker           contactWorker;
    private MemoryCacheMap<Long, Drawable> imageCache;
    
    private Drawable                   defaultImage;

    private ListView listView;
    
    // used the identify the number or email that the user had typed
    private final int                TYPED_CONTACT = -1;

    private String mSearchString;
    private String mSearchNumber;
    
    private static final float VIEW_QUEUE_FACTOR = 2.5f;  // size of queue relative to number of views
    private static final int QUEUE_CONTACT_IMAGES = 0;
    private static final int MIN_VIEWS = 10;
    private static final int MIN_QUEUE_SIZE = (int)(MIN_VIEWS * VIEW_QUEUE_FACTOR);
    
    private static final int MSG_CONTACT_IMAGES = 1;

    class ViewHolder {
        TextView  contactName;
        TextView  contactKey;
        ImageView iv;
    }

    

    public void setSearchString(String searchString) {
    	searchString = searchString.trim();
        if (Pattern.matches("[0-9,),(,-]+", searchString.replaceAll("\\s+", ""))) {
            mSearchNumber = searchString.replaceAll("\\D+", "");
        } else {
            mSearchNumber = "";
        }
        mSearchString = searchString;
    }

    public ContactsCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
            ListView listview) {
        super(context, layout, c, from, to);
        this.context = context;

        defaultImage = context.getResources().getDrawable(R.drawable.avatar_blank);

        listView = listview;
        
        createDataWorker();

        this.context = context;
    }

    /**
     * This Method initializes the dataworker and the cache used to store images
     */
    private void createDataWorker() {
        imageCache = new MemoryCacheMap<Long, Drawable>(50);
        // create a worker thread to get images
        contactWorker = new ListDataWorker();
        contactWorker.addQueue(handler, QUEUE_CONTACT_IMAGES, MSG_CONTACT_IMAGES, MIN_QUEUE_SIZE, null);
        contactWorker.start();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = super.newView(context, cursor, parent);
        ViewHolder vh = new ViewHolder();

        vh.contactName = (TextView) v.findViewById(R.id.contactname);
        vh.contactKey = (TextView) v.findViewById(R.id.contactkey);
        vh.iv = (ImageView) v.findViewById(R.id.photo);

        // set the tag which can be used during bindview
        // instead of doing a findviewbyid again and again
        v.setTag(vh);

        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int pos = cursor.getPosition();
        long contactId = cursor.getLong(ContactSearchTask.CONTACT_ID);

        ViewHolder vh = (ViewHolder) view.getTag();
        CharSequence keyType = null;
        String mimeType = cursor.getString(ContactSearchTask.MIME_TYPE);

        if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
            int TypeValue = cursor.getInt(ContactSearchTask.PHONE_TYPE);
            String customLabel = cursor.getString(ContactSearchTask.PHONE_LABEL);
            
            keyType = Phone.getTypeLabel(context.getResources(), TypeValue, customLabel);
        } 
        
        String name = cursor.getString(ContactSearchTask.CONTACT_DISPLAY_NAME);
        String key = cursor.getString(ContactSearchTask.CONTACT_DATA);

        vh.contactName.setText(getSpannableString(name, 0), BufferType.SPANNABLE);
        if (name.equals(key)) {
            vh.contactKey.setText("");
        } else {
            vh.contactKey.setText(getSpannableNumber(keyType, key), BufferType.SPANNABLE);
        }

        if (contactId == TYPED_CONTACT) {
            // hide image view
            vh.iv.setVisibility(View.GONE);
        } else {
            vh.iv.setImageDrawable(getContactImage(contactId, pos));
            vh.iv.setVisibility(View.VISIBLE);
        }
        
        super.bindView(view, context, cursor);
    }

    /**
     * 
     * This Method Matches the number and returns spanable, it takes care of making the select bold even if
     * the number contains -,(,) and spaces.
     * 
     * @param keyType
     *            what type of number it is e.g Mobile, Home etc.
     * @param key
     *            the data to be searched for.
     * @return
     */

    private Spannable getSpannableNumber(CharSequence keyType, String key) {

        String target = (((keyType != null) ? keyType + ": " : "") + key);

        boolean isPhoneNumbers = Pattern.matches("[0-9,),(,-]+", key.replaceAll("\\s+", ""));

        if (isPhoneNumbers && mSearchNumber.length() > 0) {
            Spannable spaned = new SpannableString(target);

            // TODO: add multiple support
            StringBuffer numberData = new StringBuffer(target);
            String search = mSearchNumber;

            // if key is of type phone Number
            int IndexOfMatch = key.replaceAll("\\D+", "").indexOf(search);
            int numberDigit = 0;
            int searchIndex = 0;
            int numDatAt = 0;
            int startPointer = 0;

            if (IndexOfMatch > -1) {
                // match found
                while (numberData.length() > numDatAt) {
                    if (Character.isDigit(numberData.charAt(numDatAt))) {

                        if (numberDigit == IndexOfMatch)
                            startPointer = numDatAt;

                        if (numberDigit >= IndexOfMatch) {
                            if (numberData.charAt(numDatAt) == search.charAt(searchIndex)) {

                                searchIndex++;
                                if (search.length() <= searchIndex)
                                    break;
                            }
                        }
                        numberDigit++;
                    }
                    numDatAt++;
                }

                if (startPointer < (numDatAt+1))
                    spaned.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startPointer, numDatAt + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            }
            return spaned;
        } else {
            // if not a phone number then match whole.
            return getSpannableString(target, ((keyType != null) ? keyType.length() + 1 : 0));

        }

    }

    /**
     * 
     * This Method return bold spannable for sting match.
     * 
     * @param target
     *            string data.
     * @param startIndex
     *            index from where the search should begin.
     * @return
     */

    private Spannable getSpannableString(String target, int startIndex) {
        Spannable spaned = new SpannableString(target);
        final String search = mSearchString.toLowerCase();
        final int len = search.length();
        if (len > 0) {
	        // TODO: add multiple support
        	target = target.toLowerCase();
	        int start = target.indexOf(search, startIndex);
	        while (start >= 0) {
	            int end = start + len;
	            spaned.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, end,
	                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	            startIndex = end;
	            start = target.indexOf(search, startIndex);
	        }
        }

        return spaned;
    }

    /**
     * This Method adds the request to fetch the contact image to the queue if it is not
     * present in the cache
     * @param contactId
     * @return
     */
    private Drawable getContactImage(Long contactId, int pos) {
        synchronized (imageCache) {
            // try the local cache
            Drawable image = imageCache.get(contactId);
            if (image == null) {
                contactWorker.request(QUEUE_CONTACT_IMAGES, pos, imageJob, contactId);

                image = defaultImage;
            }
            return image;
        }
    }

    /*
     * Stop all the background tasks and clean up the adapter
     */
    public void closeAdapter() {
        imageCache.clear();
        contactWorker.exit();
    }

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            // update the item if it is visible
            final int first = listView.getFirstVisiblePosition();
            final int last = listView.getLastVisiblePosition();
            final int pos = msg.arg1;

            if (pos >= first && pos <= last) {
                try {
                    notifyDataSetChanged();
                }
                catch (Exception e) {
                    Logger.error(getClass(),e);
                }
            }
        }
    };
    
    private ListDataJob imageJob = new ListDataJob() {
        public Object run(int pos, Object data) {
            final long ids = (Long)data;
            
            Bitmap bitmap = Util.getPhoto(context, ids);
            Drawable image = null;
            
            if (bitmap != null) {
                image = new BitmapDrawable(bitmap);
            } else {
                image = defaultImage;
            }
            
            // local cache it
            synchronized (imageCache) {
                imageCache.put(ids, image);
            }
            return image;
        }
    };
}
