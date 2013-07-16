/*
 * Copyright (C) 2012 The CyanogenMod project
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

/**
 * A class for annotating a CharSequence with spans to convert textual Softbank
 * and Unicode emojis to graphical ones. The full Unicode proposal is available
 * here:
 * {@link "http://www.unicode.org/~scherer/emoji4unicode/snapshot/full.html"}
 */
public class EmojiParser {
    // Singleton stuff
    private static EmojiParser sInstance;

    public static EmojiParser getInstance() {
        return sInstance;
    }

    public static void init(Context context) {
        sInstance = new EmojiParser(context);
    }

    private final Context mContext;
    private final Pattern mPattern;
    private final HashMap<String, Integer> mSmileyToRes;

    /**
     * should be edited accordingly whenever we add or remove the emojis in each section
     */
    public final static int NUM_OF_SECTIONS = 5;
    
    public final static int SECTION_ONE_START_INDEX = 0;
    public final static int SECTION_TWO_START_INDEX = 189;
    public final static int SECTION_THREE_START_INDEX = 305;
    public final static int SECTION_FOUR_START_INDEX = 535;
    public final static int SECTION_FIVE_START_INDEX = 636;
    public final static int EMOJIS_LAST_INDEX = 842;

    
    private EmojiParser(Context context) {
        mContext = context;
        mSmileyToRes = buildSmileyToRes();
        mPattern = buildPattern();
    }

