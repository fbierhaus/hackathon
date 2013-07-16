package com.verizon.mms.ui;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nbi.common.NBIContext;
import com.nbi.common.NBIException;
import com.nbi.common.NBIRequest;
import com.nbi.location.Location;
import com.nbi.location.LocationException;
import com.nbi.location.LocationListener;
import com.nbi.location.LocationProvider;
import com.nbi.map.android.MapConfiguration;
import com.nbi.map.android.MapView;
import com.nbi.map.android.MapView.OnPinEventListener;
import com.nbi.map.android.Pin;
import com.nbi.map.data.Coordinates;
import com.nbi.map.data.MapLocation;
import com.nbi.map.data.POI;
import com.nbi.map.data.Place;
import com.nbi.map.data.Point;
import com.nbi.map.data.Rectangle;
import com.nbi.map.data.SearchRegion;
import com.nbi.map.data.TrafficIncident;
import com.nbi.map.geocode.ReverseGeocodeInformation;
import com.nbi.map.geocode.ReverseGeocodeListener;
import com.nbi.map.geocode.ReverseGeocodeRequest;
import com.nbi.search.SearchRequest;
import com.nbi.search.singlesearch.SuggestMatch;
import com.nbi.search.singlesearch.SuggestionSearchInformation;
import com.nbi.search.singlesearch.SuggestionSearchListener;
import com.nbi.search.singlesearch.SuggestionSearchRequest;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.AddressProvider;
import com.verizon.mms.AddressProvider.AddressHelper;
import com.verizon.mms.data.LBSManager;
import com.verizon.mms.data.ObjectSerializer;
import com.verizon.mms.model.AddressModel;
import com.verizon.mms.model.PlaceInfo;
import com.verizon.mms.ui.adapter.PlacesListAdapter;
import com.verizon.mms.ui.widget.ImageViewButton;
import com.verizon.mms.ui.widget.SingleSearchField;
import com.verizon.mms.ui.widget.SingleSearchField.SingleSearchFieldListener;
import com.verizon.mms.util.BitmapManager;
import com.verizon.mms.util.LocationUtils;
import com.verizon.mms.util.PlacesList;
import com.verizon.mms.util.RecentSearchItem;
import com.verizon.mms.util.SuggestMatchItem;
import com.verizon.mms.util.Util;
import com.verizon.mms.util.VzNavigatorUrlGenerator;

