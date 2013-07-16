package com.verizon.mms;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.content.SharedPreferences;


import com.strumsoft.android.commons.logger.Logger;

public class DeviceConfig {
	private static Device deviceConfig = null;
	private static Device deviceConfigBackup = null;
    private static final long DEFAULT_MAX_NATIVE_HEAP = 30 * 1024 * 1024;

    public static void init(Context context, int resID) {
        loadDeviceConfigs(context, resID);
    }

    public static Device getDeviceByModel(Context context, String model, boolean isTabletFromResource) {
    	Device dev = deviceConfig;   //deviceConfigs.get(model);
    	if (dev != null) {
    		if (dev.isTablet != isTabletFromResource) {
    			if (Logger.IS_ERROR_ENABLED) {
    				Logger.error(false, DeviceConfig.class, "Config problem: " + dev.model + "isTablet:" + dev.isTablet + " res: " + isTabletFromResource);
    			}
    			// if we found device in xml then trust that
    			isTabletFromResource = dev.isTablet;
    		}
    	}
    	boolean xlarge = ((context.getResources().getConfiguration().screenLayout 
							& Configuration.SCREENLAYOUT_SIZE_MASK) == 4);
    	boolean large = ((context.getResources().getConfiguration().screenLayout 
    						& Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
    	OEM.isSmallScreen = !(xlarge || large);;
    	
    	// did not find device - should not happen
		String vzDevice = "0";
    	if (dev == null) {
    		// NBI is not supported on these non-supported VZ devices.
    		OEM.isNbiLocationDisabled = true;
    		//assert(false);
    		String tab = "1";
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			String tabFromPref = prefs.getString("tab", "2");
			if (!("2".equals(tabFromPref))) {
				if (Logger.IS_ACRA_ENABLED) {
					Logger.debug(DeviceConfig.class, "found deviceType preferences: " + tabFromPref);
				}
				tab = tabFromPref;
			} else {	
	    		if (!isTabletFromResource) {
	    			tab = "0";
	    			if (OEM.deviceModel.equals("KFTT") || OEM.deviceModel.equals("TC970 (Wi-Fi)") || OEM.deviceModel.equals("MZ604") 
	    					|| OEM.deviceModel.equals("Nook Tablet") || OEM.deviceModel.equals("GT-P7510") 
	    					|| OEM.deviceModel.equals("YP-G1") || OEM.deviceModel.equals("YP-G70") 
	    					|| OEM.deviceModel.equals("YP-GS1") || OEM.deviceModel.equals("YP-G70")
	    					|| OEM.deviceModel.equals("Nexus 7") || OEM.deviceModel.equals("Nexus 10")
	    					|| OEM.deviceModel.equals("GT-P3113") || OEM.deviceModel.equals("GT-P5113")) {
	    				if (Logger.IS_ACRA_ENABLED) {
	    					Logger.debug(DeviceConfig.class, "one of hardcoded tablet devices");
	    				}
	    				isTabletFromResource = true;
	    				tab = "1";
	    			}
	    		}
//				//TODO: Improve the logic of deciding on what is a tablet and what is a handset
//	    		if (!isTabletFromResource) {
//	    			String brand = android.os.Build.BRAND;
//	    			if (brand.toLowerCase().contains("verizon")) {
//	        			tab = "0";    				
//	            		vzDevice = "1";
//	    			} else {
//	    				TelephonyManager mTeleManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//	    				String opName = mTeleManager.getNetworkOperatorName();
//	    				if (opName.toLowerCase().contains("verizon")) {
//	            			tab = "0";    				    					
//	                		vzDevice = "2";
//	    				}
//	    			}
//	    		}				
	    		prefs.edit().putString("tab", tab).commit();
	    		Logger.postErrorToAcra(DeviceConfig.class, "Device not found. Adding: " + OEM.deviceManufacturer + "-" + OEM.deviceModel 
    					+ "-" + MmsConfig.DEFAULT_USER_AGENT + "-tab:" + tab + "-verizon:"+ vzDevice + "-smallS:" + OEM.isSmallScreen);
			}
    		dev = new Device(OEM.deviceManufacturer, OEM.deviceModel, MmsConfig.DEFAULT_USER_AGENT, tab, DEFAULT_MAX_NATIVE_HEAP, false);
    		deviceConfig = dev;
    	}
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(DeviceConfig.class, "Device Config: isTablet: " + dev.isTablet + " isSmallScreen:" + OEM.isSmallScreen);
		}
        
        return dev;
    }

    public static final void beginDocument(XmlPullParser parser, String firstElementName)
            throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
            ;
        }

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName()
                    + ", expected " + firstElementName);
        }
    }

    public static final void nextElement(XmlPullParser parser) throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
            ;
        }
    }

    private static void loadDeviceConfigs(Context context, int resID) {
        XmlResourceParser parser = context.getResources().getXml(resID);

        try {
            beginDocument(parser, "device_config");

            deviceConfig = null;
            while (true) {
                nextElement(parser);
                String tag = parser.getName();
                if (tag == null) {
                    break;
                }

                if (!"device".equalsIgnoreCase(tag)) {
                    break;
                }

                String attModel = parser.getAttributeName(1);
                String attUA = parser.getAttributeName(2);

                String valManufacturer = parser.getAttributeValue(0);
                String valModel = parser.getAttributeValue(1);
                String valUA = parser.getAttributeValue(2);
                String valDeviceType = parser.getAttributeValue(null, "deviceType");
                Boolean isLedSupported = parser.getAttributeBooleanValue(null, "ledSupported", false);

                // maxNativeHeap is only required for pre-Honeycomb devices
                long heap = DEFAULT_MAX_NATIVE_HEAP;
                String valHeap = parser.getAttributeValue(null, "maxNativeHeap");
                if (valHeap != null) {
	                try {
	                	heap = Long.parseLong(valHeap);
	                }
	                catch (Exception e) {
	                	Logger.error("DeviceConfig.loadDeviceConfigs: invalid maxNativeHeap value <" + valHeap + ">");
	                }
                }

                
                if ("model".equalsIgnoreCase(attModel) && "ua".equalsIgnoreCase(attUA)) {
                	if (Logger.IS_DEBUG_ENABLED) {
                		Logger.debug(DeviceConfig.class, "DeviceModel: " + OEM.deviceModel + " xmlModel: " + valModel);
                	}
                	String devModel = OEM.deviceModel.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
                	String xmlModel = valModel.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
                	if (devModel.equalsIgnoreCase(xmlModel)) {
                		deviceConfig = new Device(valManufacturer, valModel, valUA, valDeviceType, heap, isLedSupported);
                		break;                		
                	} else if (devModel.startsWith(xmlModel)) {
                		deviceConfigBackup = new Device(valManufacturer, valModel, valUA, valDeviceType, heap, isLedSupported);
                	}
                }
            }
        } catch (Exception e) {
            Logger.postErrorToAcra(DeviceConfig.class, e);
        } finally {
            parser.close();
            if ((deviceConfig == null) && (deviceConfigBackup != null)) {
            	deviceConfig = deviceConfigBackup; 
            }
        }
    }

    public static class Device {
        /**
         * The UA String
         */
        private final String mUserAgent;
        private final String model;
        private final String manufacturer;
        private boolean isTablet;
        private final long maxNativeHeap;
        private boolean isLedSupported;

        public Device(String manufacturer, String model, String mUserAgent, String deviceType, long maxNativeHeap, Boolean isLedSupported) {
            this.manufacturer = manufacturer;
            this.model = model;
            this.mUserAgent = mUserAgent;
            if ("1".equalsIgnoreCase(deviceType)){
                isTablet = true;
            }else{
                isTablet = false;
            }
            this.maxNativeHeap = maxNativeHeap;
            this.isLedSupported = isLedSupported;
            if (OEM.isSamsungS3PriorToJB) {
            	this.isLedSupported = false;
            	if (Logger.IS_DEBUG_ENABLED) {
            		Logger.debug(getClass(), "S3 but not at JB so disabling LED");
            	}
            }

        }

        public String getModel() {
            return model;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public String getmUserAgent() {
            return mUserAgent;
        }

        /**
         * Returns the Value of the isTablet
         * @return the  {@link boolean}
         */
        public boolean isTablet() {
            return isTablet;
        }

        public boolean isNotificationLedSupported() {
            return isLedSupported;
        }

        public long getMaxNativeHeap() {
        	return maxNativeHeap;
        }

        @Override
        public String toString() {
            return "Device [manufacturer=" + manufacturer + ", model=" + model + ", mUserAgent=" + mUserAgent
                    + ", maxNativeHeap=" + maxNativeHeap + "]";
        }

    }

    public static class OEM {
        public static final String  deviceManufacturer       = Build.MANUFACTURER;
        public static final String  deviceModel              = Build.MODEL;
        public static final int     buildVersion             = Build.VERSION.SDK_INT;

        public static final boolean isGingerbread            = buildVersion >= 9;
        public static final boolean isHoneycomb              = buildVersion >= 11;
        public static final boolean isIceCreamSandwich       = buildVersion >= 14;
        public static final boolean isJellyBean				 = buildVersion >= 16;

        public static final boolean isHTC                    = matches(deviceManufacturer, "HTC");
        public static final boolean isSAMSUNG                = matches(deviceManufacturer, "SAMSUNG");
        public static final boolean isMOTOROLA               = matches(deviceManufacturer, "MOTOROLA");
        public static final boolean isLG                     = matches(deviceManufacturer, "LG") || matches(deviceManufacturer, "LGE");
        public static final boolean isZTE                    = matches(deviceManufacturer, "ZTE");

        public static final boolean isHTCIncredible = deviceModel.equalsIgnoreCase("ADR6300");
        public static final boolean isHTCIncredible2 = deviceModel.equalsIgnoreCase("ADR6350");
        public static final boolean isHTCEris = deviceModel.equalsIgnoreCase("ADR6200") || deviceModel.equalsIgnoreCase("Eris");
        public static final boolean isHTCMocha = deviceModel.equalsIgnoreCase("ADR6400L");
        public static final boolean isHTCBliss = deviceModel.equalsIgnoreCase("ADR6330VW");
        public static final boolean isHTCVigor = deviceModel.equalsIgnoreCase("ADR6425LVW");
        public static final boolean isMOTOROLADroid = deviceModel.equalsIgnoreCase("motoa855") || deviceModel.equalsIgnoreCase("Droid");
        public static final boolean isMOTOROLADroid2 = deviceModel.equalsIgnoreCase("DROID2");
        public static final boolean isMOTOROLADroidX = deviceModel.equalsIgnoreCase("MB810") || deviceModel.equalsIgnoreCase("DroidX");
        public static final boolean isMOTOROLADroidGlobal = deviceModel.equalsIgnoreCase("DROID2 GLOBAL");
        public static final boolean isMOTOROLADroidPro = deviceModel.equalsIgnoreCase("DROID PRO");
        public static final boolean isMOTOROLADroidCitrus = deviceModel.equalsIgnoreCase("WX445");
        public static final boolean isMOTOROLADroid3 = deviceModel.equalsIgnoreCase("DROID3");
        public static final boolean isMOTOROLADroidX2 = deviceModel.equalsIgnoreCase("DROID X2");
        public static final boolean isMOTOROLADroidBionic = deviceModel.equalsIgnoreCase("DROID BIONIC");
        public static final boolean isMOTOROLAXoom = deviceModel.equalsIgnoreCase("XOOM");
        public static final boolean isMOTOROLAMZ617 = deviceModel.equalsIgnoreCase("MZ617");
        public static final boolean isMOTOROLARazr = deviceModel.equalsIgnoreCase("DROID RAZR");
        public static final boolean isSAMSUNGGem = deviceModel.equalsIgnoreCase("SCH-I110");
        public static final boolean isSAMSUNGContinuum = deviceModel.equalsIgnoreCase("SCH-I400");
        public static final boolean isSAMSUNGAegis = deviceModel.equalsIgnoreCase("SCH-I405");
        public static final boolean isSAMSUNGFascinate = deviceModel.equalsIgnoreCase("SCH-I500");
        public static final boolean isSAMSUNGCharge = deviceModel.equalsIgnoreCase("SCH-I510");
        public static final boolean isSAMSUNGGalaxytab = deviceModel.equalsIgnoreCase("SCH-I800") || deviceModel.equalsIgnoreCase("GT-P3100");
        public static final boolean isSAMSUNGGalaxytab10Inch = deviceModel.equalsIgnoreCase("SCH-I905") || deviceModel.equalsIgnoreCase("GT-P7500");
        public static final boolean isLGAlly = deviceModel.equalsIgnoreCase("Ally");
        public static final boolean isLGVortex = deviceModel.equalsIgnoreCase("Vortex");
        public static final boolean isLGRevolution = deviceModel.equalsIgnoreCase("VS910 4G");
        public static final boolean isLGRevolution2 = deviceModel.equalsIgnoreCase("VS920 4G");
        public static final boolean isCasioC771 = deviceModel.equalsIgnoreCase("C771");
        public static final boolean isSonyXperia = deviceModel.equalsIgnoreCase("R800x");
        public static final boolean isPantechApache = deviceModel.equalsIgnoreCase("ADR8995");
        public static final boolean isPantech910L = deviceModel.equalsIgnoreCase("ADR910L");
        public static final boolean isZTEV66 = deviceModel.equalsIgnoreCase("V66");
        public static final boolean isNotSupportingSaveCapturedImgInGalleryFromNGM = isMOTOROLAXoom || isSAMSUNGGalaxytab10Inch || isZTEV66;
        public static final boolean isMOTOROLABlur = isMOTOROLADroid || isMOTOROLADroid2 || isMOTOROLADroidX;
        public static final boolean deviceRequiresRawQueryForAllThreads = isHTC && !isHTCEris;
        public static final boolean deviceRequiresExtraBlurColumns = isMOTOROLADroid2 || isMOTOROLADroidX;
        public static final boolean isNexusTab = deviceModel.equalsIgnoreCase("Nexus 7");
        public static boolean isNbiLocationDisabled = isNexusTab;
        public static final boolean isSamsungS3PriorToJB = deviceModel.equalsIgnoreCase("SCH-I535") && !isJellyBean;
        public static final boolean isSamsungGalaxyCamera = deviceModel.equalsIgnoreCase("EK-GC120");
        public static final boolean isHTCDNA = deviceModel.equalsIgnoreCase("HTC6435LVW");
        public static final boolean isHTCInc3 = deviceModel.equalsIgnoreCase("ADR6410LVW") ||
											deviceModel.equalsIgnoreCase("ADR6410");
        public static final boolean isHTCRezound = deviceModel.equalsIgnoreCase("ADR6425LVW") ||
        									deviceModel.equalsIgnoreCase("ADR6425VW");
        
        public static boolean isSmallScreen =  false;
        
        @Deprecated
        public static final boolean deviceIsTablet = isSAMSUNGGalaxytab || isMOTOROLAXoom || isZTEV66;
        public static final boolean isNavigationSupportedDevice = (!isHTCEris) && (!isLGAlly);
        public static final boolean isFirstGenLTEDevice = (isHTCMocha) || ((isLGRevolution) && (!isGingerbread));

        public static final boolean isNotSupportingSaveCapturedImg  = isMOTOROLAXoom || 
                isSAMSUNGGalaxytab10Inch || 
                isZTEV66;
    }

    public static boolean matches(String one, String two) {
        return (null == one || null == two) ? false : one.equalsIgnoreCase(two);
    }

}