    static class Emojis {
        private static final int[] allEmojiIcons={
                R.drawable.e418,
                R.drawable.e417,
                R.drawable.em_1f617,
                R.drawable.em_1f619,
                R.drawable.e105,
                R.drawable.e409,
                R.drawable.em_1f61b,
                R.drawable.e415,
                R.drawable.e057,
                R.drawable.em_1f600,
                R.drawable.e056,
                R.drawable.e414,
                R.drawable.e405,
                R.drawable.e106,
                R.drawable.e40d,
                R.drawable.e404,
                R.drawable.e403,
                R.drawable.e40a,
                R.drawable.e40e,
                R.drawable.e058,
                R.drawable.e406,
                R.drawable.e413,
                R.drawable.e412,
                R.drawable.e411,
                R.drawable.e408,
                R.drawable.e401,
                R.drawable.e40f,
                R.drawable.em_1f605,
                R.drawable.e108,
                R.drawable.em_1f629,
                R.drawable.em_1f62b,
                R.drawable.e40b,
                R.drawable.e107,
                R.drawable.e059,
                R.drawable.e416,
                R.drawable.em_1f624,
                R.drawable.e407,
                R.drawable.em_1f606,
                R.drawable.em_1f60b,
                R.drawable.e40c,
                R.drawable.em_1f60e,
                R.drawable.em_1f634,
                R.drawable.em_1f635,
                R.drawable.e410,
                R.drawable.em_1f61f,
                R.drawable.em_1f626,
                R.drawable.em_1f627,
                R.drawable.em_1f608,
                R.drawable.e11a,
                R.drawable.em_1f62e,
                R.drawable.em_1f62c,
                R.drawable.em_1f610,
                R.drawable.em_1f615,
                R.drawable.em_1f62f,
                R.drawable.em_1f636,
                R.drawable.em_1f607,
                R.drawable.e402,
                R.drawable.em_1f611,
                R.drawable.e516,
                R.drawable.e517,
                R.drawable.e152,
                R.drawable.e51b,
                R.drawable.e51e,
                R.drawable.e51a,
                R.drawable.e001,
                R.drawable.e002,
                R.drawable.e004,
                R.drawable.e005,
                R.drawable.e518,
                R.drawable.e519,
                R.drawable.e515,
                R.drawable.e04e,
                R.drawable.e51c,
                R.drawable.em_1f63a,
                R.drawable.em_1f638,
                R.drawable.em_1f63b,
                R.drawable.em_1f63d,
                R.drawable.em_1f63c,
                R.drawable.em_1f640,
                R.drawable.em_1f63f,
                R.drawable.em_1f639,
                R.drawable.em_1f63e,
                R.drawable.em_1f479,
                R.drawable.em_1f47a,
                R.drawable.em_1f648,
                R.drawable.em_1f649,
                R.drawable.em_1f64a,
                R.drawable.e11c,
                R.drawable.e10c,
                R.drawable.e05a,
                R.drawable.e11d,
                R.drawable.e32e,
                R.drawable.e335,
                R.drawable.em_1f4ab,
                R.drawable.em_1f4a5,
                R.drawable.e334,
                R.drawable.em_1f4a6,
                R.drawable.em_1f4a7,
                R.drawable.e13c,
                R.drawable.e330,
                R.drawable.e41b,
                R.drawable.e419,
                R.drawable.e41a,
                R.drawable.em_1f445,
                R.drawable.e41c,
                R.drawable.e00e,
                R.drawable.e421,
                R.drawable.e420,
                R.drawable.e00d,
                R.drawable.e010,
                R.drawable.e011,
                R.drawable.e41e,
                R.drawable.e012,
                R.drawable.e422,
                R.drawable.e22e,
                R.drawable.e22f,
                R.drawable.e231,
                R.drawable.e230,
                R.drawable.e427,
                R.drawable.e41d,
                R.drawable.e00f,
                R.drawable.e41f,
                R.drawable.e14c,
                R.drawable.e201,
                R.drawable.e115,
                R.drawable.e51f,
                R.drawable.e428,
                R.drawable.em_1f46a,
                R.drawable.em_1f46c,
                R.drawable.em_1f46d,
                R.drawable.e111,
                R.drawable.e425,
                R.drawable.e429,
                R.drawable.e424,
                R.drawable.e423,
                R.drawable.e253,
                R.drawable.em_1f64b,
                R.drawable.e31e,
                R.drawable.e31f,
                R.drawable.e31d,
                R.drawable.em_1f470,
                R.drawable.em_1f64e,
                R.drawable.em_1f64d,
                R.drawable.e426,
                R.drawable.e503,
                R.drawable.e10e,
                R.drawable.e318,
                R.drawable.em_1f45f,
                R.drawable.em_1f45e,
                R.drawable.e31a,
                R.drawable.e13e,
                R.drawable.e31b,
                R.drawable.e006,
                R.drawable.e302,
                R.drawable.em_1f45a,
                R.drawable.e319,
                R.drawable.em_1f3bd,
                R.drawable.em_1f456,
                R.drawable.e321,
                R.drawable.e322,
                R.drawable.e11e,
                R.drawable.e323,
                R.drawable.em_1f45d,
                R.drawable.em_1f45b,
                R.drawable.em_1f453,
                R.drawable.e314,
                R.drawable.e43c,
                R.drawable.e31c,
                R.drawable.e32c,
                R.drawable.e32a,
                R.drawable.e32d,
                R.drawable.e32b,
                R.drawable.e022,
                R.drawable.e023,
                R.drawable.e328,
                R.drawable.e327,
                R.drawable.em_1f495,
                R.drawable.em_1f496,
                R.drawable.em_1f49e,
                R.drawable.e329,
                R.drawable.em_1f48c,
                R.drawable.e003,
                R.drawable.e034,
                R.drawable.e035,
                R.drawable.em_1f464,
                R.drawable.em_1f465,
                R.drawable.em_1f4ac,
                R.drawable.em_1f463,
                R.drawable.em_1f4ad,
                 R.drawable.e052,
                R.drawable.e52a,
                R.drawable.e04f,
                R.drawable.e053,
                R.drawable.e524,
                R.drawable.e52c,
                R.drawable.e531,
                R.drawable.e050,
                R.drawable.e527,
                R.drawable.e051,
                R.drawable.e10b,
                R.drawable.em_1f43d,
                R.drawable.e52b,
                R.drawable.e52f,
                R.drawable.e109,
                R.drawable.e528,
                R.drawable.e01a,
                R.drawable.e529,
                R.drawable.e526,
                R.drawable.em_1f43c,
                R.drawable.e52e,
                R.drawable.e055,
                R.drawable.e521,
                R.drawable.e523,
                R.drawable.em_1f423,
                R.drawable.em_1f425,
                R.drawable.e52d,
                R.drawable.em_1f422,
                R.drawable.e525,
                R.drawable.em_1f41c,
                R.drawable.em_1f41d,
                R.drawable.em_1f41e,
                R.drawable.em_1f40c,
                R.drawable.e10a,
                R.drawable.e441,
                R.drawable.e019,
                R.drawable.e054,
                R.drawable.e520,
                R.drawable.e522,
                R.drawable.em_1f40b,
                R.drawable.em_1f404,
                R.drawable.em_1f40f,
                R.drawable.em_1f400,
                R.drawable.em_1f401,
                R.drawable.em_1f402,
                R.drawable.em_1f403,
                R.drawable.em_1f405,
                R.drawable.e134,
                R.drawable.em_1f406,
                R.drawable.em_1f407,
                R.drawable.em_1f408,
                R.drawable.em_1f409,
                R.drawable.e530,
                R.drawable.em_1f42a,
                R.drawable.em_1f40a,
                R.drawable.em_1f410,
                R.drawable.em_1f413,
                R.drawable.em_1f415,
                R.drawable.em_1f416,
                R.drawable.em_1f421,
                R.drawable.em_1f432,
                R.drawable.em_1f429,
                R.drawable.em_1f43e,
                R.drawable.e303,
                R.drawable.e304,
                R.drawable.e305,
                R.drawable.e306,
                R.drawable.e307,
                R.drawable.e308,
                R.drawable.e110,
                R.drawable.e118,
                R.drawable.e119,
                R.drawable.e032,
                R.drawable.e447,
                R.drawable.e444,
                R.drawable.e030,
                R.drawable.em_1f33c,
                R.drawable.em_1f33f,
                R.drawable.em_1f331,
                R.drawable.em_1f332,
                R.drawable.em_1f330,
                R.drawable.em_1f333,
                R.drawable.em_1f344,
                R.drawable.em_1f310,
                R.drawable.em_1f31e,
                R.drawable.em_1f31d,
                R.drawable.em_1f31a,
                R.drawable.em_1f311,
                R.drawable.em_1f312,
                R.drawable.em_1f313,
                R.drawable.em_1f314,
                R.drawable.em_1f315,
                R.drawable.em_1f316,
                R.drawable.em_1f317,
                R.drawable.em_1f318,
                R.drawable.em_1f31c,
                R.drawable.em_1f31b,
                R.drawable.e04c,
                R.drawable.em_1f30d,
                R.drawable.em_1f30e,
                R.drawable.em_1f30f,
                R.drawable.em_1f30b,
                R.drawable.em_1f30c,
                R.drawable.em_1f320,
                R.drawable.e32f,
                R.drawable.e04a,
                R.drawable.em_26c5,
                R.drawable.e049,
                R.drawable.e13d,
                R.drawable.e04b,
                R.drawable.em_2744,
                R.drawable.e048,
                R.drawable.e443,
                R.drawable.em_1f301,
                R.drawable.e44c,
                R.drawable.e43e,
                R.drawable.e436,
                R.drawable.e437,
                R.drawable.e438,
                R.drawable.e43a,
                R.drawable.e439,
                R.drawable.e43b,
                R.drawable.e117,
                R.drawable.e440,
                R.drawable.e442,
                R.drawable.e446,
                R.drawable.e445,
                R.drawable.e11b,
                R.drawable.e448,
                R.drawable.e033,
                R.drawable.e112,
                R.drawable.em_1f38b,
                R.drawable.e312,
                R.drawable.em_1f38a,
                R.drawable.e310,
                R.drawable.e143,
                R.drawable.em_1f52e,
                R.drawable.e03d,
                R.drawable.e008,
                R.drawable.em_1f4f9,
                R.drawable.e129,
                R.drawable.e126,
                R.drawable.e127,
                R.drawable.e316,
                R.drawable.em_1f4be,
                R.drawable.e00c,
                R.drawable.e00a,
                R.drawable.e009,
                R.drawable.em_1f4de,
                R.drawable.em_1f4df,
                R.drawable.e00b,
                R.drawable.e14b,
                R.drawable.e12a,
                R.drawable.e128,
                R.drawable.e141,
                R.drawable.em_1f507,
                R.drawable.em_1f508,
                R.drawable.em_1f509,
                R.drawable.e325,
                R.drawable.em_1f515,
                R.drawable.e142,
                R.drawable.e317,
                R.drawable.em_23f3,
                R.drawable.em_231b,
                R.drawable.em_23f0,
                R.drawable.em_231a,
                R.drawable.e145,
                R.drawable.e144,
                R.drawable.em_1f50f,
                R.drawable.em_1f510,
                R.drawable.e03f,
                R.drawable.em_1f50e,
                R.drawable.e10f,
                R.drawable.em_1f526,
                R.drawable.em_1f506,
                R.drawable.em_1f505,
                R.drawable.em_1f50c,
                R.drawable.em_1f50b,
                R.drawable.em_1f6c1,
                R.drawable.e114,
                R.drawable.e13f,
                R.drawable.em_1f6bf,
                R.drawable.e140,
                R.drawable.em_1f527,
                R.drawable.em_1f529,
                R.drawable.e116,
                R.drawable.em_1f6aa,
                R.drawable.e30e,
                R.drawable.e311,
                R.drawable.e113,
                R.drawable.em_1f52a,
                R.drawable.e30f,
                R.drawable.e13b,
                R.drawable.e12f,
                R.drawable.em_1f4b4,
                R.drawable.em_1f4b5,
                R.drawable.em_1f4b6,
                R.drawable.em_1f4b7,
                R.drawable.em_1f4b3,
                R.drawable.em_1f4b8,
                R.drawable.e104,
                R.drawable.em_1f4e7,
                R.drawable.em_1f4e4,
                R.drawable.em_1f4e5,
                R.drawable.em_2709,
                R.drawable.e103,
                R.drawable.em_1f4e8,
                R.drawable.em_1f4ef,
                R.drawable.e101,
                R.drawable.em_1f4ea,
                R.drawable.em_1f4ec,
                R.drawable.em_1f4ed,
                R.drawable.e102,
                R.drawable.em_1f4e6,
                R.drawable.e301,
                R.drawable.em_1f4c4,
                R.drawable.em_1f4c3,
                R.drawable.em_1f4d1,
                R.drawable.em_1f4ca,
                R.drawable.em_1f4c8,
                R.drawable.em_1f4c9,
                R.drawable.em_1f4dc,
                R.drawable.em_1f4cb,
                R.drawable.em_1f4c5,
                R.drawable.em_1f4c6,
                R.drawable.em_1f4c7,
                R.drawable.em_1f4c1,
                R.drawable.em_1f4c2,
                R.drawable.e313,
                R.drawable.em_1f4cc,
                R.drawable.em_1f4ce,
                R.drawable.em_2712,
                R.drawable.em_270f,
                R.drawable.em_1f4cf,
                R.drawable.em_1f4d0,
                R.drawable.em_1f4d2,
                R.drawable.em_1f4d4,
                R.drawable.em_1f4d5,
                R.drawable.em_1f4d3,
                R.drawable.em_1f4d7,
                R.drawable.em_1f4d8,
                R.drawable.em_1f4d9,
                R.drawable.em_1f4da,
                R.drawable.em_1f4d6,
                R.drawable.em_1f516,
                R.drawable.em_1f4db,
                R.drawable.em_1f52c,
                R.drawable.em_1f52d,
                R.drawable.em_1f4f0,
                R.drawable.e502,
                R.drawable.e324,
                R.drawable.e03c,
                R.drawable.e30a,
                R.drawable.em_1f3bc,
                R.drawable.e03e,
                R.drawable.e326,
                R.drawable.em_1f3b9,
                R.drawable.em_1f3bb,
                R.drawable.e040,
                R.drawable.e041,
                R.drawable.e042,
                R.drawable.e12b,
                R.drawable.em_1f3ae,
                R.drawable.em_1f0cf,
                R.drawable.em_1f3b4,
                R.drawable.e12d,
                R.drawable.em_1f3b2,
                R.drawable.e130,
                R.drawable.e42b,
                R.drawable.e42a,
                R.drawable.e018,
                R.drawable.e016,
                R.drawable.e015,
                R.drawable.e42c,
                R.drawable.em_1f3c9,
                R.drawable.em_1f3b3,
                R.drawable.e014,
                R.drawable.em_1f6b4,
                R.drawable.em_1f6b5,
                R.drawable.e132,
                R.drawable.em_1f3c7,
                R.drawable.e131,
                R.drawable.e013,
                R.drawable.em_1f3c2,
                R.drawable.e42d,
                R.drawable.e017,
                R.drawable.em_1f3a3,
                R.drawable.e045,
                R.drawable.e338,
                R.drawable.e30b,
                R.drawable.em_1f37c,
                R.drawable.e047,
                R.drawable.e30c,
                R.drawable.e044,
                R.drawable.em_1f379,
                R.drawable.em_1f377,
                R.drawable.e043,
                R.drawable.em_1f355,
                R.drawable.e120,
                R.drawable.e33b,
                R.drawable.em_1f357,
                R.drawable.em_1f356,
                R.drawable.e33f,
                R.drawable.e341,
                R.drawable.em_1f364,
                R.drawable.e34c,
                R.drawable.e344,
                R.drawable.em_1f365,
                R.drawable.e342,
                R.drawable.e33d,
                R.drawable.e33e,
                R.drawable.e340,
                R.drawable.e34d,
                R.drawable.e343,
                R.drawable.e33c,
                R.drawable.e147,
                R.drawable.e339,
                R.drawable.em_1f369,
                R.drawable.em_1f36e,
                R.drawable.e33a,
                R.drawable.em_1f368,
                R.drawable.e43f,
                R.drawable.e34b,
                R.drawable.e046,
                R.drawable.em_1f36a,
                R.drawable.em_1f36b,
                R.drawable.em_1f36c,
                R.drawable.em_1f36d,
                R.drawable.em_1f36f,
                R.drawable.e345,
                R.drawable.e346,
                R.drawable.e347,
                R.drawable.e348,
                R.drawable.e349,
                R.drawable.em_1f34b,
                R.drawable.em_1f34c,
                R.drawable.em_1f34d,
                R.drawable.em_1f34f,
                R.drawable.em_1f347,
                R.drawable.em_1f348,
                R.drawable.em_1f350,
                R.drawable.em_1f351,
                R.drawable.em_1f352,
                R.drawable.e34a,
                R.drawable.em_1f360,
                R.drawable.em_1f33d,
                R.drawable.e036,
                R.drawable.em_1f3e1,
                R.drawable.e157,
                R.drawable.e038,
                R.drawable.e153,
                R.drawable.e155,
                R.drawable.e14d,
                R.drawable.e156,
                R.drawable.e501,
                R.drawable.e158,
                R.drawable.e43d,
                R.drawable.e037,
                R.drawable.e504,
                R.drawable.em_1f3e4,
                R.drawable.e44a,
                R.drawable.e146,
                R.drawable.e505,
                R.drawable.e506,
                R.drawable.e122,
                R.drawable.e508,
                R.drawable.e509,
                R.drawable.em_1f5fe,
                R.drawable.e03b,
                R.drawable.e04d,
                R.drawable.e449,
                R.drawable.e44b,
                R.drawable.e51d,
                R.drawable.em_1f309,
                R.drawable.em_1f3a0,
                R.drawable.e124,
                R.drawable.e121,
                R.drawable.e433,
                R.drawable.e202,
                R.drawable.e135,
                R.drawable.e01c,
                R.drawable.em_1f6a3,
                R.drawable.em_2693,
                R.drawable.e10d,
                R.drawable.e01d,
                R.drawable.e11f,
                R.drawable.em_1f681,
                R.drawable.em_1f682,
                R.drawable.em_1f68a,
                R.drawable.e039,
                R.drawable.em_1f69e,
                R.drawable.em_1f686,
                R.drawable.e435,
                R.drawable.e01f,
                R.drawable.em_1f688,
                R.drawable.e434,
                R.drawable.em_1f69d,
                R.drawable.e159,
                R.drawable.em_1f68b,
                R.drawable.e01e,
                R.drawable.em_1f68e,
                R.drawable.em_1f68d,
                R.drawable.e42e,
                R.drawable.em_1f698,
                R.drawable.e01b,
                R.drawable.e15a,
                R.drawable.em_1f696,
                R.drawable.em_1f69b,
                R.drawable.e42f,
                R.drawable.em_1f6a8,
                R.drawable.e432,
                R.drawable.em_1f694,
                R.drawable.e430,
                R.drawable.e431,
                R.drawable.em_1f690,
                R.drawable.e136,
                R.drawable.em_1f6a1,
                R.drawable.em_1f6a0,
                R.drawable.em_1f69f,
                R.drawable.em_1f69c,
                R.drawable.e320,
                R.drawable.e150,
                R.drawable.e125,
                R.drawable.em_1f6a6,
                R.drawable.e14e,
                R.drawable.e252,
                R.drawable.e137,
                R.drawable.e209,
                R.drawable.e03a,
                R.drawable.em_1f3ee,
                R.drawable.e133,
                R.drawable.e123,
                R.drawable.em_1f5ff,
                R.drawable.em_1f3aa,
                R.drawable.em_1f3ad,
                R.drawable.em_1f4cd,
                R.drawable.em_1f6a9,
                R.drawable.e50b,
                R.drawable.e514,
                R.drawable.e50e,
                R.drawable.e513,
                R.drawable.e50c,
                R.drawable.e50d,
                R.drawable.e511,
                R.drawable.e50f,
                R.drawable.e512,
                R.drawable.e510,
                        R.drawable.e21c,
                        R.drawable.e21d,
                        R.drawable.e21e,
                        R.drawable.e21f,
                        R.drawable.e220,
                        R.drawable.e221,
                        R.drawable.e222,
                        R.drawable.e223,
                        R.drawable.e224,
                        R.drawable.e225,
                        R.drawable.em_1f51f,
                        R.drawable.e210,
                        R.drawable.em_1f520,
                        R.drawable.em_1f521,
                        R.drawable.em_1f522,
                        R.drawable.em_1f523,
                        R.drawable.em_1f524,
                        R.drawable.e232,
                        R.drawable.e233,
                        R.drawable.e234,
                        R.drawable.e235,
                        R.drawable.e236,
                        R.drawable.e237,
                        R.drawable.e238,
                        R.drawable.e239,
                        R.drawable.em_2194,
                        R.drawable.em_2195,
                        R.drawable.e23a,
                        R.drawable.e23b,
                        R.drawable.e23c,
                        R.drawable.e23d,
                        R.drawable.em_2139,
                        R.drawable.em_1f53c,
                        R.drawable.em_1f53d,
                        R.drawable.em_23eb,
                        R.drawable.em_23ec,
                        R.drawable.em_21a9,
                        R.drawable.em_21aa,
                        R.drawable.em_2934,
                        R.drawable.em_2935,
                        R.drawable.e24d,
                        R.drawable.em_1f500,
                        R.drawable.em_1f501,
                        R.drawable.em_1f502,
                        R.drawable.em_1f504,
                        R.drawable.e212,
                        R.drawable.e213,
                        R.drawable.e214,
                        R.drawable.em_1f193,
                        R.drawable.em_1f196,
                        R.drawable.e20b,
                        R.drawable.e507,
                        R.drawable.e203,
                        R.drawable.e22c,
                        R.drawable.e22b,
                        R.drawable.e22a,
                        R.drawable.em_1f234,
                        R.drawable.em_1f232,
                        R.drawable.e226,
                        R.drawable.e227,
                        R.drawable.e22d,
                        R.drawable.e215,
                        R.drawable.e216,
                        R.drawable.e151,
                        R.drawable.e138,
                        R.drawable.e139,
                        R.drawable.e13a,
                        R.drawable.e309,
                        R.drawable.em_1f6b0,
                        R.drawable.em_1f6ae,
                        R.drawable.e14f,
                        R.drawable.e20a,
                        R.drawable.e208,
                        R.drawable.e217,
                        R.drawable.e218,
                        R.drawable.e228,
                        R.drawable.em_24c2,
                        R.drawable.em_1f6c2,
                        R.drawable.em_1f6c4,
                        R.drawable.em_1f6c5,
                        R.drawable.em_1f6c3,
                        R.drawable. em_1f251,
                        R.drawable. e315,
                        R.drawable. e30d,
                        R.drawable. em_1f191,
                        R.drawable. em_1f198,
                        R.drawable. e229,
                        R.drawable. em_1f6ab,
                        R.drawable. e207,
                        R.drawable. em_1f4f5,
                        R.drawable. em_1f6af,
                        R.drawable. em_1f6b1,
                        R.drawable. em_1f6b3,
                        R.drawable. em_1f6b7,
                        R.drawable. em_1f6b8,
                        R.drawable. em_26d4,
                        R.drawable. e206,
                        R.drawable. em_2747,
                        R.drawable. em_274e,
                        R.drawable. em_2705,
                        R.drawable. e205,
                        R.drawable. e204,
                        R.drawable. e12e,
                        R.drawable.e250,
                        R.drawable.e251,
                        R.drawable.e532,
                        R.drawable.e533,
                        R.drawable.e534,
                        R.drawable.e535,
                        R.drawable.e211, 
                        R.drawable.em_1f4a0,
                        R.drawable.em_267b,
                        R.drawable.e23f,
                        R.drawable.e240,
                        R.drawable.e241,
                        R.drawable.e242,
                        R.drawable.e243,
                        R.drawable.e244,
                        R.drawable.e245,
                        R.drawable.e246,
                        R.drawable.e247,
                        R.drawable.e248,
                        R.drawable.e249,
                        R.drawable.e24a,
                        R.drawable.e24b,
                        R.drawable.e23e,
                        R.drawable.e154,
                        R.drawable.e14a,
                        R.drawable.em_1f4b2,
                        R.drawable.e149,
                        R.drawable.e24e,
                        R.drawable.e24f,
                        R.drawable.e537,
                        R.drawable.e12c,
                        R.drawable.em_3030,
                        R.drawable.e24c,
                        R.drawable.em_1f51a,
                        R.drawable.em_1f519,
                        R.drawable.em_1f51b,
                        R.drawable.em_1f51c,
                        R.drawable.e333,
                        R.drawable.e332,
                        R.drawable.e021,
                        R.drawable.e020,
                        R.drawable.e337,
                        R.drawable.e336,
                        R.drawable.em_1f503,
                        R.drawable.e02f,
                        R.drawable.em_1f567,
                        R.drawable.e024,
                        R.drawable.em_1f55c,
                        R.drawable.e025,
                        R.drawable.em_1f55d,
                        R.drawable.e026,
                        R.drawable.em_1f55e,
                        R.drawable.e027,
                        R.drawable.em_1f55f,
                        R.drawable.e028,
                        R.drawable.em_1f560,
                        R.drawable.e029,
                        R.drawable.em_1f561,
                        R.drawable.e02a,
                        R.drawable.em_1f562,
                        R.drawable.e02b,
                        R.drawable.em_1f563,
                        R.drawable.e02c,
                        R.drawable.em_1f564,
                        R.drawable.e02d,
                        R.drawable.em_1f565,
                        R.drawable.e02e,
                        R.drawable.em_1f566,
                        R.drawable.em_2716,
                        R.drawable.em_2795,
                        R.drawable.em_2796,
                        R.drawable.em_2797,
                        R.drawable.e20e,
                        R.drawable.e20c,
                        R.drawable.e20f,
                        R.drawable.e20d,
                        R.drawable.em_1f4ae,
                        R.drawable.em_1f4af,
                        R.drawable.em_2714,
                        R.drawable.em_2611,
                        R.drawable.em_1f518,
                        R.drawable.em_1f517,
                        R.drawable.em_27b0,
                        R.drawable.e031,
                        R.drawable.e21a,
                        R.drawable.e21b,
                        R.drawable.em_25fe,
                        R.drawable.em_25fd,
                        R.drawable.em_2b1b,
                        R.drawable.em_2b1c,
                        R.drawable.em_25aa,
                        R.drawable.em_25ab,
                        R.drawable.em_1f53a,
                        R.drawable.em_25fb,
                        R.drawable.em_25fc,
                        R.drawable.e219,
                        R.drawable.em_26ab,
                        R.drawable.em_26aa,
                        R.drawable.em_1f535,
                        R.drawable.em_1f53b,
                        R.drawable.em_1f536,
                        R.drawable.em_1f537,
                        R.drawable.em_1f538,
                        R.drawable.em_1f539
                    };
        