public class AddLocationActivity extends VZMActivity implements OnClickListener,
		OnPinEventListener, LocationListener {
	private LBSManager lbsManager;
	private LocationProvider locManager;
	private NBIContext nbiContext;
	private PlacesList placesList = null;
	private SharedPreferences prefs;
	private boolean shownLocationDialog;
	private boolean calledBack;
	private RelativeLayout bottomLayout;
	private boolean dragging;
	private NinePatchDrawable bubbleBackground;
	private ImageView bubbleStar;
	private Dialog locationDialog;
	private String fromPlacesAddress;
	private String fromPlacesName;
	private String fromPlacesContext;
	private boolean showingDialog;
	private float starX;
	private float starY;
	private boolean placePin;
	private float starWidth;
	private float starHeight;
	private Paint processingPaint;
	private String beenTouched;
	private Rectangle bubbleRectangle;
	private MapView mapView;
	private MapConfiguration mapConfiguration;
	private boolean multiTouch;
	private boolean isPortrait;
	private MapLocation lastSearched;
	private Pin lastPin;
	private ImageView favIcon;
	private Rect titleBounds;
	private Rect subtitleBounds;
	private Place lastPlace;
	private String imageUri;
	private Bitmap fromMediaImage;
	private Paint title;
	private Paint subtitle;
	private ImageViewButton searchButt;
	private String mapUrl;
	private int lastAccuracy;
	private ImageViewButton zoomIn;
	private ImageViewButton zoomOut;
	private ImageViewButton findPin;
	private Button attachButton;
	private ListView favRecUsdListView;
	private AppAlignedDialog pd;
	// Search Variables
	private SingleSearchField searchField;
	private RecentListAdapter recentItemsListAdapter;
	private ArrayList<RecentSearchItem> recentItemsList;
	private SuggestionListAdapter suggestionsListAdapter;
	private ArrayList<SuggestMatchItem> suggestionsList;
	// NBI Variables
	private SuggestionSearchRequest mSuggestionSearchRequest = null;
	private SuggestionSearchInformation mSuggestionSearchInfo = null;
	// View Variables
	private ListView recentItemsListView;
	private ListView suggestionsListView;
	private AppAlignedDialog requestTimeOutDlg;
	private List<NBIRequest> nbiReqs = Collections.synchronizedList(new ArrayList<NBIRequest>());

	private static final int REQUEST_CODE_FROM_MEDIA_LOCATION = 22;
	private static final int MAX_RECENT_SEARCHES = 5;
	private static final String showWifiKey = "addlocation.showwifidialog";
	private static final String MAP_IMAGE_FILENAME = "location.jpeg";
	private static final Uri CACHE_URI = Uri.parse("content://address-cache/address");

	public static final String ADDRESS_LON = "address_lon";
	public static final String ADDRESS_LAT = "address_lat";
	public static final String CITYSTATEZIP = "citystatezip";
	public static final String ADDRESS_TITLE = "addressTitle";
	public static final String STREET = "street";
	public static final String PLACE_LONG = "placeLong";
	public static final String PLACE_LAT = "placeLat";
	public static final String IMAGE_FILE_PATH = "imageFilePath";
	public static final String MAP_URL = "mapUrl";
	public static final String STATE = "state";
	public static final String ZIP = "zip";
	public static final String CITY = "city";
	
	private float titleFontSize = 12;
	private float subTitleFontSize = 18;  

	private Handler zoomTo = new Handler() {
		public void handleMessage(Message msg) {

		};
	};

	private Handler myCallback = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				if (!calledBack && pd != null && pd.isShowing()) {
					pd.cancel();
					pd = null;
					try {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(),
									"canceling GetLocation from LM");
						}

						locManager.cancelGetLocation(
								AddLocationActivity.this);
					} catch (LocationException e) {
						Logger.error(e);
					}
					onRequestTimeOut(null);
				}
				break;
			case 1:
				if (!calledBack && pd != null && pd.isShowing()) {
					onRequestTimeOut(null);
				}
				break;
			}

		}
	};

	private Runnable runCallback = new Runnable() { // timeout of 15 seconds
		@Override
		public void run() {
			myCallback.sendEmptyMessage(0);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.addlocationlayout);
		
		Resources resources = getResources();
		
		if (MessageUtils.getTheme(this) == R.style.Theme_Large) {
			titleFontSize = resources.getDimensionPixelSize(R.dimen.locationTitle_large);
			subTitleFontSize = resources.getDimensionPixelSize(R.dimen.locationSubTitle_large);
		} else {
			titleFontSize = resources.getDimensionPixelSize(R.dimen.locationTitle_normal);
			subTitleFontSize = resources.getDimensionPixelSize(R.dimen.locationSubTitle_normal);
		}
		
		initLocManager();
		initViews();
		beenTouched = null;

		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (locationDialog == null) {
		    if (mapUrl != null) {
				initFromUrl();
			} else {
				showLocSettingIfNecessary();
				Context context = AddLocationActivity.this;
				if (pd == null && showingDialog == false) {
					pd = new AppAlignedDialog(context,
							context.getString(R.string.processing_dialog_text), 
							context.getString(R.string.finding_user_location), true, false);
					pd.show();
					
					initializeGetUserLocation(true);
				}
			}
		}
	}

	private void initLocManager() {
		lbsManager = new LBSManager(getApplicationContext());
		locManager = lbsManager.getLocationManager();
		nbiContext = lbsManager.getNBIContext();
	}

	private void initFromUrl() {
		//mapView.mapFromURL(URLDecoder.decode(mapUrl));
		mapView.mapFromURL(mapUrl);

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(AddLocationActivity.class, "Initing map from url: " + mapUrl);
		}
		
		lastPin = mapView.getPin(mapView.getNextPin(-1));
		if (lastPin == null) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.error(false, AddLocationActivity.class, "Error - No pin found - dropping a new one");
			}
			dropPin();
			lastPin = mapView.getPin(mapView.getNextPin(-1));
			if (Logger.IS_DEBUG_ENABLED) {
				if (lastPin == null) {
					Logger.error(false, AddLocationActivity.class, "Error - New pin was still null");
				}
			}
		} 
		lastPin.setPinType(Pin.PIN_BLUE);
		lastPlace = lastPin.getPlace();
		if (!lastPin.getTitle().startsWith(getString(R.string.latwithcolon))) {
			fromPlacesAddress = lastPin.getTitle();
			fromPlacesContext = lastPin.getSubTitle();
		} else {
			fromPlacesAddress = lastPin.getTitle().trim() + " "
					+ lastPin.getSubTitle().trim();
		}
		updateBeenPressed();
		((TextView) findViewById(R.id.title))
				.setText(getString(R.string.location_header));
		((LinearLayout) findViewById(R.id.addressentrylayout))
				.setVisibility(View.GONE);
		Button navigate = (Button) findViewById(R.id.navigate);
		navigate.setVisibility(View.VISIBLE);
		bottomLayout.findViewById(R.id.attachbutton).setVisibility(View.GONE);
		navigate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				PackageManager pm = getPackageManager();
				String uri = "geo:"
						+ mapView.getPin(mapView.getNextPin(-1)).getPlace()
								.getLocation().getLatitude()
						+ ","
						+ mapView.getPin(mapView.getNextPin(-1)).getPlace()
								.getLocation().getLongitude() + "?q="
						+ lastPin.getTitle() + " " + (lastPin.getSubTitle().equalsIgnoreCase(lastPin.getTitle())? "": lastPin.getSubTitle());
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "initFromUrl() uri: " + uri);
					Logger.debug(getClass(), "initFromUrl() lastpin title :"+lastPin.getTitle()+"\t subtitle:"+lastPin.getSubTitle()+ 
							" and city :" +lastPlace.getLocation().getCity() + ",state: "
										+ lastPlace.getLocation().getState() + ",postalCode: "
										+ lastPlace.getLocation().getPostalCode());
				}
				List<ResolveInfo> resolves = pm.queryIntentActivities(i, 0);
				if (resolves != null && resolves.size() > 0) {
					startActivity(i);
				} else {
					showGetVzDialog();
				}
			}
		});
	}

	private void showGetVzDialog() {
		Builder builder = new Builder(this);
		builder.setTitle(getString(R.string.error_dialog_title));
		builder.setMessage(getString(R.string.navigator_not_installed));
		builder.setPositiveButton(getString(R.string.yes),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							Intent goToMarket = new Intent(
									Intent.ACTION_VIEW,
									Uri.parse(VzNavigatorUrlGenerator.VZNAV_PACKAGE_URI));
							startActivity(goToMarket);
						} catch (ActivityNotFoundException anf) {
							Toast.makeText(AddLocationActivity.this,
									getString(R.string.invalid_device),
									Toast.LENGTH_LONG).show();
						}
					}
				});
		builder.setNegativeButton(getString(R.string.no),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
		builder.show();
	}

	@Override
	protected void onPause() {
		if (pd != null && pd.isShowing()) {
			pd.cancel();
			pd = null;
			finish();
		}
		if (requestTimeOutDlg != null) {
			requestTimeOutDlg.cancel();
		}
		myCallback.removeCallbacks(runCallback);
		super.onPause();
	}

	public void initViews() {
		lastAccuracy = 1000000;
		dragging = false;
		mapView = (MapView) findViewById(R.id.mapview);
		findPin = (ImageViewButton) findViewById(R.id.findpin);
		zoomIn = (ImageViewButton) findViewById(R.id.zoomin);
		zoomOut = (ImageViewButton) findViewById(R.id.zoomout);
		bottomLayout = (RelativeLayout) findViewById(R.id.bottom_layout);
		searchField = (SingleSearchField) findViewById(R.id.search_field);
		recentItemsListView = (ListView) findViewById(R.id.recent_list);
		suggestionsListView = (ListView) findViewById(R.id.suggestion_list);
		favIcon = (ImageView) findViewById(R.id.staricon);
		bubbleBackground = (NinePatchDrawable) getResources().getDrawable(
				R.drawable.popup_bg);

		favRecUsdListView = (ListView) findViewById(R.id.favRecentlyUsedList);
		favIcon.setImageDrawable(getResources().getDrawable(
				R.drawable.list_favorites_star_off));
		if (beenTouched != null) {
			if (bubbleStar != null) {
				if (beenTouched.equals("yes")) {
					favIcon.setImageDrawable(getResources().getDrawable(
							R.drawable.list_favorites_star_on));
				} else {
					favIcon.setImageDrawable(getResources().getDrawable(
							R.drawable.list_favorites_star_off));
				}
			}
		} else {
			favIcon.setImageDrawable(getResources().getDrawable(
					R.drawable.list_favorites_star_off));
		}
		favIcon.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent m) {
				if (mapView.getVisibility() == View.GONE) {
					mapView.setVisibility(View.VISIBLE);
					bottomLayout.setVisibility(View.VISIBLE);
					suggestionsListView.setVisibility(View.GONE);
					recentItemsListView.setVisibility(View.GONE);
				}
				favIcon.setImageDrawable(getResources().getDrawable(
						R.drawable.list_favorites_star_on));
				showLocationItemsList(true);
				return false;
			}
		});
		// create the PlacesList object
		placesList = new PlacesList(this, nbiContext);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		searchButt = (ImageViewButton) findViewById(R.id.searchicon);
		attachButton = (Button) findViewById(R.id.attachbutton);
		// create the map configuration
		mapConfiguration = new MapConfiguration(nbiContext);
		attachButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				Drawable d = getResources().getDrawable(
						R.drawable.attach_button_box);
				if (action == MotionEvent.ACTION_DOWN) {
					d.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
				} else if (action == MotionEvent.ACTION_UP
						|| action == MotionEvent.ACTION_OUTSIDE) {
					d.clearColorFilter();
				}
				attachButton.setBackgroundDrawable(d);
				return false;
			}
		});

		searchField.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Drawable dRight = searchField.getCompoundDrawables()[2];
				Util.forceShowKeyboard(AddLocationActivity.this,
						searchField);
				if ((event.getAction() == MotionEvent.ACTION_DOWN
						|| event.getAction() == MotionEvent.ACTION_MOVE || event
						.getAction() == MotionEvent.ACTION_UP)
						&& dRight != null) {
					Rect rBounds = dRight.getBounds();
					final int x = (int) event.getX() + 5; // Added Extra padding
					final int y = (int) event.getY() + 5;
					// Checks for remove button click action
					if (x > (v.getRight() - rBounds.width())
							&& x <= (v.getRight() - v.getPaddingRight())
							&& y > v.getPaddingTop()
							&& y <= (v.getHeight() - v.getPaddingBottom())) {
						searchField.getText().clear();
						searchField.setText("");
						searchField.setCompoundDrawablesWithIntrinsicBounds(0,
								0, 0, 0);
					
					}
				}
				favRecUsdListView.setVisibility(View.GONE);
				suggestionsListView.setVisibility(View.GONE);
				recentItemsListView.setVisibility(View.GONE);
				mapView.setVisibility(View.VISIBLE);
				bottomLayout.setVisibility(View.VISIBLE);
				return false;
			}
		});
		searchField
				.setSingleSearchFieldListener(new SingleSearchFieldListener() {

					@Override
					public void onFocusChanged(boolean hasFocus) {
						//showSuggestionList(hasFocus);
                        if (!hasFocus) {
                        	hideSoftKeyboard(searchField.getWindowToken());
                        }
					}

					@Override
					public void onEnterKey() {
						doSearch(searchField.getText().toString());

					}

					@Override
					public void onBackKey() {
						if (mSuggestionSearchRequest != null) {
							mSuggestionSearchRequest.cancelRequest();
						}
						hideSoftKeyboard(searchField.getWindowToken());
						clearSearchField(true);
						
			        }

					@Override
					public void afterTextChanged() {

						String searchText = searchField.getText().toString();
						int numChars = searchText.length();
						
						if (numChars > 0) {
							// create a suggestion search
							Coordinates coordinate = null;
							if (lastPlace != null) {
								coordinate = new Coordinates(lastPlace
										.getLocation().getLatitude(), lastPlace
										.getLocation().getLongitude());
							} else {
								coordinate = mapView.getMapCenter();
							}
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(getClass(), "Sending new search with: " + searchText);
							}
							mSuggestionSearchRequest = new SuggestionSearchRequest(
									nbiContext,
									searchText,
									new SearchRegion(coordinate),
									SuggestionSearchRequest.SLICE_RESULTS_DEFAULT,
									SearchRequest.POI_ENHANCED,
									mSuggestionSearchListener);
							mSuggestionSearchRequest.startRequest();
							searchField.setCompoundDrawablesWithIntrinsicBounds(0,
												0, R.drawable.close_button, 0);
						
						} else {
							if (searchField.hasFocus()) {
								if (suggestionsList.size() > 0) {
									showSuggestionList(true);
								}
								
							}

						}
						searchButt.setVisibility(View.VISIBLE);
					}
				});
		searchField.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		
		// create the suggestions list
		recentItemsList = new ArrayList<RecentSearchItem>();
		suggestionsList = new ArrayList<SuggestMatchItem>();
		if (prefs.contains("recent_search")) {
			try {
				ArrayList<RecentSearchItem> deserialize = (ArrayList<RecentSearchItem>) ObjectSerializer
						.deserialize(prefs.getString(
								"recent_search",
								ObjectSerializer
										.serialize(new ArrayList<RecentSearchItem>())));
				recentItemsList = deserialize;
			} catch (IOException e) {
				// TODO Auto-generated catch block
                Logger.error(e);
			}
		}

		showSuggestionList(false);

		recentItemsListAdapter = new RecentListAdapter(recentItemsList);
		recentItemsListView.setAdapter(recentItemsListAdapter);
		recentItemsListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// when clicked, populate the search field and do the search
				RecentSearchItem item = ((RecentSearchItem) recentItemsListView
						.getItemAtPosition(position));
				recentItemsList.remove(item);
				recentItemsList.add(0, item);
				recentItemsListAdapter.notifyDataSetChanged();
				doSearchForRecent(item);
			}
		});
		suggestionsListAdapter = new SuggestionListAdapter(suggestionsList);
		suggestionsListView.setAdapter(suggestionsListAdapter);
		suggestionsListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// when clicked, populate the search field and do the search
				doSubSearch(position);
			}
		});
		suggestionsListView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// if this list is touched, then hide the keyboard
				hideSoftKeyboard(searchField.getWindowToken());
				return false;
			}
		});

		findPin.setOnClickListener(this);
		searchButt.setOnClickListener(this);
		zoomIn.setOnClickListener(this);
		zoomOut.setOnClickListener(this);
		attachButton.setOnClickListener(this);

		fromMediaImage = null;

		
		// display the map

	
		mapView.setOnPinEventListener(this);
		mapView.setOnTouchListener(new OnTouchListener() {

			boolean eventStarted = false;
			boolean multiTouch = false;
			boolean eventCompleted = false;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				hideSoftKeyboard(searchField.getWindowToken());
				
				Runnable r = new Runnable() {
					@Override
					public void run() {
						AddLocationActivity.this.multiTouch = false;
					}
				};
				AddLocationActivity.this.multiTouch = false;

				if (event.getPointerCount() > 1) {
					multiTouch = true;
					AddLocationActivity.this.multiTouch = true;
					mapView.deselectAllPins();
				} else if (event.getAction() == MotionEvent.ACTION_DOWN) {
					eventStarted = true;
					multiTouch = false;
					new Handler().postDelayed(r, 7000);
					eventCompleted = false;
				}
				if (event.getAction() == MotionEvent.ACTION_UP && eventStarted) {
					if (!multiTouch) {
						eventStarted = false;
						eventCompleted = true;
					}
					new Handler().postDelayed(r, 7000);
				}
				float x = event.getX();
				float y = event.getY();
				int dp10 = (int) TypedValue.applyDimension(
						TypedValue.COMPLEX_UNIT_DIP, 10, getResources()
								.getDisplayMetrics());
				if (event.getX() > starX - dp10
						&& event.getX() < starX + starWidth + dp10
						&& event.getY() > starY - dp10
						&& event.getY() < starY + starHeight + dp10) {
					if (eventCompleted) {
						Cursor one = null;
						if (!lastPin.getTitle().startsWith(getString(R.string.latwithcolon))) {
							one = getContentResolver()
									.query(PlacesTabActivity.CACHE_URI,
											null,
											AddressProvider.AddressHelper.ADDRESS
													+ "=? AND "
													+ AddressHelper.CITYSTATEZIP
													+ "=?",
											new String[] { lastPin.getTitle(),
													lastPin.getSubTitle() },
											null);
						} else {
							one = getContentResolver().query(
									PlacesTabActivity.CACHE_URI,
									null,
									AddressProvider.AddressHelper.ADDRESS
											+ "=?",
									new String[] { lastPin.getTitle() + " "
											+ lastPin.getSubTitle() }, null);
						}
						Cursor two = null;
						if (fromPlacesName != null) {
							two = getContentResolver().query(
									PlacesTabActivity.CACHE_URI,
									null,
									AddressHelper.NAME + "=? AND "
											+ AddressHelper.CITYSTATEZIP
											+ "=? AND " + AddressHelper.ADDRESS
											+ "=?",
									new String[] { fromPlacesName,
											fromPlacesContext,
											fromPlacesAddress }, null);
						} else if (fromPlacesAddress != null) {
							if (fromPlacesAddress.startsWith(getString(R.string.latwithcolon))) {
								two = getContentResolver().query(
										PlacesTabActivity.CACHE_URI, null,
										AddressHelper.ADDRESS + "=?",
										new String[] { fromPlacesAddress },
										null);
							} else {
								two = getContentResolver().query(
										PlacesTabActivity.CACHE_URI,
										null,
										AddressHelper.CITYSTATEZIP + "=? AND "
												+ AddressHelper.ADDRESS + "=?",
										new String[] { fromPlacesContext,
												fromPlacesAddress }, null);
							}
						}
						if (beenTouched == null
								|| !beenTouched.equalsIgnoreCase("yes")) {
							if (lastPin != null) {
								if (fromPlacesName != null) {
									if (two.getCount() == 0) {
										ContentValues cv = AddressHelper
												.toContentValues(new AddressModel(
														fromPlacesAddress,
														System.currentTimeMillis(),
														true,
														lastPlace.getLocation()
																.getLatitude(),
														lastPlace.getLocation()
																.getLongitude(),
														fromPlacesContext,
														fromPlacesName));
										getContentResolver()
												.insert(PlacesTabActivity.CACHE_URI,
														cv);
									} else {
										ContentValues values = new ContentValues();
										values.put(AddressHelper.FAVORITE, 1);
										two.moveToFirst();
										int _id = two.getInt(two
												.getColumnIndex(AddressHelper._ID));
										getContentResolver()
												.update(CACHE_URI,
														values,
														AddressHelper._ID
																+ " = " + _id,
														null);
									}
								} else if (fromPlacesAddress != null) {
									if (two.getCount() == 0) {
										ContentValues cv = AddressHelper
												.toContentValues(new AddressModel(
														fromPlacesAddress,
														System.currentTimeMillis(),
														true,
														lastPlace.getLocation()
																.getLatitude(),
														lastPlace.getLocation()
																.getLongitude(),
														fromPlacesContext, null));
										getContentResolver()
												.insert(PlacesTabActivity.CACHE_URI,
														cv);
									} else {
										ContentValues values = new ContentValues();
										values.put(AddressHelper.FAVORITE, 1);
										two.moveToFirst();
										int _id = two.getInt(two
												.getColumnIndex(AddressHelper._ID));
										getContentResolver()
												.update(CACHE_URI,
														values,
														AddressHelper._ID
																+ " = " + _id,
														null);
									}
								} else if (one.getCount() == 0) {
									if (!lastPin.getTitle().startsWith(
											getString(R.string.lat))) {
										ContentValues cv = AddressHelper
												.toContentValues(new AddressModel(
														lastPin.getTitle(),
														System.currentTimeMillis(),
														true,
														lastPlace.getLocation()
																.getLatitude(),
														lastPlace.getLocation()
																.getLongitude(),
														lastPin.getSubTitle(),
														null));
										getContentResolver()
												.insert(PlacesTabActivity.CACHE_URI,
														cv);
									} else {
										ContentValues cv = AddressHelper
												.toContentValues(new AddressModel(
														lastPin.getTitle()
																+ " "
																+ lastPin
																		.getSubTitle(),
														System.currentTimeMillis(),
														true,
														lastPlace.getLocation()
																.getLatitude(),
														lastPlace.getLocation()
																.getLongitude(),
														null, null));
										getContentResolver()
												.insert(PlacesTabActivity.CACHE_URI,
														cv);
									}
								} else if (one.getCount() > 0) {
									ContentValues values = new ContentValues();
									values.put(AddressHelper.FAVORITE, 1);
									one.moveToFirst();
									int _id = one.getInt(one
											.getColumnIndex(AddressHelper._ID));
									getContentResolver().update(CACHE_URI,
											values,
											AddressHelper._ID + " = " + _id,
											null);
								}
							}
							beenTouched = "yes";
						} else if (one.getCount() > 0) {
							ContentValues values = new ContentValues();
							values.put(AddressHelper.FAVORITE, 0);
							one.moveToFirst();
							int _id = one.getInt(one
									.getColumnIndex(AddressHelper._ID));
							getContentResolver().update(CACHE_URI, values,
									AddressHelper._ID + " = " + _id, null);
							beenTouched = "no";
						} else if (two != null && two.getCount() > 0) {
							ContentValues values = new ContentValues();
							values.put(AddressHelper.FAVORITE, 0);
							two.moveToFirst();
							int _id = two.getInt(two
									.getColumnIndex(AddressHelper._ID));
							getContentResolver().update(CACHE_URI, values,
									AddressHelper._ID + " = " + _id, null);
							beenTouched = "no";
							two.close();
						}
						one.close();
						mapView.invalidate();
						return true;
					}

				}
			
				if (bubbleRectangle != null
						&& bubbleRectangle.isPointInRectangle((int) x, (int) y)) {
					return true;
				}
				return false;
			}
		});
		try {
			mapView.setConfiguration(mapConfiguration);
		} catch (OutOfMemoryError error) {
			// insufficient memory to create map view
			displayOutOfMemoryDialog();
		}
		mapView.mapShowLocation(false);
		mapUrl = getIntent().getStringExtra("mapURL");
	}

	private void displayOutOfMemoryDialog() {
		AlertDialog alert = new AlertDialog.Builder(this).setCancelable(false)
				.create();
		alert.setCancelable(false);
		alert.setMessage(getString(R.string.insufficient_memory));
		alert.setButton(Dialog.BUTTON_POSITIVE, "OK",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						finish();
					}
				});
		alert.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.add_location_menu, menu);
		if (mapUrl != null) {
			menu.removeItem(R.id.drop_pin);
		}
		return true;
	}

	private void dropPin() {
		Coordinates mapCoords = mapView.convertMapXYToCoordinates(new Point(mapView.getWidth() / 2, mapView.getHeight() / 2));
		if (mapCoords != null) {
			Location l = new Location();
			l.setLatitude(mapCoords.getLatitude());
			l.setLongitude(mapCoords.getLongitude());
			lastPlace = new Place(l);
			placePin(true);
			ReverseGeocodeRequest rgr = new ReverseGeocodeRequest(
					nbiContext, mapCoords.getLatitude(),
					mapCoords.getLongitude(), false,
					new ReverseGeocodeHandler(mapCoords.getLatitude(),
							mapCoords.getLongitude(), lastPin));
			startRequest(rgr);
		}		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.drop_pin:
			dropPin();
			return true;
		case R.id.my_location:
			try {
				locManager.cancelGetLocation(this);
			} catch (Exception e) {
			}
			if ((pd == null || (pd != null && !pd.isShowing()))
					&& showingDialog == false) {
				initializeGetUserLocationAcc(false);
				Context context = AddLocationActivity.this;
				pd = new AppAlignedDialog(context,
						context.getString(R.string.processing_dialog_text), 
						context.getString(R.string.finding_user_location), true, false);
				pd.show();
			}
			return true;
		case R.id.refresh:
			placePin(true);
			ReverseGeocodeRequest rgr = new ReverseGeocodeRequest(nbiContext,
					lastPlace.getLocation().getLatitude(), lastPlace
							.getLocation().getLongitude(), false,
					new ReverseGeocodeHandler(lastPlace.getLocation()
							.getLatitude(), lastPlace.getLocation()
							.getLongitude(), lastPin));
			startRequest(rgr);
			return true;
		case R.id.from_media:
			MessageUtils.selectImage(AddLocationActivity.this,
					REQUEST_CODE_FROM_MEDIA_LOCATION);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void showLocationItemsList(boolean isFavorite) {
		Cursor cursor = null;
		try {
			if (isFavorite) {
				cursor = getContentResolver().query(CACHE_URI, null,
						AddressHelper.FAVORITE + " = " + 1, null,
						AddressHelper.DATE + " desc limit 50"); // Maximum list size
																// is 50 as per VzM
																// doc
			} else {
				cursor = getContentResolver().query(CACHE_URI, null, null, null,
						AddressHelper.DATE + " desc limit 50"); // Maximum list size
																// is 50 as per VzM
																// doc
			}
			if (cursor == null || cursor.getCount() == 0) {
				favRecUsdListView.setAdapter(null);
				favRecUsdListView.invalidate();
				favIcon.setImageDrawable(getResources().getDrawable(
						R.drawable.list_favorites_star_off));
				if (!isFavorite) {
					Toast.makeText(AddLocationActivity.this,
							getString(R.string.no_recentlyused_dlg_msg),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(AddLocationActivity.this,
							getString(R.string.no_favorites_dlg_msg),
							Toast.LENGTH_SHORT).show();
				}
			} else {
				recentItemsListView.setVisibility(View.GONE);
				suggestionsListView.setVisibility(View.GONE);
				cursor.moveToFirst();
				if (favRecUsdListView != null) {
					favRecUsdListView.setAdapter(null);
					favRecUsdListView.setVisibility(View.GONE);
				}
				PlacesListAdapter pLA = new PlacesListAdapter(this, cursor, true);
				favRecUsdListView.setAdapter(pLA);
				mapView.setVisibility(View.GONE);
				bottomLayout.setVisibility(View.GONE);
				favRecUsdListView.setVisibility(View.VISIBLE);
				
				cursor = null;
			}
		}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@Override
	public void onBackPressed() {
		
		
		if (favRecUsdListView.getVisibility() == View.VISIBLE) {
			mapView.setVisibility(View.VISIBLE);
			bottomLayout.setVisibility(View.VISIBLE);
			favRecUsdListView.setVisibility(View.GONE);
			favIcon.setImageDrawable(getResources().getDrawable(
					R.drawable.list_favorites_star_off));
			updateBeenPressed();
		} else if (recentItemsListView.getVisibility() == View.VISIBLE){
			mapView.setVisibility(View.VISIBLE);
			bottomLayout.setVisibility(View.VISIBLE);
			recentItemsListView.setVisibility(View.GONE);
		} else if (suggestionsListView.getVisibility() == View.VISIBLE) {
			mapView.setVisibility(View.VISIBLE);
			bottomLayout.setVisibility(View.VISIBLE);
			suggestionsListView.setVisibility(View.GONE);
		} else {
			super.onBackPressed();
		}
	}

	private void initializeGetUserLocation(boolean placePin) {
		try {
			this.placePin = placePin;
			calledBack = false;
			locManager.startReceivingFixes(this); 
			myCallback.postDelayed(runCallback, 60000);
		} catch (LocationException e) {
			Logger.error(e);
			showErrorDialog();
		}
	}

	private void initializeGetUserLocationAcc(boolean placePin) {
		try {
			this.placePin = placePin;
			calledBack = false;
			locManager.startReceivingFixes(this); 
			myCallback.postDelayed(runCallback, 20000);
		} catch (LocationException e) {
			Logger.error(e);
			showErrorDialog();
		}
	}

	private void showErrorDialog() {
		final AppAlignedDialog d = new AppAlignedDialog(this, 0,
				R.string.location_carrier_invalid_title,
				R.string.location_carrier_invalid_message);
		Button canelButton = (Button) d.findViewById(R.id.negative_button);
		canelButton.setText(R.string.location_carrier_invalid_button);
		canelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
				d.dismiss();
			}
		});
		d.show();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.zoomin:

			int zoomLevel = mapView.getZoomLevel();
			if (zoomLevel != mapView.getMaximumZoomLevel()) {
				mapView.setZoomLevel(zoomLevel + 1, true);
			}
			break;
		case R.id.zoomout:

			int zoomLevel2 = mapView.getZoomLevel();
			if (zoomLevel2 != mapView.getMinimumZoomLevel()) {
				mapView.setZoomLevel(zoomLevel2 - 1, true);
			}
			break;
		case R.id.searchicon:
			favIcon.setImageDrawable(getResources().getDrawable(
					R.drawable.list_favorites_star_off));
			showLocationItemsList(false);
			break;
		case R.id.search_field:
			doSearch(searchField.getText().toString());
			break;
		case R.id.attachbutton:
			// we do not want to show current location - not sure if this is the right place to put it
			mapView.mapShowLocation(false);

			if (lastPin == null
					|| lastPin.getTitle().equalsIgnoreCase(
							getString(R.string.processing_dialog_text))) {
				break;
			}

			if (mapView.getPinCount() > 0) {
				getWindow()
						.setSoftInputMode(
								WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
				Context context = AddLocationActivity.this;
				pd = new AppAlignedDialog(context,
						context.getString(R.string.processing_dialog_text), 
						context.getString(R.string.attaching_location), true, false);
				pd.show();
				try {
					locManager.stopReceivingFixes(this);
				} catch (LocationException e) {
					Logger.error(e);
				}
				final Handler stopAttaching = new Handler() {
					@Override
					public void handleMessage(Message msg) {
						if (pd != null && pd.isShowing()) {
							pd.cancel();
							pd = null;
							final AppAlignedDialog d = new AppAlignedDialog(
									AddLocationActivity.this, 0,
									R.string.error_dialog_title,
									R.string.cant_attach);
							Button saveButton = (Button) d
									.findViewById(R.id.positive_button);
							saveButton.setText(R.string.ok);
							saveButton
									.setOnClickListener(new View.OnClickListener() {
										@Override
										public void onClick(View v) {
											d.dismiss();
											if (lastPin == null) {
												finish();
											}
										}
									});
							d.show();
						}
					}
				};
				final Runnable runStopAttaching = new Runnable() {

					@Override
					public void run() {
						stopAttaching.sendEmptyMessage(0);
					}
				};
				stopAttaching.postDelayed(runStopAttaching, 15000);

				final Handler imageCaptureHandler = new Handler();

				Runnable imageCaptureRunnable = new Runnable() {
					@Override
					public void run() {

						if (mapView == null) {
							return;
						}

						final int mapWidth = mapView.getMeasuredWidth();
						final int mapHeight = mapView.getMeasuredHeight();
						Coordinates coordinate = null;
						if (lastPlace != null) {
							coordinate = new Coordinates(lastPlace
									.getLocation().getLatitude(), lastPlace
									.getLocation().getLongitude());
							mapView.setMapCenter(coordinate, false);
						}

						if (mapView.isImageReady(0, 0, mapWidth, mapHeight)
								&& mapView.getMapCenter().getLatitude() == coordinate
										.getLatitude()
								&& mapView.getMapCenter().getLongitude() == coordinate
										.getLongitude()) {
							stopAttaching.removeCallbacks(runStopAttaching);
							if (mapView.getSelectedPin() < 0) {
								mapView.selectPin(mapView.getNextPin(-1));
							}

							/*
							 * String addressString = null; if
							 * (lastPlace.getLocation().getFreeform().length() >
							 * 0){ addressString =
							 * lastPlace.getLocation().getFreeform(); } else {
							 * double latValue =
							 * lastPlace.getLocation().getLatitude(); double
							 * longValue
							 * =lastPlace.getLocation().getLongitude();
							 * addressString = String.valueOf(latValue) +"\n"+
							 * String.valueOf(longValue); } ContentValues cv =
							 * AddressHelper.toContentValues(new
							 * AddressModel(addressString,
							 * System.currentTimeMillis(), true));
							 */
							ContentValues cv = null;
							String addressToAdd = null;
							Cursor two = null;
							Cursor one = null;
							Cursor cursor = null;
							try {
								if (!lastPin.getTitle().startsWith(getString(R.string.latwithcolon))) {
									one = getContentResolver()
											.query(PlacesTabActivity.CACHE_URI,
													null,
													AddressHelper.ADDRESS
															+ "=? AND "
															+ AddressHelper.CITYSTATEZIP
															+ "=?",
													new String[] {
															lastPin.getTitle(),
															lastPlace
																	.getLocation()
																	.getCity()
																	+ ", "
																	+ lastPlace
																			.getLocation()
																			.getState()
																	+ ", "
																	+ lastPlace
																			.getLocation()
																			.getPostalCode() },
													null);
								} else {
									one = getContentResolver()
											.query(PlacesTabActivity.CACHE_URI,
													null,
													AddressProvider.AddressHelper.ADDRESS
															+ "=?",
													new String[] { lastPin
															.getTitle()
															+ " "
															+ lastPin
																	.getSubTitle() },
													null);
								}
								if (fromPlacesAddress != null
										&& !fromPlacesAddress
												.startsWith(getString(R.string.latwithcolon))) {
									two = getContentResolver().query(
											PlacesTabActivity.CACHE_URI,
											null,
											AddressHelper.CITYSTATEZIP
													+ "=? AND "
													+ AddressHelper.ADDRESS
													+ "=?",
											new String[] { fromPlacesContext,
													fromPlacesAddress }, null);
								} else if (fromPlacesAddress != null) {
									two = getContentResolver().query(
											PlacesTabActivity.CACHE_URI,
											null,
											AddressHelper.ADDRESS + "=?",
											new String[] { lastPin.getTitle()
													+ " "
													+ lastPin.getSubTitle() },
											null);
								}
								if (fromPlacesName != null) {
									if (two.getCount() == 0) {
										addressToAdd = fromPlacesName;
										cv = AddressHelper
												.toContentValues(new AddressModel(
														fromPlacesAddress,
														System.currentTimeMillis(),
														false,
														lastPlace.getLocation()
																.getLatitude(),
														lastPlace.getLocation()
																.getLongitude(),
														fromPlacesContext,
														fromPlacesName));
									}
								} else if (fromPlacesAddress != null) {
									if (two.getCount() == 0) {
										addressToAdd = fromPlacesAddress;
										cv = AddressHelper
												.toContentValues(new AddressModel(
														fromPlacesAddress,
														System.currentTimeMillis(),
														false,
														lastPlace.getLocation()
																.getLatitude(),
														lastPlace.getLocation()
																.getLongitude(),
														fromPlacesContext, null));
									}
								} else if (one.getCount() == 0) {
									if (!lastPin.getTitle().startsWith(
											getString(R.string.latwithcolon))) {
										addressToAdd = lastPin.getTitle();
										cv = AddressHelper
												.toContentValues(new AddressModel(
														addressToAdd,
														System.currentTimeMillis(),
														false,
														lastPin.getPlace()
																.getLocation()
																.getLatitude(),
														lastPin.getPlace()
																.getLocation()
																.getLongitude(),
														lastPin.getPlace()
																.getLocation()
																.getCity()
																+ ", "
																+ lastPin
																		.getPlace()
																		.getLocation()
																		.getState()
																+ ", "
																+ lastPin
																		.getPlace()
																		.getLocation()
																		.getPostalCode(),
														null));
									} else {
										addressToAdd = lastPin.getTitle() + " "
												+ lastPin.getSubTitle();
										cv = AddressHelper
												.toContentValues(new AddressModel(
														addressToAdd,
														System.currentTimeMillis(),
														false,
														lastPin.getPlace()
																.getLocation()
																.getLatitude(),
														lastPin.getPlace()
																.getLocation()
																.getLongitude(),
														null, null));
									}

								}
								if (addressToAdd != null) {
									cursor = getContentResolver()
											.query(CACHE_URI,
													new String[] { AddressHelper.ADDRESS },
													AddressHelper.ADDRESS
															+ "=?",
													new String[] { addressToAdd },
													null);
									cursor.moveToFirst();
									if (cursor.getCount() == 0) {
										getContentResolver()
												.insert(PlacesTabActivity.CACHE_URI,
														cv);
									}
								}
								final String tmpImagePath = createMapImage();
								Intent intent = new Intent();
								intent.putExtra(IMAGE_FILE_PATH, tmpImagePath);
								String mapUrl = mapView.mapToURL();
								if (Logger.IS_DEBUG_ENABLED) {
									Logger.debug(getClass(), "mapUrl is:" + mapUrl);
								}
								intent.putExtra(MAP_URL, mapUrl);
								intent.putExtra(PLACE_LAT, lastPlace
										.getLocation().getLatitude());
								intent.putExtra(PLACE_LONG, lastPlace
										.getLocation().getLongitude());
								intent.putExtra("address", lastPin.getTitle());
								if (lastPlace.getLocation().getState().length() > 0) {
									String[] addressItems = null;
									if (lastPin != null) {
									   intent.putExtra(ADDRESS_TITLE, lastPin.getTitle());
									   addressItems = lastPin.getSubTitle().split(",");
									   if(!lastPin.getTitle().equalsIgnoreCase(addressItems[0]))
									   intent.putExtra(STREET, addressItems[0]);
								    } 
									   
									if (addressItems != null && !addressItems[0].equalsIgnoreCase(lastPin.getPlace()
											.getLocation().getState()))   {
										intent.putExtra(STATE, lastPin.getPlace()
												.getLocation().getState());	
									}
									if (addressItems != null && !addressItems[0].equalsIgnoreCase(lastPin.getPlace()
											.getLocation().getCity()))   {
										intent.putExtra(CITY, lastPlace
												.getLocation().getCity());
									}
									
								    intent.putExtra(ZIP, lastPlace
												.getLocation().getPostalCode());
									
									
								} else if (lastPin.getPlace().getLocation()
										.getState().length() > 0) {
									String[] addressItems = lastPin.getSubTitle().split(",");
									intent.putExtra(ADDRESS_TITLE,
											lastPin.getTitle());
									if(!lastPin.getTitle().equalsIgnoreCase(addressItems[0]))
										   intent.putExtra(STREET, addressItems[0]);
									if (!addressItems[0].equalsIgnoreCase(lastPin.getPlace()
											.getLocation().getState()))   {
										intent.putExtra(STATE, lastPin.getPlace()
												.getLocation().getState());	
									}
									if (!addressItems[0].equalsIgnoreCase(lastPin.getPlace()
											.getLocation().getCity()))   {
										intent.putExtra(CITY, lastPlace
												.getLocation().getCity());
									}
									
									intent.putExtra(ZIP, lastPin.getPlace()
											.getLocation().getPostalCode());
								} else {
									intent.putExtra(ADDRESS_LAT,
											lastPin.getTitle());
									intent.putExtra(ADDRESS_LON,
											lastPin.getSubTitle());
								}
								setResult(Activity.RESULT_OK, intent);
								if (pd != null && pd.isShowing()) {
									pd.cancel();
									pd = null;
								}
								finish();
							} catch (Exception e) {
								Logger.error(e);
							} finally {
								if (one != null) {
									one.close();
								}
								if (two != null) {
									two.close();
								}
								if (cursor != null) {
									cursor.close();
								}
							}

						} else {

							if (imageCaptureHandler != null) {
								imageCaptureHandler.postDelayed(this, 200);
							}
						}
					}
				};

				imageCaptureHandler.post(imageCaptureRunnable);

			}
			break;
		case R.id.findpin:

			if (lastPlace != null) {
				Coordinates coordinate = new Coordinates(lastPlace
						.getLocation().getLatitude(), lastPlace.getLocation()
						.getLongitude());
				mapView.setMapCenter(coordinate, false);
			}
			break;
		}
	}

	public void drawPin(Place place, Coordinates coor) {
		if (coor.getLongitude() != 0 && coor.getLatitude() != 0) {
			lastPlace = place;
			
			if (mapView != null) {
				mapView.setCenterAndZoomLevel(coor, 15, true);
				placePin(true);
				favRecUsdListView.setVisibility(View.GONE);
				suggestionsListView.setVisibility(View.GONE);
				recentItemsListView.setVisibility(View.GONE);
				mapView.setVisibility(View.VISIBLE);
				bottomLayout.setVisibility(View.VISIBLE);
			}
		}
		
	}
	
   
	
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_FROM_MEDIA_LOCATION) {
			if (data != null && data.getData() != null) { 
				useMediaLocation(data.getData());	
			}
		}
	}

	public void onRequestTimeOut(NBIRequest arg0) {
		requestTimeOutDlg = new AppAlignedDialog(this, 0,
				R.string.error_dialog_title, R.string.request_timeout);
		Button continueButton = (Button) requestTimeOutDlg.findViewById(R.id.positive_button);
		requestTimeOutDlg.setCancelable(false);
		continueButton.setText(R.string.continue_save);
		continueButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Calling the initial Location fix request
				requestTimeOutDlg.dismiss();
				showLocSettingIfNecessary();
				Context context = AddLocationActivity.this;
				if (pd == null && showingDialog == false) {
					pd = new AppAlignedDialog(context,
							context.getString(R.string.processing_dialog_text), 
							context.getString(R.string.finding_user_location), true, false);
					pd.show();
					initializeGetUserLocation(true);
				}
			}
		});
		Button cancelButton = (Button) requestTimeOutDlg.findViewById(R.id.negative_button);
		cancelButton.setVisibility(View.VISIBLE);
		cancelButton.setText(R.string.ok);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				requestTimeOutDlg.dismiss();
				if (lastPin == null) {
					finish();
				}
			}
		});
		requestTimeOutDlg.show();
	}

	/**
	 * Create and store an image of the map view in the application temporary
	 * file space.
	 */
	private String createMapImage() {
		Context appContext = getApplicationContext();
		Bitmap image = null;
		try {
			image = createScaledMapImage();

			FileOutputStream imageFile = appContext.openFileOutput(
					MAP_IMAGE_FILENAME, Context.MODE_WORLD_READABLE);
			boolean result = image.compress(Bitmap.CompressFormat.JPEG, 100,
					imageFile);
			imageFile.close();

			if (result) {
				// we successfully wrote the image file, return it
				return appContext.getFileStreamPath(MAP_IMAGE_FILENAME)
						.getAbsolutePath();
			} else {
				// for some reason we weren't able to compress the image to a
				// jpeg
				Logger.error(getClass(),
						"createMapImage() - could not compress map image to JPEG.");
			}
		} catch (OutOfMemoryError oomException) {
			Logger.error(getClass(), oomException.getMessage(), oomException);
		} catch (Exception e) {
			// an exception was thrown when attempting to open the file output
			// stream
			Logger.error(getClass(),
					"createMapImage() - error writing out the map image file. Details: "
							+ e.getLocalizedMessage());
		} finally {
			if (image != null) {
				image.recycle();
			}
		}

		// if we get here there was a problem writing the image
		return null;
	}

	private Bitmap createScaledMapImage() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "calling - createScaledImage");
		}
		return mapView.createImage(0, 0, mapView.getWidth(),
				mapView.getHeight());
	}
	
	private void useMediaLocation(Uri mediaUri) {
		
		float[] latAndLon = LocationUtils.retrieveGeocoordinates(mediaUri, AddLocationActivity.this);

		if (latAndLon != null) {
			    imageUri = mediaUri.toString();
		        try {
					fromMediaImage = getThumbnail(mediaUri);
					if (fromMediaImage.getHeight() > fromMediaImage.getWidth()) {
						isPortrait = true;
					} else {
						isPortrait = false;
					}
				} catch (FileNotFoundException e) {
					Logger.error(e);
				} catch (IOException e) {
					Logger.error(e);
				}
				float lat = latAndLon[0];
				float lon = latAndLon[1];
				Location imageLoc = new Location();
				imageLoc.setLatitude(lat);
				imageLoc.setLongitude(lon);
				mapView.setCenterAndZoomLevel(new Coordinates(lat, lon), 15,
						false);
				lastPlace = new Place(imageLoc);
				placePin(true);
				ReverseGeocodeRequest rgr = new ReverseGeocodeRequest(
						nbiContext, lat, lon, false, new ReverseGeocodeHandler(
								lat, lon, lastPin));
				startRequest(rgr);
		}
	}

	@Override
	public void onPinBubbleDraw(MapView m, Canvas canvas, int pid, int x, int y) {
//		if (Logger.IS_DEBUG_ENABLED) {
//			Logger.debug(getClass(), "onPinBubbleDraw - starting");
//		}
		if (m == null) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onPinBubbleDraw - not doing anything");
			}
			return;
		}
		Pin currPin = m.getPin(pid);
		if (currPin == null || canvas == null) {
			return;
		}
		bubbleBackground.setBounds(bubbleRectangle.getLeft(),
				bubbleRectangle.getTop(), bubbleRectangle.getLeft()
						+ bubbleRectangle.getWidth(), bubbleRectangle.getTop()
						+ bubbleRectangle.getHeight());
		bubbleBackground.draw(canvas);
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		if (currPin.getTitle().equalsIgnoreCase(
				getString(R.string.processing_dialog_text))) {
			canvas.drawText(currPin.getTitle(), x - bubbleRectangle.getWidth()
					/ 2 + (19 * metrics.density),
					y - bubbleRectangle.getHeight() + (38 * metrics.density),
					processingPaint);
//			if (Logger.IS_DEBUG_ENABLED) {
//				Logger.debug(getClass(), "onPinBubbleDraw - returning1");
//			}
			return;
		}
		if (imageUri == null) {
			Bitmap b = null;
			starX = x - bubbleRectangle.getWidth() / 2
					+ Math.round(3 * metrics.density);
			starY = y - bubbleRectangle.getHeight() + (2 * metrics.density);
			if (beenTouched == null || !beenTouched.equalsIgnoreCase("yes")) {
				b = BitmapManager.INSTANCE.decodeResource(getResources(),
						R.drawable.list_favorites_star_off);
				canvas.drawBitmap(
						b,
						x - bubbleRectangle.getWidth() / 2
								+ Math.round(14 * metrics.density), y
								- bubbleRectangle.getHeight()
								+ (21 * metrics.density), new Paint());
			} else {
				b = BitmapManager.INSTANCE.decodeResource(getResources(),
						R.drawable.list_favorites_star_on);
				canvas.drawBitmap(
						b,
						x - bubbleRectangle.getWidth() / 2
								+ Math.round(14 * metrics.density), y
								- bubbleRectangle.getHeight()
								+ (21 * metrics.density), new Paint());
			}
			starWidth = b.getWidth();
			starHeight = b.getHeight();
			canvas.drawText(currPin.getTitle(), x - bubbleRectangle.getWidth()
					/ 2 + b.getWidth() + (19 * metrics.density), y
					- bubbleRectangle.getHeight() + (44 * metrics.density),
					title);
			canvas.drawText(currPin.getSubTitle(),
					x - bubbleRectangle.getWidth() / 2 + b.getWidth()
							+ (19 * metrics.density),
					y - bubbleRectangle.getHeight() + (60 * metrics.density),
					subtitle);
		} else {
			starX = x - bubbleRectangle.getWidth() / 2
					+ Math.round(14 * metrics.density);
			starY = y - bubbleRectangle.getHeight()
					+ fromMediaImage.getHeight() + (31 * metrics.density);
			Bitmap b = null;
			if (beenTouched == null || !beenTouched.equalsIgnoreCase("yes")) {
				b = BitmapManager.INSTANCE.decodeResource(getResources(),
						R.drawable.list_favorites_star_off);
				canvas.drawBitmap(
						b,
						x - bubbleRectangle.getWidth() / 2
								+ Math.round(14 * metrics.density),
						y - bubbleRectangle.getHeight()
								+ fromMediaImage.getHeight()
								+ (31 * metrics.density), new Paint());
			} else {
				b = BitmapManager.INSTANCE.decodeResource(getResources(),
						R.drawable.list_favorites_star_on);
				canvas.drawBitmap(
						b,
						x - bubbleRectangle.getWidth() / 2
								+ Math.round(14 * metrics.density),
						y - bubbleRectangle.getHeight()
								+ fromMediaImage.getHeight()
								+ (31 * metrics.density), new Paint());
			}
			starWidth = b.getWidth();
			starHeight = b.getHeight();
			canvas.drawText(currPin.getTitle(), x - bubbleRectangle.getWidth()
					/ 2 + b.getWidth() + (19 * metrics.density), y
					- bubbleRectangle.getHeight() + fromMediaImage.getHeight()
					+ (56 * metrics.density), title);
			canvas.drawText(
					currPin.getSubTitle(),
					x - bubbleRectangle.getWidth() / 2 + b.getWidth()
							+ (19 * metrics.density),
					y - bubbleRectangle.getHeight()
							+ fromMediaImage.getHeight()
							+ (70 * metrics.density), subtitle);
			canvas.drawBitmap(fromMediaImage,
					x - fromMediaImage.getWidth() / 2,
					y - bubbleRectangle.getHeight() + (32 * metrics.density),
					new Paint());
		}
