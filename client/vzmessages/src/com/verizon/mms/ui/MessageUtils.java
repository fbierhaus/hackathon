/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verizon.mms.ui;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Sms;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.rocketmobile.asimov.Asimov;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ContentType;
import com.verizon.mms.DeviceConfig;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.MmsException;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.WorkingMessage;
import com.verizon.mms.model.LocationModel;
import com.verizon.mms.model.MediaModel;
import com.verizon.mms.model.SlideModel;
import com.verizon.mms.model.SlideshowModel;
import com.verizon.mms.model.VCardModel;
import com.verizon.mms.pdu.CharacterSets;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.PduBody;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduPart;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.ui.adapter.UrlAdapter;
import com.verizon.mms.util.EmojiParser;
import com.verizon.mms.util.Prefs;
import com.verizon.mms.util.SmileyParser;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.sync.ConversationDataObserver;
import com.verizon.sync.VMASyncHook;
import com.verizon.vcard.android.provider.VCardContacts;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct.ContactMethod;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct.OrganizationData;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct.PhoneData;

/**
 * An utility class for managing messages.
 */
public class MessageUtils {
    private static String sLocalNumber;
    private static String yesterday;
    private static String[] weekDays;
	private static StringBuffer dateBuf = new StringBuffer();
	private static PhoneNumberUtil phone = PhoneNumberUtil.getInstance();
	private static TelephonyManager telephonyMgr;
	private static long lastCountryCache;

	private static final long COUNTRY_CACHE_INTERVAL = 24 * 60 * 60 * 1000;

	private static final int MAX_DAYS_AGO = 7;  // we assume a 7-day week

	private static int mTheme = -1;
	// Cache of both groups of space-separated ids to their full
    // comma-separated display names, as well as individual ids to
    // display names.
    // TODO: is it possible for canonical address ID keys to be
    // re-used?  SQLite does reuse IDs on NULL id_ insert, but does
    // anything ever delete from the mmssms.db canonical_addresses
    // table?  Nothing that I could find.
    private static final Map<String, String> sRecipientAddress =
            new ConcurrentHashMap<String, String>(20 /* initial capacity */);


    /**
     * MMS address parsing data structures
     */
    // allowable phone number separators
    private static final char[] NUMERIC_CHARS_SUGAR = {
        '-', '.', ',', '(', ')', ' ', '/', '\\', '*', '#', '+'
    };

    private static HashMap<Character, Character> numericSugarMap = new HashMap<Character, Character>(NUMERIC_CHARS_SUGAR.length);

    static {
        for (int i = 0; i < NUMERIC_CHARS_SUGAR.length; i++) {
            numericSugarMap.put(NUMERIC_CHARS_SUGAR[i], NUMERIC_CHARS_SUGAR[i]);
        }
    }


    interface ResizeImageResultCallback {
        void onResizeResult(PduPart part, boolean append);
    }


	public static void init(Context context) {
		final Resources res = context.getResources();
		yesterday = res.getString(R.string.yesterday);
		weekDays = res.getStringArray(R.array.week_days);
	}

    private MessageUtils() {
        // Forbidden being instantiated.
    }

    public static int getAttachmentType(SlideshowModel model) {
        if (model == null) {
            return WorkingMessage.TEXT;
        }

        int numberOfSlides = model.size();
        if (numberOfSlides > 1) {
            return WorkingMessage.SLIDESHOW;
        } else if (numberOfSlides == 1) {
            // Only one slide in the slide-show.
            SlideModel slide = model.get(0);
            
            if (slide.hasLocation()) {
                return WorkingMessage.LOCATION;
            }
            
            if (slide.hasVideo()) {
                return WorkingMessage.VIDEO;
            }

            if (slide.hasAudio() && slide.hasImage()) {
                return WorkingMessage.SLIDESHOW;
            }

            if (slide.hasAudio()) {
                return WorkingMessage.AUDIO;
            }

            if (slide.hasImage()) {
                return WorkingMessage.IMAGE;
            }
            
            if (slide.hasVCard()) {
                return WorkingMessage.VCARD;
            }

            if (slide.hasText()) {
                return WorkingMessage.TEXT;
            }
        }

        return WorkingMessage.TEXT;
    }

    /**
     * Formats timestamps in mixed relative and absolute format.  If date is today then time only is returned,
     * yesterday is "yesterday", < 7 days is day of the week, others are absolute date.
     * 
     * @param when UTC milliseconds since the epoch
     * @param includeTime If true, include the time for timestamps before today
     * @return Formatted time/date string
     */
	public static String formatTimeStampString(long when, boolean includeTime) {
		String ret;
		final Calendar date = Calendar.getInstance();
		date.setTimeInMillis(when);
		final Calendar now = Calendar.getInstance();

		// check for date >= now or day == today
		final Locale locale = MessagingPreferenceActivity.getCurrentLocale(Asimov.getApplication());
		final boolean today = date.get(Calendar.YEAR) == now.get(Calendar.YEAR) && date.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
		synchronized (dateBuf) {
			if (today || date.getTimeInMillis() >= now.getTimeInMillis()) {
				// today: use short time format
				dateBuf.setLength(0);
				final DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
				ret = timeFormat.format(date.getTime(), dateBuf, new FieldPosition(0)).toString();
			}
			else {
				// get number of calendar days ago
				final long daysAgo = daysAgo(date, now);
				if (daysAgo == 1) {
					// "yesterday"
					ret = yesterday;
				}
				else if (daysAgo < 7) {
					// day of week
					ret = weekDays[date.get(Calendar.DAY_OF_WEEK) - 1];
				}
				else {
					// format as absolute short date
					dateBuf.setLength(0);
					final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
					ret = dateFormat.format(date.getTime(), dateBuf, new FieldPosition(0)).toString();
				}
				if (includeTime) {
					dateBuf.setLength(0);
					final StringBuilder sb = new StringBuilder(ret);
					sb.append(' ');
					final DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
					sb.append(timeFormat.format(date.getTime(), dateBuf, new FieldPosition(0)));
					ret = sb.toString();
				}
			}
		}
		return ret;
	}