        public static int getSmileyResource(int which) {
            return allEmojiIcons[which];
        }
    }

    // NOTE: if you change anything about this array, you must make the
    // corresponding change in both the string array: default_smiley_texts in
    // res/values/arrays.xml
    // You also need to update the unified emoji table in mEmojiTexts
    public static int[] DEFAULT_EMOJI_RES_IDS;

   

    public static final String[] allEmojiTexts = {
            "\uD83D\uDE18", // Softbank: E418 - Unified: 1F618
            "\uD83D\uDE1A", // Softbank: E417 - Unified: 1F61A
            "\uD83D\uDE17", // 1F617
            "\uD83D\uDE19", // 1F619
            "\uD83D\uDE1C", // Softbank: E105 - Unified: 1F61C
            "\uD83D\uDE1D", // Softbank: E409 - Unified: 1F61D
            "\uD83D\uDE1B", // 1F61B
            "\uD83D\uDE04", // Softbank: E415 - Unified: 1F604
            "\uD83D\uDE03", // Softbank: E057 - Unified: 1F603
            "\uD83D\uDE00", // 1F600
            "\uD83D\uDE0A", // Softbank: E056 - Unified: 1F60A
            "\u263A", // Softbank: E414 - Unified: 263A
            "\uD83D\uDE09", // Softbank: E405 - Unified: 1F609
            "\uD83D\uDE0D", // Softbank: E106 - Unified: 1F60D
            "\uD83D\uDE33", // Softbank: E40D - Unified: 1F633
            "\uD83D\uDE01", // Softbank: E404 - Unified: 1F601
            "\uD83D\uDE14", // Softbank: E403 - Unified: 1F614
            "\uD83D\uDE0C", // Softbank: E40A - Unified: 1F60C
            "\uD83D\uDE12", // Softbank: E40E - Unified: 1F612
            "\uD83D\uDE1E", // Softbank: E058 - Unified: 1F61E
            "\uD83D\uDE23", // Softbank: E406 - Unified: 1F623
            "\uD83D\uDE22", // Softbank: E413 - Unified: 1F622
            "\uD83D\uDE02", // Softbank: E412 - Unified: 1F602
            "\uD83D\uDE2D", // Softbank: E411 - Unified: 1F62D
            "\uD83D\uDE2A", // Softbank: E408 - Unified: 1F62A
            "\uD83D\uDE25", // Softbank: E401 - Unified: 1F625
            "\uD83D\uDE30", // Softbank: E40F - Unified: 1F630
            "\uD83D\uDE05", // Unified: 1F4A6
            "\uD83D\uDE13", // Softbank: E108 - Unified: 1F613
            "\uD83D\uDE29", // 1F629
            "\uD83D\uDE2B", // 1F62B
            "\uD83D\uDE28", // Softbank: E40B - Unified: 1F628
            "\uD83D\uDE31", // Softbank: E107 - Unified: 1F631
            "\uD83D\uDE20", // Softbank: E059 - Unified: 1F620
            "\uD83D\uDE21", // Softbank: E416 - Unified: 1F621
            "\uD83D\uDE24", // 1F624
            "\uD83D\uDE16", // Softbank: E407 - Unified: 1F616
            "\uD83D\uDE06", // 1F606
            "\uD83D\uDE0B", // 1F60B
            "\uD83D\uDE37", // Softbank: E40C - Unified: 1F637
            "\uD83D\uDE0E", // 1F60E
            "\uD83D\uDE34", // 1F634
            "\uD83D\uDE35", // 1F635
            "\uD83D\uDE32", // Softbank: E410 - Unified: 1F632
            "\uD83D\uDE1F", // 1F61F
            "\uD83D\uDE26", // 1F626
            "\uD83D\uDE27", // 1F627
            "\uD83D\uDE08", // 1F608
            "\uD83D\uDC7F", // Softbank: E11A - Unified: 1F47F
            "\uD83D\uDE2E", // 1F62E
            "\uD83D\uDE2C", // 1F62C
            "\uD83D\uDE10", // 1F610
            "\uD83D\uDE15", // 1F615
            "\uD83D\uDE2F", // 1F62F
            "\uD83D\uDE36", // 1F636
            "\uD83D\uDE07", // 1F607
            "\uD83D\uDE0F", // Softbank: E402 - Unified: 1F60F
            "\uD83D\uDE11", // 1F611
            "\uD83D\uDC72", // Softbank: E516 - Unified: 1F472
            "\uD83D\uDC73", // Softbank: E517 - Unified: 1F473
            "\uD83D\uDC6E", // Softbank: E152 - Unified: 1F46E
            "\uD83D\uDC77", // Softbank: E51B - Unified: 1F477
            "\uD83D\uDC82", // Softbank: E51E - Unified: 1F482
            "\uD83D\uDC76", // Softbank: E51A - Unified: 1F476
            "\uD83D\uDC66", // Softbank: E001 - Unified: 1F466
            "\uD83D\uDC67", // Softbank: E002 - Unified: 1F467
            "\uD83D\uDC68", // Softbank: E004 - Unified: 1F468
            "\uD83D\uDC69", // Softbank: E005 - Unified: 1F469
            "\uD83D\uDC74", // Softbank: E518 - Unified: 1F474
            "\uD83D\uDC75", // Softbank: E519 - Unified: 1F475
            "\uD83D\uDC71", // Softbank: E515 - Unified: 1F471
            "\uD83D\uDC7C", // Softbank: E04E - Unified: 1F47C
            "\uD83D\uDC78", // Softbank: E51C - Unified: 1F478
            "\uD83D\uDE3A", // 1F63A
            "\uD83D\uDE38", // 1F638
            "\uD83D\uDE3B", // 1F63B
            "\uD83D\uDE3D", // 1F63D
            "\uD83D\uDE3C", // 1F63C
            "\uD83D\uDE40", // 1F640
            "\uD83D\uDE3F", // 1F63F
            "\uD83D\uDE39", // 1F639
            "\uD83D\uDE3E", // 1F63E
            "\uD83D\uDC79", // 1F479
            "\uD83D\uDC7A", // 1F47A
            "\uD83D\uDE48", // 1F648
            "\uD83D\uDE49", // 1F649
            "\uD83D\uDE4A", // 1F64A
            "\uD83D\uDC80", // Softbank: E11C - Unified: 1F480
            "\uD83D\uDC7D", // Softbank: E10C - Unified: 1F47D
            "\uD83D\uDCA9", // Softbank: E05A - Unified: 1F4A9
            "\uD83D\uDD25", // Softbank: E11D - Unified: 1F525
            "\u2728", // Softbank: E32E - Unified: 2728
            "\uD83C\uDF1F", // Softbank: E335 - Unified: 1F31F
            "\uD83D\uDCAB", // 1F4AB
            "\uD83D\uDCA5", // 1F4A5
            "\uD83D\uDCA2", // Softbank: E334 - Unified: 1F4A2
            "\uD83D\uDCA6", // Softbank: E331 - Unified: 1F605
            "\uD83D\uDCA7", // 1F4A7
            "\uD83D\uDCA4", // Softbank: E13C - Unified: 1F4A4
            "\uD83D\uDCA8", // Softbank: E330 - Unified: 1F4A8
            "\uD83D\uDC42", // Softbank: E41B - Unified: 1F442
            "\uD83D\uDC40", // Softbank: E419 - Unified: 1F440
            "\uD83D\uDC43", // Softbank: E41A - Unified: 1F443
            "\uD83D\uDC45", // 1F445
            "\uD83D\uDC44", // Softbank: E41C - Unified: 1F444
            "\uD83D\uDC4D", // Softbank: E00E - Unified: 1F44D
            "\uD83D\uDC4E", // Softbank: E421 - Unified: 1F44E
            "\uD83D\uDC4C", // Softbank: E420 - Unified: 1F44C
            "\uD83D\uDC4A", // Softbank: E00D - Unified: 1F44A
            "\u270A", // Softbank: E010 - Unified: 270A
            "\u270C", // Softbank: E011 - Unified: 270C
            "\uD83D\uDC4B", // Softbank: E41E - Unified: 1F44B
            "\u270B", // Softbank: E012
            "\uD83D\uDC50", // Softbank: E422 - Unified: 1F450
            "\uD83D\uDC46", // Softbank: E22E - Unified: 1F446
            "\uD83D\uDC47", // Softbank: E22F - Unified: 1F447
            "\uD83D\uDC49", // Softbank: E231 - Unified: 1F449
            "\uD83D\uDC48", // Softbank: E230 - Unified: 1F448
            "\uD83D\uDE4C", // Softbank: E427 - Unified: 1F64C
            "\uD83D\uDE4F", // Softbank: E41D - Unified: 1F64F
            "\u261D", // Softbank: E00F - Unified: 261D
            "\uD83D\uDC4F", // Softbank: E41F - Unified: 1F44F
            "\uD83D\uDCAA", // Softbank: E14C - Unified: 1F4AA
            "\uD83D\uDEB6", // Softbank: E201 - Unified: 1F6B6
            "\uD83C\uDFC3", // Softbank: E115 - Unified: 1F3C3
            "\uD83D\uDC83", // Softbank: E51F - Unified: 1F483
            "\uD83D\uDC6B", // Softbank: E428 - Unified: 1F46B
            "\uD83D\uDC6A", // 1F46A
            "\uD83D\uDC6C", // 1F46C
            "\uD83D\uDC6D", // 1F46D
            "\uD83D\uDC8F", // Softbank: E111 - Unified: 1F48F
            "\uD83D\uDC91", // Softbank: E425 - Unified: 1F491
            "\uD83D\uDC6F", // Softbank: E429 - Unified: 1F46F
            "\uD83D\uDE46", // Softbank: E424 - Unified: 1F646
            "\uD83D\uDE45", // Softbank: E423 - Unified: 1F645
            "\uD83D\uDC81", // Softbank: E253 - Unified: 1F481
            "\uD83D\uDE4B", // 1F64B
            "\uD83D\uDC86", // Softbank: E31E - Unified: 1F486
            "\uD83D\uDC87", // Softbank: E31F - Unified: 1F487
            "\uD83D\uDC85", // Softbank: E31D - Unified: 1F485
            "\uD83D\uDC70", // 1F470
            "\uD83D\uDE4E", // 1F64E
            "\uD83D\uDE4D", // 1F64D
            "\uD83D\uDE47", // Softbank: E426 - Unified: 1F647
            "\uD83C\uDFA9", // Softbank: E503 - Unified: 1F3A9
            "\uD83D\uDC51", // Softbank: E10E - Unified: 1F451
            "\uD83D\uDC52", // Softbank: E318 - Unified: 1F452
            "\uD83D\uDC5F", // Unified: 1F45F
            "\uD83D\uDC5E", // Unified: 1F45E
            "\uD83D\uDC61", // Softbank: E31A - Unified: 1F461
            "\uD83D\uDC60", // Softbank: E13E - Unified: 1F460
            "\uD83D\uDC62", // Softbank: E31B - Unified: 1F462
            "\uD83D\uDC55", // Softbank: E006 - Unified: 1F455
            "\uD83D\uDC54", // Softbank: E302 - Unified: 1F454
            "\uD83D\uDC5A", // 1F45A
            "\uD83D\uDC57", // Softbank: E319 - Unified: 1F457
            "\uD83C\uDFBD", // 1F3BD
            "\uD83D\uDC56", // 1F456
            "\uD83D\uDC58", // Softbank: E321 - Unified: 1F458
            "\uD83D\uDC59", // Softbank: E322 - Unified: 1F459
            "\uD83D\uDCBC", // Softbank: E11E - Unified: 1F4BC
            "\uD83D\uDC5C", // Softbank: E323 - Unified: 1F45C
            "\uD83D\uDC5D", // 1F45D
            "\uD83D\uDC5B", // 1F45B
            "\uD83D\uDC53", // 1F453
            "\uD83C\uDF80", // Softbank: E314 - Unified: 1F380
            "\uD83C\uDF02", // Softbank: E43C - Unified: 1F302
            "\uD83D\uDC84", // Softbank: E31C - Unified: 1F484
            "\uD83D\uDC9B", // Softbank: E32C - Unified: 1F49B
            "\uD83D\uDC99", // Softbank: E32A - Unified: 1F499
            "\uD83D\uDC9C", // Softbank: E32D - Unified: 1F49C
            "\uD83D\uDC9A", // Softbank: E32B - Unified: 1F49A
            "\u2764", // Softbank: E022 - Unified: 2764
            "\uD83D\uDC94", // Softbank: E023 - Unified: 1F494
            "\uD83D\uDC97", // Softbank: E328 - Unified: 1F497
            "\uD83D\uDC93", // Softbank: E327 - Unified: 1F493
            "\uD83D\uDC95", // 1F495
            "\uD83D\uDC96", // 1F496
            "\uD83D\uDC9E", // 1F49E
            "\uD83D\uDC98", // Softbank: E329 - Unified: 1F498
            "\uD83D\uDC8C", // 1F48C
            "\uD83D\uDC8B", // Softbank: E003 - Unified: 1F48B
            "\uD83D\uDC8D", // Softbank: E034 - Unified: 1F48D
            "\uD83D\uDC8E", // Softbank: E035 - Unified: 1F48E
            "\uD83D\uDC64", // 1F464
            "\uD83D\uDC65", // 1F465
            "\uD83D\uDCAC", // 1F4AC
            "\uD83D\uDC63", // Softbank: E536 - Unified: 1F43E
            "\uD83D\uDCAD", // 1F4AD
            "\uD83D\uDC36", // Softbank: E052 - Unified: 1F463
            "\uD83D\uDC3A", // Softbank: E52A - Unified: 1F43A
            "\uD83D\uDC31", // Softbank: E04F - Unified: 1F431
            "\uD83D\uDC2D", // Softbank: E053 - Unified: 1F42D
            "\uD83D\uDC39", // Softbank: E524 - Unified: 1F439
            "\uD83D\uDC30", // Softbank: E52C - Unified: 1F430
            "\uD83D\uDC38", // Softbank: E531 - Unified: 1F438
            "\uD83D\uDC2F", // Softbank: E050 - Unified: 1F42F
            "\uD83D\uDC28", // Softbank: E527 - Unified: 1F428
            "\uD83D\uDC3B", // Softbank: E051 - Unified: 1F43B
            "\uD83D\uDC37", // Softbank: E10B - Unified: 1F437
            "\uD83D\uDC3D", // 1F43D
            "\uD83D\uDC2E", // Softbank: E52B - Unified: 1F42E
            "\uD83D\uDC17", // Softbank: E52F - Unified: 1F417
            "\uD83D\uDC35", // Softbank: E109 - Unified: 1F435
            "\uD83D\uDC12", // Softbank: E528 - Unified: 1F412
            "\uD83D\uDC34", // Softbank: E01A - Unified: 1F434
            "\uD83D\uDC11", // Softbank: E529 - Unified: 1F411
            "\uD83D\uDC18", // Softbank: E526 - Unified: 1F418
            "\uD83D\uDC3C", // 1F43C
            "\uD83D\uDC14", // Softbank: E52E - Unified: 1F414
            "\uD83D\uDC27", // Softbank: E055 - Unified: 1F427
            "\uD83D\uDC26", // Softbank: E521 - Unified: 1F426
            "\uD83D\uDC24", // Softbank: E523 - Unified: 1F424
            "\uD83D\uDC23", // 1F423
            "\uD83D\uDC25", // 1F425
            "\uD83D\uDC0D", // Softbank: E52D - Unified: 1F40D
            "\uD83D\uDC22", // 1F422
            "\uD83D\uDC1B", // Softbank: E525 - Unified: 1F41B
            "\uD83D\uDC1C", // 1F41C
            "\uD83D\uDC1D", // 1F41D
            "\uD83D\uDC1E", // 1F41E
            "\uD83D\uDC0C", // 1F40C
            "\uD83D\uDC19", // Softbank: E10A - Unified: 1F419
            "\uD83D\uDC1A", // Softbank: E441 - Unified: 1F41A
            "\uD83D\uDC1F", // Softbank: E019 - Unified: 1F41F
            "\uD83D\uDC33", // Softbank: E054 - Unified: 1F433
            "\uD83D\uDC2C", // Softbank: E520 - Unified: 1F42C
            "\uD83D\uDC20", // Softbank: E522 - Unified: 1F420
            "\uD83D\uDC0B", // 1F40B
            "\uD83D\uDC04", // 1F404
            "\uD83D\uDC0F", // 1F40F
            "\uD83D\uDC00", // 1F400
            "\uD83D\uDC01", // 1F401
            "\uD83D\uDC02", // 1F402
            "\uD83D\uDC03", // 1F403
            "\uD83D\uDC05", // 1F405
            "\uD83D\uDC0E", // Softbank: E134 - Unified: 1F40E
            "\uD83D\uDC06", // 1F406
            "\uD83D\uDC07", // 1F407
            "\uD83D\uDC08", // 1F408
            "\uD83D\uDC09", // 1F409
            "\uD83D\uDC2B", // Softbank: E530 - Unified: 1F42B
            "\uD83D\uDC2A", // 1F42A
            "\uD83D\uDC0A", // 1F40A
            "\uD83D\uDC10", // 1F410
            "\uD83D\uDC13", // 1F413
            "\uD83D\uDC15", // 1F415
            "\uD83D\uDC16", // 1F416
            "\uD83D\uDC21", // 1F421
            "\uD83D\uDC32", // 1F432
            "\uD83D\uDC29", // 1F429
            "\uD83D\uDC3E", // 1F43E
            "\uD83C\uDF3A", // Softbank: E303 - Unified: 1F33A
            "\uD83C\uDF37", // Softbank: E304 - Unified: 1F337
            "\uD83C\uDF3B", // Softbank: E305 - Unified: 1F33B
            "\uD83D\uDC90", // Softbank: E306 - Unified: 1F490
            "\uD83C\uDF34", // Softbank: E307 - Unified: 1F334
            "\uD83C\uDF35", // Softbank: E308 - Unified: 1F335
            "\uD83C\uDF40", // Softbank: E110 - Unified: 1F340
            "\uD83C\uDF41", // Softbank: E118 - Unified: 1F341
            "\uD83C\uDF42", // Softbank: E119 - Unified: 1F342
            "\uD83C\uDF39", // Softbank: E032 - Unified: 1F339
            "\uD83C\uDF43", // Softbank: E447 - Unified: 1F343
            "\uD83C\uDF3E", // Softbank: E444 - Unified: 1F33E
            "\uD83C\uDF38", // Softbank: E030 - Unified: 1F338
            "\uD83C\uDF3C", // 1F33C
            "\uD83C\uDF3F", // 1F33F
            "\uD83C\uDF31", // 1F331
            "\uD83C\uDF32", // 1F332
            "\uD83C\uDF30", // 1F330
            "\uD83C\uDF33", // 1F333
            "\uD83C\uDF44", // 1F344
            "\uD83C\uDF10", // 1F310
            "\uD83C\uDF1E", // 1F31E
            "\uD83C\uDF1D", // 1F31D
            "\uD83C\uDF1A", // 1F31A
            "\uD83C\uDF11", // 1F311
            "\uD83C\uDF12", // 1F312
            "\uD83C\uDF13", // 1F313
            "\uD83C\uDF14", // 1F314
            "\uD83C\uDF15", // 1F315
            "\uD83C\uDF16", // 1F316
            "\uD83C\uDF17", // 1F317
            "\uD83C\uDF18", // 1F318
            "\uD83C\uDF1C", // 1F31C
            "\uD83C\uDF1B", // 1F31B
            "\uD83C\uDF19", // Softbank: E04C - Unified: 1F319
            "\uD83C\uDF0D", // 1F30D
            "\uD83C\uDF0E", // 1F30E
            "\uD83C\uDF0F", // 1F30F
            "\uD83C\uDF0B", // 1F30B
            "\uD83C\uDF0C", // 1F30C
            "\uD83C\uDF20", // 1F320
            "\u2B50", // Softbank: E32F - Unified: 2B50
            "\u2600", // Softbank: E04A - Unified: 2600
            "\u26C5", // 26C5
            "\u2601", // Softbank: E049 - Unified: 2601
            "\u26A1", // Softbank: E13D - Unified: 26A1
            "\u2614", // Softbank: E04B - Unified: 2614
            "\u2744", // 2744
            "\u26C4", // Softbank: E048 - Unified: 26C4
            "\uD83C\uDF00", // Softbank: E443 - Unified: 1F300
            "\uD83C\uDF01", // 1F301
            "\uD83C\uDF08", // Softbank: E44C - Unified: 1F308
            "\uD83C\uDF0A", // Softbank: E43E - Unified: 1F30A
            "\uD83C\uDF8D", // Softbank: E436 - Unified: 1F38D
            "\uD83D\uDC9D", // Softbank: E437 - Unified: 1F49D
            "\uD83C\uDF8E", // Softbank: E438 - Unified: 1F38E
            "\uD83C\uDF92", // Softbank: E43A - Unified: 1F392
            "\uD83C\uDF93", // Softbank: E439 - Unified: 1F393
            "\uD83C\uDF8F", // Softbank: E43B - Unified: 1F38F
            "\uD83C\uDF86", // Softbank: E117 - Unified: 1F386
            "\uD83C\uDF87", // Softbank: E440 - Unified: 1F387
            "\uD83C\uDF90", // Softbank: E442 - Unified: 1F390
            "\uD83C\uDF91", // Softbank: E446 - Unified: 1F391
            "\uD83C\uDF83", // Softbank: E445 - Unified: 1F383
            "\uD83D\uDC7B", // Softbank: E11B - Unified: 1F47B
            "\uD83C\uDF85", // Softbank: E448 - Unified: 1F385
            "\uD83C\uDF84", // Softbank: E033 - Unified: 1F384
            "\uD83C\uDF81", // Softbank: E112 - Unified: 1F381
            "\uD83C\uDF8B", // 1F38B
            "\uD83C\uDF89", // Softbank: E312 - Unified: 1F389
            "\uD83C\uDF8A", // 1F38A
            "\uD83C\uDF88", // Softbank: E310 - Unified: 1F388
            "\uD83C\uDF8C", // Softbank: E143 - Unified: 1F38C
            "\uD83D\uDD2E", // 1F52E
            "\uD83C\uDFA5", // Softbank: E03D - Unified: 1F3A5
            "\uD83D\uDCF7", // Softbank: E008 - Unified: 1F4F7
            "\uD83D\uDCF9", // Softbank: E03D - Unified: 1F4F9
            "\uD83D\uDCFC", // Softbank: E129 - Unified: 1F4FC
            "\uD83D\uDCBF", // Softbank: E126 - Unified: 1F4BF
            "\uD83D\uDCC0", // Softbank: E127 - Unified: 1F4C0
            "\uD83D\uDCBD", // Softbank: E316 - Unified: 1F4BD
            "\uD83D\uDCBE", // 1F4BE
            "\uD83D\uDCBB", // Softbank: E00C - Unified: 1F4BB
            "\uD83D\uDCF1", // Softbank: E00A - Unified: 1F4F1
            "\u260E", // Softbank: E009 - Unified: 260E
            "\uD83D\uDCDE", // 1F4DE
            "\uD83D\uDCDF", // 1F4DF
            "\uD83D\uDCE0", // Softbank: E00B - Unified: 1F4E0
            "\uD83D\uDCE1", // Softbank: E14B - Unified: 1F4E1
            "\uD83D\uDCFA", // Softbank: E12A - Unified: 1F4FA
            "\uD83D\uDCFB", // Softbank: E128 - Unified: 1F4FB
            "\uD83D\uDD0A", // Softbank: E141 - Unified: 1F50A
            "\uD83D\uDD07", // 1F507
            "\uD83D\uDD08", // 1F508
            "\uD83D\uDD09", // 1F509
            "\uD83D\uDD14", // Softbank: E325 - Unified: 1F514
            "\uD83D\uDD15", // 1F515
            "\uD83D\uDCE2", // Softbank: E142 - Unified: 1F4E2
            "\uD83D\uDCE3", // Softbank: E317 - Unified: 1F4E3
            "\u23F3", // 23F3
            "\u231B", // 231B
            "\u23F0", // 23F0
            "\u231A", // 231A
            "\uD83D\uDD13", // Softbank: E145 - Unified: 1F513
            "\uD83D\uDD12", // Softbank: E144 - Unified: 1F512
            "\uD83D\uDD0F", // 1F50F
            "\uD83D\uDD10", // 1F510
            "\uD83D\uDD11", // Softbank: E03F - Unified: 1F511
            "\uD83D\uDD0E", // 1F50E
            "\uD83D\uDCA1", // Softbank: E10F - Unified: 1F4A1
            "\uD83D\uDD26", // 1F526
            "\uD83D\uDD06", // 1F506
            "\uD83D\uDD05", // 1F505
            "\uD83D\uDD0C", // 1F50C
            "\uD83D\uDD0B", // 1F50B
            "\uD83D\uDEC1", // 1F6C1
            "\uD83D\uDD0D", // Softbank: E114 - Unified: 1F50D
            "\uD83D\uDEC0", // Softbank: E13F - Unified: 1F6C0
            "\uD83D\uDEBF", // 1F6BF
            "\uD83D\uDEBD", // Softbank: E140 - Unified: 1F6BD
            "\uD83D\uDD27", // 1F527
            "\uD83D\uDD29", // 1F529
            "\uD83D\uDD28", // Softbank: E116 - Unified: 1F528
            "\uD83D\uDEAA", // 1F6AA
            "\uD83D\uDEAC", // Softbank: E30E - Unified: 1F6AC
            "\uD83D\uDCA3", // Softbank: E311 - Unified: 1F4A3
            "\uD83D\uDD2B", // Softbank: E113 - Unified: 1F52B
            "\uD83D\uDD2A", // 1F52A
            "\uD83D\uDC8A", // Softbank: E30F - Unified: 1F48A
            "\uD83D\uDC89", // Softbank: E13B - Unified: 1F489
            "\uD83D\uDCB0", // Softbank: E12F - Unified: 1F4B0
            "\uD83D\uDCB4", // 1F4B4
            "\uD83D\uDCB5", // 1F4B5
            "\uD83D\uDCB6", // 1F4B6
            "\uD83D\uDCB7", // 1F4B7
            "\uD83D\uDCB3", // 1F4B3
            "\uD83D\uDCB8", // 1F4B8
            "\uD83D\uDCF2", // Softbank: E104 - Unified: 1F4F2
            "\uD83D\uDCE7", // 1F4E7
            "\uD83D\uDCE4", // 1F4E4
            "\uD83D\uDCE5", // 1F4E5
            "\u2709", // 2709
            "\uD83D\uDCE9", // Softbank: E103 - Unified: 1F4E9
            "\uD83D\uDCE8", // 1F4E8
            "\uD83D\uDCEF", // 1F4EF
            "\uD83D\uDCEB", // Softbank: E101 - Unified: 1F4EB
            "\uD83D\uDCEA", // 1F4EA
            "\uD83D\uDCEC", // 1F4EC
            "\uD83D\uDCED", // 1F4ED
            "\uD83D\uDCEE", // Softbank: E102 - Unified: 1F4EE
            "\uD83D\uDCE6", // 1F4E6
            "\uD83D\uDCDD", // Softbank: E301 - Unified: 1F4DD
            "\uD83D\uDCC4", // 1F4C4
            "\uD83D\uDCC3", // 1F4C3
            "\uD83D\uDCD1", // 1F4D1
            "\uD83D\uDCCA", // 1F4CA
            "\uD83D\uDCC8", // 1F4C8
            "\uD83D\uDCC9", // 1F4C9
            "\uD83D\uDCDC", // 1F4DC
            "\uD83D\uDCCB", // 1F4CB
            "\uD83D\uDCC5", // 1F4C5
            "\uD83D\uDCC6", // 1F4C6
            "\uD83D\uDCC7", // 1F4C7
            "\uD83D\uDCC1", // 1F4C1
            "\uD83D\uDCC2", // 1F4C2
            "\u2702", // Softbank: E313 - Unified: 2702
            "\uD83D\uDCCC", // 1F4CC
            "\uD83D\uDCCE", // 1F4CE
            "\u2712", // 2712
            "\u270F", // 270F
            "\uD83D\uDCCF", // 1F4CF
            "\uD83D\uDCD0", // 1F4D0
            "\uD83D\uDCD2", // 1F4D2
            "\uD83D\uDCD4", // 1F4D4
            "\uD83D\uDCD5", // 1F4D5
            "\uD83D\uDCD3", //1F4D3
            "\uD83D\uDCD7", // 1F4D7
            "\uD83D\uDCD8", // 1F4D8
            "\uD83D\uDCD9", // 1F4D9
            "\uD83D\uDCDA", // 1F4DA
            "\uD83D\uDCD6", // Softbank: E148 - Unified: 1F4D6
            "\uD83D\uDD16", // 1F516
            "\uD83D\uDCDB", // 1F4DB
            "\uD83D\uDD2C", // 1F52C
            "\uD83D\uDD2D", // 1F52D
            "\uD83D\uDCF0", // 1F4F0
            "\uD83C\uDFA8", // Softbank: E502 - Unified: 1F3A8
            "\uD83C\uDFAC", // Softbank: E324 - Unified: 1F3AC
            "\uD83C\uDFA4", // Softbank: E03C - Unified: 1F3A4
            "\uD83C\uDFA7", // Softbank: E30A - Unified: 1F3A7
            "\uD83C\uDFBC", // 1F3BC
            "\uD83C\uDFB5", // Softbank: E03E - Unified: 1F3B5
            "\uD83C\uDFB6", // Softbank: E326 - Unified: 1F3B6
            "\uD83C\uDFB9", // 1F3B9
            "\uD83C\uDFBB", // 1F3BB
            "\uD83C\uDFB7", // Softbank: E040 - Unified: 1F3B7
            "\uD83C\uDFB8", // Softbank: E041 - Unified: 1F3B8
            "\uD83C\uDFBA", // Softbank: E042 - Unified: 1F3BA
            "\uD83D\uDC7E", // Softbank: E12B - Unified: 1F47E
            "\uD83C\uDFAE", // 1F3AE
            "\uD83C\uDCCF", // 1F0CF
            "\uD83C\uDFB4", // 1F3B4
            "\uD83C\uDC04", // Softbank: E12D - Unified: 1F004
            "\uD83C\uDFB2", // 1F3B2
            "\uD83C\uDFAF", // Softbank: E130 - Unified: 1F3AF
            "\uD83C\uDFC8", // Softbank: E42B - Unified: 1F3C8
            "\uD83C\uDFC0", // Softbank: E42A - Unified: 1F3C0
            "\u26BD", // Softbank: E018 - Unified: 26BD
            "\u26BE", // Softbank: E016 - Unified: 26BE
            "\uD83C\uDFBE", // Softbank: E015 - Unified: 1F3BE
            "\uD83C\uDFB1", // Softbank: E42C - Unified: 1F3B1
            "\uD83C\uDFC9", // 1F3C9
            "\uD83C\uDFB3", // 1F3B3
            "\u26F3", // Softbank: E014 - Unified: 26F3
            "\uD83D\uDEB4", // 1F6B4
            "\uD83D\uDEB5", // 1F6B5
            "\uD83C\uDFC1", // Softbank: E132 - Unified: 1F3C1
            "\uD83C\uDFC7", // 1F3C7
            "\uD83C\uDFC6", // Softbank: E131 - Unified: 1F3C6
            "\uD83C\uDFBF", // Softbank: E013 - Unified: 1F3BF
            "\uD83C\uDFC2", // 1F3C2
            "\uD83C\uDFCA", // Softbank: E42D - Unified: 1F3CA
            "\uD83C\uDFC4", // Softbank: E017 - Unified: 1F3C4
            "\uD83C\uDFA3", // 1F3A3
            "\u2615", // Softbank: E045 - Unified: 2615
            "\uD83C\uDF75", // Softbank: E338 - Unified: 1F375
            "\uD83C\uDF76", // Softbank: E30B - Unified: 1F376
            "\uD83C\uDF7C", // 1F37C
            "\uD83C\uDF7A", // Softbank: E047 - Unified: 1F37A
            "\uD83C\uDF7B", // Softbank: E30C - Unified: 1F37B
            "\uD83C\uDF78", // Softbank: E044 - Unified: 1F378
            "\uD83C\uDF79", // 1F379
            "\uD83C\uDF77", // 1F377
            "\uD83C\uDF74", // Softbank: E043 - Unified: 1F374
            "\uD83C\uDF55", // 1F355
            "\uD83C\uDF54", // Softbank: E120 - Unified: 1F354
            "\uD83C\uDF5F", // Softbank: E33B - Unified: 1F35F
            "\uD83C\uDF57", // 1F357
            "\uD83C\uDF56", // 1F356
            "\uD83C\uDF5D", // Softbank: E33F - Unified: 1F35D
            "\uD83C\uDF5B", // Softbank: E341 - Unified: 1F35B
            "\uD83C\uDF64", // 1F364
            "\uD83C\uDF71", // Softbank: E34C - Unified: 1F371
            "\uD83C\uDF63", // Softbank: E344 - Unified: 1F363
            "\uD83C\uDF65", // 1F365
            "\uD83C\uDF59", // Softbank: E342 - Unified: 1F359
            "\uD83C\uDF58", // Softbank: E33D - Unified: 1F358
            "\uD83C\uDF5A", // Softbank: E33E - Unified: 1F35A
            "\uD83C\uDF5C", // Softbank: E340 - Unified: 1F35C
            "\uD83C\uDF72", // Softbank: E34D - Unified: 1F372
            "\uD83C\uDF62", // Softbank: E343 - Unified: 1F362
            "\uD83C\uDF61", // Softbank: E33C - Unified: 1F361
            "\uD83C\uDF73", // Softbank: E147 - Unified: 1F373
            "\uD83C\uDF5E", // Softbank: E339 - Unified: 1F35E
            "\uD83C\uDF69", // 1F369
            "\uD83C\uDF6E", // 1F36E
            "\uD83C\uDF66", // Softbank: E33A - Unified: 1F366
            "\uD83C\uDF68", // 1F368
            "\uD83C\uDF67", // Softbank: E43F - Unified: 1F367
            "\uD83C\uDF82", // Softbank: E34B - Unified: 1F382
            "\uD83C\uDF70", // Softbank: E046 - Unified: 1F370
            "\uD83C\uDF6A", // 1F36A
            "\uD83C\uDF6B", // 1F36B
            "\uD83C\uDF6C", // 1F36C
            "\uD83C\uDF6D", // 1F36D
            "\uD83C\uDF6F", // 1F36F
            "\uD83C\uDF4E", // Softbank: E345 - Unified: 1F34E
            "\uD83C\uDF4A", // Softbank: E346 - Unified: 1F34A
            "\uD83C\uDF53", // Softbank: E347 - Unified: 1F353
            "\uD83C\uDF49", // Softbank: E348 - Unified: 1F349
            "\uD83C\uDF45", // Softbank: E349 - Unified: 1F345
            "\uD83C\uDF4B", // 1F34B
            "\uD83C\uDF4C", // 1F34C
            "\uD83C\uDF4D", // 1F34D
            "\uD83C\uDF4F", // 1F34F
            "\uD83C\uDF47", // 1F347
            "\uD83C\uDF48", // 1F348
            "\uD83C\uDF50", // 1F350
            "\uD83C\uDF51", // 1F351
            "\uD83C\uDF52", // 1F352
            "\uD83C\uDF46", // Softbank: E34A - Unified: 1F346
            "\uD83C\uDF60", // 1F360
            "\uD83C\uDF3D", // 1F33D
            "\uD83C\uDFE0", // Softbank: E036 - Unified: 1F3E0
            "\uD83C\uDFE1", // 1F3E1
            "\uD83C\uDFEB", // Softbank: E157 - Unified: 1F3EB
            "\uD83C\uDFE2", // Softbank: E038 - Unified: 1F3E2
            "\uD83C\uDFE3", // Softbank: E153 - Unified: 1F3E3
            "\uD83C\uDFE5", // Softbank: E155 - Unified: 1F3E5
            "\uD83C\uDFE6", // Softbank: E14D - Unified: 1F3E6
            "\uD83C\uDFEA", // Softbank: E156 - Unified: 1F3EA
            "\uD83C\uDFE9", // Softbank: E501 - Unified: 1F3E9
            "\uD83C\uDFE8", // Softbank: E158 - Unified: 1F3E8
            "\uD83D\uDC92", // Softbank: E43D - Unified: 1F492
            "\u26EA", // Softbank: E037 - Unified: 26EA
            "\uD83C\uDFEC", // Softbank: E504 - Unified: 1F3EC
            "\uD83C\uDFE4", // 1F3E4
            "\uD83C\uDF07", // Softbank: E44A - Unified: 1F307
            "\uD83C\uDF06", // Softbank: E146 - Unified: 1F306
            "\uD83C\uDFEF", // Softbank: E505 - Unified: 1F3EF
            "\uD83C\uDFF0", // Softbank: E506 - Unified: 1F3F0
            "\u26FA", // Softbank: E122 - Unified: 26FA
            "\uD83C\uDFED", // Softbank: E508 - Unified: 1F3ED
            "\uD83D\uDDFC", // Softbank: E509 - Unified: 1F5FC
            "\uD83D\uDDFE", // 1F5FE
            "\uD83D\uDDFB", // Softbank: E03B - Unified: 1F5FB
            "\uD83C\uDF04", // Softbank: E04D - Unified: 1F304
            "\uD83C\uDF05", // Softbank: E449 - Unified: 1F305
            "\uD83C\uDF03", // Softbank: E44B - Unified: 1F303
            "\uD83D\uDDFD", // Softbank: E51D - Unified: 1F5FD
            "\uD83C\uDF09", // 1F309
            "\uD83C\uDFA0", // 1F3A0
            "\uD83C\uDFA1", // Softbank: E124 - Unified: 1F3A1
            "\u26F2", // Softbank: E121 - Unified: 26F2
            "\uD83C\uDFA2", // Softbank: E433 - Unified: 1F3A2
            "\uD83D\uDEA2", // Softbank: E202 - Unified: 1F6A2
            "\uD83D\uDEA4", // Softbank: E135 - Unified: 1F6A4
            "\u26F5", // Softbank: E01C - Unified: 26F5
            "\uD83D\uDEA3", // 1F6A3
            "\u2693", // 2693
            "\uD83D\uDE80", // Softbank: E10D - Unified: 1F680
            "\u2708", // Softbank: E01D - Unified: 2708
            "\uD83D\uDCBA", // Softbank: E11F - Unified: 1F4BA
            "\uD83D\uDE81", // 1F681
            "\uD83D\uDE82", // 1F682
            "\uD83D\uDE8A", // 1F68A
            "\uD83D\uDE89", // Softbank: E039 - Unified: 1F689
            "\uD83D\uDE9E", // 1F69E
            "\uD83D\uDE86", // 1F686
            "\uD83D\uDE84", // Softbank: E435 - Unified: 1F684
            "\uD83D\uDE85", // Softbank: E01F - Unified: 1F685
            "\uD83D\uDE88", // 1F688
            "\uD83D\uDE87", // Softbank: E434 - Unified: 1F687
            "\uD83D\uDE9D", // 1F69D
            "\uD83D\uDE8C", // Softbank: E159 - Unified: 1F68C
            "\uD83D\uDE8B", // 1F68B
            "\uD83D\uDE83", // Softbank: E01E - Unified: 1F683
            "\uD83D\uDE8E", // 1F68E
            "\uD83D\uDE8D", // 1F68D
            "\uD83D\uDE99", // Softbank: E42E - Unified: 1F699
            "\uD83D\uDE98", // 1F698
            "\uD83D\uDE97", // Softbank: E01B - Unified: 1F697
            "\uD83D\uDE95", // Softbank: E15A - Unified: 1F695
            "\uD83D\uDE96", // 1F696
            "\uD83D\uDE9B", // 1F69B
            "\uD83D\uDE9A", // Softbank: E42F - Unified: 1F69A
            "\uD83D\uDEA8", // 1F6A8
            "\uD83D\uDE93", // Softbank: E432 - Unified: 1F693
            "\uD83D\uDE94", // 1F694
            "\uD83D\uDE92", // Softbank: E430 - Unified: 1F692
            "\uD83D\uDE91", // Softbank: E431 - Unified: 1F691
            "\uD83D\uDE90", // 1F690
            "\uD83D\uDEB2", // Softbank: E136 - Unified: 1F6B2
            "\uD83D\uDEA1", // 1F6A1
            "\uD83D\uDEA0", // 1F6A0
            "\uD83D\uDE9F", // 1F69F
            "\uD83D\uDE9C", // 1F69C
            "\uD83D\uDC88", // Softbank: E320 - Unified: 1F488
            "\uD83D\uDE8F", // Softbank: E150 - Unified: 1F68F
            "\uD83C\uDFAB", // Softbank: E125 - Unified: 1F3AB
            "\uD83D\uDEA6", // 1F6A6
            "\uD83D\uDEA5", // Softbank: E14E - Unified: 1F6A5
            "\u26A0", // Softbank: E252 - Unified: 26A0
            "\uD83D\uDEA7", // Softbank: E137 - Unified: 1F6A7
            "\uD83D\uDD30", // Softbank: E209 - Unified: 1F530
            "\u26FD", // Softbank: E03A - Unified: 26FD
            "\uD83C\uDFEE", // 1F3EE
            "\uD83C\uDFB0", // Softbank: E133 - Unified: 1F3B0
            "\u2668", // Softbank: E123 - Unified: 2668
            "\uD83D\uDDFF", // 1F5FF
            "\uD83C\uDFAA", // 1F3AA
            "\uD83C\uDFAD", // 1F3AD
            "\uD83D\uDCCD", // 1F4CD
            "\uD83D\uDEA9", // 1F6A9
            "\uD83C\uDDEF\uD83C\uDDF5", // Softbank: E50B - Unified: 1F1EF 1F1F5
            "\uD83C\uDDF0\uD83C\uDDF7", // Softbank: E514 - Unified: 1F1F0 1F1F7
            "\uD83C\uDDE9\uD83C\uDDEA", // Softbank: E50E - Unified: 1F1E9 1F1EA
            "\uD83C\uDDE8\uD83C\uDDF3", // Softbank: E513 - Unified: 1F1E8 1F1F3
            "\uD83C\uDDFA\uD83C\uDDF8", // Softbank: E50C - Unified: 1F1FA 1F1F8
            "\uD83C\uDDEB\uD83C\uDDF7", // Softbank: E50D - Unified: 1F1EB 1F1F7
            "\uD83C\uDDEA\uD83C\uDDF8", // Softbank: E511 - Unified: 1F1EA 1F1F8
            "\uD83C\uDDEE\uD83C\uDDF9", // Softbank: E50F - Unified: 1F1EE 1F1F9
            "\uD83C\uDDF7\uD83C\uDDFA", // Softbank: E512 - Unified: 1F1F7 1F1FA
            "\uD83C\uDDEC\uD83C\uDDE7", // Softbank: E510 - Unified: 1F1EC 1F1E7
            "\u0031\u20E3", // Softbank: E21C - Unified: 0031 20E3
            "\u0032\u20E3", // Softbank: E21D - Unified: 0032 20E3
            "\u0033\u20E3", // Softbank: E21E - Unified: 0033 20E3
            "\u0034\u20E3", // Softbank: E21F - Unified: 0034 20E3
            "\u0035\u20E3", // Softbank: E220 - Unified: 0035 20E3
            "\u0036\u20E3", // Softbank: E221 - Unified: 0036 20E3
            "\u0037\u20E3", // Softbank: E222 - Unified: 0037 20E3
            "\u0038\u20E3", // Softbank: E223 - Unified: 0038 20E3
            "\u0039\u20E3", // Softbank: E224 - Unified: 0039 20E3
            "\u0030\u20E3", // Softbank: E225 - Unified: 0030 20E3
            "\uD83D\uDD1F", // 1F51F
            "\u0023\u20E3", // Softbank: E210 - Unified: 0023 20E3
            "\uD83D\uDD20", // 1F520
            "\uD83D\uDD21", // 1F521
            "\uD83D\uDD22", // 1F522
            "\uD83D\uDD23", // 1F523
            "\uD83D\uDD24", // 1F524
            "\u2B06", // Softbank: E232 - Unified: 2B06
            "\u2B07", // Softbank: E233 - Unified: 2B07
            "\u27A1", // Softbank: E234 - Unified: 27A1
            "\u2B05", // Softbank: E235 - Unified: 2B05
            "\u2197", // Softbank: E236 - Unified: 2197
            "\u2196", // Softbank: E237 - Unified: 2196
            "\u2198", // Softbank: E238 - Unified: 2198
            "\u2199", // Softbank: E239 - Unified: 2199
            "\u2194", // 2194
            "\u2195", // 2195
            "\u25B6", // Softbank: E23A - Unified: 25B6
            "\u25C0", // Softbank: E23B - Unified: 25C0
            "\u23E9", // Softbank: E23C - Unified: 23E9
            "\u23EA", // Softbank: E23D - Unified: 23EA
            "\u2139", // 2139
            "\uD83D\uDD3C", // 1F53C
            "\uD83D\uDD3D", // 1F53D
            "\u23EB", // 23EB
            "\u23EC", // 23EC
            "\u21A9", // 21A9
            "\u21AA", // 21AA
            "\u2934", // 2934
            "\u2935", // 2935
            "\uD83C\uDD97", // Softbank: E24D - Unified: 1F197
            "\uD83D\uDD00", // 1F500
            "\uD83D\uDD01", // 1F501
            "\uD83D\uDD02", // 1F502
            "\uD83D\uDD04", // 1F504
            "\uD83C\uDD95", // Softbank: E212 - Unified: 1F195
            "\uD83C\uDD99", // Softbank: E213 - Unified: 1F199
            "\uD83C\uDD92", // Softbank: E214 - Unified: 1F192
            "\uD83C\uDD93", // 1F193
            "\uD83C\uDD96", // 1F196
            "\uD83D\uDCF6", // Softbank: E20B - Unified: 1F4F6
            "\uD83C\uDFA6", // Softbank: E507 - Unified: 1F3A6
            "\uD83C\uDE01", // Softbank: E203 - Unified: 1F201
            "\uD83C\uDE2F", // Softbank: E22C - Unified: 1F22F
            "\uD83C\uDE33", // Softbank: E22B - Unified: 1F233
            "\uD83C\uDE35", // Softbank: E22A - Unified: 1F235
            "\uD83C\uDE34", // 1F234
            "\uD83C\uDE32", // 1F232
            "\uD83C\uDE50", // Softbank: E226 - Unified: 1F250
            "\uD83C\uDE39", // Softbank: E227 - Unified: 1F239
            "\uD83C\uDE3A", // Softbank: E22D - Unified: 1F23A
            "\uD83C\uDE36", // Softbank: E215 - Unified: 1F236
            "\uD83C\uDE1A", // Softbank: E216 - Unified: 1F21A
            "\uD83D\uDEBB", // Softbank: E151 - Unified: 1F6BB
            "\uD83D\uDEB9", // Softbank: E138 - Unified: 1F6B9
            "\uD83D\uDEBA", // Softbank: E139 - Unified: 1F6BA
            "\uD83D\uDEBC", // Softbank: E13A - Unified: 1F6BC
            "\uD83D\uDEBE", // Softbank: E309 - Unified: 1F6BE
            "\uD83D\uDEB0", // 1F6B0
            "\uD83D\uDEAE", // 1F6AE
            "\uD83C\uDD7F", // Softbank: E14F - Unified: 1F17F
            "\u267F", // Softbank: E20A - Unified: 267F
            "\uD83D\uDEAD", // Softbank: E208 - Unified: 1F6AD
            "\uD83C\uDE37", // Softbank: E217 - Unified: 1F237
            "\uD83C\uDE38", // Softbank: E218 - Unified: 1F238
            "\uD83C\uDE02", // Softbank: E228 - Unified: 1F202
            "\u24C2", // 24C2
            "\uD83D\uDEC2", // 1F6C2
            "\uD83D\uDEC4", // 1F6C4
            "\uD83D\uDEC5", // 1F6C5
            "\uD83D\uDEC3", // 1F6C3
            "\uD83C\uDE51", // 1F251
            "\u3299", // Softbank: E315 - Unified: 3299
            "\u3297", // Softbank: E30D - Unified: 3297
            "\uD83C\uDD91", // 1F191
            "\uD83C\uDD98", // 1F198
            "\uD83C\uDD94", // Softbank: E229 - Unified: 1F194
            "\uD83D\uDEAB", // 1F6AB
            "\uD83D\uDD1E", // Softbank: E207 - Unified: 1F51E
            "\uD83D\uDCF5", // 1F4F5
            "\uD83D\uDEAF", // 1F6AF
            "\uD83D\uDEB1", // 1F6B1
            "\uD83D\uDEB3", // 1F6B3
            "\uD83D\uDEB7", // 1F6B7
            "\uD83D\uDEB8", // 1F6B8
            "\u26D4", // 26D4
            "\u2733", // Softbank: E206 - Unified: 2733
            "\u2747", // 2747
            "\u274E", // 274E
            "\u2705", // 2705
            "\u2734", // Softbank: E205 - Unified: 2734
            "\uD83D\uDC9F", // Softbank: E204 - Unified: 1F49F
            "\uD83C\uDD9A", // Softbank: E12E - Unified: 1F19A
            "\uD83D\uDCF3", // Softbank: E250 - Unified: 1F4F3
            "\uD83D\uDCF4", // Softbank: E251 - Unified: 1F4F4
            "\uD83C\uDD70", // Softbank: E532 - Unified: 1F170
            "\uD83C\uDD71", // Softbank: E533 - Unified: 1F171
            "\uD83C\uDD8E", // Softbank: E534 - Unified: 1F18E
            "\uD83C\uDD7E", // Softbank: E535 - Unified: 1F17E
            "\u27BF", // Softbank: E211 - Unified: 27BF
            "\uD83D\uDCA0", // 1F4A0
            "\u267B", // 267B
            "\u2648", // Softbank: E23F - Unified: 2648
            "\u2649", // Softbank: E240 - Unified: 2649
            "\u264A", // Softbank: E241 - Unified: 264A
            "\u264B", // Softbank: E242 - Unified: 264B
            "\u264C", // Softbank: E243 - Unified: 264C
            "\u264D", // Softbank: E244 - Unified: 264D
            "\u264E", // Softbank: E245 - Unified: 264E
            "\u264F", // Softbank: E246 - Unified: 264F
            "\u2650", // Softbank: E247 - Unified: 2650
            "\u2651", // Softbank: E248 - Unified: 2651
            "\u2652", // Softbank: E249 - Unified: 2652
            "\u2653", // Softbank: E24A - Unified: 2653
            "\u26CE", // Softbank: E24B - Unified: 26CE
            "\uD83D\uDD2F", // Softbank: E23E - Unified: 1F52F
            "\uD83C\uDFE7", // Softbank: E154 - Unified: 1F3E7
            "\uD83D\uDCB9", // Softbank: E14A - Unified: 1F4B9
            "\uD83D\uDCB2", // 1F4B2
            "\uD83D\uDCB1", // Softbank: E149 - Unified: 1F4B1
            "\u00A9", // Softbank: E24E - Unified: 00A9
            "\u00AE", // Softbank: E24F - Unified: 00AE
            "\u2122", // Softbank: E537 - Unified: 2122
            "\u303D", // Softbank: E12C - Unified: 303D
            "\u3030", // 3030
            "\uD83D\uDD1D", // Softbank: E24C - Unified: 1F51D
            "\uD83D\uDD1A", // 1F51A
            "\uD83D\uDD19", // 1F519
            "\uD83D\uDD1B", // 1F51B
            "\uD83D\uDD1C", // 1F51C
            "\u274C", // Softbank: E333 - Unified: 274C
            "\u2B55", // Softbank: E332 - Unified: 2B55
            "\u2757", // Softbank: E021 - Unified: 2757
            "\u2753", // Softbank: E020 - Unified: 2753
            "\u2755", // Softbank: E337 - Unified: 2755
            "\u2754", // Softbank: E336 - Unified: 2754
            "\uD83D\uDD03", // 1F503
            "\uD83D\uDD5B", // Softbank: E02F - Unified: 1F55B
            "\uD83D\uDD67", // 1F567
            "\uD83D\uDD50", // Softbank: E024 - Unified: 1F550
            "\uD83D\uDD5C", // 1F55C
            "\uD83D\uDD51", // Softbank: E025 - Unified: 1F551
            "\uD83D\uDD5D", // 1F55D
            "\uD83D\uDD52", // Softbank: E026 - Unified: 1F552
            "\uD83D\uDD5E", // 1F55E
            "\uD83D\uDD53", // Softbank: E027 - Unified: 1F553
            "\uD83D\uDD5F", // 1F55F
            "\uD83D\uDD54", // Softbank: E028 - Unified: 1F554
            "\uD83D\uDD60", // 1F560
            "\uD83D\uDD55", // Softbank: E029 - Unified: 1F555
            "\uD83D\uDD61", // 1F561
            "\uD83D\uDD56", // Softbank: E02A - Unified: 1F556
            "\uD83D\uDD62", // 1F562
            "\uD83D\uDD57", // Softbank: E02B - Unified: 1F557
            "\uD83D\uDD63", // 1F563
            "\uD83D\uDD58", // Softbank: E02C - Unified: 1F558
            "\uD83D\uDD64", // 1F564
            "\uD83D\uDD59", // Softbank: E02D - Unified: 1F559
            "\uD83D\uDD65", // 1F565
            "\uD83D\uDD5A", // Softbank: E02E - Unified: 1F55A
            "\uD83D\uDD66", // 1F566
            "\u2716", // 2716
            "\u2795", // 2795
            "\u2796", // 2796
            "\u2797", // 2797
            "\u2660", // Softbank: E20E - Unified: 2660
            "\u2665", // Softbank: E20C - Unified: 2665
            "\u2663", // Softbank: E20F - Unified: 2663
            "\u2666", // Softbank: E20D - Unified: 2666
            "\uD83D\uDCAE", // 1F4AE
            "\uD83D\uDCAF", // 1F4AF
            "\u2714", // 2714
            "\u2611", // 2611
            "\uD83D\uDD18", // 1F518
            "\uD83D\uDD17", // 1F517
            "\u27B0", // 27B0
            "\uD83D\uDD31", // Softbank: E031 - Unified: 1F531
            "\uD83D\uDD32", // Softbank: E21A - Unified: 1F532
            "\uD83D\uDD33", // Softbank: E21B - Unified: 1F533
            "\u25FE", // 25FE
            "\u25FD", // 25FD
            "\u2B1B", // 2B1B
            "\u2B1C", // 2B1C
            "\u25AA", // 25AA
            "\u25AB", // 25AB
            "\uD83D\uDD3A", // 1F53A
            "\u25FB", // 25FB
            "\u25FC", // 25FC
            "\uD83D\uDD34", // Softbank: E219 - Unified: 1F534
            "\u26AB", // 26AB
            "\u26AA", // 26AA
            "\uD83D\uDD35", // 1F535
            "\uD83D\uDD3B", // 1F53B
            "\uD83D\uDD36", // 1F536
            "\uD83D\uDD37", // 1F537
            "\uD83D\uDD38", // 1F538
            "\uD83D\uDD39" // 1F539
    };
    
    
    