//		if (Logger.IS_DEBUG_ENABLED) {
//			Logger.debug(getClass(), "onPinBubbleDraw - returning2");
//		}
	}

	@Override
	public Rectangle onPinBubbleGetRectangle(MapView mview, int pid, int x,
			int y) {
		if (lastPin == null || mview == null || multiTouch == true) {
			return null;
		}
		DisplayMetrics metrics = new DisplayMetrics();
//		if (Logger.IS_DEBUG_ENABLED) {
//			Logger.debug(getClass(), "onPinBubbleGetRectangle - got metrics1");
//		}
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
//		if (Logger.IS_DEBUG_ENABLED) {
//			Logger.debug(getClass(), "onPinBubbleGetRectangle - got metrics2");
//		}
		if (lastPin.getTitle().equalsIgnoreCase(
				getString(R.string.processing_dialog_text))) {
			processingPaint = new Paint();
			processingPaint.setColor(Color.BLACK);
			processingPaint.setTypeface(Typeface.DEFAULT_BOLD);
			processingPaint.setAntiAlias(true);
			processingPaint.setTextAlign(Align.LEFT);
			processingPaint.setTextSize(18 * metrics.density);
			Rect processingBounds = new Rect();
			processingPaint.getTextBounds(lastPin.getTitle(), 0, lastPin
					.getTitle().length(), processingBounds);
			bubbleRectangle = new Rectangle();
			bubbleRectangle.setWidth(processingBounds.width()
					+ Math.round((42 * metrics.density)));
			bubbleRectangle.setHeight(processingBounds.height()
					+ Math.round(42 * metrics.density));
			if (metrics.densityDpi <= DisplayMetrics.DENSITY_MEDIUM) {
				bubbleRectangle.setLeft(x - bubbleRectangle.getWidth() / 2
						+ Math.round(2 * metrics.density));
			} else {
				bubbleRectangle.setLeft(x - bubbleRectangle.getWidth() / 2
						- Math.round(1 * metrics.density));
			}
			bubbleRectangle.setTop(y - bubbleRectangle.getHeight()
					+ Math.round(7 * metrics.density));
			return bubbleRectangle;
		}

		title = new Paint();
		title.setColor(Color.BLACK);
		title.setTypeface(Typeface.DEFAULT);
		title.setAntiAlias(true);
		subtitle = new Paint();
		subtitle.setColor(Color.BLACK);
		subtitle.setTypeface(Typeface.DEFAULT);
		subtitle.setAntiAlias(true);
		subtitle.setTextSize(subTitleFontSize);
		if (lastPin.getTitle().startsWith(getString(R.string.lat))) {
			title.setTextSize(subTitleFontSize);
		} else {
			title.setTextSize(titleFontSize);
			title.setTypeface(Typeface.DEFAULT_BOLD);
		}
		title.setTextAlign(Align.LEFT);
		subtitle.setTextAlign(Align.LEFT);
		titleBounds = new Rect();
		title.getTextBounds(lastPin.getTitle(), 0, lastPin.getTitle().length(),
				titleBounds);
		subtitleBounds = new Rect();
		subtitle.getTextBounds(lastPin.getSubTitle(), 0, lastPin.getSubTitle()
				.length(), subtitleBounds);
		Bitmap star = BitmapManager.INSTANCE.decodeResource(getResources(),
				R.drawable.list_favorites_star_on);
//		if (Logger.IS_DEBUG_ENABLED) {
//			Logger.debug(getClass(), "onPinBubbleGetRectangle - 3");
//		}
		if (imageUri != null) {
			if (!isPortrait) {
				bubbleRectangle = new Rectangle();
				bubbleRectangle.setWidth(star.getWidth()
						+ Math.max(titleBounds.width(), subtitleBounds.width())
						+ Math.round((42 * metrics.density)));
				bubbleRectangle.setHeight(fromMediaImage.getHeight()
						+ Math.max(star.getHeight(), titleBounds.height()
								+ subtitleBounds.height())
						+ Math.round(47 * metrics.density));
				if (metrics.densityDpi <= DisplayMetrics.DENSITY_MEDIUM) {
					bubbleRectangle.setLeft(x - bubbleRectangle.getWidth() / 2
							+ Math.round(2 * metrics.density));
					bubbleRectangle.setTop(y - bubbleRectangle.getHeight()
							+ Math.round(7 * metrics.density));
				} else {
					bubbleRectangle.setLeft(x - bubbleRectangle.getWidth() / 2
							- Math.round(1 * metrics.density));
					bubbleRectangle.setTop(y - bubbleRectangle.getHeight() / 2
							- Math.round(59 * metrics.density));
				}

				return bubbleRectangle;
			} else {
				bubbleRectangle = new Rectangle();
				bubbleRectangle.setWidth(star.getWidth()
						+ Math.max(titleBounds.width(), subtitleBounds.width())
						+ Math.round((42 * metrics.density)));
				bubbleRectangle.setHeight(fromMediaImage.getHeight()
						+ Math.max(star.getHeight(), titleBounds.height()
								+ subtitleBounds.height())
						+ Math.round(53 * metrics.density));
				if (metrics.densityDpi <= DisplayMetrics.DENSITY_MEDIUM) {
					bubbleRectangle.setLeft(x - bubbleRectangle.getWidth() / 2
							+ Math.round(2 * metrics.density));
					bubbleRectangle.setTop(y - bubbleRectangle.getHeight()
							+ Math.round(7 * metrics.density));
				} else {
					bubbleRectangle.setLeft(x - bubbleRectangle.getWidth() / 2
							- Math.round(1 * metrics.density));
					bubbleRectangle.setTop(y - bubbleRectangle.getHeight() / 2
							- Math.round(77 * metrics.density));
				}
				return bubbleRectangle;
			}
		}
//		if (Logger.IS_DEBUG_ENABLED) {
//			Logger.debug(getClass(), "onPinBubbleGetRectangle - 4");
//		}
		bubbleRectangle = new Rectangle();
		bubbleRectangle.setWidth(star.getWidth()
				+ Math.max(titleBounds.width(), subtitleBounds.width())
				+ Math.round((48 * metrics.density)));
		bubbleRectangle.setHeight(Math.max(star.getHeight(),
				titleBounds.height() + subtitleBounds.height())
				+ Math.round(42 * metrics.density));
		if (metrics.densityDpi <= DisplayMetrics.DENSITY_MEDIUM) {
			bubbleRectangle.setLeft(x - bubbleRectangle.getWidth() / 2
					+ Math.round(2 * metrics.density));
		} else {
			bubbleRectangle.setLeft(x - bubbleRectangle.getWidth() / 2
					- Math.round(1 * metrics.density));
		}
		bubbleRectangle.setTop(y - bubbleRectangle.getHeight()
				+ Math.round(7 * metrics.density));
//		if (Logger.IS_DEBUG_ENABLED) {
//			Logger.debug(getClass(), "onPinBubbleGetRectangle - 5");
//		}
		return bubbleRectangle;
	}

	@Override
	public boolean onPinDragEnd(MapView arg0, int pid, Coordinates c) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onPinDragEnd");
		}
		dragging = false;
		if (lastPlace.getLocation().getLatitude() != c.getLatitude()
				|| lastPlace.getLocation().getLongitude() != c.getLongitude()) {
			imageUri = null;
		}
		Point newMapPosition = mapView.convertCoordindatesToMapXY(c);
		Coordinates mapCoords = mapView.convertMapXYToCoordinates(new Point(
				newMapPosition.getX(), newMapPosition.getY()));
		if (mapCoords != null) {
			Location l = new Location();
			l.setLatitude(mapCoords.getLatitude());
			l.setLongitude(mapCoords.getLongitude());
			lastPlace = new Place(l);
			placePin(false);
			mapView.mapShowLocation(false);
			mapView.mapUpdateLocation(mapView.getMapCenter(), 1, false, false, false);
			ReverseGeocodeRequest rgr = new ReverseGeocodeRequest(nbiContext,
					mapCoords.getLatitude(), mapCoords.getLongitude(), false,
					new ReverseGeocodeHandler(mapCoords.getLatitude(),
							mapCoords.getLongitude(), lastPin));
			startRequest(rgr);
		}
		return true;
	}

	@Override
	public boolean onPinDragRequest(MapView arg0, int arg1) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onPinDragRequest");
		}
		if (mapUrl != null) {
			return false;
		}
		dragging = true;
		return true;
	}

	@Override
	public boolean onPinDrop(MapView m, int x, int y) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onPinDrop");
		}
		if (mapUrl != null) {
			return true;
		}
		return false;
	}

	@Override
	public void onPinDropped(MapView m, int pid) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onPinDropped");
		}
		if (lastPlace == null || mapView == null) {
			return;
		}
		
		if (lastPlace.getLocation().getLatitude() != mapView.getPin(pid)
				.getPlace().getLocation().getLatitude()
				|| lastPlace.getLocation().getLongitude() != mapView
						.getPin(pid).getPlace().getLocation().getLongitude()) {
			imageUri = null;
		}
		lastPlace = mapView.getPin(pid).getPlace();
		lastSearched = lastPlace.getLocation();
		placePin(false);
	    mapView.mapShowLocation(false);
		mapView.mapUpdateLocation(mapView.getMapCenter(), 1, false, false, false);
		ReverseGeocodeRequest rgr = new ReverseGeocodeRequest(nbiContext,
				lastSearched.getLatitude(), lastSearched.getLongitude(), false,
				new ReverseGeocodeHandler(lastSearched.getLatitude(),
						lastSearched.getLongitude(), lastPin));
		startRequest(rgr);
	}

	@Override
	public void onPinSelected(MapView arg0, int arg1) {
		// TODO Auto-generated method stub
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onPinSelected");
		}
	}

	@Override
	public boolean onPinTouched(MapView arg0, int arg1, boolean arg2) {
		// TODO Auto-generated method stub
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onPinTouched");
		}
		return false;
	}

	@Override
	public void onPinTrafficBubbleTouched(MapView arg0, TrafficIncident arg1) {
		// TODO Auto-generated method stub
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onPinTrafficBubbleTouched");
		}

	}

	@Override
	public void locationUpdated(Location location) { // user current location
														// gotten
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(AddLocationActivity.class,
					"locationUpdated got called");
		}
		if (isFinishing()) {
			return;
		}
		boolean wasCalled = calledBack;
		calledBack = true;
		myCallback.removeCallbacks(runCallback);
		if (location != null) {
			if (location.getAccuracy() < 75) {
				try {
					locManager.stopReceivingFixes(this);
				} catch (LocationException e) {
					Logger.error(e);
				}
			}
			final Coordinates center = new Coordinates(location.getLatitude(),
					location.getLongitude());
			Util.setMapCenter(center);
			
			if (lastAccuracy > location.getAccuracy()) {
				lastAccuracy = location.getAccuracy();
				if (wasCalled == false) {
					try {
						if (location.getAccuracy() > 75) {
							locManager
									.startReceivingFixes(this);
							Toast.makeText(
									AddLocationActivity.this,
									getString(R.string.determining_acc_location),
									Toast.LENGTH_LONG).show();
						}
					} catch (LocationException e) {
						Logger.error(e);
					}
					setCenterAndZoom(center);
				}
				if (placePin == true) {
					lastPlace = new Place(location);
					placePin(true);
					ReverseGeocodeRequest rgr = new ReverseGeocodeRequest(
							nbiContext, location.getLatitude(),
							location.getLongitude(), false,
							new ReverseGeocodeHandler(location.getLatitude(),
									location.getLongitude(), lastPin));
					startRequest(rgr);
					setCenterAndZoom(center);
				}
				mapView.mapShowLocation(true);
				mapView.mapUpdateLocation(center, location.getAccuracy(),
						false, false, false);
			}
		} else {
			onLocationError(0);
		}
		if (pd != null) {
			pd.cancel();
			pd = null;
		}
	}

	private void startRequest(NBIRequest req) {
		req.startRequest();
		nbiReqs.add(req);
	}

	public void setCenterAndZoom(final Coordinates c) {
		if (mapView.getHeight() > 0) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "setCenterAndZoom do now");
			}
			mapView.setCenterAndZoomLevel(c, 15, true);
		} else {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "setCenterAndZoom do delayed");
			}
			zoomTo.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (mapView.getHeight() > 0) {
						mapView.setCenterAndZoomLevel(c, 15, true);
					} else {
						zoomTo.postDelayed(this, 250);
					}
				}
			}, 250);
		}
	}

	@Override
	public void onLocationError(int arg0) { // error getting user current
											// location
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onLocationError");
		}
		calledBack = true;
		if (pd != null) {
			pd.cancel();
			pd = null;
		}
		final AppAlignedDialog d = new AppAlignedDialog(this, 0,
				R.string.error_dialog_title, R.string.user_location_not_found);
		d.setCancelable(false);
		Button saveButton = (Button) d.findViewById(R.id.positive_button);
		saveButton.setText(R.string.ok);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
				if (lastPin == null) { 
					finish(); 
				}
				 
			}
		});
		d.show();
	}

	public boolean isAllLocationProviderEnabled(Context context) {
		ContentResolver res = context.getContentResolver();
		String loc = Secure.getString(res, Secure.LOCATION_PROVIDERS_ALLOWED);

		// The matrix of options has changed a few times. To make it easier if
		// changes are required later, the options have been placed into an
		// array.
		String[] locationOptionsMatrix = { LocationManager.GPS_PROVIDER };

		if (loc != null) {
			for (String option : locationOptionsMatrix) {
				if (!loc.contains(option)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public boolean isWifiEnabled() {
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		return wifi.isWifiEnabled();
	}

	public void enableWifi() {
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifi.setWifiEnabled(true);
	}
	
	public void dismissProgressDialag() {
		if (pd != null) {
			pd.cancel();
			pd = null;
		}
	}

	private void showLocSettingIfNecessary() {
		// Pop-up will come up every time the user makes a location request
		// when GPS/network provider or Wi-Fi setting is disabled
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (!isAllLocationProviderEnabled(getApplicationContext())) {
			if (shownLocationDialog == true) {
				// the dialog had been shown, and came back from Setting screen
				// without enable both GPS and network
			} else {
				showingDialog = true;
				// Set to true before go to Setting screen
				shownLocationDialog = true;
				locationDialog = createLocSettingDialog();
				locationDialog.show();
			}
		} else if (!isWifiEnabled() && sp.getBoolean(showWifiKey, true)) {
			showingDialog = true;
			shownLocationDialog = true;
			createWifiDialog().show();
		} else {
			shownLocationDialog = false;
			if (locationDialog != null) {
				locationDialog.cancel();
			}
		}
	}

	private Dialog createLocSettingDialog() {
		final AppAlignedDialog d = new AppAlignedDialog(this, 0,
				R.string.loc_setting_dialog_title,
				R.string.loc_setting_dialog_msg);
		d.setCancelable(false);
		Button saveButton = (Button) d.findViewById(R.id.positive_button);
		saveButton.setText(R.string.menu_preferences);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
				Intent intent = new Intent(
						Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivity(intent);
				finish();
			}
		});
		Button cancelButton = (Button) d.findViewById(R.id.negative_button);
		cancelButton.setText(R.string.no);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
				finish();
			}
		});
		d.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				finish();
			}
		});
		d.show();
		return d;
	}

	private Dialog createWifiDialog() {
		final AppAlignedDialog d = new AppAlignedDialog(this, 0,
				R.string.loc_setting_dialog_title,
				R.string.wifi_setting_dialog_msg);
		CheckBox cb = (CheckBox) d.findViewById(R.id.do_not_ask_again);
		cb.setVisibility(View.VISIBLE);
		final SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton b, boolean isChecked) {
				if (isChecked) {
					Editor e = sp.edit();
					e.putBoolean(showWifiKey, false);
					e.commit();
				} else {
					Editor e = sp.edit();
					e.putBoolean(showWifiKey, true);
					e.commit();
				}
			}
		});
		d.setCancelable(false);
		Button saveButton = (Button) d.findViewById(R.id.positive_button);
		saveButton.setText(R.string.auto_enable);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
				enableWifi();
				d.cancel();
				showingDialog = false;
				initializeGetUserLocation(true);
			}
		});
		Button cancelButton = (Button) d.findViewById(R.id.negative_button);
		cancelButton.setText(R.string.no);
		cancelButton.setVisibility(View.VISIBLE);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
				showingDialog = false;
				initializeGetUserLocation(true);
			}
		});
		d.show();
		return d;
	}

	@Override
	public void providerStateChanged(int newState) {

	}
    
	
	
	
     public void placePin(boolean moveTo) {
		fromPlacesAddress = null;
		fromPlacesContext = null;
		fromPlacesName = null;
		if (lastPlace == null) {
			return;
		}
		lastPin = new Pin(Pin.PIN_BLUE, lastPlace);
		double latCordinate = (double) Math.round(lastPlace.getLocation()
				.getLatitude() * 100) / 100;
		double longCordinate = (double) Math.round(lastPlace.getLocation()
				.getLongitude() * 100) / 100;
		if(Logger.IS_DEBUG_ENABLED){
			Logger.debug(getClass(), "placepin(): title " + lastPin.getTitle()
					+ " subtitle " + lastPin.getSubTitle() + " ,city :"
					+ lastPlace.getLocation().getCity() + ",state: "
					+ lastPlace.getLocation().getState()
					+ ",postalCode: "+ lastPlace.getLocation().getPostalCode());
		}
		if (lastPin.getSubTitle().length() > 0
				&& lastPlace.getLocation().getState().length() > 0) {
			// if it has reverse geocode info to put and only a subtitle, put
			// the info as the sub and the sub as the title.
			if (lastPin.getTitle().length() > 0) {
				String subTitleString = lastPin.getSubTitle();
				String titleString = lastPin.getTitle();
				lastPin.setTitle(titleString);
				if (titleString.equalsIgnoreCase(subTitleString)) {
					lastPin.setSubTitle(lastPlace.getLocation().getCity() + ", "
							+ lastPlace.getLocation().getState() + ", "
							+ lastPlace.getLocation().getPostalCode());
				} else {
					if (lastPin.getSubTitle().contains(lastPlace.getLocation().getCity()) && lastPin.getSubTitle().contains(lastPlace.getLocation().getState())) {
						lastPin.setSubTitle(lastPlace.getLocation().getCity() + ", "
								+ lastPlace.getLocation().getState() + ", "
								+ lastPlace.getLocation().getPostalCode());
					}
					else {
						lastPin.setSubTitle(lastPin.getSubTitle() + ", "+lastPlace.getLocation().getCity() + ", "
								+ lastPlace.getLocation().getState() + ", "
								+ lastPlace.getLocation().getPostalCode());
					}
				}
			} else {
				lastPin.setTitle(lastPin.getSubTitle());
				lastPin.setSubTitle(lastPlace.getLocation().getCity() + ", "
						+ lastPlace.getLocation().getState() + ", "
						+ lastPlace.getLocation().getPostalCode());
			}
			
		} else if (lastPin.getTitle().length() > 0
				&& lastPlace.getLocation().getState().length() > 0) {
			// if it has reverse geocode info to put and only a title, put the
			// subtitle to the info.
			//To avoid unnecessary commas we do this check 
			if (lastPlace.getLocation().getCity().length() > 0) {
				lastPin.setSubTitle(lastPlace.getLocation().getCity() + ", "
						+ lastPlace.getLocation().getState() + ", "
						+ lastPlace.getLocation().getPostalCode());
			} else {
				lastPin.setSubTitle(lastPlace.getLocation().getState() + ((lastPlace.getLocation().getPostalCode().length()>0) ?
						", "+ lastPlace.getLocation().getPostalCode():""));	
			}
			
		} else if ((lastPin.getTitle().length() < 1 && lastPin.getSubTitle()
				.length() < 1)
				|| (lastPin.getTitle().equalsIgnoreCase("Info not available"))) {

			lastPin.setTitle(getString(R.string.latitude) + latCordinate);
			lastPin.setSubTitle(getString(R.string.longitude) + longCordinate);
		}
		updateBeenPressed();

		synchronized (mapView) { // remove all other pings and place this one

			if (mapView.getPinCount() > 0) {
				mapView.removeAllPins();
			}
			mapView.addPin(lastPin, true, moveTo);
			
		}
	}

	public void placePin(String title, String subtitle) {
		if (lastPlace == null) {
			return;
		}
		lastPin = new Pin(Pin.PIN_BLUE, lastPlace);
		lastPin.setTitle(title);
		lastPin.setSubTitle(subtitle);

		updateBeenPressed();

		synchronized (mapView) { // remove all other pings and place this one

			if (mapView.getPinCount() > 0) {
				mapView.removeAllPins();
			}  
			mapView.addPin(lastPin, true, true);
		}
	}

	@Override
	protected void onDestroy() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onDestroy: nbiReqs = " + nbiReqs);
		}
		super.onDestroy();

		if (requestTimeOutDlg != null) {
			requestTimeOutDlg.cancel();
			requestTimeOutDlg = null;
		}

		lbsManager.destroy(this);
		placesList.destroy();

		// since NBI keeps a persistent reference to us we need to null out our refs
		// TODO remove (most of) these once NBI is fixed
		lbsManager = null;
		locManager = null;
		nbiContext = null;
		placesList = null;
		
		if (mapView != null) {
			mapView.setOnTouchListener(null);
			mapView = null;
		}
		
		mapConfiguration = null;

		for (NBIRequest req : nbiReqs) {
			try {
				req.cancelRequest();
			}
			catch (Throwable t) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.error(getClass(), t);
				}
			}
		}
		nbiReqs.clear();

		bottomLayout = null;
		bubbleBackground = null;
		bubbleStar = null;
		locationDialog = null;
		fromPlacesAddress = null;
		fromPlacesName = null;
		fromPlacesContext = null;
		processingPaint = null;
		beenTouched = null;
		bubbleRectangle = null;
		mapConfiguration = null;
		lastSearched = null;
		lastPin = null;
		favIcon = null;
		titleBounds = null;
		subtitleBounds = null;
		lastPlace = null;
		imageUri = null;
		fromMediaImage = null;
		title = null;
		subtitle = null;
		
		if (searchButt != null) {
			searchButt.setOnClickListener(null);
			searchButt = null;
		}
		
		mapUrl = null;
		
		if (findPin != null) {
			findPin.setOnClickListener(null);
			findPin = null;
		}
		if (zoomIn != null) {
			zoomIn.setOnClickListener(null);
			zoomIn = null;
		}
		
		if (zoomOut != null) {
			zoomOut.setOnClickListener(null);
			zoomOut = null;
		}
		
		if (attachButton != null) {
			attachButton.setOnTouchListener(null);
			attachButton = null;
		}
		
		favRecUsdListView = null;
		pd = null;
		
		if (searchField != null) {
			searchField.setOnTouchListener(null);
			searchField = null;
		}
		
		recentItemsListAdapter = null;
		recentItemsList = null;
		suggestionsListAdapter = null;
		suggestionsList = null;
		mSuggestionSearchRequest = null;
		mSuggestionSearchInfo = null;
		recentItemsListView = null;
		suggestionsListView = null;
		requestTimeOutDlg = null;
		zoomTo = null;
		myCallback = null;
		runCallback = null;
		mSuggestionSearchListener = null;
	}

	public Bitmap getThumbnail(Uri uri) throws FileNotFoundException,
			IOException {
		InputStream input = this.getContentResolver().openInputStream(uri);

		BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
		onlyBoundsOptions.inJustDecodeBounds = true;
		onlyBoundsOptions.inDither = true; // optional
		onlyBoundsOptions.inPreferredConfig = Bitmap.Config.RGB_565;
		BitmapManager.INSTANCE.decodeStream(input, null, onlyBoundsOptions);
		input.close();
		if ((onlyBoundsOptions.outWidth == -1)
				|| (onlyBoundsOptions.outHeight == -1)) {
			return null;
		}

		int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight
				: onlyBoundsOptions.outWidth;

		double ratio = (originalSize > 70) ? (originalSize / 70) : 1.0;

		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
		bitmapOptions.inDither = true; // optional
		bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
		input = this.getContentResolver().openInputStream(uri);
		Bitmap bitmap = BitmapManager.INSTANCE.decodeStream(input, null,
				bitmapOptions);
		input.close();
		return bitmap;
	}

	private static int getPowerOfTwoForSampleRatio(double ratio) {
		int k = Integer.highestOneBit((int) Math.floor(ratio));
		if (k == 0)
			return 1;
		else
			return k;
	}

	/**
	 * This listener is for looking up addresses based on latitude and
	 * longitude.
	 */
	private class ReverseGeocodeHandler implements ReverseGeocodeListener {
		private final double latitude;
		private final double longitude;
		private final Pin myPin;

		public ReverseGeocodeHandler(double latitude, double longitude,
				Pin myPin) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "constructed");
			}

			this.latitude = latitude;
			this.longitude = longitude;
			this.myPin = myPin;
			myPin.setTitle(getString(R.string.processing_dialog_text));
			bubbleStar = null;
			myPin.setSubTitle("");
		}

		@Override
		public void onReverseGeocode(ReverseGeocodeInformation information,
				ReverseGeocodeRequest request) {

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onReverseGeocode");
			}

			if (!dragging) {
				MapLocation returnedInfo = information.getMapLocation();
				if (returnedInfo == null) {
					return;
				}

				returnedInfo.setLatitude(latitude);
				returnedInfo.setLongitude(longitude);

				lastPlace = new Place();
				lastPlace.setLocation(returnedInfo);
				if (returnedInfo.getState().length() > 0) {
					myPin.setTitle(returnedInfo.getAddress());
					myPin.setSubTitle(returnedInfo.getCity() + ", "
							+ returnedInfo.getState() + ", "
							+ returnedInfo.getPostalCode());
				} else {
					myPin.setTitle("");
					myPin.setSubTitle("");
				}
				placePin(false);
			}
		}

		@Override
		public void onRequestCancelled(NBIRequest request) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onRequestCancelled");
			}
			nbiReqs.remove(request);
		}

		@Override
		public void onRequestComplete(NBIRequest request) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onRequestComplete");
			}
			nbiReqs.remove(request);
		}

		@Override
		public void onRequestError(NBIException exception, NBIRequest request) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onRequestError");
			}
			if (!dragging) {
				MapLocation returnedInfo = new MapLocation();

				returnedInfo.setLatitude(latitude);
				returnedInfo.setLongitude(longitude);

				lastPlace = new Place();
				lastPlace.setLocation(returnedInfo);
				if (returnedInfo.getState().length() > 0) {
					myPin.setTitle(returnedInfo.getAddress());
					myPin.setSubTitle(returnedInfo.getCity() + ", "
							+ returnedInfo.getState() + ", "
							+ returnedInfo.getPostalCode());
				} else {
					myPin.setTitle("");
					myPin.setSubTitle("");
				}
				placePin(false);
			}
			nbiReqs.remove(request);
		}

		@Override
		public void onRequestProgress(int percentage, NBIRequest request) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onRequestProgress");
			}
		}

		@Override
		public void onRequestStart(NBIRequest request) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onRequestStart");
			}
		}

		@Override
		public void onRequestTimeOut(NBIRequest request) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onRequestTimeOut");
			}
			nbiReqs.remove(request);
		}
	}

	// Displays no result dialog.
	public void displayNoResultsDialog(String searchToken) {
		// show the no results found dialog
        
		String noResultsString;
		if ((searchToken != null) && (searchToken.trim().length() > 0)) {
			String formatString = getString(R.string.no_results);
			noResultsString = String.format(formatString, searchToken);
		} else {
			noResultsString = getString(R.string.no_results);
		}
		final AppAlignedDialog d = new AppAlignedDialog(this,
				R.drawable.dialog_alert, R.string.error, noResultsString);
		Button saveButton = (Button) d.findViewById(R.id.positive_button);
		saveButton.setText(R.string.ok);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
			}
		});
		d.show();
	}

	private void updateBeenPressed() {
		Cursor cursor = null;
		try {
			if (fromPlacesAddress != null) {
				if (fromPlacesAddress.startsWith(getString(R.string.latwithcolon))) {
					cursor = getContentResolver().query(
							PlacesTabActivity.CACHE_URI,
							null,
							AddressHelper.ADDRESS + "=? AND "
									+ AddressHelper.FAVORITE + "=1",
							new String[] { fromPlacesAddress }, null);
				} else if (fromPlacesName != null) {
					cursor = getContentResolver().query(
							PlacesTabActivity.CACHE_URI,
							null,
							AddressHelper.NAME + "=? AND "
									+ AddressHelper.CITYSTATEZIP + "=? AND "
									+ AddressHelper.ADDRESS + "=? AND "
									+ AddressHelper.FAVORITE + "=1",
							new String[] { fromPlacesName, fromPlacesContext,
									fromPlacesAddress }, null);
				} else {
					cursor = getContentResolver()
							.query(PlacesTabActivity.CACHE_URI,
									null,
									AddressHelper.CITYSTATEZIP + "=? AND "
											+ AddressHelper.ADDRESS + "=? AND "
											+ AddressHelper.FAVORITE + "=1",
									new String[] { fromPlacesContext,
											fromPlacesAddress }, null);
				}
				if (cursor != null && cursor.getCount() != 0) {
					beenTouched = "yes";
				} else {
					beenTouched = "no";
				}
			} else {
				cursor = getContentResolver().query(
						PlacesTabActivity.CACHE_URI,
						null,
						AddressHelper.ADDRESS + "=? AND "
								+ AddressHelper.CITYSTATEZIP + "=? AND "
								+ AddressHelper.FAVORITE + "=1",
						new String[] {
								lastPin.getTitle(),
								lastPlace.getLocation().getCity()
										+ ", "
										+ lastPlace.getLocation().getState()
										+ ", "
										+ lastPlace.getLocation()
												.getPostalCode() }, null);

				if (cursor.getCount() != 0) {
					beenTouched = "yes";
				} else {
					beenTouched = "no";
				}
			}
		} catch (Exception e) {
			Logger.error(e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	

	private class SuggestionListAdapter extends ArrayAdapter<SuggestMatchItem> {

		public SuggestionListAdapter(List<SuggestMatchItem> list) {
			super(getApplicationContext(), R.layout.suggestions_list_item, list);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			SuggestionViewHolder holder;
			SuggestMatchItem suggest = getItem(position);
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(
						R.layout.suggestions_list_item, null);
				holder = new SuggestionViewHolder();
				holder.line1 = (TextView) convertView
						.findViewById(R.id.sug_line1);
				holder.line2 = (TextView) convertView
						.findViewById(R.id.sug_line2);
				convertView.setTag(holder);
			} else {
				holder = (SuggestionViewHolder) convertView.getTag();
			}

			holder.line1.setText(suggest.getLine1());
			String line2Str = suggest.getLine2();

			if (line2Str != null && line2Str.length() > 0) {
				holder.line2.setText(suggest.getLine2());
				holder.line2.setVisibility(View.VISIBLE);
				holder.line1.setLines(1);
			} else {
				holder.line2.setVisibility(View.GONE);
				holder.line1.setLines(2);
				holder.line1.setGravity(Gravity.CENTER_VERTICAL);
			}
			return convertView;
		}
	}

	private class RecentListAdapter extends ArrayAdapter<RecentSearchItem> {

		public RecentListAdapter(List<RecentSearchItem> list) {
			super(getApplicationContext(), R.layout.suggestions_list_item, list);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			SuggestionViewHolder holder;
			// View itemView = convertView;
			RecentSearchItem suggest = getItem(position);
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(
						R.layout.suggestions_list_item, null);
				holder = new SuggestionViewHolder();
				holder.line1 = (TextView) convertView
						.findViewById(R.id.sug_line1);
				holder.line2 = (TextView) convertView
						.findViewById(R.id.sug_line2);
				convertView.setTag(holder);
			} else {
				holder = (SuggestionViewHolder) convertView.getTag();
			}

			holder.line1.setText(suggest.getLine1());
			String line2Str = suggest.getLine2();

			if (line2Str != null && line2Str.length() > 0) {
				holder.line2.setText(suggest.getLine2());
				holder.line2.setVisibility(View.VISIBLE);
				holder.line1.setLines(1);
			} else {
				holder.line2.setVisibility(View.GONE);
				holder.line1.setLines(2);
				holder.line1.setGravity(Gravity.CENTER_VERTICAL);
			}
			return convertView;
		}

	}

	private class SuggestionViewHolder {
		TextView line1;
		TextView line2;
	}

	private SuggestionSearchListener mSuggestionSearchListener = new SuggestionSearchListener() {
		@Override
		public void onSuggestionSearch(SuggestionSearchInformation info,
				SuggestionSearchRequest request) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onSuggestionSearch called");
			}
			
			if (suggestionsList != null) {
				suggestionsList.clear();
				for (int index = 0; index < info.getResultCount(); index++) {
					SuggestMatch match = info.getSuggestMatch(index);
					SuggestMatchItem newItem = new SuggestMatchItem(match);
					suggestionsList.add(newItem);
				}
				// force the UI to update
				suggestionsListAdapter.notifyDataSetChanged();
				showSuggestionList(true);
				// save the info
				mSuggestionSearchInfo = info;
			}
		}

		@Override
		public void onRequestCancelled(NBIRequest request) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onRequestCancelled called" + request);
			}
		}

		@Override
		public void onRequestComplete(NBIRequest request) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onRequestComplete called" + request);
			}
		}

		@Override
		public void onRequestError(NBIException e, NBIRequest request) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onRequestError called" + request);
			}
		}

		@Override
		public void onRequestProgress(int progress, NBIRequest request) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onRequestProgress called" + request);
			}
		}

		@Override
		public void onRequestStart(NBIRequest request) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onRequestStart called" + request);
			}
		}

		@Override
		public void onRequestTimeOut(NBIRequest request) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onRequestTimeOut called" + request);
			}
		}
	};

	// Clears the search field
	public void clearSearchField(boolean clearText) {
		if (clearText) {
			searchField.setText("");

		}
		searchField.clearFocus();
		showSuggestionList(false);
	}

	private void showSuggestionList(boolean show) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "showSuggestionList:" + show);
		}
		
		View layout = findViewById(R.id.search_info);
		if (show) {
			if (searchField.getText().length() == 0) {
				recentItemsListView.setVisibility(View.VISIBLE);
				suggestionsListView.setVisibility(View.GONE);
			} else {
				recentItemsListView.setVisibility(View.GONE);
				suggestionsListView.setVisibility(View.VISIBLE);
			}
//			mapView.setVisibility(View.GONE);
			bottomLayout.setVisibility(View.GONE);
//			layout.setBackgroundColor(0xFF000000); // set the background to opaque black
		
		} else {
			recentItemsListView.setVisibility(View.GONE);
			suggestionsListView.setVisibility(View.GONE);
//			mapView.setVisibility(View.VISIBLE);
			bottomLayout.setVisibility(View.VISIBLE);
//			layout.setBackgroundColor(0xCCCCCCCC); // set the background to semi-transparent light gray
		}
	}

	// does a single search on the search field
	public void doSearch(String searchString) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "doSearch:" + searchString);
		}
		
		// get the string to use for the search query
		searchField.requestFocus();
		
		updateRecentSearchList(new RecentSearchItem(searchString, null));
		// hide the keyboard
		hideSoftKeyboard(searchField.getWindowToken());
		// stop any suggestion search
