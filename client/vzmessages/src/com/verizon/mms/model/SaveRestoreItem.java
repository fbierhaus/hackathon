/**
 * SaveRestoreItem.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.text.format.Time;

import com.verizon.messaging.vzmsgs.R;

/**
 * This class/interface   
 * @author Imthiaz
 * @Since  May 29, 2012
 */
public class SaveRestoreItem implements Serializable {

    private static final long   serialVersionUID    = 5413631570235914316L;
     
    private String mFilename    = "";
   // private int    mFilesize    = -1;
    private long   mDate        = -1;
    private int    mType        = -1;

    public static int TYPE_INVALID    = 0;
    public static int TYPE_DIRECTORY  = 1;
    public static int TYPE_XML_FILE   = 2;
    public static int TYPE_OTHER_FILE = 3;

    public SaveRestoreItem() {
     
    }
    
    /**
     * Set the Value of the mFilename
     * @return the mFilename
     */
    public String getStringFilename() {
        return mFilename;
    }
    
    /**
     * Set the Value of the mFilename
     * @return the mFilename
     */
    public CharSequence getFilename() {
        return mFilename;
    }

    /**
     * Return the Value of the fielmFilename
     *
     * @param mFilename the mFilename to set
     */
    public void setFilename(String mFilename) {
        this.mFilename = mFilename;
    }

    /**
     * Set the Value of the mFilesize
     * @return the mFilesize
     */
   /* public int getFilesize() {
        return mFilesize;
    }
*/
    /**
     * Return the Value of the fielmFilesize
     *
     * @param mFilesize the mFilesize to set
     */
/*    public void setFilesize(int mFilesize) {
        this.mFilesize = mFilesize;
    }
*/
    /**
     * Set the Value of the mDate
     * @return the mDate
     */
    public long getDate() {
        return mDate;
    }

    /**
     * Set the Value of the mType
     * @return the mType
     */
    public int getType() {
        return mType;
    }

    /**
     * Return the Value of the fielmDate
     *
     * @param mDate the mDate to set
     */
    public void setDate(long mDate) {
        this.mDate = mDate;
    }

    /**
     * Return the Value of the fielmType
     *
     * @param mType the mType to set
     */
    public void setType(int mType) {
        this.mType = mType;
    }

   /* public CharSequence getSizeString(Context context) {
        
        int num = mFilesize;
        if (mFilesize > 1028576) { //Greater then 1MB
            num = mFilesize / 1028576;
            return "" + num + context.getResources().getString(R.string.megabyte);
        } else if (mFilesize > 1024 ) { //Greater then 1KB
            num = mFilesize / 1024;
            return "" + num + context.getResources().getString(R.string.kilobyte);
        } else {
           return "" + num + context.getResources().getString(R.string.bytes);
        }
         
    }*/
    
    public CharSequence getDateString(Context context)
    {
        String date = null;
        if (mDate > 0) {
        	date = getTimeDescription(context, mDate);
        	if (date.contains("AM") || date.contains("PM"))
                date = addSpaceInTime(date);
        }		
        return date;
    }
    
    
    public String getTimeDescription(Context context, long timeInMs)
    {
        final int DAYS_IN_LEAP_YEAR = 366;
        final int DAYS_IN_NON_LEAP_YEAR = 365;
        int dayDiff = -1;
        Time then = new Time();
        then.set(timeInMs);
        Time now = new Time();
        now.setToNow();

        /* If the date is in the future, display a full time stamp, or the years don't match,
         display a full timestamp*/
        if( (then.toMillis(false) > now.toMillis(false)) ||
            (then.year != now.year))
        {
            return formatTimeStampString(context, timeInMs);
        }

        // Weeks & days ago
        if(then.year == now.year)
        {
            // Simply just calculate how many different day(s) in the same year
            dayDiff = now.yearDay - then.yearDay;
        }
        else
        {
            // Calculate how many different day(s) between this year and last year and only within one year window.
            // Other than that, we just display the date and time stamp.
            if(then.year == now.year - 1)
            {
                dayDiff = now.yearDay + ( isLeapYear(then.year) ? DAYS_IN_LEAP_YEAR - then.yearDay : DAYS_IN_NON_LEAP_YEAR - then.yearDay );
            }
        }

        if( dayDiff > 0 && dayDiff <= DAYS_IN_LEAP_YEAR )
        {
            if( dayDiff == 1 )
            {
                return context.getString(R.string.date_yesterday);
            }

            int weeksAgo = dayDiff / 7;
            //LexaSG: Change for Localization plurals for date weeek ago and days ago
            if(weeksAgo > 0)
              {
                return String.format(context.getResources().getQuantityString(R.plurals.date_weeks_ago, weeksAgo),weeksAgo);
              }
                return String.format(context.getResources().getQuantityString(R.plurals.date_days_ago, dayDiff),dayDiff);
            
        }

        return formatTimeStampString(context, timeInMs);
    }
    
