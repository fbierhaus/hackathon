package com.verizon.mms.util;

import java.io.IOException;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.Settings.Secure;
import android.widget.Toast;

import com.nbi.location.Location;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;


public class LocationUtils {
	


	/**
	 * Checks the settings of the phone to make sure the location preferences
	 * have been set for using location services.
	 * 
	 * @param context
	 * @return true if out required location preferences are enabled
	 */
	public static boolean isAllLocationProviderEnabled(Context context) {
		ContentResolver res = context.getContentResolver();
		String loc = Secure.getString(res, Secure.LOCATION_PROVIDERS_ALLOWED);
		
		String[] locationOptionsMatrix = { LocationManager.GPS_PROVIDER,
		// LocationManager.NETWORK_PROVIDER,
		};

		if (loc != null) {
			for (String option : locationOptionsMatrix) {
				if (!loc.contains(option))
					return false;
			}

			return true;
		}

		return false;
	}

	/**
	 * Returns true if the GEO coded coordinates are valid.
	 * 
	 * @param latAndLon
	 *            Float array of Latitude at element [0] and Longitude at
	 *            element [1]
	 * @return True if the coordinates are valid
	 */
	private static boolean isValidGeoCoordinates(float[] latAndLon) {
		return !Util.compareFloat(latAndLon[0], 0f)
				&& !Util.compareFloat(latAndLon[1], 0f);
	}

	/**
	 * Retrieves the GPS coordinates from the content provider or the
	 * coordinates embedded within the image file itself.
	 * 
	 * @param mediaUri
	 *            The URI of the media to analyze for GEO coordinates.
	 * @param appContext
	 *            The context is used for showing toasts and getting string
	 *            resources.
	 * @return The GPS coordinates of the media item or null, on failure to find
	 *         coordinates.
	 */
	public static float[] retrieveGeocoordinates(Uri mediaUri, Context appContext) {
		String uriScheme = mediaUri.getScheme();
		float[] latAndLon = new float[2];

		if (null == uriScheme) {
			Toast.makeText(appContext,
					appContext.getString(R.string.location_image_not_found),
					Toast.LENGTH_LONG).show();

			return null;
		}

		if (uriScheme.equals("file")) {
			findFileGeoCoordinates(mediaUri, appContext, latAndLon);
		} else if (uriScheme.equals("content")) {
			findMediaContentGeoCoordinates(mediaUri, appContext, latAndLon);
		}

		if (isValidGeoCoordinates(latAndLon)) {
			return latAndLon;
		}

		return null;
	}