	// enhancement of http://tripoverit.blogspot.com/2007/07/java-calculate-difference-between-two.html
    public static long daysAgo(Calendar startDate, Calendar endDate) {
		Calendar date = (Calendar)startDate.clone();
		long daysAgo = 0;
		while (date.get(Calendar.DAY_OF_YEAR) != endDate.get(Calendar.DAY_OF_YEAR) || date.get(Calendar.YEAR) != endDate.get(Calendar.YEAR)) {
			date.add(Calendar.DAY_OF_MONTH, 1);
			// stop when we reach MAX_DAYS_AGO since we use absolute dates after that point
			if (++daysAgo >= MAX_DAYS_AGO) {
				break;
			}
		}
		return daysAgo;
	}  

    /**
     * @parameter recipientIds space-separated list of ids
     */
    public static String getRecipientsByIds(Context context, String recipientIds,
                                            boolean allowQuery) {
        String value = sRecipientAddress.get(recipientIds);
        if (value != null) {
            return value;
        }
        if (!TextUtils.isEmpty(recipientIds)) {
            StringBuilder addressBuf = extractIdsToAddresses(
                    context, recipientIds, allowQuery);
            if (addressBuf == null) {
                // temporary error?  Don't memoize.
                return "";
            }
            value = addressBuf.toString();
        } else {
            value = "";
        }
        sRecipientAddress.put(recipientIds, value);
        return value;
    }

