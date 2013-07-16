package com.verizon.mms.ui;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.preference.PreferenceManager;

public class ConversationResHelper
{	
	public static final int RIGHT_BUBBL_DEF_COLOR = 0xFF00BBFF;
	public static final int[] LEFTT_BUBBL_DEF_COLORS = {0xFFFF0000, 0xFF39B54A, 
		0xFFDF5E97, 0xFF9972E5, 0xFF8CE0FF, 0xFFFF8A23, 0xFF89E595, 0xFFFFB2D4, 0xFFD8B2FF, 0xFF4784ED, 0xFFFFD000, 0xFF87B728, 
		0xFF99684F, 0xFF000000, 0xFF99BEFF, 0xFFFFF600, 0xFFBAE57E, 0xFFCCAA99, 0xFF6D6E71, 0xFFBCBEC0, };
	public static final int BACKGROUND_DEF_COLOR = 0xffdadada;
	
	public static final int TIME_STAMP_LIGHT_R = 0xD0;
	public static final int TIME_STAMP_DARK_R = 0x51;
	
	public static final int TEXT_LIGHT_R = 0xFF;
	public static final int TEXT_DARK_R = 0;
	
	private static int mRightBubbleColor;
	private static int mLeftBubbleColors[];
	private static int mBgColor;
	private static boolean mFillBubble = false;

	public static final String CONV_LEFT_BUBBLE_COLORS = "leftColors";
	public static final String CONV_BG_COLOR = "bgcolor";
	public static final String CONV_RIGHT_BUBBLE_COLOR = "rightColor";
	public static final String CONV_FILL_BUBBLE_COLOR = "fillbubble";
	
	public static final  int[] COLORS = new int[] {
           0xFFFF0000, 0xFFFF0700, 0xFFFF0E00, 0xFFFF1400, 0xFFFF1B00, 0xFFFF2200, 0xFFFF2900, 0xFFFF3000, 0xFFFF3600, 0xFFFF3D00,
           0xFFFF4400, 0xFFFF4B00, 0xFFFF5200, 0xFFFF5800, 0xFFFF5F00, 0xFFFF6600, 0xFFFF6D00, 0xFFFF7400, 0xFFFF7A00, 0xFFFF8100, 
           0xFFFF8800, 0xFFFE8E00, 0xFFFE9300, 0xFFFD9900, 0xFFFD9E00, 0xFFFCA400, 0xFFFBAA00, 0xFFFBAF00, 0xFFFAB500, 0xFFFABA00, 
           0xFFF9C000, 0xFFF8C600, 0xFFF8CB00, 0xFFF7D100, 0xFFF7D600, 0xFFF6DC00, 0xFFF5E200, 0xFFF5E700, 0xFFF4ED00, 0xFFF4F200, 
           0xFFF3F800, 0xFFE7F606, 0xFFDBF30C, 0xFFCFF112, 0xFFC2EF19, 0xFFB6EC1F, 0xFFAAEA25, 0xFF9EE82B, 0xFF92E531, 0xFF86E337, 
           0xFF7AE03E, 0xFF6DDE44, 0xFF61DC4A, 0xFF55D950, 0xFF49D756, 0xFF3DD55C, 0xFF31D262, 0xFF24D069, 0xFF18CE6F, 0xFF0CCB75, 
           0xFF00C97B, 0xFF00C282, 0xFF01BC88, 0xFF01B58F, 0xFF01AF95, 0xFF02A89C, 0xFF02A1A3, 0xFF029BA9, 0xFF0294B0, 0xFF038EB6, 
           0xFF0387BD, 0xFF0380C4, 0xFF047ACA, 0xFF0473D1, 0xFF046DD7, 0xFF0466DE, 0xFF055FE5, 0xFF0559EB, 0xFF0552F2, 0xFF064CF8, 
           0xFF0645FF, 0xFF0E42FF, 0xFF163EFF, 0xFF1F3BFF, 0xFF2737FF, 0xFF2F34FF, 0xFF3830FF, 0xFF402DFF, 0xFF4829FF, 0xFF5026FF, 
           0xFF5822FE, 0xFF611FFE, 0xFF691CFE, 0xFF7118FE, 0xFF7915FE, 0xFF8211FE, 0xFF8A0EFE, 0xFF920AFE, 0xFF9A07FE, 0xFFA303FE, 
           0xFFAB00FE, 0xFFAC06FA, 0xFFAD0CF7, 0xFFAE12F4, 0xFFAF18F0, 0xFFB01EEC, 0xFFB024E9, 0xFFB12AE6, 0xFFB230E2, 0xFFB336DE, 
           0xFFB43CDB, 0xFFB541D8, 0xFFB647D4, 0xFFB74DD0, 0xFFB853CD, 0xFFB859CA, 0xFFB95FC6, 0xFFBA65C2, 0xFFBB6BBF, 0xFFBC71BC, 
           0xFFBD77B8, 0xFFC07EBC, 0xFFC485BF, 0xFFC78BC3, 0xFFCA92C6, 0xFFCE99CA, 0xFFD1A0CD, 0xFFD4A7D1, 0xFFD7ADD4, 0xFFDBB4D8,
           0xFFDEBBDC, 0xFFE1C2DF, 0xFFE5C9E3, 0xFFE8CFE6, 0xFFEBD6EA, 0xFFEEDDED, 0xFFF2E4F1, 0xFFF5EBF4, 0xFFF8F1F8, 0xFFFCF8FB,
           0xFFFFFFFF, 0xFFF9FAF9, 0xFFF4F4F4, 0xFFEEEEEE, 0xFFE9E9E9, 0xFFE3E4E3, 0xFFDEDEDE, 0xFFDADADA, 0xFFD3D3D3, 0xFFCDCECD, 
           0xFFC8C8C8, 0xFFC2C2C2, 0xFFBCBDBC, 0xFFB7B8B7, 0xFFB1B2B1, 0xFFACACAC, 0xFFA6A7A6, 0xFFA1A2A1, 0xFF9B9C9B, 0xFF969696, 
           0xFF909190, 0xFF8C8D8C, 0xFF878887, 0xFF838483, 0xFF7F807F, 0xFF7A7B7A, 0xFF767776, 0xFF727372, 0xFF6E6E6E, 0xFF696A69, 
           0xFF656665, 0xFF616161, 0xFF5C5D5C, 0xFF585858, 0xFF545454, 0xFF505050, 0xFF4B4B4B, 0xFF474747, 0xFF434343, 0xFF3E3E3E, 
           0xFF3A3A3A, 0xFF373737, 0xFF343434, 0xFF313131, 0xFF2E2E2E, 0xFF2C2C2C, 0xFF292929, 0xFF262626, 0xFF232323, 0xFF202020,
           0xFF1D1D1D, 0xFF1A1A1A, 0xFF171717, 0xFF141414, 0xFF111111, 0xFF0E0E0E, 0xFF0C0C0C, 0xFF090909, 0xFF060606, 0xFF030303,
           0xFF000000
	       };
	public static void init(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		mRightBubbleColor = prefs.getInt(CONV_RIGHT_BUBBLE_COLOR, RIGHT_BUBBL_DEF_COLOR);
		mBgColor = prefs.getInt(CONV_BG_COLOR, BACKGROUND_DEF_COLOR);
		mFillBubble = prefs.getBoolean(CONV_FILL_BUBBLE_COLOR, false);
		String leftColor = prefs.getString(CONV_LEFT_BUBBLE_COLORS, null);
		
		if (leftColor != null) {
			mLeftBubbleColors = convertStringtoIntArray(leftColor);
		} else {
			mLeftBubbleColors = LEFTT_BUBBL_DEF_COLORS;
		}
	}
	