    /**
     * This Method 
     * @param year
     * @return
     */
    private boolean isLeapYear(int year) {
        // TODO Auto-generated method stub
        if( 0 != year % 4 )
            return false;

        if( 0 != year % 100 )
            return true;

        if( 0 != year % 400 )
            return false;

        return true;
    }
    
    private String addSpaceInTime(String input)
    {
        String newStr = null;

        if (input.contains("PM"))
        {
            int index = input.indexOf("PM", 0);
            if (index != -1)
            {
                newStr = input.substring(0,index) + " " + input.substring(index).replace("PM", "pm");
                input = newStr;
            }
        }
        else if (input.contains("AM"))
        {
            int index = input.indexOf("AM", 0);
            if (index != -1)
            {
                newStr = input.substring(0,index) + " " + input.substring(index).replace("AM", "am");
                input = newStr;
            }
        }

        if (newStr == null)
            newStr = input;

        return newStr;
    }

    private String formatTimeStampString(Context context, long when) {
        return formatTimeStampString(context, when, false);
    }
    
    private String formatTimeStampString(Context context, long when, boolean fullFormat)
    {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();
        String prefix = "";
      
        int timeFormat = 12;
        try
        {
            String tf = Settings.System.getString(context.getContentResolver(),Settings.System.TIME_12_24);
            timeFormat = Integer.parseInt(tf);
        }
        catch (Exception e)
        {
            timeFormat = 12;
        }
       

     
        int format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT |
                           DateUtils.FORMAT_ABBREV_ALL;
       

       
        if (timeFormat == 12)
            format_flags |= DateUtils.FORMAT_CAP_AMPM;
       

        // If the message is from a different year, show the date and year.
        if (then.year != now.year) {
            format_flags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            // If it is from a different day than today, show only the date.
            format_flags |= DateUtils.FORMAT_SHOW_DATE;
        } else {
            // Otherwise, if the message is from today, show the time.
            format_flags |= DateUtils.FORMAT_SHOW_TIME;
        }


        if (fullFormat)
        {
            SimpleDateFormat dateformat= new SimpleDateFormat("h:mm a, MM/d",Locale.getDefault());
            if (then.year == now.year)
            {
                if (timeFormat == 24)
                {
                    dateformat.applyPattern("HH:mm , MM/d");
                }
            }
            else
            {
                dateformat.applyPattern("h:mm a, MM/d/yy");
                if (timeFormat == 24)
                {
                    dateformat.applyPattern("HH:mm, MM/d/yy");
                }
            }

           
            return prefix+dateformat.format(when);
        }

       
        if(then.year != now.year)
          {
            
            SimpleDateFormat dateformat= new SimpleDateFormat("MM/d/yy",
                                    Locale.getDefault());
            return prefix+dateformat.format(new Date(DateUtils.formatDateTime(context, when, format_flags)));
        }

        else
            return prefix+DateUtils.formatDateTime(context, when, format_flags);
      
    }
    
    
    
}