    public static final int[] mImsmileyIcons = {
        R.drawable.emo_im_tongue_out,
        R.drawable.emo_im_tongue_out,
        R.drawable.emo_im_tongue_out,
        R.drawable.emo_im_tongue_out,
        R.drawable.emo_im_surprised,
        R.drawable.emo_im_surprised,
        R.drawable.emo_im_surprised,
        R.drawable.emo_im_surprised,
        R.drawable.emo_im_surprised,
        R.drawable.emo_im_surprised,
        R.drawable.emo_im_smile,
        R.drawable.emo_im_smile,
        R.drawable.emo_im_sad,
        R.drawable.emo_im_sad,
        R.drawable.emo_im_open_mouth,
        R.drawable.emo_im_open_mouth,
        R.drawable.emo_im_hot,
        R.drawable.emo_im_hot,
        R.drawable.emo_im_embarassed,
        R.drawable.emo_im_embarassed,
        R.drawable.emo_im_embarassed,
        R.drawable.emo_im_dont_tell,
        R.drawable.emo_im_dont_tell,
        R.drawable.emo_im_dont_tell,
        R.drawable.emo_im_dont_tell,
        R.drawable.emo_im_disappointed,
        R.drawable.emo_im_disappointed,
        R.drawable.emo_im_devil,
        R.drawable.emo_im_crying,
        R.drawable.emo_im_confused,
        R.drawable.emo_im_confused,
        R.drawable.emo_im_confused,
        R.drawable.emo_im_confused,
        R.drawable.emo_im_barring_teeth,
        R.drawable.emo_im_barring_teeth,
        R.drawable.emo_im_angry,
        R.drawable.emo_im_angry,
        R.drawable.emo_im_angel,
        R.drawable.emo_im_angel,
        R.drawable.emo_im_angel,
        R.drawable.emo_im_wink,
        R.drawable.emo_im_wink,
        R.drawable.emo_im_cool,
        R.drawable.emo_im_cool,
        R.drawable.emo_im_kissing,
        R.drawable.emo_im_yelling,
        R.drawable.emo_im_foot_in_mouth
    };
     public static final String[] mImsmileyText = {
            ":-P",":P",":p",":-p",
            ":-O",":o",":-o","=-o","=-0","=-O",
            ":-)",":)",
            ":-(",":(",
            ":-D",":d",
            "(H)","(h)",
            ":-$",":$",":-[",
            ":-#",":#",":-x",":-X",
            ":-|",":|",
            ">:)",
            ":\'(",
            ":-S",":s",":-\\","o-O",
            ":-E",":e",
            ":-@",":@",
            "O:-)","o:)","o:-)",
            ";-)",";)",
            "B-)","8-)",    
            ":-*",
            ":O",
            ":-!"
        };

