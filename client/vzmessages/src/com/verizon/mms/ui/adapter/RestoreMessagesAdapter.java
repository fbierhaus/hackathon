package com.verizon.mms.ui.adapter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Video.Thumbnails;
import android.provider.Telephony.Sms;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.BaseAdapter;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ContentType;
import com.verizon.mms.model.SmilHelper;
import com.verizon.mms.pdu.CharacterSets;
import com.verizon.mms.ui.ListDataWorker;
import com.verizon.mms.ui.ListDataWorker.ListDataJob;
import com.verizon.mms.ui.MessageUtils;
import com.verizon.mms.ui.widget.ImageViewCorrected;
import com.verizon.mms.util.BitmapManager;
import com.verizon.mms.util.MemoryCacheMap;
import com.verizon.mms.util.SmileyParser;
import com.verizon.vcard.android.syncml.pim.VDataBuilder;
import com.verizon.vcard.android.syncml.pim.VNode;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct.ContactMethod;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct.OrganizationData;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct.PhoneData;
import com.verizon.vcard.android.syncml.pim.vcard.VCardException;
import com.verizon.vcard.android.syncml.pim.vcard.VCardParser;
import com.verizon.vzmsgs.saverestore.BackUpMessage;

public class RestoreMessagesAdapter extends BaseAdapter {

	protected LayoutInflater mInflater;
	private View imageLayout;
	private View textLayout;
	private MemoryCacheMap<Long, Bitmap> cacheImages;
	private ListDataWorker bitMapWorker;
	private final float VIEW_QUEUE_FACTOR = 2.5f;  // size of queue relative to number of views
	private final int QUEUE_IMAGES = 0;
	private final int MIN_VIEWS = 10;
	private final int MIN_QUEUE_SIZE = (int)(MIN_VIEWS * VIEW_QUEUE_FACTOR);
	private ArrayList<BackUpMessage> bMessages;
	private Context mContext;
	private Resources res;
	private boolean isGroup;
	private static final long TIMESTAMP_GAP_MAX = 300000;
	private int minImageMessageHeight;
	private int maxImageMessageHeight;
	private int minImageBorder;
	private int maxImageBorder;
	private int minMessageWidth;
	private static int[] minMessageWidths = new int[2];
	private Rect imageDimensions;
	private int maxMessageBorder;
	private final String parentDirPath; 
	private ListView listView;
	private Bitmap imagePlaceHolder;
	private Bitmap videoPlaceHolder;
	private Bitmap audioPlaceHolder;
	private Bitmap nameCardPlaceHolder;
	private Bitmap locationPlaceHolder;
	private final int PHONE_NUM_LIMIT = 3;
	private static final float MIN_MESSAGE_WIDTH_RATIO = 295f / 614f;
	private static final float IMAGE_WIDTH_HEIGHT_RATIO = 1.5f; 
	private static final float IMAGE_SCALE_LIMIT = 4.0f; 

	private static final int TYPE_TEXT = 0;
	private static final int TYPE_AUDIO = 1;
	private static final int TYPE_VIDEO = 2;
	private static final int TYPE_IMAGE = 4;
	private static final int TYPE_LOCATION = 8;
	private static final int TYPE_VCARD = 16;
	private static final int TYPE_SLIDESHOW = 32;



	private static class ViewMsgData {
		private TextView timeStamp;
		private View message;
		private View bubble;
		private TextView msgText;
		private ImageView errorIcon;
		private TextView groupModeView;
		// MMS View Variables
		private RelativeLayout mmsLayout;
		private ImageViewCorrected imageOrVideoView;
		private ImageView playBtn;
		private ProgressBar progress;
		private View vcardView;
		private TextView contactText;
		private ImageView contactPicture;
		private ViewGroup content;
	}


	class MessageItem {
		String text;
		String locVcardText;
		int type;
		String path;
	}

