package com.verizon.mms.ui.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.model.AudioModel;
import com.verizon.mms.model.ImageModel;
import com.verizon.mms.model.LocationModel;
import com.verizon.mms.model.MediaModel;
import com.verizon.mms.model.SlideModel;
import com.verizon.mms.model.SlideshowModel;
import com.verizon.mms.model.VCardModel;
import com.verizon.mms.model.VideoModel;
import com.verizon.mms.ui.ComposeMessageFragment;
import com.verizon.mms.ui.ListDataWorker;
import com.verizon.mms.ui.ListDataWorker.ListDataJob;
import com.verizon.mms.util.BitmapManager;
import com.verizon.mms.util.MemoryCacheMap;

public class MultiAttachmentAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private List<SlideModel> models = new ArrayList<SlideModel>();
	private Context mContext;
	private Bitmap missingPicture = null;
	private Bitmap missingVideo = null;
	private final Rect mImageRect;
	private Handler handler;
	
	private ListView mListView;
	private ListDataWorker modelDataFetcher;
	private MemoryCacheMap<MediaModel, Boolean> modelCache;
	private MemoryCacheMap<MediaModel, Boolean> bitmapFetchError;

	private static final int QUEUE_MODEL_DATA = 0;
    private static final int MIN_QUEUE_SIZE = 5;
    
    private static final int MSG_MODEL_DATA = 1;
    
    static class Tag {
    	TextView nameView;
    	ImageView overlayView;
		ImageView closeButton;
		MediaModel model;
		int position;
		public View playVideo;
		public ImageView attachmentPicture;
	}
    
	public MultiAttachmentAdapter(Context context, Handler handler, ListView listView) {
		mContext = context;
		inflater = LayoutInflater.from(context);
		mImageRect = new Rect(0, 0, 200, 200);
		this.handler = handler;
		mListView = listView;
		
		createDataWorker();
	}

	/**
     * This Method initializes the dataworker and the cache used to store images
     */
    private void createDataWorker() {
        modelCache = new MemoryCacheMap<MediaModel, Boolean>(ComposeMessageFragment.MULTI_ATTACHMENT_MAX_LIMIT);
        bitmapFetchError = new MemoryCacheMap<MediaModel, Boolean>(ComposeMessageFragment.MULTI_ATTACHMENT_MAX_LIMIT);
        // create a worker thread to get images
        modelDataFetcher = new ListDataWorker();
        modelDataFetcher.addQueue(modelDataHandler, QUEUE_MODEL_DATA, MSG_MODEL_DATA, MIN_QUEUE_SIZE, null);
        modelDataFetcher.start();
    }
    
	public void addItem(SlideModel model) {
		models.add(model);
	}

	public void removeItem(int position) {
		SlideModel model = models.remove(position);
		
		synchronized (modelCache) {
			modelCache.remove(model);
			bitmapFetchError.remove(model);
		}
	}

	public void setItems(SlideshowModel slideshow) {
		final int size = slideshow != null ? slideshow.size() : 0;
		final ArrayList<SlideModel> models = new ArrayList<SlideModel>(size);
		for (int i = 0; i < size; ++i) {
			final SlideModel slide = slideshow.get(i);

			if (slide.hasMedia()) {
				models.add(slide);
			}
		}
		this.models = models;
	}

	public void clearItems() {
		modelDataFetcher.clear();
		synchronized (bitmapFetchError) {
			bitmapFetchError.clear();
		}
		synchronized (modelCache) {
			modelCache.clear();
		}
		models.clear();
	}

	@Override
	public int getCount() {
		return models.size();
	}

	@Override
	public SlideModel getItem(int position) {
		return models.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Tag tag = null;
		SlideModel sm = models.get(position);
		
		MediaModel model = null;

		for (MediaModel media : sm.getMedia()) {
			if (!media.isText()) {
				model = media;
				break;
			}
		}
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.attachment_view, null);
			
			tag = new Tag();
			tag.nameView = (TextView) convertView.findViewById(R.id.attachmentText);
			tag.overlayView = (ImageView) convertView.findViewById(R.id.image_content);
			tag.closeButton = (ImageView) convertView.findViewById(R.id.close_button);
			tag.playVideo = convertView.findViewById(R.id.play_video);
			tag.attachmentPicture = (ImageView)convertView.findViewById(R.id.attachmentPicture);
			
			convertView.setTag(tag);
		} else {
			tag = (Tag)convertView.getTag();
		}
		tag.position = position;

		synchronized(modelCache) {
			Boolean loaded = modelCache.get(model);
			View progress = convertView.findViewById(R.id.progress);
			
			if (loaded == null && !model.isAudio()) {
				progress.setVisibility(View.VISIBLE);
				tag.closeButton.setTag(position);
				hideResources(tag.overlayView, tag.playVideo, tag.nameView, tag.attachmentPicture);
				tag.model = model;
				
				modelDataFetcher.request(position, modelDataJob, model);
				
				return convertView;
			} else {
				tag.model = null;
				progress.setVisibility(View.GONE);
			}
		}
		
		updateView(model, position, convertView);
		
		return convertView;
	}
	
	private void updateView(MediaModel model, int position, View convertView) {
		Tag tag = (Tag)convertView.getTag();
		ImageView overlayView = null;
		ImageView closeButton = tag.closeButton;
		
		hideResources(tag.overlayView, tag.playVideo, tag.nameView, tag.attachmentPicture);
		
		if (model.isImage()) {
			overlayView = tag.overlayView;
			showResources(tag.overlayView);
			
			ImageModel imageModel = (ImageModel) model;
			Bitmap bmp = null;
			
			synchronized(bitmapFetchError) {
				if (bitmapFetchError.get(imageModel) == null) {
					bmp = imageModel.getBitmap(mImageRect);
				}
			}
			
			if (bmp == null) {
				if (missingPicture == null) {
					BitmapManager bitmapMgr = BitmapManager.INSTANCE;
					missingPicture = bitmapMgr.decodeResource(
							mContext.getResources(),
							R.drawable.ic_missing_thumbnail_picture);
				}
				bmp = missingPicture;
			}
			overlayView.setImageBitmap(bmp);
		} else if (model.isAudio()) {
			TextView name = tag.nameView;
			overlayView = tag.attachmentPicture;
			
			showResources(tag.attachmentPicture, tag.nameView);
			
			overlayView.setImageResource(R.drawable.audio);

			AudioModel audioModel = (AudioModel) model;
			Map<String, ?> extras = audioModel.getExtras();
			String audioText = audioModel.getSrc();
			String album = (String) extras.get("album");
			if (album != null) {
				audioText = audioText + "\n" + album;
			}
			String artist = (String) extras.get("artist");
			if (artist != null) {
				audioText = audioText + "\n" + artist;
			}
			name.setText(audioText);
			name.setTag(position);
			name.setOnClickListener(slideClickListener);
		} else if (model.isVideo()) {
			overlayView = tag.overlayView;
			showResources(tag.overlayView, tag.playVideo);
			
			VideoModel videoModel = (VideoModel) model;

			Bitmap bmp = null;

			synchronized(bitmapFetchError) {
				if (bitmapFetchError.get(videoModel) == null) {
					bmp = videoModel.getBitmap();
				}
			}
			
			if (bmp == null) {
				if (missingVideo == null) {
					BitmapManager bitmapMgr = BitmapManager.INSTANCE;
					missingVideo = bitmapMgr.decodeResource(
							mContext.getResources(),
							R.drawable.ic_missing_thumbnail_video);
				}
				bmp = missingVideo;
			}
			overlayView.setImageBitmap(bmp);
		} else if (model.isVcard()) {
			overlayView = tag.attachmentPicture;
			showResources(tag.attachmentPicture, tag.nameView);
			VCardModel vcardModel = (VCardModel) model;

			Bitmap contactImage = vcardModel.getContactPicture();
			if (contactImage == null) {
				overlayView.setImageResource(R.drawable.list_namecard);
			} else {
				overlayView.setImageBitmap(contactImage);
			}

			tag.nameView.setText(vcardModel.getFormattedMsg());
			tag.nameView.setTag(position);
			tag.nameView.setOnClickListener(slideClickListener);
		} else if (model.isLocation()) {
			overlayView = tag.attachmentPicture;
			showResources(tag.attachmentPicture, tag.nameView);
			
			LocationModel locationModel = (LocationModel) model;

			// requirement not clear whether to use
			// the default pin image or the place attached by the user
			// Bitmap locationImage = null;
			// if (locationImage == null) {
			overlayView.setImageResource(R.drawable.attach_location);
			// } else {
			// overlayView.setImageBitmap(locationImage);
			// }

			// vcardModel.registerModelChangedObserver(mVCardModelChangedObserver);

			tag.nameView.setText(locationModel.getFormattedMsg());
			tag.nameView.setTag(position);
			tag.nameView.setOnClickListener(slideClickListener);
		}
		overlayView.setTag(position);
		overlayView.setOnClickListener(slideClickListener);
		
		closeButton.setTag(position);
		closeButton.setOnClickListener(slideClickListener);
	}
	
	private void hideResources(View... views) {
		for (View view : views) {
			view.setVisibility(View.GONE);
		}
	}

	private void showResources(View... views) {
		for (View view : views) {
			view.setVisibility(View.VISIBLE);
		}
	}
	
	View.OnClickListener slideClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			int id = view.getId();
			int position = (Integer)view.getTag();
			Message msg = handler.obtainMessage();
			msg.arg1 = position;
			
			if (id == R.id.close_button) {
				msg.what = ComposeMessageFragment.MSG_REMOVE_SLIDE;
			} else {
				msg.what = ComposeMessageFragment.MSG_PLAY_SLIDE;
			}
			handler.sendMessage(msg);
		}
	};
	
	ListDataJob modelDataJob = new ListDataJob() {
		@Override
		public Object run(int pos, Object data) {
			MediaModel model = (MediaModel)data;

			/*try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			
			if (model.isImage()) {
				Bitmap bmp = ((ImageModel)model).getBitmap(mImageRect);
				
				if (bmp == null) {
					synchronized (bitmapFetchError) {
						bitmapFetchError.put(model, true);
					}
				}
			} else if (model.isVideo()) {
				Bitmap bmp = ((VideoModel)model).getBitmap();

				if (bmp == null) {
					synchronized (bitmapFetchError) {
						bitmapFetchError.put(model, true);
					}
				}
			} else if (model.isVcard()) {
				((VCardModel)model).getFormattedMsg();
			} else if (model.isLocation()) {
				((LocationModel)model).getFormattedMsg();
			}
			return model;
		}
	};
	
	private Handler modelDataHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_MODEL_DATA: {
				int size = mListView.getChildCount();

				for (int i = 0; i < size; i++) {
					View child = (View)mListView.getChildAt(i);
					Tag tag = (Tag)child.getTag();
					
					if (tag != null) {
						MediaModel model = tag.model;
						if (model != null && msg.obj == model) {
							int position = tag.position;
							View view = child.findViewById(R.id.progress);
							view.setVisibility(View.GONE);
							updateView(tag.model, position, child);
							tag.model = null;
							
							synchronized(modelCache) {
								modelCache.put(model, true);
							}
						}
					}
				}
			}
			}
		}
	};
	
	/*
     * Stop all the background tasks and clean up the adapter
     */
    public void closeAdapter() {
        clearItems();
        
        modelDataFetcher.exit();
    }
}