    /**
     * Builds the hashtable we use for mapping the string version of a smiley
     * (e.g. ":-)") to a resource ID for the icon version.
     */
    private HashMap<String, Integer> buildSmileyToRes() {
        /*
         * if (allEmojiTexts.length != mSoftbankEmojiTexts.length) { // Throw an exception if someone updated
         * mEmojiTexts // and failed to update arrays.xml (Softbank) throw new
         * IllegalStateException("Smiley resource ID/text mismatch - " + mEmojiTexts.length + " != " +
         * mSoftbankEmojiTexts.length); }
         */

        // Initialize the resource ids
        DEFAULT_EMOJI_RES_IDS = new int[allEmojiTexts.length];
        for (int i = 0; i < allEmojiTexts.length; i++) {
            DEFAULT_EMOJI_RES_IDS[i] = Emojis.getSmileyResource(i);
        }

        HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>(allEmojiTexts.length);
        for (int i = 0; i < allEmojiTexts.length; i++) {
            smileyToRes.put(allEmojiTexts[i], DEFAULT_EMOJI_RES_IDS[i]);
        }

        for (int i = 0; i < mImsmileyText.length; i++) {
            smileyToRes.put(mImsmileyText[i], mImsmileyIcons[i]);
        }

        return smileyToRes;
    }

