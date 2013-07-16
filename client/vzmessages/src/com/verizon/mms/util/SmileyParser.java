/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.verizon.mms.util;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.DeviceConfig.OEM;

/**
 * A class for annotating a CharSequence with spans to convert textual emoticons
 * to graphical ones.
 */
public class SmileyParser {
    // Singleton stuff
    private static SmileyParser sInstance;
    public static SmileyParser getInstance() { return sInstance; }
    public static void init(Context context) {
        sInstance = new SmileyParser(context);
    }
    
    private final HashMap<String, Integer> mSmileyToRes;
    private final String[] mSmileyTexts;
    
    private final Context mContext;    
    private final Pattern mPattern;
 
    private SmileyParser(Context context) {
        mContext = context;
        mSmileyTexts = mContext.getResources().getStringArray(ALL_CHARACTERSET_SMILEY_TEXTS);        
        mSmileyToRes = buildSmileyToRes();    
        mPattern = buildPattern();
    }

    static class Smileys {
        private static final int[] sIconIds = {
            R.drawable.emo_im_tongue_out,
            R.drawable.emo_im_surprised,
            R.drawable.emo_im_smile,
            R.drawable.emo_im_sad,
            R.drawable.emo_im_open_mouth,
            R.drawable.emo_im_hot,
            R.drawable.emo_im_embarassed,
            R.drawable.emo_im_dont_tell,
            R.drawable.emo_im_disappointed,
            R.drawable.emo_im_devil,
            R.drawable.emo_im_crying,
            R.drawable.emo_im_confused,
            R.drawable.emo_im_barring_teeth,
            R.drawable.emo_im_angry,
            R.drawable.emo_im_angel,
            R.drawable.emo_im_wink,
            R.drawable.emo_im_cool,
            R.drawable.emo_im_kissing,
            R.drawable.emo_im_yelling,
            R.drawable.emo_im_foot_in_mouth
        };

        public static int TONGUE_OUT = 0;
        public static int SURPRISED = 1;
        public static int SMILE = 2;
        public static int SAD = 3;
        public static int OPEN_MOUTH = 4;
        public static int HOT = 5;
        public static int EMBARRASSED = 6;
        public static int DONT_TELL_ANYONE = 7;
        public static int DISAPPOINTED = 8;
        public static int DEVIL = 9;
        public static int CRYING = 10;
        public static int CONFUSED = 11;
        public static int BARRING_TEETH = 12;
        public static int ANGRY = 13;
        public static int ANGEL = 14;
        public static int WINK = 15;
        public static int COOL = 16;
        public static int KISSING = 17;
        public static int YELLING = 18;
        public static int FOOT_IN_MOUTH = 19;
        
        public static int getSmileyResource(int which) {
            return sIconIds[which];
        }
    }

    // NOTE: if you change anything about this array, you must make the corresponding change
    // to the string arrays: default_smiley_texts and default_smiley_names in res/values/arrays.xml
  /** Old order
    Smileys.getSmileyResource(Smileys.ANGEL),
	Smileys.getSmileyResource(Smileys.ANGRY),
	Smileys.getSmileyResource(Smileys.BARRING_TEETH),
	Smileys.getSmileyResource(Smileys.CONFUSED),
	Smileys.getSmileyResource(Smileys.COOL),
	Smileys.getSmileyResource(Smileys.CRYING),
	Smileys.getSmileyResource(Smileys.DEVIL),
	Smileys.getSmileyResource(Smileys.DISAPPOINTED),
	Smileys.getSmileyResource(Smileys.DONT_TELL_ANYONE), 
	Smileys.getSmileyResource(Smileys.EMBARRASSED),
	Smileys.getSmileyResource(Smileys.FOOT_IN_MOUTH),
	Smileys.getSmileyResource(Smileys.HOT),
	Smileys.getSmileyResource(Smileys.KISSING),
	Smileys.getSmileyResource(Smileys.OPEN_MOUTH),
	Smileys.getSmileyResource(Smileys.SAD),
	Smileys.getSmileyResource(Smileys.SMILE), 
	Smileys.getSmileyResource(Smileys.SURPRISED),
	Smileys.getSmileyResource(Smileys.TONGUE_OUT),            
	Smileys.getSmileyResource(Smileys.WINK),
	Smileys.getSmileyResource(Smileys.YELLING)};
	*/
	