	/**
	 * 
	 * Constructor
	 */
	public RestoreMessagesAdapter(Context context, ArrayList<BackUpMessage> items, ListView listView, String rootPath, boolean isGroup) {
		super();
		this.mContext = context;
		this.bMessages = items;
		this.listView = listView;
		this.isGroup = isGroup;
		
		mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		res = mContext.getResources();
		imagePlaceHolder = BitmapFactory.decodeResource(res, R.drawable.ic_missing_thumbnail_picture);
		videoPlaceHolder = BitmapFactory.decodeResource(res, R.drawable.ic_missing_thumbnail_video);
		audioPlaceHolder = BitmapFactory.decodeResource(res, R.drawable.ic_missing_thumbnail_audio);
		nameCardPlaceHolder = BitmapFactory.decodeResource(res, R.drawable.list_namecard);
		locationPlaceHolder = BitmapFactory.decodeResource(res, R.drawable.attach_location);
		
		File file = new File(rootPath);
		parentDirPath = file.getParent();
		onConfigChanged(res.getConfiguration());

		createBitMapWorker();
	}
	
	
	private void createBitMapWorker() {
		cacheImages = new MemoryCacheMap<Long, Bitmap>(25);
		bitMapWorker = new ListDataWorker();
		bitMapWorker.addQueue(handler, QUEUE_IMAGES, 1, MIN_QUEUE_SIZE, null);
	    bitMapWorker.start();	
	}
	
	public void closeAdapter() {
		if (cacheImages != null && bitMapWorker != null) {
			cacheImages.clear();
			bitMapWorker.exit();
		}
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

	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return bMessages.size();
		
	}

	@Override
	public Object getItem(int pos) {
		// TODO Auto-generated method stub
		return bMessages.get(pos);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	//TODO: store the parsed msgItems in a cache so that we dont have through 
	//the same cycle again and again
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewMsgData msgData = null;
		ViewGroup content = null;
		View msg_itemView = null;
		
		final LayoutInflater inflator = mInflater;
		View message = null;
		final SmileyParser parser = SmileyParser.getInstance();
		
		if (convertView == null) {
			msgData = new ViewMsgData();
			
			convertView = inflator.inflate(R.layout.rmsg_item, parent, false);
			msgData.content = content = (ViewGroup) convertView
					.findViewById(R.id.content);

			msg_itemView = inflator.inflate(R.layout.restore_msg_item,
					content, true);
			//Placing all views here
			textLayout = msg_itemView.findViewById(R.id.textLayout);
			imageLayout = msg_itemView.findViewById(R.id.mms_view);
			content.setMinimumWidth(minMessageWidth);
			message = convertView.findViewById(R.id.message);
			msgData.timeStamp = (TextView) convertView
					.findViewById(R.id.timestamp);
			msgData.groupModeView = (TextView) convertView
					.findViewById(R.id.groupModeChange);
			msgData.message = convertView.findViewById(R.id.message);
			msgData.bubble = convertView.findViewById(R.id.bubble);

			msgData.errorIcon = (ImageView) convertView
					.findViewById(R.id.error);
			msgData.msgText = (TextView) content.findViewById(R.id.text);
			msgData.mmsLayout = (RelativeLayout) content
					.findViewById(R.id.mms_view);
			msgData.playBtn = (ImageView) content.findViewById(R.id.playBtn);
			msgData.imageOrVideoView = (ImageViewCorrected) content
					.findViewById(R.id.image_view);
			msgData.progress = (ProgressBar) content
					.findViewById(R.id.progress);
			msgData.vcardView = (View) content.findViewById(R.id.vcardView);
			msgData.contactText = (TextView) content
					.findViewById(R.id.contactText);
			msgData.contactPicture = (ImageView) content
					.findViewById(R.id.contactPicture);
			msgData.timeStamp.setVisibility(View.GONE);

			msgData.errorIcon.setVisibility(View.GONE);
			convertView.setTag(msgData);
			//
		} else {
			content = (ViewGroup) convertView
					.findViewById(R.id.content);
			message = convertView.findViewById(R.id.message);
			msg_itemView = convertView.findViewById(R.id.textLayout);
			textLayout = convertView.findViewById(R.id.textLayout);
			imageLayout = msg_itemView.findViewById(R.id.mms_view);
			imageLayout.setBackgroundDrawable(null);
			msgData = (ViewMsgData) convertView.getTag();
			msgData.content = content;
		}
		msgData.imageOrVideoView.setImageBitmap(null);
		msgData.groupModeView.setVisibility(View.GONE);
		msgData.groupModeView.setText(null);
		msgData.imageOrVideoView.setVisibility(View.GONE);
		msgData.playBtn.setVisibility(View.GONE);
		msgData.msgText.setVisibility(View.GONE);
		msgData.mmsLayout.setVisibility(View.GONE);
		msgData.vcardView.setVisibility(View.GONE);



		final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) msgData.message
				.getLayoutParams();
		int resId;
		BackUpMessage item = (BackUpMessage) bMessages.get(position);
		HashMap<String, String> pdu = item.getPduData();

