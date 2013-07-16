package com.verizon.mms.util;

import android.os.Build;

public class VzNavigatorUrlGenerator {

	public static String PACKAGE_NAME;

	public static final String deviceManufacturer = Build.MANUFACTURER;
	public static final String deviceModel = Build.MODEL;
	public static final int buildVersion = Build.VERSION.SDK_INT;

	public static final boolean isGingerbread = buildVersion >= 9;
	public static final boolean isIceCreamSandwich = buildVersion >= 14;

	public static final boolean isHTC = deviceManufacturer
			.equalsIgnoreCase("HTC");
	public static final boolean isSAMSUNG = deviceManufacturer
			.equalsIgnoreCase("SAMSUNG");
	public static final boolean isMOTOROLA = deviceManufacturer
			.equalsIgnoreCase("MOTOROLA");
	public static final boolean isLG = deviceManufacturer
			.equalsIgnoreCase("LG");
	public static final boolean isZTE = deviceManufacturer
			.equalsIgnoreCase("ZTE");

	public static final boolean isCasio = deviceManufacturer.toLowerCase()
			.contains("casio");
	public static final boolean isSony = deviceManufacturer.toLowerCase()
			.contains("sony");
	public static final boolean isPantech = deviceManufacturer.toLowerCase()
			.contains("pantech");

	public static final boolean isHTCIncredible = deviceModel
			.equalsIgnoreCase("ADR6300");
	public static final boolean isHTCIncredible2 = deviceModel
			.equalsIgnoreCase("ADR6350");
	public static final boolean isHTCEris = (deviceModel
			.equalsIgnoreCase("ADR6200"))
			|| (deviceModel.equalsIgnoreCase("Eris"));

	public static final boolean isHTCMocha = deviceModel
			.equalsIgnoreCase("ADR6400L");
	public static final boolean isHTCBliss = deviceModel
			.equalsIgnoreCase("ADR6330VW");
	public static final boolean isHTCVigor = deviceModel
			.equalsIgnoreCase("ADR6425LVW");

	public static final boolean isMOTOROLADroid = (deviceModel
			.equalsIgnoreCase("motoa855"))
			|| (deviceModel.equalsIgnoreCase("Droid"));

	public static final boolean isMOTOROLADroid2 = deviceModel
			.equalsIgnoreCase("DROID2");
	public static final boolean isMOTOROLADroidX = (deviceModel
			.equalsIgnoreCase("MB810"))
			|| (deviceModel.equalsIgnoreCase("DroidX"));

	public static final boolean isMOTOROLADroidGlobal = deviceModel
			.equalsIgnoreCase("DROID2 GLOBAL");
	public static final boolean isMOTOROLADroidPro = deviceModel
			.equalsIgnoreCase("DROID PRO");
	public static final boolean isMOTOROLADroidCitrus = deviceModel
			.equalsIgnoreCase("WX445");
	public static final boolean isMOTOROLADroid3 = deviceModel
			.equalsIgnoreCase("DROID3");
	public static final boolean isMOTOROLADroidX2 = deviceModel
			.equalsIgnoreCase("DROID X2");
	public static final boolean isMOTOROLADroidBionic = deviceModel
			.equalsIgnoreCase("DROID BIONIC");
	public static final boolean isMOTOROLAXoom = deviceModel
			.equalsIgnoreCase("XOOM");

	public static final boolean isSAMSUNGGem = deviceModel
			.equalsIgnoreCase("SCH-I110");
	public static final boolean isSAMSUNGContinuum = deviceModel
			.equalsIgnoreCase("SCH-I400");
	public static final boolean isSAMSUNGAegis = deviceModel
			.equalsIgnoreCase("SCH-I405");
	public static final boolean isSAMSUNGFascinate = deviceModel
			.equalsIgnoreCase("SCH-I500");
	public static final boolean isSAMSUNGCharge = deviceModel
			.equalsIgnoreCase("SCH-I510");
	public static final boolean isSAMSUNGGalaxytab = deviceModel
			.equalsIgnoreCase("SCH-I800");
	public static final boolean isSAMSUNGGalaxytab10Inch = deviceModel
			.equalsIgnoreCase("SCH-I905");

	public static final boolean isLGAlly = deviceModel.equalsIgnoreCase("Ally");
	public static final boolean isLGVortex = deviceModel
			.equalsIgnoreCase("Vortex");
	public static final boolean isLGRevolution = deviceModel
			.equalsIgnoreCase("VS910 4G");
	public static final boolean isLGRevolution2 = deviceModel
			.equalsIgnoreCase("VS920 4G");

	public static final boolean isCasioC771 = deviceModel
			.equalsIgnoreCase("C771");

	public static final boolean isSonyXperia = deviceModel
			.equalsIgnoreCase("R800x");

	public static final boolean isPantechApache = deviceModel
			.equalsIgnoreCase("ADR8995");

	public static final boolean isZTEV66 = deviceModel.equalsIgnoreCase("V66");
	public static final int MOTOROLADroidPro_STATUS_COMPLETE = 2;
	public static final boolean isNotSupportingSaveCapturedImgInGalleryFromNGM = (isMOTOROLAXoom)
			|| (isSAMSUNGGalaxytab10Inch) || (isZTEV66);

	public static final boolean isMOTOROLABlur = (isMOTOROLADroid)
			|| (isMOTOROLADroid2) || (isMOTOROLADroidX);

	public static final boolean deviceRequiresRawQueryForAllThreads = (isHTC)
			&& (!isHTCEris);
	public static final boolean deviceRequiresExtraBlurColumns = (isMOTOROLADroid2)
			|| (isMOTOROLADroidX);

	public static final boolean deviceIsTablet = (isSAMSUNGGalaxytab)
			|| (isMOTOROLAXoom) || (isZTEV66);

	public static final boolean isNavigationSupportedDevice = (!isHTCEris)
			&& (!isLGAlly);

	public static final boolean isFirstGenLTEDevice = (isHTCMocha)
			|| ((isLGRevolution) && (!isGingerbread));

	private static final String model = "com.vznavigator."
			+ addEnding(deviceModel);

	public static final String VZNAV_PACKAGE_URI = "market://details?id="
			+ model;

	private static String addEnding(String deviceModelInput) {
		String str = "";

		for (int i = 0; i < deviceModelInput.length(); i++) {
			if ((deviceModelInput.charAt(i) != '-')
					&& (deviceModelInput.charAt(i) != ' ')) {
				str = str + deviceModelInput.charAt(i);
			}
		}

		if (deviceModel.equalsIgnoreCase("Droid")) {
			str = str.toUpperCase();
		}

		return str;
	}
}