    public static final int[] DEFAULT_SMILEY_RES_IDS = { 
    Smileys.getSmileyResource(Smileys.SMILE), 
    Smileys.getSmileyResource(Smileys.SAD),
    Smileys.getSmileyResource(Smileys.WINK),
    Smileys.getSmileyResource(Smileys.TONGUE_OUT),            
    Smileys.getSmileyResource(Smileys.SURPRISED),
    Smileys.getSmileyResource(Smileys.KISSING),
    Smileys.getSmileyResource(Smileys.YELLING),
    Smileys.getSmileyResource(Smileys.COOL),
    Smileys.getSmileyResource(Smileys.EMBARRASSED),
    Smileys.getSmileyResource(Smileys.FOOT_IN_MOUTH),
    Smileys.getSmileyResource(Smileys.ANGRY),
    Smileys.getSmileyResource(Smileys.ANGEL),
    Smileys.getSmileyResource(Smileys.BARRING_TEETH),
    Smileys.getSmileyResource(Smileys.CRYING),
    Smileys.getSmileyResource(Smileys.DONT_TELL_ANYONE), 
    Smileys.getSmileyResource(Smileys.OPEN_MOUTH),
    Smileys.getSmileyResource(Smileys.CONFUSED),
    Smileys.getSmileyResource(Smileys.DEVIL),
    Smileys.getSmileyResource(Smileys.DISAPPOINTED),
    Smileys.getSmileyResource(Smileys.HOT)};

    

    public static final int DEFAULT_SMILEY_TEXTS = R.array.default_smiley_texts;
    public static final int DEFAULT_SMILEY_NAMES = R.array.default_smiley_names;
    
    public static final int[] ALL_CHARACTERSET_SMILEY_RES_IDS = { 
            Smileys.getSmileyResource(Smileys.TONGUE_OUT),
            Smileys.getSmileyResource(Smileys.TONGUE_OUT),
            Smileys.getSmileyResource(Smileys.TONGUE_OUT),
            Smileys.getSmileyResource(Smileys.SURPRISED),
            Smileys.getSmileyResource(Smileys.SURPRISED),
            Smileys.getSmileyResource(Smileys.SURPRISED),
            Smileys.getSmileyResource(Smileys.SURPRISED),
            Smileys.getSmileyResource(Smileys.SURPRISED),
            Smileys.getSmileyResource(Smileys.SURPRISED),
            Smileys.getSmileyResource(Smileys.SMILE),
            Smileys.getSmileyResource(Smileys.SMILE),
            Smileys.getSmileyResource(Smileys.SAD),
            Smileys.getSmileyResource(Smileys.SAD),
            Smileys.getSmileyResource(Smileys.OPEN_MOUTH),
            Smileys.getSmileyResource(Smileys.OPEN_MOUTH),
            Smileys.getSmileyResource(Smileys.HOT),
            Smileys.getSmileyResource(Smileys.HOT),
            Smileys.getSmileyResource(Smileys.EMBARRASSED),
            Smileys.getSmileyResource(Smileys.EMBARRASSED),
            Smileys.getSmileyResource(Smileys.EMBARRASSED),
            Smileys.getSmileyResource(Smileys.DONT_TELL_ANYONE),
            Smileys.getSmileyResource(Smileys.DONT_TELL_ANYONE),
            Smileys.getSmileyResource(Smileys.DONT_TELL_ANYONE),
            Smileys.getSmileyResource(Smileys.DONT_TELL_ANYONE),
            Smileys.getSmileyResource(Smileys.DISAPPOINTED),
            Smileys.getSmileyResource(Smileys.DISAPPOINTED),
            Smileys.getSmileyResource(Smileys.FOOT_IN_MOUTH),
            Smileys.getSmileyResource(Smileys.DEVIL),
            Smileys.getSmileyResource(Smileys.CRYING),
            Smileys.getSmileyResource(Smileys.CONFUSED),
            Smileys.getSmileyResource(Smileys.CONFUSED),
            Smileys.getSmileyResource(Smileys.CONFUSED),
            Smileys.getSmileyResource(Smileys.CONFUSED),
            Smileys.getSmileyResource(Smileys.BARRING_TEETH),
            Smileys.getSmileyResource(Smileys.BARRING_TEETH),
            Smileys.getSmileyResource(Smileys.ANGRY),
            Smileys.getSmileyResource(Smileys.ANGRY),
            Smileys.getSmileyResource(Smileys.ANGEL),
            Smileys.getSmileyResource(Smileys.ANGEL),
            Smileys.getSmileyResource(Smileys.ANGEL),
            Smileys.getSmileyResource(Smileys.WINK),
            Smileys.getSmileyResource(Smileys.WINK),
            Smileys.getSmileyResource(Smileys.COOL),
            Smileys.getSmileyResource(Smileys.COOL),
            Smileys.getSmileyResource(Smileys.KISSING),
            Smileys.getSmileyResource(Smileys.YELLING),
            
                                                };
    public static final int   ALL_CHARACTERSET_SMILEY_TEXTS   = R.array.all_smiley_texts;