		showTimeStamp(msgData, position);
		
		boolean isOutGoing = false;

		msgData.msgText.setText("");
		if (item.isSms()) {
			isOutGoing = Sms.isOutgoingFolder(Integer.valueOf(pdu
					.get("type")));
			msgData.groupModeView.setVisibility(View.GONE);
			msgData.msgText.setVisibility(View.VISIBLE);
			String body = pdu.get("body");
			
			if (body != null) {
				body = SmilHelper.removeEscapeChar(body);
				msgData.msgText.setText(parser.addSmileySpans(body, false));
			}
			setImageBitmap(msgData.imageOrVideoView, null, false);
		} else {
			isOutGoing = Integer.valueOf(pdu.get("m_type")) == 128 ? true
					: false;

			SpannableStringBuilder sub = new SpannableStringBuilder();
			
			if (item.getPartsData() != null) {
				MessageItem msgItem = getMessageItem(item);
				
				if ((msgItem.type & TYPE_LOCATION) != 0) {
					msgData.vcardView.setVisibility(View.VISIBLE);
					msgData.mmsLayout.setVisibility(View.VISIBLE);
					if (msgItem.locVcardText != null) {
						msgData.contactText.setVisibility(View.VISIBLE);
						msgData.contactText.setText(msgItem.locVcardText);
						msgItem.locVcardText = null;
					}
					
					setImageBitmap(msgData.imageOrVideoView, null, false);
					msgData.contactPicture.setImageBitmap(locationPlaceHolder);
					
				} else if ((msgItem.type & TYPE_VCARD) != 0) {
					msgData.mmsLayout.setVisibility(View.VISIBLE);
					msgData.vcardView.setVisibility(View.VISIBLE);
					
					setImageBitmap(msgData.imageOrVideoView, null, false);
					if (!TextUtils.isEmpty(msgItem.path)) {
						File file = new File(parentDirPath + msgItem.path);
						ContactStruct contact = null;
						try {
							contact = getContactStruct(Uri.fromFile(file));
						}catch (Exception e) {
							//ignore the error
							Logger.error("Could not parse the vcard " + msgItem.path, e);
						}
						if (contact != null) {
							msgData.contactText.setText(getFormattedMsg(contact));
							if (contact.getContactPicture() != null) {
								msgData.contactPicture.setImageBitmap(contact
										.getContactPicture());
							} else {
								msgData.contactPicture.setImageBitmap(nameCardPlaceHolder);
							}

						} else {
							msgData.contactText.setText("Unknown Vcard");//todo move this to strings.xml
							msgData.contactPicture.setImageBitmap(nameCardPlaceHolder);
						}
					}
				} else if ((msgItem.type & TYPE_IMAGE) != 0 || (msgItem.type & TYPE_VIDEO) != 0) {
					msgData.mmsLayout.setVisibility(View.VISIBLE);
					msgData.progress.setVisibility(View.VISIBLE);

					if ((msgItem.type & TYPE_IMAGE) != 0) {
						Bitmap bitmap = getDrawableBitMap(msgItem.path, position, msgData.playBtn, msgData.progress, true);
						setImageBitmap(msgData.imageOrVideoView, bitmap, bitmap != imagePlaceHolder);
					} else {
						Bitmap bitmap = getDrawableBitMap(msgItem.path, position, msgData.playBtn, msgData.progress, false);
						setImageBitmap(msgData.imageOrVideoView, bitmap, bitmap != videoPlaceHolder);
					}
					
				} else if ((msgItem.type & TYPE_AUDIO) != 0) {
					msgData.mmsLayout.setVisibility(View.VISIBLE);
					setImageBitmap(msgData.imageOrVideoView, audioPlaceHolder, false);
					msgData.playBtn.setVisibility(View.VISIBLE);
				} else {
					setImageBitmap(msgData.imageOrVideoView, null, false);
				}
				
				if (msgItem.locVcardText != null) {
					sub.append(msgItem.locVcardText);
				}
				
				if (item.getPduData().get("sub") != null) {
					String text = item.getPduData().get("sub");
					
					if (!TextUtils.isEmpty(text)) {
						if (sub.length() > 0) {
							sub.append("\n");
						}
						
						String subject = SmilHelper.removeEscapeChar(text);
						int len = subject.length();
						sub.append(SmileyParser.getInstance().addSmileySpans(
								subject, false));
						sub.setSpan(new StyleSpan(Typeface.BOLD), 0, len, 0);
					}

				}
				
				if (msgItem.text != null) {
					if (sub.length() > 0) {
						sub.append("\n");
					}
					sub.append(parser.addSmileySpans(msgItem.text, false));
				}
				
				if ((msgItem.type & TYPE_SLIDESHOW) != 0) {
					msgData.playBtn.setVisibility(View.VISIBLE);
				}
			}
			
			if (sub.length() > 0) {
				msgData.msgText.setText(sub);
				msgData.msgText.setVisibility(View.VISIBLE);
			}
		}