    /**
     * Builds the regular expression we use to find emojis in
     * {@link #addEmojiSpans}.
     */
    private Pattern buildPattern() {
        // Set the StringBuilder capacity with the assumption that one emoji is
        // 5 char (1 surrogate pair, 1 separator, 1 utf-8 char and 1 separator)
        int totalLength=(allEmojiTexts.length+mImsmileyText.length)*5;
        StringBuilder patternString = new StringBuilder(totalLength);
         

        // Build a regex that looks like (\uDAAA\uDBBB|\uCCCC|...), but escaping
        // the emojis
        // properly so they will be interpreted literally by the regex matcher.
        patternString.append('(');
        for (int i = 0; i < allEmojiTexts.length; i++) {
            patternString.append(Pattern.quote(allEmojiTexts[i]));
            patternString.append('|');
        }

        for (int i = 0; i < mImsmileyText.length; i++) {
            patternString.append(Pattern.quote(mImsmileyText[i]));
            patternString.append('|');
        }

        // Replace the extra '|' with a ')'
        patternString.replace(patternString.length() - 1, patternString.length(), ")");

        return Pattern.compile(patternString.toString());
    }

    /**
     * Adds ImageSpans to a CharSequence that replace unicode emojis with a
     * graphical version.
     * 
     * @param text A String possibly containing emojis (using string for UTF-16)
     * @return A CharSequence annotated with ImageSpans covering any recognized
     *         emojis
     */
    public CharSequence addEmojiSpans(CharSequence text , boolean isConversationScreen) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);

        // Scan for both Softbank private Unicode emoji code points (every
        // Android SMS app + iOS < 5) and Unicode 6.1 emoji BMP and non-BMP
        // codes
        int emoticonSize;
        
        if (isConversationScreen) {
            emoticonSize = mContext.getResources().getDimensionPixelSize(R.dimen.emoticonSize);
        } else {
            emoticonSize = mContext.getResources().getDimensionPixelSize(R.dimen.emoticonComposeSize);
        }
        Matcher matcher = mPattern.matcher(text);
        while (matcher.find()) {
            int resId = mSmileyToRes.get(matcher.group());
            Drawable d = mContext.getResources().getDrawable(resId);
            d.setBounds(0, 0, emoticonSize, emoticonSize);
            builder.setSpan(new ImageSpan(d), matcher.start(), matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return builder;
    }


}