	public static void refresh(Context context)
	{
		init(context);
	}
	
	public static int getLeftBubbleColor(int index)
	{
		int resId = 0;
		
		if (mLeftBubbleColors != null)
		{
			resId = mLeftBubbleColors[index];
		}
		return resId;
	}
	
	public static int getLeftBubbleColorsSize()
	{
		return mLeftBubbleColors.length;
	}
	
	public static int getRightBubbleColor() {
		return mRightBubbleColor;
	}
	
	public static int getBGColor() {
		return mBgColor;
	}
	
	public static int[] convertStringtoIntArray(String str){
		if (str != null) {
			String[] sarray = str.split(",");
			int intarray[] = new int[sarray.length];
			for (int i = 0; i < sarray.length; i++) {
			intarray[i] = Integer.parseInt(sarray[i]);
			}

			return intarray;
		}
		return null;
	}
	
	/*
	 * returns right and left bubble colors as an arraylist
	 * it will add the right bubble color in the 0 positon
	 */
	public static ArrayList<Integer> getAllBubbles() {
		ArrayList<Integer> bubbles = new ArrayList<Integer>();
		
		bubbles.add(mRightBubbleColor);
		
		for (int color : mLeftBubbleColors) {
			bubbles.add(color);
		}
		
		return bubbles;
	}

	public static boolean fillBubble() {
		return mFillBubble;
	}
	
	private ConversationResHelper()
	{
		
	}
	
	public static void resetToDefault(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor edit = prefs.edit();
		edit.remove(CONV_LEFT_BUBBLE_COLORS);
		edit.remove(CONV_BG_COLOR);
		edit.remove(CONV_FILL_BUBBLE_COLOR);
		edit.remove(CONV_RIGHT_BUBBLE_COLOR);
		
		edit.commit();
		
		init(context);
	}
	
	private static int getTextColor(int bgColor, int lightColor, int darkColor) {
		int d = 0;
		
        if (isBrightColor(bgColor)) {
            d = darkColor; // bright colors - black font
        } else {
            d = lightColor; // dark colors - white font
        }

        return  Color.rgb(d, d, d);
	}

	public static boolean isBrightColor(int color) {
		int r = Color.red(color);
		int g = Color.green(color);
		int b = Color.blue(color);
        // Counting the perceptive luminance - human eye favors green color... 
        double a = 1 - ( 0.299 * r + 0.587 * g + 0.114 * b)/255;
        
        return a < 0.5;
	}
	
	public static int getBubbleTextColor(int bubbleColor) {
		// TODO Auto-generated method stub
		return getTextColor(bubbleColor, TEXT_LIGHT_R, TEXT_DARK_R);
	}

	public static int getTimeStampColor(int bubbleColor) {
		// TODO Auto-generated method stub
		return getTextColor(bubbleColor, TIME_STAMP_LIGHT_R, TIME_STAMP_DARK_R);
	}
	
    public static int getMsgHightlightColor() {
        boolean found = false;
        for (int i = 162; i < COLORS.length; i++) {
            if (getBGColor() == COLORS[i]) {
                found = true;
                break;
            }
        }
        return found ? 0xFFBCBEC0 : 0xFF848484;
    }
}