    /**
     * Builds the hashtable we use for mapping the string version
     * of a smiley (e.g. ":-)") to a resource ID for the icon version.
     */
    private HashMap<String, Integer> buildSmileyToRes() {
        if (ALL_CHARACTERSET_SMILEY_RES_IDS.length != mSmileyTexts.length) {
            // Throw an exception if someone updated ALL_CHARACTERSET_SMILEY_RES_IDS
            // and failed to update arrays.xml
            throw new IllegalStateException("Smiley resource ID/text mismatch");
        }

        HashMap<String, Integer> smileyToRes =
                            new HashMap<String, Integer>(mSmileyTexts.length);
        for (int i = 0; i < mSmileyTexts.length; i++) {
            smileyToRes.put(mSmileyTexts[i], ALL_CHARACTERSET_SMILEY_RES_IDS[i]);
        }

        return smileyToRes;
    }
    /**
     * Builds the regular expression we use to find smileys in {@link #addSmileySpans}.
     */
    private Pattern buildPattern() {
        // Set the StringBuilder capacity with the assumption that the average
        // smiley is 3 characters long.
        StringBuilder patternString = new StringBuilder(mSmileyTexts.length * 3);

        // Build a regex that looks like (:-)|:-(|...), but escaping the smilies
        // properly so they will be interpreted literally by the regex matcher.
        patternString.append('(');
        for (String s : mSmileyTexts) {
            patternString.append(Pattern.quote(s));
            patternString.append('|');
        }
        // Replace the extra '|' with a ')'
        patternString.replace(patternString.length() - 1, patternString.length(), ")");

        return Pattern.compile(patternString.toString());
    }
    
    /**
     * Adds ImageSpans to a CharSequence that replace textual emoticons such
     * as :-) with a graphical version.
     *
     * @param text A CharSequence possibly containing emoticons
     * @return A CharSequence annotated with ImageSpans covering any
     *         recognized emoticons.
     */
    public CharSequence addSmileySpans(CharSequence text , boolean isConversationScreen) {
    	if(OEM.deviceModel.equalsIgnoreCase("GT-P7500")){
			text = text + " ";
		}
    	SpannableStringBuilder builder = new SpannableStringBuilder(text);

		Matcher matcher = mPattern.matcher(text);
		while (matcher.find()) {
			int resId = mSmileyToRes.get(matcher.group());
			if (isConversationScreen) {
				Drawable d = mContext.getResources().getDrawable(resId);
				int emoticonSize = mContext.getResources().getDimensionPixelSize(R.dimen.emoticonSize);
				d.setBounds(0, 0, emoticonSize, emoticonSize);
				builder.setSpan(new ImageSpan(d), 
						matcher.start(), matcher.end(), 
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else {
				builder.setSpan(new ImageSpan(mContext, resId),
						matcher.start(), matcher.end(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
        
        return builder;
    }
}