//		if (mSuggestionSearchRequest != null)
//			mSuggestionSearchRequest.cancelRequest();
		// do the search
		placesList.doSearch(PlacesList.SINGLE_SEARCH, searchString,
				mapView.getMapCenter(), this);
	}

	private void updateRecentSearchList(RecentSearchItem searchItem) {
		// do not allow empty search string
		if (searchItem == null || searchItem.getLine1() == null
				|| searchItem.getLine1().length() == 0)
			return;
		// first, remove the search string from the list if already present
		Iterator<RecentSearchItem> i = recentItemsList.iterator();
		while (i.hasNext()) {
			RecentSearchItem item = i.next();
			if (item.equals(searchItem)) {
				recentItemsList.remove(item);
				break;
			}
		}
		recentItemsList.add(0, searchItem);
		// keep the list size at maximum of MAX_RECENT_SEARCHES
		if (recentItemsList.size() > MAX_RECENT_SEARCHES)
			recentItemsList.remove(MAX_RECENT_SEARCHES);
		// force the UI to update
		recentItemsListAdapter.notifyDataSetChanged();
		// update the shared prefs
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor ed = prefs.edit();
		try {
			ed.putString("recent_search",
					ObjectSerializer.serialize(recentItemsList));
		} catch (IOException e) {
			// TODO Auto-generated catch block
            Logger.error(e);
		}
		ed.commit();

	}

	private void doSearchForRecent(RecentSearchItem item) {
		byte[] placeArray = item.getPlaceByteArray();
		if (placeArray != null) {
			// Sub search recent with serialized result - do not require
			// additional search request
			POI poi = new POI(new Place(placeArray), item.getPOIDistance());
			placesList.restoreRecentSubSearchPlace(poi);
		} else {
			// Single search or sub search without serialized place - send
			// search request
			placesList.doSearch(PlacesList.SINGLE_SEARCH, item.getLine1(),
					mapView.getMapCenter(), this);
		}
		hideSoftKeyboard(searchField.getWindowToken());
	}

	// a subsearch is a search based on an item in the suggestions list
	private void doSubSearch(int position) {
		try {
			locManager.cancelGetLocation(this);
			locManager.stopReceivingFixes(this);
		} catch (LocationException e) {
			
		}
		// put the search string into the search field
		// mDontShowSuggestions = true;
		SuggestMatchItem sugItem = suggestionsList.get(position);
		String searchAddress = sugItem.getLine1() + "," + sugItem.getLine2();
		//searchField.setText(searchAddress);
		updateRecentSearchList(new RecentSearchItem(sugItem.getLine1(), sugItem.getLine2()));
		// hide the keyboard
		hideSoftKeyboard(searchField.getWindowToken());
		// do the search
		if (sugItem.getLine2().equals("")) {
			doSearch(searchField.getText().toString());
		} else {
			placesList.doSingleSubSearch(searchAddress, mapView.getMapCenter(),
					AddLocationActivity.this, mSuggestionSearchInfo, position);
		}
	
	}

	//Displays search process dialog
	public void displaySearchingDialog(final NBIRequest request, String searchToken) {
			// show the Please Wait dialog
			String waitString;
			if ((searchToken != null) && (searchToken.trim().length() > 0)) {
				String formatString = getString(R.string.please_wait_token);
				waitString = String.format(formatString, searchToken);
			} else {
				waitString = getString(R.string.please_wait);
			}
			pd = new AppAlignedDialog(this,
					getString(R.string.processing_dialog_text), 
					waitString, true, true);
			
			pd.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					request.cancelRequest();
				}
			});
			pd.show();
	}
	
	
	
	public void addPlaceToRecentItem(POI poi) {
	  if (recentItemsList != null && poi != null) {
		if (!recentItemsList.isEmpty()) {
			RecentSearchItem item = recentItemsList.get(0);
			Place place = poi.getPlace();
			if (place != null) {
				item.setPlaceByteArray(place.toByteArray());
			}
			item.setPOIDistance(poi.getDistance());
			// update the shared prefs
			SharedPreferences.Editor ed = prefs.edit();
			try {
				ed.putString("recent_search",
						ObjectSerializer.serialize(recentItemsList));
			} catch (IOException e) {
				// TODO Auto-generated catch block
                Logger.error(e);
			}
			ed.commit();
		}
	}
		// TODO :Need to check
		if (prefs.contains("recent_search")) {
			try {
				ArrayList<RecentSearchItem> deserialize = (ArrayList<RecentSearchItem>) ObjectSerializer
						.deserialize(prefs.getString(
								"recent_search",
								ObjectSerializer
										.serialize(new ArrayList<RecentSearchItem>())));
				recentItemsList = deserialize;
				recentItemsListAdapter = new RecentListAdapter(recentItemsList);
				recentItemsListView.setAdapter(recentItemsListAdapter);
			} catch (IOException e) {
				// TODO Auto-generated catch block
                Logger.error(e);
			}
		}

	}

	private void hideSoftKeyboard(IBinder binder) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(binder, 0);
	}
	
	

}