		if (isGroup) { 
			if (position == 0) {
				showGroupMode(msgData, item);
			} else {
				BackUpMessage prevItem = (BackUpMessage) bMessages.get(position - 1);
				if ((prevItem.isSms() && item.isSms()) || (!prevItem.isSms() && !item.isSms())) {
					//Do nothing since unchanged
				} else {
					showGroupMode(msgData, item);
				}
			}
			
			
			
		}

		if (isOutGoing) {
			resId = R.drawable.chat_bubble1_right;
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
					RelativeLayout.TRUE);
			params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
			if (message != null) {
				message.setPadding(maxMessageBorder, 0, 0, 0);	
			}

		} else {
			resId = R.drawable.chat_bubble2_left;
			params.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
					RelativeLayout.TRUE);
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
			if (message != null) {
				message.setPadding(0, 0, maxMessageBorder, 0);	
			}
		}
		msgData.bubble.setBackgroundResource(resId);
		return convertView;
	}


	private void showGroupMode(ViewMsgData msgData, BackUpMessage item) {
		msgData.groupModeView.setVisibility(View.VISIBLE);
		if (item.isSms()) {
			msgData.groupModeView.setText(mContext.getString(
					R.string.group_set,
					mContext.getString(R.string.group_mode_sender)));
		} else {
			msgData.groupModeView.setText(mContext.getString(
					R.string.group_set,
					mContext.getString(R.string.group_mode_group)));		
		}
	}
	
	
	private void showTimeStamp(ViewMsgData msgData, int position) {
		BackUpMessage item = (BackUpMessage) bMessages.get(position);
		HashMap<String, String> pdu = item.getPduData();
		
		long dateInMilliSec = Long.parseLong(pdu.get("date"));
		long time = item.isSms() ? dateInMilliSec : (dateInMilliSec * 1000);

		long prevMsgTime = 0;
		
		if (position != 0) {
			BackUpMessage prevItem = (BackUpMessage) bMessages.get(position - 1);
			HashMap<String, String> prevPdu = prevItem.getPduData();
			long prevDateInMilliSec = Long.parseLong(prevPdu.get("date"));
			prevMsgTime = item.isSms() ? prevDateInMilliSec : (prevDateInMilliSec * 1000); 
		}
		
		if (prevMsgTime == 0 || time > (prevMsgTime + TIMESTAMP_GAP_MAX)) {
			msgData.timeStamp.setVisibility(View.VISIBLE);
			prevMsgTime = time;
			String date = MessageUtils.formatTimeStampString(time, true);
			msgData.timeStamp.setText(String.valueOf(date));
		} else {
			msgData.timeStamp.setVisibility(View.GONE);
		}
	}


	private MessageItem getMessageItem(BackUpMessage item) {
		MessageItem msgItem = new MessageItem();

		boolean isLocation = false;
		ArrayList<HashMap<String, String>> partsData = item.getPartsData();

		if (partsData != null) {
			for (HashMap<String, String> part : partsData) {
				String contentType = part.get("ct");
				String contetnLocation = part.get("cl");
				if (contentType.equals(ContentType.TEXT_PLAIN)
						|| contentType.equalsIgnoreCase(ContentType.APP_WAP_XHTML)
						|| contentType.equals(ContentType.TEXT_HTML)) {
					if (isLocation && contetnLocation.equals("text_1.txt")) {
						msgItem.locVcardText = part.get("text");

						if (!TextUtils.isEmpty(msgItem.locVcardText)) {
							msgItem.locVcardText = SmilHelper.removeEscapeChar(msgItem.locVcardText);
						}
					} else {
						msgItem.text = part.get("text");

						if (!TextUtils.isEmpty(msgItem.text)) {
							msgItem.text = SmilHelper.removeEscapeChar(msgItem.text);
						}
					}
				} else if (ContentType.isAudioType(contentType)) {
					if (msgItem.type != TYPE_TEXT) {
						msgItem.type |= TYPE_SLIDESHOW;
						continue;
					}

					String token = part.get("_data");
					if (token != null) {
						// there should be a better way to get this
						msgItem.path = token.substring(9, token.length() - 3);
					}
					msgItem.type = TYPE_AUDIO;
				} else if (ContentType.isImageType(contentType)) {
					if (msgItem.type != TYPE_LOCATION && msgItem.type != TYPE_TEXT) {
						msgItem.type |= TYPE_SLIDESHOW;
						continue;
					}
					String token = part.get("_data");
					if (token != null) {
						// there should be a better way to get this
						msgItem.path = token.substring(9, token.length() - 3);
					}
					msgItem.type = TYPE_IMAGE;
				} else if (ContentType.isVcardTextType(contentType)) {
					String token = part.get("_data");
					if (token != null) {
						// there should be a better way to get this
						msgItem.path = token.substring(9, token.length() - 3);
						File file = new File(parentDirPath + msgItem.path);
						if (MessageUtils.hasLocation(mContext, Uri.fromFile(file))) {
							msgItem.type = TYPE_LOCATION;
							isLocation = true;
							continue;
						}
					}
					msgItem.type = TYPE_VCARD;
				} else if (ContentType.isVideoType(contentType)) {
					if (msgItem.type != TYPE_TEXT) {
						msgItem.type |= TYPE_SLIDESHOW;
						continue;
					}
					String token = part.get("_data");
					if (token != null) {
						// there should be a better way to get this
						msgItem.path = token.substring(9, token.length() - 3);
					}
					msgItem.type = TYPE_VIDEO;
				}
			}
		}
		return msgItem;
	}


	/**
	 * This Method adds the request to fetch the contact image to the queue if it is not
	 * present in the cache
	 * @param contactId
	 * @return
	 */
	private Bitmap getDrawableBitMap(String filePath, int pos, ImageView playButton, ProgressBar progress , boolean isImage) {
		synchronized (cacheImages) {
			// try the local cache
			Bitmap image = cacheImages.get((Long.valueOf(pos)));
			if (!isImage) {
				playButton.setVisibility(View.VISIBLE);
			} else {
				playButton.setVisibility(View.GONE);
			}
            if (image == null) {
            	if (Logger.IS_DEBUG_ENABLED) {
            		Logger.debug(getClass(), "Retrieving bitmap for " + pos + "with filepath : " + filePath + " cacheBitmap is null for this position");
            	}
            	File uriFile = null;
            	try {
            		 uriFile = new File(parentDirPath + filePath);
            	}catch(Exception e) {
            		e.printStackTrace();
            	}
            	
            	if (isImage) {
            		if(uriFile != null) {
            			bitMapWorker.request(QUEUE_IMAGES, pos, imagePlaceHolderJob, uriFile);
            		}
            		image = imagePlaceHolder;
            	} else {
            		if(uriFile != null) {
            			bitMapWorker.request(QUEUE_IMAGES, pos, videoPlaceHolderJob, uriFile);
            		}
            		image = videoPlaceHolder;
             	}
             }
            progress.setVisibility(View.GONE);
            return image;
        }
    }
    
    private ListDataJob imagePlaceHolderJob = new ListDataJob() {
        public Object run(int pos, Object data) {
        	File uriFile  = (File)data;
            long id = Long.valueOf(pos);
            Uri uri = Uri.fromFile(uriFile);
        	Bitmap bitmap = BitmapManager.INSTANCE.getBitmap(uri.toString(), imageDimensions.width(),
					imageDimensions.height(), true);
			
            // local cache it
            synchronized (cacheImages) {
                cacheImages.put(id, bitmap);
            }
            return bitmap;
        }
    };
    
    private ListDataJob videoPlaceHolderJob = new ListDataJob() {
        public Object run(int pos, Object data) {
            File uriFile  = (File)data;
            long id = Long.valueOf(pos);
            Bitmap bitmap = getVideoFrame(uriFile);
			
            // local cache it
            synchronized (cacheImages) {
                cacheImages.put(id, bitmap);
            }
            return bitmap;
        }
    };
    
    /**
	 * @param newConfig
	 */
    public void onConfigChanged(Configuration newConfig) {
    	minImageBorder = (int) res.getDimension(R.dimen.minImageBorder);
    	maxMessageBorder = res.getDimensionPixelSize(R.dimen.maxMessageBorder)
    			+ res.getDimensionPixelSize(R.dimen.messageBubbleBorder);

    	final int dispWidth = res.getDisplayMetrics().widthPixels;
    	int width = Math.round(dispWidth * MIN_MESSAGE_WIDTH_RATIO);
		setMessageSize(width);
	}

	private void setMessageSize(int width) {
		minMessageWidth = width;
		minImageMessageHeight = Math.round(width / IMAGE_WIDTH_HEIGHT_RATIO);
		maxImageMessageHeight = Math.round(width * IMAGE_WIDTH_HEIGHT_RATIO);
		imageDimensions = new Rect(0, 0, minMessageWidth, minImageMessageHeight);
	}
	
	private void setImageBitmap(ImageView image, Bitmap bitmap, boolean isMediaImage) {
		image.setImageBitmap(bitmap);
		if (bitmap != null) {
			final boolean adjustViewBounds;
			final ScaleType scaleType;
			int width;
			int height;
			final int minHeight;
			final int maxWidth;
			final Drawable background;

			if (isMediaImage) {
				adjustViewBounds = true;
				scaleType = ScaleType.CENTER_CROP;
				maxWidth = Integer.MAX_VALUE;
				minHeight = minImageMessageHeight;

				// if the image is smaller than the message then scale it up and
				// center it on a black background
				height = bitmap.getHeight();
				width = bitmap.getWidth();
				if (width < minMessageWidth && height < minImageMessageHeight) {
					final float aspect = (float) width / height;
					width *= IMAGE_SCALE_LIMIT;
					if (width > minMessageWidth - minImageBorder) {
						width = minMessageWidth;
					}
					height = Math.round(width / aspect);
					background = new ColorDrawable(Color.BLACK);
				} else {
					// center image and crop to the view
					width = minMessageWidth;
					height = LayoutParams.WRAP_CONTENT;
					background = null;
				}
			} else {
				// icons/placeholder images are centered on a transparent
				// background
				adjustViewBounds = false;
				scaleType = ScaleType.CENTER;
				maxWidth = minMessageWidth;
				width = minMessageWidth;
				height = LayoutParams.WRAP_CONTENT;
				background = null;
				minHeight = 0;
			}

			// set text and image layout widths equal
			setTextLayoutWidth(minMessageWidth);
			image.setAdjustViewBounds(adjustViewBounds);
			image.setScaleType(scaleType);
			final ViewGroup.LayoutParams params = image.getLayoutParams();
			params.width = width;
			params.height = height;
			image.setMinimumHeight(minHeight);
			image.setMaxWidth(maxWidth);
			image.setMaxHeight(maxImageMessageHeight);
			image.setVisibility(View.VISIBLE);
			imageLayout.setMinimumHeight(minHeight);
			imageLayout.setBackgroundDrawable(background);
		} else {
			image.setVisibility(View.GONE);
			imageLayout.setBackgroundDrawable(null);
			imageLayout.setMinimumHeight(0);
			setTextLayoutWidth(-1);
		}
	}

	protected void setTextLayoutWidth(int width) {
		final MarginLayoutParams params = (MarginLayoutParams) textLayout
				.getLayoutParams();
		if (width != -1) {
			params.width = width - params.leftMargin - params.rightMargin;
		} else {
			params.width = MarginLayoutParams.WRAP_CONTENT;
		}
	}
	
	private ContactStruct getContactStruct(Uri uri) {

		InputStream input = null;

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "VcardModel.loadVcard " + uri);
		}
		try {
			VCardParser parser = new VCardParser();
			VDataBuilder builder = new VDataBuilder();

			input = mContext.getContentResolver().openInputStream(uri);
			byte[] buffer = new byte[1024];

			StringBuilder vcard = new StringBuilder();

			int bytesRead;
			while ((bytesRead = input.read(buffer)) > 0) {
				vcard.append(new String(buffer, 0, bytesRead));
			}

			if (input != null) {
				input.close();
			}
			// parse the string
			boolean parsed = parser.parse(vcard.toString(),
					CharacterSets.getMimeName(CharacterSets.UTF_8), builder);
			if (!parsed) {
				return null;
			}

			vcard.delete(0, vcard.length());
			vcard = null;

			// get all parsed contacts
			List<VNode> pimContacts = builder.vNodeList;

			if (pimContacts.size() > 0) {
				ContactStruct contact = ContactStruct
						.constructContactFromVNode(pimContacts.get(0), 0);

				if (contact != null) {
					return contact;
				} else {
					return null;
				}
			}
		}

		catch (IOException e) {
			Logger.error(e);

		} catch (VCardException e) {
			Logger.error(e);

		} catch (Exception e) {
			Logger.error(e);

		}
		return null;
	}

	/**
	 * This Method
	 * 
	 * @param contact
	 */
	private String getFormattedMsg(ContactStruct contact) {
		StringBuilder formatMsg = new StringBuilder();

		formatMsg.append(contact.getName());

		if (contact.getOrganizationalData() != null
				&& contact.getOrganizationalData().size() > 0) {
			OrganizationData data = contact.getOrganizationalData().get(0);
			formatMsg
			.append("\n" + data.companyName + "\n" + data.positionName);
		}

		if (contact.getPhoneList() != null) {
			int i = 0;
			for (PhoneData data : contact.getPhoneList()) {
				formatMsg.append("\n" + data.data);
				i++;

				// add first three phone numbers
				if (i == PHONE_NUM_LIMIT) {
					break;
				}
			}
		}

		if (contact.getContactMethodsList() != null
				&& contact.getContactMethodsList().size() > 0) {
			ContactMethod data = contact.getContactMethodsList().get(0);
			formatMsg.append("\n" + data.data);
		}

		return formatMsg.toString();
	}

	private Bitmap getVideoFrame(File file) {

		Bitmap reSizedBitMap = null;
		try {
			reSizedBitMap = ThumbnailUtils.extractThumbnail(ThumbnailUtils
					.createVideoThumbnail(file.getAbsolutePath(),
							Thumbnails.MINI_KIND), 250, 204);
			// 250, 204
		} catch (NullPointerException e) {
			return reSizedBitMap;
		} catch (OutOfMemoryError e) {
			return reSizedBitMap;

		}
		return reSizedBitMap;
	}
	
	class GetBitMapTask extends AsyncTask<Void, Void, Bitmap> {

		private boolean isImage;
		private String filePath;
		private ImageView view;
		private ImageView playButton;
		private ProgressBar progress;
		private int position;


		public GetBitMapTask(ProgressBar progress, ImageView view,
				ImageView playButton, String path, int pos, boolean isImage) {
			this.isImage = isImage;
			this.playButton = playButton;
			this.filePath = path;
			this.view = view;
			this.progress = progress;
			this.position = pos;
		}



		@Override
		protected void onPreExecute() {
			progress.setVisibility(View.VISIBLE);
			super.onPreExecute();
		}



		@Override
		protected Bitmap doInBackground(Void... params) {
			Bitmap bitmap = null;
			File uriFile = new File(parentDirPath + filePath);
			if (isImage) {
				try {
					/* byte[] data = getBytesFromFile(filePath);

				  BitmapFactory.Options options = new
				  BitmapFactory.Options(); options.inTempStorage = new
				  byte[24 * 1024]; options.inJustDecodeBounds = false;
				  options.inSampleSize = 8; options.inPreferredConfig =
				  Bitmap.Config.RGB_565; bitmap =
				  BitmapManager.INSTANCE.decodeByteArray(data, 0,
				  data.length, options);*/

					Uri uri = Uri.fromFile(uriFile);
					bitmap = BitmapManager.INSTANCE.getBitmap(uri.toString(), imageDimensions.width(),
							imageDimensions.height(), true);
					//cacheImages.put(position, bitmap);

				} catch (OutOfMemoryError e) {
					e.printStackTrace();
				}
			} else {
				bitmap = getVideoFrame(uriFile);
				//cacheImages.put(position, bitmap);
			}
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitMap) {
			if (bitMap != null) {
				setImageBitmap(view, bitMap, true);
			}
			progress.setVisibility(View.GONE);
			if (bitMap != null) {
				if (!isImage) {
					playButton.setVisibility(View.VISIBLE);
				}
			} else {
				if (isImage) {
					view.setImageBitmap(imagePlaceHolder);
				} else {
					view.setImageBitmap(videoPlaceHolder);
					playButton.setVisibility(View.VISIBLE);
				}
			}

			super.onPostExecute(bitMap);
		}
	}

}