	/**
	 * Finds the GEO coded coordinates in a file and puts them into latAndLon
	 * 
	 * @param mediaUri
	 *            - The URI where the media file is located
	 * @param appContext
	 *            - The application context for toasting strings
	 * @param latAndLon
	 *            - Stores he latitude at [0] and longitude at [1]
	 */
	private static void findFileGeoCoordinates(Uri mediaUri, Context appContext, float[] latAndLon) {
		String imageFilePath = mediaUri.getPath();

		if (null != imageFilePath) {
			try {
				ExifInterface imageEXIFData = new ExifInterface(imageFilePath);
				boolean latLonFound = imageEXIFData.getLatLong(latAndLon);

				if (!latLonFound || !isValidGeoCoordinates(latAndLon)) {
					Toast.makeText(
							appContext,
							appContext
									.getString(R.string.location_image_not_found),
							Toast.LENGTH_LONG).show();
				}
			} catch (IOException ioe) {
				Logger.error(ioe);
				Toast.makeText(
						appContext,
						appContext
								.getString(R.string.invalid_filetype_location),
						Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(appContext,
					appContext.getString(R.string.location_image_not_found),
					Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Finds the GEO coded coordinates in a file and puts them into latAndLon
	 * 
	 * @param mediaUri
	 *            - The URI where the media file is located
	 * @param appContext
	 *            - The application context for toasting strings
	 * @param latAndLon
	 *            - Stores he latitude at [0] and longitude at [1]
	 */
	private static void findMediaContentGeoCoordinates(Uri mediaUri, Context appContext, float[] latAndLon) {
		Cursor imgCur = appContext.getContentResolver().query(mediaUri, null,
				null, null, null);

		if (imgCur != null) {
			try {
				if ((imgCur.getCount() != 1) || !imgCur.moveToFirst()) {
					Toast.makeText(
							appContext,
							appContext
									.getString(R.string.media_content_cursor_bad_operation),
							Toast.LENGTH_SHORT).show();
				} else {
					int latIndex = imgCur.getColumnIndex(Images.Media.LATITUDE);
					int lonIndex = imgCur
							.getColumnIndex(Images.Media.LONGITUDE);
					int dataIndex = imgCur.getColumnIndex(Images.Media.DATA);

					if (-1 != latIndex && -1 != lonIndex && -1 != dataIndex) {
						latAndLon[0] = imgCur.getFloat(latIndex);
						latAndLon[1] = imgCur.getFloat(lonIndex);

						if (!isValidGeoCoordinates(latAndLon)) {
							Uri fileMediaUri = Uri.parse("file:"
									+ imgCur.getString(dataIndex));
							findFileGeoCoordinates(fileMediaUri, appContext,
									latAndLon);
						}
					} else {
						Toast.makeText(
								appContext,
								appContext
										.getString(R.string.media_content_cursor_bad_operation),
								Toast.LENGTH_LONG).show();
					}
				}
			} catch (SQLException e) {
				Logger.error(e);
			} finally {
				imgCur.close();
				imgCur = null;
			}
		} else {
			Toast.makeText(
					appContext,
					appContext
							.getString(R.string.media_content_cursor_bad_operation),
					Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Starts the location map with the media attached.
	 * 
	 * @param fragment
	 *            The fragment which will receive the activity result
	 * @param mediaUri
	 *            The URI for the media item
	 * @param requestCode
	 *            the request code to identify the correct code path in
	 *            onActivityResult()
	 * @param latAndLon
	 *            The latitude and longitude of the media item.
	 */
	// private static void startMediaLocationActivity(Fragment fragment, Uri
	// mediaUri, int requestCode, float [] latAndLon)
	// {
	// Context context = fragment.getActivity().getBaseContext();
	//
	// if(!floatComp(latAndLon[0], 0f) && !floatComp(latAndLon[1], 0f))
	// {
	// PlaceBuilder placeBuilder = new PlaceBuilder();
	// Place placeFromLatLon = placeBuilder.setLatitude(latAndLon[0])
	// .setLongitude(latAndLon[1])
	// .setName(LocationMapActivity.PIN_NAME_MEDIA)
	// .buildPlace();
	//
	// Intent mapIntent = new Intent(fragment.getActivity(),
	// LocationMapActivity.class);
	// mapIntent.putExtra(LocationMapActivity.EXTRA_PLACE_BYTES,
	// placeFromLatLon.toByteArray());
	// mapIntent.putExtra(LocationMapActivity.EXTRA_IMAGE_PATH,
	// mediaUri.toString());
	// mapIntent.putExtra(LocationMapActivity.EXTRA_STARTUP_FLAG,
	// LocationMapActivity.STARTUP_MODE_MEDIA);
	// fragment.startActivityForResult(mapIntent, requestCode);
	// }
	// else {
	// Toast.makeText(
	// context,
	// context.getString(R.string.image_does_not_have_location),
	// Toast.LENGTH_SHORT).show();
	// }
	// }

	// public static void startVzwNavigate(final Activity activity,
	// com.nbi.map.data.Place navigateTo)
	// {
	// if (activity == null) { return; }
	// Context context = activity.getApplicationContext();
	// NBIContext nbicontext = null;
	//
	// try {
	// //ensure we can retrieve a valid nbi context
	// nbicontext = LBSManager.getInstance(context).getNBIContext();
	// }
	// catch (LocationException e) {
	// Logger.error(e);
	// return;
	// }
	//
	// //if the navigation application isn't available, hide the navigate button
	// if(MapUtils.isNavigationAvailable(nbicontext))
	// {
	// MapUtils.navigateTo(
	// nbicontext,
	// navigateTo,
	// "easiest", // Must be "fastest", "shortest", "easiest"
	// "", // Zero or more of the following: C (Car pool lanes), H (Highways), T
	// (Tolls), U (U-Turns)
	// "car", // Specify vehicle type. "car", "truck", "bicycle" or "pedestrian"
	// true); // True triggers navigation. False just populates the navigation
	// screen
	// }
	// else
	// {
	// //VZNavigator is not available, show a dialog giving them the option to
	// download it from the Market
	// AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	// builder.setMessage(R.string.location_vznav_not_available)
	// .setCancelable(false)
	// .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int id) {
	// if (dialog!=null){
	// dialog.cancel();
	// }
	// try {
	// //launch to the android market place to a known package name/id
	// Intent goToMarket = new Intent(Intent.ACTION_VIEW,
	// Uri.parse(OEM.VZNAV_PACKAGE_URI));
	// activity.startActivity(goToMarket);
	// }
	// catch(ActivityNotFoundException anfException)
	// {
	// Log.e(TAG, "vzNavigateListener.onClick() - Error loading market: " +
	// anfException.getLocalizedMessage());
	//
	// showToastOnUIThread(activity, R.string.location_market_not_available,
	// Toast.LENGTH_SHORT);
	// }
	// }
	// })
	// .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int id) {
	// dialog.cancel();
	// }
	// });
	// AlertDialog alert = builder.create();
	// alert.show();
	// }
	// }

	// /**
	// * Show a Toast message on the UI thread.
	// *
	// * @param stringResourceID the string ID of the message to display
	// * @param toastShowLength the amount of time the toast is displayed (ex:
	// Toast.LENGTH_SHORT)
	// */
	// public static void showToastOnUIThread(final Activity activity, final int
	// stringResourceID, final int toastShowLength)
	// {
	// activity.runOnUiThread(new Runnable() {
	// @Override
	// public void run() {
	// Toast.makeText(activity.getApplicationContext(), stringResourceID,
	// toastShowLength).show();
	// }
	// });
	// }

	// private static boolean floatComp(float float1, float float2) {
	// final float valDiff = 0.0001f;
	// return Math.abs(float1 - float2) < valDiff;
	// }

	static final int TWO_MINUTES = 1000 * 60 * 2;

	public static boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getGpsTime()
				- currentBestLocation.getGpsTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		if (isSignificantlyNewer) {
			return true;
		} else if (isSignificantlyOlder) {
			return false;
		}

		// check for accuracy
		int accuracyDelta = location.getAccuracy()
				- currentBestLocation.getAccuracy();
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;

		// check if the old and new locations are from the same provider
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		}

		return false;
	}
}