    private static StringBuilder extractIdsToAddresses(Context context, String recipients,
                                                       boolean allowQuery) {
        StringBuilder addressBuf = new StringBuilder();
        String[] recipientIds = recipients.split(" ");
        boolean firstItem = true;
        for (String recipientId : recipientIds) {
            String value = sRecipientAddress.get(recipientId);

            if (value == null) {
                if (!allowQuery) {
                    // when allowQuery is false, if any value from sRecipientAddress.get() is null,
                    // return null for the whole thing. We don't want to stick partial result
                    // into sRecipientAddress for multiple recipient ids.
                    return null;
                }

                Uri uri = Uri.withAppendedPath(VZUris.getMmsSmsCanonical(),"/" + recipientId);
                Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
                                               uri, null, null, null, null);
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            value = c.getString(0);
                            sRecipientAddress.put(recipientId, value);
                        }
                    } finally {
                        c.close();
                    }
                }
            }
            if (value == null) {
                continue;
            }
            if (firstItem) {
                firstItem = false;
            } else {
                addressBuf.append(";");
            }
            addressBuf.append(value);
        }

        return (addressBuf.length() == 0) ? null : addressBuf;
    }
  //BUG_id_51 added a Dialog chooser for ringtone/Audio/Record Audio
    public static void selectRingtone(Context context, int requestCode) {
        if (context instanceof Activity) {
          	
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_INCLUDE_DRM, false);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                    context.getString(R.string.select_audio));
            ((Activity) context).startActivityForResult(intent, requestCode);
        }
    }
   
    public static void selectAudio(Context context, int requestCode) {
        if (context instanceof Activity) {
             Intent intent = new Intent(Intent.ACTION_PICK);        	
             intent.setData(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);       
             try {              
            	 ((Activity) context).startActivityForResult(intent, requestCode);     
             } catch (ActivityNotFoundException e) {                
            	  Toast.makeText(context,R.string.provider_not_accessible, Toast.LENGTH_LONG).show();           
             }
         }
    }    
    //BUG_id_51 added a Dialog chooser for ringtone/Audio/Record Audio
	
    //Bug_id 101_Attaching recorded audio. OEM specific Intent calls made. 
    public static final int SAMSUNG_REMAIN_SIZE = 1195*1024;
    
    public static void recordSound(Context context, int requestCode, int sizeUpTo) {
        if (context instanceof Activity) {
            Intent intent = new Intent();
            intent.setClass(context, AudioRecorder.class);
            
            if (sizeUpTo > 0) {
				intent.putExtra(AudioRecorder.EXTRA_MAX_BYTES, sizeUpTo);
			}
            ((Activity) context).startActivityForResult(intent, requestCode);
        }
    }
   
    ////Bug_id 101_Attaching recorded audio. OEM specific Intent calls made.
    public static void selectVideo(Context context, int requestCode) {
        selectMediaByType(context, requestCode, ContentType.VIDEO_UNSPECIFIED);
    }

    public static void selectImage(Context context, int requestCode) {
        selectMediaByType(context, requestCode, ContentType.IMAGE_UNSPECIFIED);
    }

    private static void selectMediaByType(
            Context context, int requestCode, String contentType) {
         if (context instanceof Activity) {

            Intent innerIntent = new Intent(Intent.ACTION_GET_CONTENT);

            innerIntent.setType(contentType);

            Intent wrapperIntent = Intent.createChooser(innerIntent, null);

            ((Activity) context).startActivityForResult(wrapperIntent, requestCode);
        }
    }

    public static void viewSimpleSlideshow(Context context, SlideshowModel slideshow) {
        if (!slideshow.isSimple()) {
            throw new IllegalArgumentException(
                    "viewSimpleSlideshow() called on a non-simple slideshow");
        }
        SlideModel slide = slideshow.get(0);
        MediaModel mm = null;
        if (slide.hasLocation()) {
            mm = slide.getLocation();
            showLocation(context, (LocationModel)mm);
            return;
        } else if (slide.hasImage()) {
            mm = slide.getImage();
        } else if (slide.hasVideo()) {
            mm = slide.getVideo();
        } else if (slide.hasVCard()) {
            mm = slide.getVCard();
            showVCard(context, mm, true);
            return;
        }

        viewMediaModel(context, mm);
    }
    
	public static void viewLocationSlideshow(Context context,
			SlideshowModel slideshow) {
		MediaModel mm = null;
		for (SlideModel slide : slideshow) {
			if (slide.hasLocation()) {
				mm = slide.getLocation();
				showLocation(context, (LocationModel) mm);
				return;
			}
		}
		viewMediaModel(context, mm);
	}

    /**
     * This Method 
     * @param context
     * @param mm
     */
    private static void showLocation(Context context, LocationModel mm) {
        boolean showError = true;
        if (DeviceConfig.OEM.isNbiLocationDisabled) {
            // should not reach here.
            log("Trying to access  Nbi libs=" + mm);
            return;
        }
		ContactStruct contact = mm.getContactStruct();
        if (contact != null) {
            String url = contact.getURL();
            if (url != null) {
                Intent intent = new Intent(context, AddLocationActivity.class);
                intent.putExtra("mapURL", url);
                context.startActivity(intent);
                showError = false;
            }
        }
        
        if (showError) {
            Toast.makeText(context, R.string.url_not_present, Toast.LENGTH_LONG).show();
        }
    }

    public static void showVCard(Context context, MediaModel mm, boolean addIfNotPresent) {
    	showVCard(context, mm, addIfNotPresent, -1);
    }
    
    /**
     * This method accepts a VcardModel object and shows that contact if the primary
     * phonenumber or email in that vcard is already present else if creates a 
     * new contact
     * @param context
     * @param mm
     * @param addIfNotPresent
     * @param intentflag
     */
    public static void showVCard(Context context, MediaModel mm, boolean addIfNotPresent, int intentflag) {
        Intent intent = null;
        ContactStruct contactStruct = ((VCardModel)mm).getContactStruct();
        
        if (contactStruct != null) {
            String email = contactStruct.getFirstEmail();
            String number = contactStruct.getFirstNumber();

            Contact contact = null;
            if(!TextUtils.isEmpty(number)) {
                contact = Contact.get(number, true);
            } else if(!TextUtils.isEmpty(email)) {
                contact = Contact.get(email, true);
            }
            if(contact != null && contact.existsInDatabase())
            {
                Uri uri = contact.getUri();
                intent = new Intent(Intent.ACTION_VIEW, uri);
            } else {
                if (addIfNotPresent) {
                    intent = MessageUtils.createAddContactIntent(contactStruct);
                    
                    if (intentflag != -1) {
                    	intent.setFlags(intentflag);
                    }
                }

                if (intent == null) {
                    Toast.makeText(context, R.string.contact_has_no_address, Toast.LENGTH_LONG).show();
                    return;
                }
            }
			//this contact might change so mark it as stale for it to be refreshed
            contact.markAsStale();
            context.startActivity(intent);
        } else {
            Toast.makeText(context, context.getString(R.string.name_card_parse_error), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * This Method 
     * @param context
     * @param mm
     */
    public static void viewMediaModel(Context context, MediaModel mm) {
        if (mm.isVcard()) {
            showVCard(context, mm, false);
            return;
        }
        
        if (mm.isLocation()) {
            showLocation(context, (LocationModel)mm);
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String contentType;
        if (mm.isDrmProtected()) {
            contentType = mm.getDrmObject().getContentType();
        } else {
            contentType = mm.getContentType();
        }
        intent.setDataAndType(mm.getUri(), contentType);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
        	Toast.makeText(context, context.getString(R.string.no_media_player), Toast.LENGTH_SHORT).show();
        }
    }

    public static void showErrorDialog(Context context,
            String title, String message) {
    	final AppAlignedDialog dialog = new AppAlignedDialog(context, 
    			R.drawable.ic_sms_mms_not_delivered, title, message );
		Button okButton = (Button) dialog.findViewById(R.id.positive_button);
		okButton.setText(android.R.string.ok);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		dialog.show();
    }

    /**
     * The quality parameter which is used to compress JPEG images.
     */
    public static final int IMAGE_COMPRESSION_QUALITY = 80;
    /**
     * The minimum quality parameter which is used to compress JPEG images.
     */
    public static final int MINIMUM_IMAGE_COMPRESSION_QUALITY = 50;

    public static Uri saveBitmapAsPart(Context context, Uri messageUri, Bitmap bitmap)
            throws MmsException {

    	try {
	        ByteArrayOutputStream os = new ByteArrayOutputStream();
	        bitmap.compress(CompressFormat.JPEG, IMAGE_COMPRESSION_QUALITY, os);
	
	        PduPart part = new PduPart();
	
	        part.setContentType("image/jpeg".getBytes());
	        String contentId = "Image" + System.currentTimeMillis();
	        part.setContentLocation((contentId + ".jpg").getBytes());
	        part.setContentId(contentId.getBytes());
	        part.setData(os.toByteArray());
	
	        Uri retVal = PduPersister.getPduPersister(context).persistPart(part,
	                        ContentUris.parseId(messageUri));
	
	        if (Logger.IS_DEBUG_ENABLED) {
	            log("saveBitmapAsPart: persisted part with uri=" + retVal);
	        }

	        return retVal;
    	}
    	catch (Throwable t) {
    		throw new MmsException(t);
    	}
    }

    /**
     * Message overhead that reduces the maximum image byte size.
     * 5000 is a realistic overhead number that allows for user to also include
     * a small MIDI file or a couple pages of text along with the picture.
     */
    public static final int MESSAGE_OVERHEAD = 5000;

    public static void resizeImageAsync(final Context context,
            final Uri imageUri, final Handler handler,
            final ResizeImageResultCallback cb,
            final boolean append) {

        // Show a progress toast if the resize hasn't finished
        // within one second.
        // Stash the runnable for showing it away so we can cancel
        // it later if the resize completes ahead of the deadline.
        final Runnable showProgress = new Runnable() {
            public void run() {
                Toast.makeText(context, R.string.compressing, Toast.LENGTH_SHORT).show();
            }
        };
        // Schedule it for one second from now.
        handler.postDelayed(showProgress, 1000);

        new Thread(new Runnable() {
            public void run() {
                final PduPart part;
                try {
                    UriImage image = new UriImage(context, imageUri);
                    part = image.getResizedImageAsPart(
                        MmsConfig.getMaxImageWidth(),
                        MmsConfig.getMaxImageHeight(),
                        MmsConfig.getMaxMessageSize() - MESSAGE_OVERHEAD);
                } finally {
                    // Cancel pending show of the progress toast if necessary.
                    handler.removeCallbacks(showProgress);
                }

                handler.post(new Runnable() {
                    public void run() {
                        cb.onResizeResult(part, append);
                    }
                });
            }
        }).start();
    }

    public static void showDiscardDraftConfirmDialog(Context context,
            ComposeMessageFragment.DiscardDraftListener listener) {
    	final AppAlignedDialog dialog = new AppAlignedDialog(context, 
    			R.drawable.dialog_alert, R.string.discard_message, R.string.discard_message_reason );
		Button okButton = (Button) dialog.findViewById(R.id.positive_button);
		okButton.setText(R.string.yes);
		listener.setDialog(dialog);
		okButton.setOnClickListener(listener);
		Button noButton = (Button) dialog.findViewById(R.id.negative_button);
		noButton.setText(R.string.no);
		noButton.setVisibility(View.VISIBLE);
		noButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		dialog.show();
    }

    public static String getLocalNumber() {
    	if (MmsConfig.isTabletDevice()) {
			//if it is a tablet return the mdn with which the tablet was provisioned
    		return ApplicationSettings.getInstance().getMDN();
    	}
        if (null == sLocalNumber) {
            sLocalNumber = Asimov.getApplication().getTelephonyManager().getLine1Number();
        }
        return sLocalNumber;
    }

    public static boolean isLocalNumber(String number) {
        if (number == null) {
            return false;
        }

        // we don't use Mms.isEmailAddress() because it is too strict for comparing addresses like
        // "foo+caf_=6505551212=tmomail.net@gmail.com", which is the 'from' address from a forwarded email
        // message from Gmail. We don't want to treat "foo+caf_=6505551212=tmomail.net@gmail.com" and
        // "6505551212" to be the same.
        if (number.indexOf('@') >= 0) {
            return false;
        }

        return PhoneNumberUtils.compare(number, getLocalNumber());
    }
 
    public static String extractEncStrFromCursor(Cursor cursor,
            int columnRawBytes, int columnCharset) {
        String rawBytes = cursor.getString(columnRawBytes);
        int charset = cursor.getInt(columnCharset);

        if (TextUtils.isEmpty(rawBytes)) {
            return "";
        } else if (charset == CharacterSets.ANY_CHARSET) {
            return rawBytes;
        } else {
            return new EncodedStringValue(charset, PduPersister.getBytes(rawBytes)).getString();
        }
    }

	public static CharSequence parseEmoticons(CharSequence text) {
		if (text != null && text.length() != 0) {
			if(MmsConfig.enableEmojis) {
				text = EmojiParser.getInstance().addEmojiSpans(text, true);
			}else{
				text = SmileyParser.getInstance().addSmileySpans(text, true);
			}
		}
		return text;
	}

    public static ArrayList<String> extractUrisForMessageClick(URLSpan[] spans) {
        int size = spans.length;
        ArrayList<String> accumulator = new ArrayList<String>();

        for (int i = 0; i < size; i++) {
        	if( MmsConfig.isTabletDevice() && spans[i].getURL().startsWith(UrlAdapter.TEL_PREFIX)
        			){
        		if(Logger.IS_DEBUG_ENABLED){
        			Logger.debug("Not showing phone number for Tablet for uri :"+ spans[i].getURL());
        		}
        	}
        	else
        	{
        		accumulator.add(spans[i].getURL());
        	}
        }
        return accumulator;
    }

    public static ArrayList<String> extractUris(URLSpan[] spans) {
        int size = spans.length;
        ArrayList<String> accumulator = new ArrayList<String>();

        for (int i = 0; i < size; i++) {
        	accumulator.add(spans[i].getURL());
        }
        return accumulator;
    }
    /**
     * Play/view the message attachments.
     * TOOD: We need to save the draft before launching another activity to view the attachments.
     *       This is hacky though since we will do saveDraft twice and slow down the UI.
     *       We should pass the slideshow in intent extra to the view activity instead of
     *       asking it to read attachments from database.
     * @param context
     * @param msgUri the MMS message URI in database
     * @param slideshow the slideshow to save
     * @param persister the PDU persister for updating the database
     * @param sendReq the SendReq for updating the database
     */
    public static void viewMmsMessageAttachment(Context context, Uri msgUri,
            SlideshowModel slideshow) {
        boolean isSimple = (slideshow == null) ? false : slideshow.isSimple();
        if (isSimple) {
            // In attachment-editor mode, we only ever have one slide.
            MessageUtils.viewSimpleSlideshow(context, slideshow);
        } else {
            // If a slideshow was provided, save it to disk first.
            if (slideshow != null) {
            	boolean isLocation = slideshow.hasLocation();
            	if(isLocation){
            		viewLocationSlideshow(context, slideshow);
            		return;
            	}
                PduPersister persister = PduPersister.getPduPersister(context);
                try {
                    PduBody pb = slideshow.toPduBody();
                    persister.updateParts(msgUri, pb);
                    slideshow.sync(pb);
                } catch (MmsException e) {
                    Logger.error(MessageUtils.class, "Unable to save message for preview");
                    return;
                } catch (Exception e) {
                	Logger.error("viewMmsMessageAttachment ", e);
                	if (Logger.IS_DEBUG_ENABLED) {
                		throw new RuntimeException(e);
                	} else {
                		return;
                	}
                }
            }
            // Launch the slideshow activity to play/view.
            Intent intent = new Intent(context, SlideshowActivity.class);
            intent.setData(msgUri);
            context.startActivity(intent);
        }
    }

    public static void viewMmsMessageAttachment(Context context, WorkingMessage msg) {
        SlideshowModel slideshow = msg.getSlideshow();
        if (slideshow == null) {
            throw new IllegalStateException("msg.getSlideshow() == null");
        }
        if (slideshow.isSimple()) {
            MessageUtils.viewSimpleSlideshow(context, slideshow);
        } else {
            Uri uri = msg.saveAsMms(false);
            viewMmsMessageAttachment(context, uri, slideshow);
        }
    }

    /**
     * Debugging
     */
    public static void writeHprofDataToFile(){
        String filename = Environment.getExternalStorageDirectory() + "/mms_oom_hprof_data";
        try {
            android.os.Debug.dumpHprofData(filename);
            Logger.debug(MessageUtils.class, "##### written hprof data to " + filename);
        } catch (IOException ex) {
            Logger.error(MessageUtils.class, "writeHprofDataToFile: caught " + ex);
        }
    }

    public static boolean isAlias(String string) {
        if (!MmsConfig.isAliasEnabled()) {
            return false;
        }

        if (TextUtils.isEmpty(string)) {
            return false;
        }

        // TODO: not sure if this is the right thing to use. Mms.isPhoneNumber() is
        // intended for searching for things that look like they might be phone numbers
        // in arbitrary text, not for validating whether something is in fact a phone number.
        // It will miss many things that are legitimate phone numbers.
        if (Mms.isPhoneNumber(string)) {
            return false;
        }

        if (!isAlphaNumeric(string)) {
            return false;
        }

        int len = string.length();

        if (len < MmsConfig.getAliasMinChars() || len > MmsConfig.getAliasMaxChars()) {
            return false;
        }

        return true;
    }

    public static boolean isAlphaNumeric(String s) {
        char[] chars = s.toCharArray();
        for (int x = 0; x < chars.length; x++) {
            char c = chars[x];

            if ((c >= 'a') && (c <= 'z')) {
                continue;
            }
            if ((c >= 'A') && (c <= 'Z')) {
                continue;
            }
            if ((c >= '0') && (c <= '9')) {
                continue;
            }

            return false;
        }
        return true;
    }




    /**
     * Given a phone number, return the string without syntactic sugar, meaning parens,
     * spaces, slashes, dots, dashes, etc. If the input string contains non-numeric
     * non-punctuation characters, return null.
     */
    private static String parsePhoneNumberForMms(String address, boolean addPlus) {
        if(!(Pattern.compile("[0-9]").matcher(address).find())) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        int len = address.length();

        for (int i = 0; i < len; i++) {
            char c = address.charAt(i);

            // accept the first '+' in the address
            if (c == '+' && builder.length() == 0 && addPlus) {
                builder.append(c);
                continue;
            }

            if (Character.isDigit(c)) {
                builder.append(c);
                continue;
            }

            if (numericSugarMap.get(c) == null) {
                return null;
            }           
        }

        return builder.toString();
    }

    /**
     * Returns true if the address passed in is a valid MMS address.
     */
    public static boolean isValidMmsAddress(String address) {
        String retVal = parseMmsAddress(address);
        return (retVal != null);
    }

    /**
     * parse the input address to be a valid MMS address.
     * - if the address is an email address, leave it as is.
     * - if the address can be parsed into a valid MMS phone number, return the parsed number.
     * - if the address is a compliant alias address, leave it as is.
     */
    public static String parseMmsAddress(String address) {
    	return parseMmsAddress(address, true);
    }
    
    public static String parseMmsAddress(String address, boolean addPlus) {
        // if it's a valid Email address, use that.
        if (Mms.isEmailAddress(address)) {
            return address;
        }

        // if we are able to parse the address to a MMS compliant phone number, take that.
        String retVal = parsePhoneNumberForMms(address, addPlus);
        if (retVal != null) {
            return retVal;
        }

        // if it's an alias compliant address, use that.
        if (isAlias(address)) {
            return address;
        }

        // it's not a valid MMS address, return null
        return null;
    }

    private static void log(String msg) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(MessageUtils.class, "[MsgUtils] " + msg);
        }
    }
    
    public static Intent createAddContactIntent(ContactStruct contact)
    {
        // address must be a single recipient
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);

        intent.putExtra(ContactsContract.Intents.Insert.FULL_MODE, true);

        int phoneNumberCount = 0;
        int emailCount = 0;

        addBasicIntentString(intent, ContactsContract.Intents.Insert.NAME, contact.getName());

        List<PhoneData> numbers = contact.getPhoneList();
        if (numbers != null) {
            for(PhoneData phoneData : numbers)
            {
                addNumberToIntent(intent, phoneNumberCount++, phoneData);
            }
        }

        List<ContactMethod> contactMethods = contact.getContactMethodsList();
        if (contactMethods != null) {
            for(ContactMethod contactMethod : contactMethods)
            {
                if (contactMethod.kind == VCardContacts.KIND_EMAIL) {
                    addEmailToIntent(intent, emailCount++, contactMethod);
                } else if (contactMethod.kind == VCardContacts.KIND_POSTAL) {
                    addAddressToIntent(intent, contactMethod);
                }
            }
        }

        List<OrganizationData> organizations = contact.getOrganizationalData();
        
        if (organizations != null && organizations.size() > 0) {
            OrganizationData organization = organizations.get(0);
            addBasicIntentString(intent, ContactsContract.Intents.Insert.JOB_TITLE, organization.positionName);
            addBasicIntentString(intent, ContactsContract.Intents.Insert.COMPANY, organization.companyName);
        }

        if (phoneNumberCount == 0 && emailCount == 0)
        {
            return null; // has neither email nor number to add
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        return intent;
    }
    
    private static void addBasicIntentString(Intent intent, String key, String value)
    {
        if(!TextUtils.isEmpty(value))
            intent.putExtra(key, value);
    }

    private static void addAddressToIntent(Intent intent, ContactMethod address)
    {
        int addressType = address.type;

        intent.putExtra(ContactsContract.Intents.Insert.POSTAL, address.data.trim());
        intent.putExtra(ContactsContract.Intents.Insert.POSTAL_ISPRIMARY, address.isPrimary);
        intent.putExtra(ContactsContract.Intents.Insert.POSTAL_TYPE, addressType);
    }

    private static void addEmailToIntent(Intent intent, int emailCount, ContactMethod email)
    {
        final String EMAIL = emailCount == 0 ? ContactsContract.Intents.Insert.EMAIL :
            (emailCount == 1 ? ContactsContract.Intents.Insert.SECONDARY_EMAIL :
                ContactsContract.Intents.Insert.TERTIARY_EMAIL);
        final String EMAIL_TYPE = emailCount == 0 ? ContactsContract.Intents.Insert.EMAIL_TYPE :
            (emailCount == 1 ? ContactsContract.Intents.Insert.SECONDARY_EMAIL_TYPE :
                ContactsContract.Intents.Insert.TERTIARY_EMAIL_TYPE);

        if(emailCount > 2)
            return;

        int emailType = email.type;

        intent.putExtra(EMAIL, email.data);
        intent.putExtra(EMAIL_TYPE, emailType);
        
        if(emailCount == 0) {
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL_ISPRIMARY, true);
        }
    }

    private static void addNumberToIntent(Intent intent, int phoneCount, PhoneData telephone)
    {
        if(phoneCount > 2)
            return;

        final String PHONE = phoneCount == 0 ? ContactsContract.Intents.Insert.PHONE :
            (phoneCount == 1 ? ContactsContract.Intents.Insert.SECONDARY_PHONE :
                ContactsContract.Intents.Insert.TERTIARY_PHONE);
        final String PHONE_TYPE = phoneCount == 0 ? ContactsContract.Intents.Insert.PHONE_TYPE :
            (phoneCount == 1 ? ContactsContract.Intents.Insert.SECONDARY_PHONE_TYPE :
                ContactsContract.Intents.Insert.TERTIARY_PHONE_TYPE);

        int phoneType = telephone.type;

        intent.putExtra(PHONE, telephone.data);
        intent.putExtra(PHONE_TYPE, phoneType);
        
        if(phoneCount == 0) {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_ISPRIMARY, true);
        }
    }
    
	/**
	 * 
	 * This Method checks to see if the VZW_LOCATION is present in the vcard 
	 * @param context
	 * @param uri
	 * @return
	 */
	public static boolean hasLocation(Context context, Uri uri) {
		boolean hasLocation = false;

		BufferedInputStream is = null;
		try {
			final ContentResolver res = Asimov.getApplication().getContentResolver();
			is = new BufferedInputStream(res.openInputStream(uri), 4096);
			int buflen = 8 * 1024;
			do {
				final byte[] buf = new byte[buflen];
				final int read = is.read(buf);
				if (read < 0) {
					break;
				}
				if (read < buflen) {
					final String vcard = new String(buf, 0, read);
					hasLocation = vcard.contains(ContactStruct.VZW_LOCATION);
					break;
				}
				buflen *= 2;
			} while (buflen <= 512 * 1024);
			if (buflen > 512 * 1024) {
				Logger.error(MessageUtils.class,"hasLocation: hit size limit for " + uri);
			}
		}
		catch (Exception e) {
			Logger.error(MessageUtils.class,"hasLocation: uri = " + uri, e);
		}
		finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (Exception e) {
				}
			}
		}

		return hasLocation;
	}
	
	/**
	 * 
	 * This Method checks to see if the VZW_LOCATION is present in the vcard 
	 * @param context
	 * @param data vcard data in bytes
	 * @return
	 */
	public static boolean hasLocation(Context context, byte data[]) {
		boolean hasLocation = false;

		BufferedInputStream is = null;
		try {
			is = new BufferedInputStream(new ByteArrayInputStream(data), 4096);
			int buflen = 8 * 1024;
			do {
				final byte[] buf = new byte[buflen];
				final int read = is.read(buf);
				if (read < 0) {
					break;
				}
				if (read < buflen) {
					final String vcard = new String(buf, 0, read);
					hasLocation = vcard.contains(ContactStruct.VZW_LOCATION);
					break;
				}
				buflen *= 2;
			} while (buflen <= 512 * 1024);
			if (buflen > 512 * 1024) {
				Logger.error(MessageUtils.class,"hasLocation: hit size limit ");
			}
		}
		catch (Exception e) {
			Logger.error(MessageUtils.class,"hasLocation: data = " + new String(data));
		}
		finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (Exception e) {
				}
			}
		}

		return hasLocation;
	}
	
	public static CharSequence extractTextFromData(byte[] data, int charset) {
        if (data != null) {
        	String text;
            try {
                if (CharacterSets.ANY_CHARSET == charset) {
                    text = new String(data); // system default encoding.
                } else {
                    String name = CharacterSets.getMimeName(charset);
                    text = new String(data, name);
                }
            } catch (UnsupportedEncodingException e) {
                Logger.error(MessageUtils.class, "Unsupported encoding: " + charset, e);
                text = new String(data); // system default encoding.
            }

            // truncate long messages to max length
            final int maxLength = MmsConfig.getMaxTextLength();
            if (text.length() > maxLength) {
            	return text.substring(0, maxLength);
            }
            else {
            	return text;
            }
        }
        return "";
    }

	public static String getSizeString(Context context, long size) {
		if (size < 1024) {
			return size + " " + context.getResources().getString(R.string.bytes);
		}
		else {
			final int resid;
			final float num;
			if (size > 1024 * 1024) {
				num = (float)size / (1024 * 1024);
				resid = R.string.megabyte;
			}
			else {
				num = (float)size / 1024;
				resid = R.string.kilobyte;
			}
			return String.format("%.1f %s", num, context.getString(resid));
		}
	}

	/**
	 * Return a normalized MMS address, stripping non-numbers from phone addresses and lowercasing
	 * email addresses.
	 * @return the normalized address
	 */
	public static String normalizeMmsAddress(String addr) {
		String ret = null;
		if (addr != null) {
			if (!Mms.isEmailAddress(addr)) {
				// try to parse as a phone number
				final String country = getCountry(true, true);
				try {
					final PhoneNumber num = phone.parse(addr, country);
					if (num != null) {
						int cc = num.getCountryCode();
						if (cc == 0) {
							// check saved pref
							cc = Prefs.getInt(MessagingPreferenceActivity.COUNTRY_CODE, 0);
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug("MessageUtils.normalizeMmsAddress: got code from prefs: " + cc);
							}
							if (cc == 0) {
								// default to US
								cc = 1;
							}
						}

						ret = Integer.toString(cc) + Long.toString(num.getNationalNumber());

						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug("MessageUtils.normalizeMmsAddress: <" + addr + "> parsed to: " + num);
						}
					}
				}
				catch (Exception e) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.error("exception parsing <" + addr + ">, <" + country + ">:", e);
					}
				}
			}
			if (ret == null) {
				// assume it's an email address, just do a basic strip and lowercase it
				ret = Mms.extractAddrSpec(addr).toLowerCase(Locale.US);
			}
		}
		return ret;
	}

	/**
	 * Gets the device's ISO country code, trying a number of methods in declining order of desirability.
	 *
	 * @param useCache If true then try getting the cached value if no current country source is available
	 * @param defaultToUS If true then always return "US" if no country source is available
	 * @return The device's ISO country code or null if not available
	 */
	public static String getCountry(boolean useCache, boolean defaultToUS) {
		String country = null;
		boolean fromCache = false;
		try {
			synchronized (MessageUtils.class) {
				if (telephonyMgr == null) {
					telephonyMgr = (TelephonyManager)Asimov.getApplication().getSystemService(Context.TELEPHONY_SERVICE);
				}
			}

			country = telephonyMgr.getNetworkCountryIso();
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("MessageUtils.getCountry: got from network: " + country);
			}
			if (country == null || country.length() == 0) {
				country = telephonyMgr.getSimCountryIso();
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug("MessageUtils.getCountry: got from SIM: " + country);
				}
				if (country == null || country.length() == 0) {
					country = Locale.getDefault().getCountry();
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug("MessageUtils.getCountry: got from locale " + Locale.getDefault() + ": " + country);
					}
					if ((country == null || country.length() == 0) && useCache) {
						// try the cache
						country = Prefs.getString(MessagingPreferenceActivity.COUNTRY, null);
						fromCache = true;
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug("MessageUtils.getCountry: got from cache: " + country);
						}
					}
				}
			}
		}
		catch (Exception e) {
			Logger.error(e);
		}

		if (country == null || country.length() == 0) {
			if (defaultToUS) {
				country = "US";
			}
		}
		else {
			country = country.toUpperCase(Locale.US);
			if (!fromCache) {
				// cache it periodically
				final long now = System.currentTimeMillis();
				if (lastCountryCache < now - COUNTRY_CACHE_INTERVAL) {
					lastCountryCache = now;
					try {
						final Editor ed = Prefs.getPrefsEditor();
						ed.putString(MessagingPreferenceActivity.COUNTRY, country);
						int code = phone.getCountryCodeForRegion(country);
						if (code == 0 && country.equals("US")) {
							code = 1;
						}
						ed.putInt(MessagingPreferenceActivity.COUNTRY_CODE, code);
						ed.commit();
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug("MessageUtils.getCountry: set country code pref = " + code);
						}
					}
					catch (Exception e) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.error("MessageUtils.getCountry: error setting prefs for country " + country, e);
						}
					}
				}
			}
		}

		return country;
	}
	
	public static void markMmsMessageWithError(Context context, ContentResolver resolver, Uri mmsUri, boolean alreadyInOutbox) {
		if (mmsUri == null) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("Error - Being asked to mark MmsMessageWithError but uri is null");
			}
			
			return;
		}
		boolean alreadyInPendingTable = false;
		Cursor cursor = null;
		try {
			// if entry already exists in the pending table then dont try to force create pending table entry
	        Uri.Builder uriBuilder = VZUris.getNativeMmsSmsPendingUri().buildUpon();
	        uriBuilder.appendQueryParameter("protocol", "mms");
	        long mid = ContentUris.parseId(mmsUri);
	        
	        String selection = PendingMessages.MSG_TYPE + "=" + PduHeaders.MESSAGE_TYPE_SEND_REQ + " AND " + 
	        		PendingMessages.MSG_ID + "= ?";

	        String[] selectionArgs = new String[] { String.valueOf(mid)};

	        cursor = SqliteWrapper.query(context, resolver, uriBuilder.build(), null, selection,
	                    selectionArgs, null);
	        
	        if ((cursor != null) && (cursor.getCount() > 0)) {
	        	if (Logger.IS_DEBUG_ENABLED) {
	        		Logger.error(MessageUtils.class, "BYPASS: pending table entry already exists. Strange!");
	        	}
	        	alreadyInPendingTable = true;
	        }
		} finally {
			if (cursor != null)
				cursor.close();
		}

		try {
			if (!alreadyInPendingTable) {
				PduPersister p = PduPersister.getPduPersister(context);
				if (alreadyInOutbox) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(MessageUtils.class, "BYPASS: already in outbox so move out first");
					}
					// If message is already in outbox then move the message out. Because trigger works only if moving into outbox
					// the "pending_msgs" table.
					p.move(mmsUri, VZUris.getMmsSentUri());				
				}
				// Move the message into MMS Outbox. A trigger will create an entry in
				// the "pending_msgs" table.
				p.move(mmsUri, VZUris.getMmsOutboxUri());
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(MessageUtils.class, "BYPASS: now moved to outbox");
				}
	        }		        

			// Now update the pending_msgs table with an error for that new item.
			ContentValues values = new ContentValues(1);
			values.put(PendingMessages.ERROR_TYPE, MmsSms.ERR_TYPE_GENERIC_PERMANENT);
			final String where = PendingMessages.MSG_ID + "=" + ContentUris.parseId(mmsUri);
			int rows = SqliteWrapper.update(context, resolver, VZUris.getNativeMmsSmsPendingUri(), values, where, null);
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(MessageUtils.class, "BYPASS: error type updated in native pending table - rows:" + rows);
			}
		} catch (Exception e) {
			// Not much we can do here. If the p.move throws an exception, we'll just
			// leave the message in the draft box.
			Logger.error(MessageUtils.class,"Failed to move message to outbox and mark as error: " + mmsUri, e);
		}
		// inform the other side that MMS send has failed
        VMASyncHook.markMMSSendFailed(context, ContentUris.parseId(mmsUri));
        long id = ContentUris.parseId(mmsUri);
        ConversationDataObserver.onMessageStatusChanged(findMmsThreadId(context, id), id, ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
        
	}
	
	public static void setTheme(Context context) {
		context.setTheme(getTheme(context));
	}
	
	public static int getTheme(Context context) {
		if (mTheme == -1) {
			String fontSupport = PreferenceManager.getDefaultSharedPreferences(context)
					.getString(MessagingPreferenceActivity.APP_FONT_SUPPORT, MessagingPreferenceActivity.APP_FONT_SUPPORT_DEFAULT);
			
			if (fontSupport.equals(MessagingPreferenceActivity.APP_FONT_SUPPORT_DEFAULT)) {
				if (android.provider.Settings.System.getFloat(
						context.getContentResolver(),
						android.provider.Settings.System.FONT_SCALE, (float) 1.0) > 1.0) {
					mTheme = R.style.Theme_Large;
				} else {
					mTheme = R.style.Theme_Normal;
				}
			} else {
				if (fontSupport.equals(MessagingPreferenceActivity.APP_FONT_SUPPORT_LARGE)) {
					mTheme = R.style.Theme_Large;
				} else {
					mTheme = R.style.Theme_Normal;
				}
			}
		}
		
		return mTheme;
	}
	
	public static void resetTheme() {
		mTheme = -1;
	}
   public static String readTextFile(Context context, int resId) {
        InputStream inputStream = context.getResources().openRawResource(resId);
        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader bufferedreader = new BufferedReader(inputreader);
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            while ((line = bufferedreader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return stringBuilder.toString();
    }    


   /**
    *   Strips the seperators from number
    *	if the number is only 7 digit number append area code to it 
    */
   public static String getParsedKey(String key) {
	   String parsedKey = key;

	   if (Mms.isEmailAddress(key)) {
		   parsedKey = key;
	   } else {
		   String formattedKey = MessageUtils.parsePhoneNumberForMms(key, true);
		   
		   //before stripping check if it is a valid phone number 
		   //since there are chances of messages coming in as SBI23XM
		   if (formattedKey != null) {
			   if (formattedKey.length() == 7 && formattedKey.charAt(0) != '+') {
				   String areaCode = ApplicationSettings.getAreaCode();
				   
				   if (areaCode != null) {
					   formattedKey = areaCode + formattedKey;
				   }
			   }
			   parsedKey = formattedKey;
		   }
	   }
	   
	   Logger.debug("getParsedKey: actual key: " + key + " processed key " + parsedKey);
	   return parsedKey;
   }
   
   public static long findMmsThreadId(Context context, long id) {
       StringBuilder sb = new StringBuilder('(');
       sb.append(Mms._ID);
       sb.append('=');
       sb.append(id);
       
       Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
       		VZUris.getMmsUri(), new String[] { Mms.THREAD_ID },
                           sb.toString(), null, null);
       if (cursor != null) {
           try {
               if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                   return cursor.getLong(0);
               }
           } finally {
               cursor.close();
           }
       }

       return -1;
   }
   
   public static long findSmsThreadId(Context context, long id) {
	   long threadId = -1;
	   StringBuilder sb = new StringBuilder('(');
       sb.append(Sms._ID);
       sb.append('=');
       sb.append(id);
       
	   Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
			   VZUris.getSmsUri(), new String[] { Sms.THREAD_ID }, sb.toString(), null, null);
	   try {
		   if ((cursor != null) && (cursor.moveToFirst())) {
			   threadId = cursor.getLong(0);
		   }
	   } finally {
		   if (cursor != null) {
			   cursor.close();
		   }
	   }
	   
	   return threadId;
   }
}
