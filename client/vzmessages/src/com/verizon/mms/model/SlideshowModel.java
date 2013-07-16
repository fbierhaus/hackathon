/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.verizon.mms.model;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.smil.SMILDocument;
import org.w3c.dom.smil.SMILElement;
import org.w3c.dom.smil.SMILLayoutElement;
import org.w3c.dom.smil.SMILMediaElement;
import org.w3c.dom.smil.SMILParElement;
import org.w3c.dom.smil.SMILRegionElement;
import org.w3c.dom.smil.SMILRootLayoutElement;

import android.content.ContentUris;
import android.content.Context;
import android.drm.mobile1.DrmException;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.Toast;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ContentRestrictionException;
import com.verizon.mms.ContentType;
import com.verizon.mms.ExceedMessageSizeException;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.MmsException;
import com.verizon.mms.data.WorkingMessage;
import com.verizon.mms.dom.smil.parser.SmilXmlSerializer;
import com.verizon.mms.drm.DrmWrapper;
import com.verizon.mms.layout.LayoutManager;
import com.verizon.mms.model.SmilHelper.MMSMessage;
import com.verizon.mms.model.SmilHelper.PduPartContentType;
import com.verizon.mms.pdu.CharacterSets;
import com.verizon.mms.pdu.GenericPdu;
import com.verizon.mms.pdu.MultimediaMessagePdu;
import com.verizon.mms.pdu.PduBody;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduPart;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.pdu.PduPersister.PduDetail;
import com.verizon.mms.ui.MessageItem.MMSData;
import com.verizon.mms.ui.MessageUtils;

public class SlideshowModel extends Model
        implements List<SlideModel>, IModelChangedObserver {

    private final LayoutModel mLayout;
    private final ArrayList<SlideModel> mSlides;
    private SMILDocument mDocumentCache;
    private PduBody mPduBodyCache;
    //private int mCurrentMessageSize;
    private Context mContext;

    public static final String FIRST_SLIDE_TEXT_SRC = "text_0.txt";

    // amount of space to leave in a slideshow for text and overhead.
    public static final int SLIDESHOW_SLOP = 1024;

    // for controlling fixing image orientation before size calculation or the other order
	private static final boolean FIX_ORIENTATION_BEFORE_SIZE_CALCULATION = false;

    private SlideshowModel(Context context) {
        mLayout = new LayoutModel();
        mSlides = new ArrayList<SlideModel>();
        mContext = context;
    }

    private SlideshowModel (
            LayoutModel layouts, ArrayList<SlideModel> slides,
            SMILDocument documentCache, PduBody pbCache,
            Context context) {
        mLayout = layouts;
        mSlides = slides;
        mContext = context;

        mDocumentCache = documentCache;
        mPduBodyCache = pbCache;
        for (SlideModel slide : mSlides) {
            slide.setParent(this);
        }
    }

    public static SlideshowModel createNew(Context context) {
        return new SlideshowModel(context);
    }

    public static SlideshowModel createFromMessageUri(
            Context context, Uri uri) throws MmsException {
        return createFromPduBody(context, getPduBody(context, uri));
    }

    public static SlideshowModel createFromPduBody(Context context, PduBody pb) throws MmsException {
        SMILDocument document = SmilHelper.getDocument(context, pb);

        // Create root-layout model.
        SMILLayoutElement sle = document.getLayout();
        SMILRootLayoutElement srle = sle.getRootLayout();
        int w = srle.getWidth();
        int h = srle.getHeight();
        if ((w == 0) || (h == 0)) {
            w = LayoutManager.getInstance().getLayoutParameters().getWidth();
            h = LayoutManager.getInstance().getLayoutParameters().getHeight();
            srle.setWidth(w);
            srle.setHeight(h);
        }
        RegionModel rootLayout = new RegionModel(
                null, 0, 0, w, h);

        // Create region models.
        ArrayList<RegionModel> regions = new ArrayList<RegionModel>();
        NodeList nlRegions = sle.getRegions();
        int regionsNum = nlRegions.getLength();

        for (int i = 0; i < regionsNum; i++) {
            SMILRegionElement sre = (SMILRegionElement) nlRegions.item(i);
            RegionModel r = new RegionModel(sre.getId(), sre.getFit(),
                    sre.getLeft(), sre.getTop(), sre.getWidth(), sre.getHeight(),
                    sre.getBackgroundColor());
            regions.add(r);
        }
        LayoutModel layouts = new LayoutModel(rootLayout, regions);

        // Create slide models.
        SMILElement docBody = document.getBody();
        NodeList slideNodes = docBody.getChildNodes();
        int slidesNum = slideNodes.getLength();
        ArrayList<SlideModel> slides = new ArrayList<SlideModel>(slidesNum);

        for (int i = 0; i < slidesNum; i++) {
            // FIXME: This is NOT compatible with the SMILDocument which is
            // generated by some other mobile phones.
            SMILParElement par = (SMILParElement) slideNodes.item(i);

            // Create media models for each slide.
            NodeList mediaNodes = par.getChildNodes();
            int mediaNum = mediaNodes.getLength();
            ArrayList<MediaModel> mediaSet = new ArrayList<MediaModel>(mediaNum);

            for (int j = 0; j < mediaNum; j++) {
                SMILMediaElement sme = (SMILMediaElement) mediaNodes.item(j);
                try {
                    MediaModel media = MediaModelFactory.getMediaModel(
                            context, sme, layouts, pb);
                    SmilHelper.addMediaElementEventListeners(
                            (EventTarget) sme, media);
                    mediaSet.add(media);
                } catch (DrmException e) {
                	Logger.error(SlideModel.class, e.getMessage(), e);
                } catch (IOException e) {
                	Logger.error(SlideModel.class, e.getMessage(), e);
                } catch (IllegalArgumentException e) {
                	Logger.error(SlideModel.class, e.getMessage(), e);
                }
            }

            SlideModel slide = new SlideModel((int) (par.getDur() * 1000), mediaSet);
            if (slide.hasText() && slide.hasAudio()) {
                int dp20 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, context.getResources().getDisplayMetrics());
                slide.getText().mRegion.setTop(slide.getText().mRegion.getTop()+dp20);
            }
            slide.setFill(par.getFill());
            SmilHelper.addParElementEventListeners((EventTarget) par, slide);
            slides.add(slide);
        }

        SlideshowModel slideshow = new SlideshowModel(layouts, slides, document, pb, context);
        slideshow.registerModelChangedObserver(slideshow);
        return slideshow;
    }
    
    /**
     * Function optimized to load the slideshow particularly for the Conversation Screen
     * @param context
     * @param pb
     * @return
     * @throws MmsException
     */
    public static SlideshowModel createConversationSlideShowFromPduBody(Context context, PduBody pb) throws MmsException {
        List<MMSMessage> message = SmilHelper.createModelList(context, pb);

        int slidesNum = message.size();
        ArrayList<SlideModel> slides = new ArrayList<SlideModel>(slidesNum);

        for (int i = 0; i < slidesNum; i++) {
            // FIXME: This is NOT compatible with the SMILDocument which is
            // generated by some other mobile phones.
            MMSMessage par = message.get(i);

            // Create media models for each slide.
            List<PduPartContentType> attachment = par.getAttachment();
            int mediaNum = attachment.size();
            ArrayList<MediaModel> mediaSet = new ArrayList<MediaModel>(mediaNum);

            for (int j = 0; j < mediaNum; j++) {
                try {
                    MediaModel media = MediaModelFactory.getConversationGenericMediaModel(context ,attachment.get(j));
                    mediaSet.add(media);
                } catch (DrmException e) {
                	Logger.error(SlideModel.class, e.getMessage(), e);
                } catch (IOException e) {
                	Logger.error(SlideModel.class, e.getMessage(), e);
                } catch (IllegalArgumentException e) {
                	Logger.error(SlideModel.class, e.getMessage(), e);
                }
            }

            SlideModel slide = new SlideModel((int) (1000), mediaSet);
            slides.add(slide);
        }

        SlideshowModel slideshow = new SlideshowModel(new LayoutModel(), slides, null, pb, context);
        slideshow.registerModelChangedObserver(slideshow);
        return slideshow;
    }

    public PduBody toPduBody() {
        if (mPduBodyCache == null) {
            mDocumentCache = SmilHelper.getDocument(this, mContext);
            mPduBodyCache = makePduBody(mDocumentCache);
        }
        return mPduBodyCache;
    }

    private PduBody makePduBody(SMILDocument document) {
        return makePduBody(null, document, false);
    }

    private PduBody makePduBody(Context context, SMILDocument document, boolean isMakingCopy) {
        PduBody pb = new PduBody();

        SlideshowModel slideShowModel = SlideshowModel.rebuildSlideShow(this, mContext);
        
        boolean hasForwardLock = false;
        for (SlideModel slide : slideShowModel) {
            for (MediaModel media : slide.getMedia()) {
                if (isMakingCopy) {
                    if (media.isDrmProtected() && !media.isAllowedToForward()) {
                        hasForwardLock = true;
                        continue;
                    }
                }

                PduPart part = new PduPart();

                if (media.isText()) {
                        TextModel text = (TextModel) media;
                        // Don't create empty text part.
                        if (TextUtils.isEmpty(text.getText())) {
                            continue;
                        }
                        // Set Charset if it's a text media.
                        part.setCharset(text.getCharset());
                }
                
                if (media.isVcard()) {
                    VCardModel vcardModel = (VCardModel)media;
                    part.setCharset(vcardModel.getCharset());
                } else if (media.isLocation()) {
                    LocationModel vcardModel = (LocationModel)media;
                    part.setCharset(vcardModel.getCharset());
                }

                // Set Content-Type.
                part.setContentType(media.getContentType().getBytes());

                String src = media.getSrc();
                String location;
                boolean startWithContentId = src.startsWith("cid:");
                if (startWithContentId) {
                    location = src.substring("cid:".length());
                } else {
                    location = src;
                }

                // Set Content-Location.
                part.setContentLocation(location.getBytes());

                // Set Content-Id.
                if (startWithContentId) {
                    //Keep the original Content-Id.
                    part.setContentId(location.getBytes());
                }
                else {
                    int index = location.lastIndexOf(".");
                    String contentId = (index == -1) ? location
                            : location.substring(0, index);
                    part.setContentId(contentId.getBytes());
                }

                if (media.isDrmProtected()) {
                    DrmWrapper wrapper = media.getDrmObject();
                    part.setDataUri(wrapper.getOriginalUri());
                    part.setData(wrapper.getOriginalData());
                } else if (media.isText()) {
                    part.setData(((TextModel) media).getText().getBytes());
                } else if (media.isImage()) {
                	part.setDataUri(media.getUri());
                }
                else if (media.isVideo() || 
                        media.isAudio() ||
                        media.isVcard() ||
                        media.isLocation()) {
           			part.setDataUri(media.getUri());
                } else {
                	Logger.warn(SlideModel.class, "Unsupported media: " + media);
                }

                pb.addPart(part);
            }
        }

        if (hasForwardLock && isMakingCopy && context != null) {
            Toast.makeText(context,
                    context.getString(R.string.cannot_forward_drm_obj),
                    Toast.LENGTH_LONG).show();
            document = SmilHelper.getDocument(context, pb);
        }

        // Create and insert SMIL part(as the first part) into the PduBody.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmilXmlSerializer.serialize(document, out);
        PduPart smilPart = new PduPart();
        smilPart.setContentId("smil".getBytes());
        smilPart.setContentLocation("smil.xml".getBytes());
        smilPart.setContentType(ContentType.APP_SMIL.getBytes());
        smilPart.setData(out.toByteArray());
        pb.addPart(0, smilPart);

        return pb;
    }

    public PduBody makeCopy(Context context) {
        return makePduBody(context, SmilHelper.getDocument(this, context), true);
    }

    public SMILDocument toSmilDocument() {
        if (mDocumentCache == null) {
            mDocumentCache = SmilHelper.getDocument(this, mContext);
        }
        return mDocumentCache;
    }

    public static PduBody getPduBody(Context context, Uri msg) throws MmsException {
        PduPersister p = PduPersister.getPduPersister(context);
        GenericPdu pdu = p.load(msg);

        int msgType = pdu.getMessageType();
        if ((msgType == PduHeaders.MESSAGE_TYPE_SEND_REQ) || (msgType == PduHeaders.MESSAGE_TYPE_SEND_REQ_X)
                || (msgType == PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF)) {
            return ((MultimediaMessagePdu) pdu).getBody();
        } else {
            throw new MmsException();
        }
    }

    /*public void setCurrentMessageSize(int size) {
        mCurrentMessageSize = size;
    }

    public int getCurrentMessageSize() {
        return mCurrentMessageSize;
    }

    public void increaseMessageSize(int increaseSize) {
        if (increaseSize > 0) {
            mCurrentMessageSize += increaseSize;
        }
    }

    public void decreaseMessageSize(int decreaseSize) {
        if (decreaseSize > 0) {
            mCurrentMessageSize -= decreaseSize;
        }
    }*/

    public LayoutModel getLayout() {
        return mLayout;
    }

    //
    // Implement List<E> interface.
    //
    public boolean add(SlideModel object) {
        int increaseSize = object.getSlideSize();
        checkMessageSize(increaseSize);

        if ((object != null) && mSlides.add(object)) {
            //increaseMessageSize(increaseSize);
            object.registerModelChangedObserver(this);
            for (IModelChangedObserver observer : getModelChangedObservers()) {
                object.registerModelChangedObserver(observer);
            }
            notifyModelChanged(true);
            return true;
        }
        return false;
    }

    public boolean addAll(Collection<? extends SlideModel> collection) {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    public void clear() {
        if (mSlides.size() > 0) {
            for (SlideModel slide : mSlides) {
                slide.unregisterModelChangedObserver(this);
                for (IModelChangedObserver observer : getModelChangedObservers()) {
                    slide.unregisterModelChangedObserver(observer);
                }
            }
            mSlides.clear();
            notifyModelChanged(true);
        }
    }

    public boolean contains(Object object) {
        return mSlides.contains(object);
    }

    public boolean containsAll(Collection<?> collection) {
        return mSlides.containsAll(collection);
    }

    public boolean isEmpty() {
        return mSlides.isEmpty();
    }

    public Iterator<SlideModel> iterator() {
        return mSlides.iterator();
    }

    public boolean remove(Object object) {
        if ((object != null) && mSlides.remove(object)) {
            SlideModel slide = (SlideModel) object;
            slide.unregisterAllModelChangedObservers();
            notifyModelChanged(true);
            return true;
        }
        return false;
    }

    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    public int size() {
        return mSlides.size();
    }

    public Object[] toArray() {
        return mSlides.toArray();
    }

    public <T> T[] toArray(T[] array) {
        return mSlides.toArray(array);
    }

    public void add(int location, SlideModel object) {
        if (object != null) {
            int increaseSize = object.getSlideSize();
            checkMessageSize(increaseSize);

            mSlides.add(location, object);
            object.registerModelChangedObserver(this);
            for (IModelChangedObserver observer : getModelChangedObservers()) {
                object.registerModelChangedObserver(observer);
            }
            notifyModelChanged(true);
        }
    }

    public boolean addAll(int location,
            Collection<? extends SlideModel> collection) {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    public SlideModel get(int location) {
        return (location >= 0 && location < mSlides.size()) ? mSlides.get(location) : null;
    }

    public int indexOf(Object object) {
        return mSlides.indexOf(object);
    }

    public int lastIndexOf(Object object) {
        return mSlides.lastIndexOf(object);
    }

    public ListIterator<SlideModel> listIterator() {
        return mSlides.listIterator();
    }

    public ListIterator<SlideModel> listIterator(int location) {
        return mSlides.listIterator(location);
    }

    public SlideModel remove(int location) {
    	return remove(location, true);
    }

    public SlideModel remove(int location, boolean unregister) {
        SlideModel slide = mSlides.remove(location);
        if (slide != null) {
            if (unregister) {
            	slide.unregisterAllModelChangedObservers();
            }
        }
        return slide;
    }

    public SlideModel set(int location, SlideModel object) {
        SlideModel slide = mSlides.get(location);
        if (null != object) {
            int removeSize = 0;
            int addSize = object.getSlideSize();
            if (null != slide) {
                removeSize = slide.getSlideSize();
            }
            if (addSize > removeSize) {
                checkMessageSize(addSize - removeSize);
            }
        }

        slide =  mSlides.set(location, object);
        if (slide != null) {
            slide.unregisterAllModelChangedObservers();
        }

        if (object != null) {
            object.registerModelChangedObserver(this);
            for (IModelChangedObserver observer : getModelChangedObservers()) {
                object.registerModelChangedObserver(observer);
            }
        }

        notifyModelChanged(true);
        return slide;
    }

    public List<SlideModel> subList(int start, int end) {
        return mSlides.subList(start, end);
    }

    @Override
    protected void registerModelChangedObserverInDescendants(
            IModelChangedObserver observer) {
        mLayout.registerModelChangedObserver(observer);

        for (SlideModel slide : mSlides) {
            slide.registerModelChangedObserver(observer);
        }
    }

    @Override
    protected void unregisterModelChangedObserverInDescendants(
            IModelChangedObserver observer) {
        mLayout.unregisterModelChangedObserver(observer);

        for (SlideModel slide : mSlides) {
            slide.unregisterModelChangedObserver(observer);
        }
    }

    @Override
    protected void unregisterAllModelChangedObserversInDescendants() {
        mLayout.unregisterAllModelChangedObservers();

        for (SlideModel slide : mSlides) {
            slide.unregisterAllModelChangedObservers();
        }
    }

    public void onModelChanged(Model model, boolean dataChanged) {
        if (dataChanged) {
            mDocumentCache = null;
            mPduBodyCache = null;
        }
    }

    public void sync(PduBody pb) {
        for (SlideModel slide : mSlides) {
            for (MediaModel media : slide.getMedia()) {
                PduPart part = pb.getPartByContentLocation(media.getSrc());
                if (part != null) {
                    WorkingMessage.freeScrapSpace(media.getUri(), part.getDataUri());
                    
                    media.setUri(part.getDataUri());
                }
            }
        }
    }

    public void checkMessageSize(int messageSize) throws ContentRestrictionException {
        ContentRestriction cr = ContentRestrictionFactory.getContentRestriction();
        cr.checkMessageSize(0, messageSize, mContext.getContentResolver());
    }

    /**
     * Determines whether this is a "simple" slideshow.
     * Criteria:
     * - Exactly one slide
     * - Exactly one multimedia attachment, but no audio
     * - It can optionally have a caption
    */
    public boolean isSimple() {
        // There must be one (and only one) slide.
        if (size() != 1)
            return false;

        SlideModel slide = get(0);
        
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug("Slide has audio " + slide.hasAudio() + " Slide has video " + slide.hasVideo() 
        			+ " Slide has location " + slide.hasLocation() + " Slide has vcard " + slide.hasVCard());
        }
        // The slide haslocation so return true here
        if (slide.hasLocation() && !slide.hasVideo() && !slide.hasAudio())
            return true;
        
        // The slide must have either an image or video or vcard, but not both.
        if (!((slide.hasImage() ^ slide.hasVideo()) ^ slide.hasVCard()))
            return false;

        // No audio allowed.
        if (slide.hasAudio())
            return false;

        return true;
    }

    /**
     * Make sure the text in slide 0 is no longer holding onto a reference to the text
     * in the message text box.
     */
    public void prepareForSend() {
        if (size() >= 1) {
            TextModel text = get(0).getText();
            if (text != null) {
                text.cloneText();
            }
        }
    }

    /**
     * Resize all the resizeable media objects to fit in the remaining size of the slideshow.
     * This should be called off of the UI thread.
     *
     * @throws MmsException, ExceedMessageSizeException
     */
    public void finalResize(Uri messageUri) throws MmsException, ExceedMessageSizeException {
        finalResize(messageUri, MmsConfig.getMaxMessageSize());
    }

    /**
     * Resize all the resizeable media objects to fit in the remaining size of the slideshow.
     * This should be called off of the UI thread.
     *
     *@param messageUri Uri of the message to be resized
     *@param maxMsgSize Maximum size of the message supported
     *
     *@return ture if the message was resized else it returns false
     * @throws MmsException, ExceedMessageSizeException
     */
    public boolean finalResize(Uri messageUri, int maxMsgSize) throws MmsException, ExceedMessageSizeException {
        // Figure out if we have any media items that need to be resized and total up the
        // sizes of the items that can't be resized.
    	ArrayList<MediaModel> resizeMediaList = new ArrayList<MediaModel>();

//    	if (FIX_ORIENTATION_BEFORE_SIZE_CALCULATION) {
//    		// use this code if we want to maintain the best image quality by resizing without size limit,
//    		// though it might not be the case depending on the resize quality setting
//    		// (usually with default quality, after resizing it will go well below the size limit)
//    		// it might use more memory because a copy of full dimension image, if it requires rotation, is kept in the memory
//    		// before the message is calculated to meet the maximum size limit
//    		// there is no impact if no rotation is required, resizing will be calculated in next part
//    		for (SlideModel slide : mSlides) {
//    			for (MediaModel media : slide.getMedia()) {
//    				if (media.isImage()) {
//    					ImageModel image = (ImageModel)media;
//    					if (image.getExifOrientation() > ExifInterface.ORIENTATION_NORMAL) {
//    						// we need to rotate it before sending it out
//    						resizeMediaList.add(media);
//    					}
//    				}
//    			}
//    		}
//
//    		// we now rotate or resize the images that requires rotation,
//    		// we do it here before calculating the final size for further resizing
//    		long messageId = ContentUris.parseId(messageUri);
//    		if (resizeMediaList.size() > 0) {
//    			for (MediaModel media : resizeMediaList) {
//    				media.resizeMedia(0, messageId);
//    				if (Logger.IS_DEBUG_ENABLED) {
//    					int size = media.getMediaSize(); 
//    					Logger.debug(getClass(), "resize media size: " + size);
//    				}
//    			}        
//
//    			onModelChanged(this, true);     // clear the cached pdu body
//    			PduBody pb = toPduBody();
//    			PduPersister.getPduPersister(mContext).updateParts(messageUri, pb);
//    		}
//
//    		// remove the list
//    		resizeMediaList.clear();
//    	}
    	
        boolean isResized = false;
        int totalSize = 0;
        int resizableCnt = 0;
        int resizableMediaSize = 0;
        boolean forceResize = false;        

        for (SlideModel slide : mSlides) {
        	for (MediaModel media : slide.getMedia()) {
        		boolean needResize = false;
        		
        		if (!FIX_ORIENTATION_BEFORE_SIZE_CALCULATION) {
        			if (media.isImage()) {
        				ImageModel image = (ImageModel)media;
        				if (image.getExifOrientation() > ExifInterface.ORIENTATION_NORMAL) {
        					needResize = true;

        					// force it to resize even if total size is less than the limit
        					forceResize = true;	
        				}
        			}
        		}

        		if (media.getMediaResizable() || needResize) {
        			++resizableCnt;
        			resizableMediaSize += media.getMediaSize();
					resizeMediaList.add(media);
        		} else {
        			totalSize += media.getMediaSize();
        		}
        	}
        }
        
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "finalResize: " + messageUri + ": fixed size = " + totalSize +
            	", resizable = " + resizableMediaSize);
        }

        if (resizableCnt > 0) {
            int remainingSize = maxMsgSize - totalSize - SLIDESHOW_SLOP;
            if (remainingSize <= 0) {
                throw new ExceedMessageSizeException("No room for pictures");
            }
            
            // resize only if the current message is exceeding the remainingSize            
            if (resizableMediaSize >= remainingSize || forceResize) {
                long messageId = ContentUris.parseId(messageUri);
                int bytesPerMediaItem = remainingSize / resizableCnt;

                // Resize the resizable media items to fit within their byte limit.
                for (MediaModel media : resizeMediaList) {
                    media.resizeMedia(bytesPerMediaItem, messageId);
                    totalSize += media.getMediaSize();
                }

                if (totalSize > MmsConfig.getMaxMessageSize()) {
                    throw new ExceedMessageSizeException("Message still too large after resizing media");
                }

                onModelChanged(this, true);     // clear the cached pdu body
                PduBody pb = toPduBody();
                // This will write out all the new parts to:
                //      /data/data/com.android.providers.telephony/app_parts
                // and at the same time delete the old parts.
                PduPersister.getPduPersister(mContext).updateParts(messageUri, pb);
                
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "finalResize: " + messageUri + " resized to " + totalSize);
                }

                isResized = true;
            }
        }
        return isResized;
    }

    public String toString() {
    	return mSlides.toString();
    }
    
    /**
     * This Method rebuilds the slideshow if it has a LocationModel in it,
     * it creates a new slideShowModel with two slides if the slideshow has only one slide in it
     * 1) has text_0.txt TextModel or a blank slide
     * 2) has LocationModel, TextModel and ImageModel
     * @param slideShowModel
     * @return
     */
    public static SlideshowModel rebuildSlideShow(SlideshowModel slideShowModel, Context context) {
        SlideshowModel updatedSlideShow = SlideshowModel.createNew(context);
        int size = slideShowModel.size();

        for (SlideModel sm : slideShowModel) {
            if (sm != null && sm.hasLocation()) {
                LocationModel location = sm.getLocation();
                ImageModel image = sm.getImage();
                
                TextModel text = sm.getText();
                //textModel should contain either text_0.txt or blank slide
                //to match the requirement of previous version of the app
                //to insert additional <par> element
                SlideModel textModel = new SlideModel(updatedSlideShow);
                boolean isMessageText = text != null ? text.getSrc().equals(FIRST_SLIDE_TEXT_SRC) : false;
                if (isMessageText) {
                    textModel.add(text);
                }
                if (size == 1 || isMessageText) {
                    updatedSlideShow.add(textModel);
                }

                //now create the location model
                SlideModel locationSlide = new SlideModel(updatedSlideShow);
                locationSlide.add(location);
                
                //if size is greater than one then we are just saving it and not
                //sending it, so avoid adding an extra row to the db
                if (size == 1) {
                    text = new TextModel(context, ContentType.TEXT_PLAIN, "text_1.txt", updatedSlideShow.getLayout().getTextRegion());
                    locationSlide.add(text);
                    
                    if (location.getFormattedMsg() != null) {
                        text.setText(location.getFormattedMsg());
                    }
                }

                if (image != null) {
                    locationSlide.add(image);
                }
                updatedSlideShow.add(locationSlide);
            } else {
                updatedSlideShow.add(sm);
            }
        }
        return updatedSlideShow;
    }
    
    /**
     * This Method ensures that the TextModel (text_0.txt) is present in the first
     * slide as the text will go to a random slides when loading the
     * the draft which has location part in it
     * @param slideShow
     */
    public static void reorderSlideShow(SlideshowModel slideShow) {
        SlideModel firstSlide = null;
        
        for (SlideModel slide : slideShow) {
            if (firstSlide == null) {
                if (slide.hasText()) {
                    //if we have the text in first slide then break
                    break;
                }
                
                firstSlide = slide;
                continue;
            }
            if (slide.hasText()) {
                TextModel text = slide.getText();
                String src = text.getSrc();
                if (src != null && src.equals(FIRST_SLIDE_TEXT_SRC)) {
                    firstSlide.add(text);
                    slide.removeText();
                    break;
                }
            }
        }
    }
    
    public int getCurrentMessageSize() {
        int size = 0;
        
        for (SlideModel slide : mSlides) {
            size += slide.getSlideSize();
        }
        
        return size;
    }
    
    /*
     * for an image slide which is resizable the size is considered to be zero 
     * hence fetch its size from ImageModel itself
     */
    public int getActualMessageSize() {
        int size = 0;
        
        for (SlideModel slide : mSlides) {
            size += slide.getSlideSize();
            
            if (slide.hasImage()) {
                ImageModel im = slide.getImage();
                if (im.getMediaResizable()) {
                    size += im.getMediaSize();
                }
            }
        }
        
        return size;
    }

    /**
	 * @return the amount of memory in bytes currently used by the slideshow's media content
     */
	public int getMemorySize() {
		int size = 0;
		for (SlideModel slide : mSlides) {
			size += slide.getMemorySize();
		}
		return size;
	}

    /**
     * This Method is used to fetch the attachment type and the text in the PduBody
     * @param partBody
     * @return
     */
    public static MMSData getAttachmentFromBody(Context context, PduBody partBody) {
        //we dont support slideshow so if we do receive a slideshow
        //we show first possible attachment
        int num = partBody.getPartsNum();
        PduPart pduPart = null;
        
        int attachType = WorkingMessage.NONE;
        String text = null;

        for (int i = 0; i < num; i++) {
            pduPart = partBody.getPart(i);

            byte[] content = pduPart.getContentType();
            if (content != null) {
                String contentType = PduPersister.toIsoString(pduPart.getContentType());

                if (attachType == WorkingMessage.NONE) {
                    if (contentType.equals(ContentType.APP_SMIL)) {
                        continue;
                    }
                    
                    if (ContentType.isAudioType(contentType)) {
                        attachType = WorkingMessage.AUDIO;
                    } else if (ContentType.isVideoType(contentType)) {
                        attachType = WorkingMessage.VIDEO;
                    } else if (ContentType.isImageType(contentType)) {
                        attachType = WorkingMessage.IMAGE;
                    } else if (ContentType.isVcardTextType(contentType)) {
                        if (MessageUtils.hasLocation(context, pduPart.getDataUri())) {
                            attachType = WorkingMessage.LOCATION;
                        } else {
                            attachType = WorkingMessage.VCARD;
                        }
                    } else if (ContentType.isDrmType(contentType)) {
                        //ignore the drm type messages for now
                        //we might add and icon in future to show the message is DRM protected
                        attachType = WorkingMessage.DRM_TYPE;
                        break;
                    }
                }
                
                if (text == null && (ContentType.isPlainTextType(contentType) || 
                        ContentType.isHtmlTextType(contentType))) {
                    int charSet = pduPart.getCharset();
                    if (charSet == CharacterSets.ANY_CHARSET) {
                        // By default, we use ISO_8859_1 to decode the data
                        // which character set wasn't set.
                        charSet = CharacterSets.ISO_8859_1;
                    }
                    text = MessageUtils.extractTextFromData(pduPart.getData(), charSet).toString();
                }
                
                if (text != null && attachType != WorkingMessage.NONE) {
                    break;
                }
            }
        }
        return new MMSData(attachType, text);
    }
    
    /**
     * fetches the media attachment type and body of the mms
     * @param context
     * @param p
     * @param uri
     * @return
     */
    public static MMSData getBodyAttachmentType(Context context, PduPersister p, Uri uri) {
    	MMSData mmsData = new MMSData(WorkingMessage.NONE, "");
    	
    	try {
    		PduDetail mmsLastMsg = p.getPduDetail(uri);
    		mmsData.mBody = mmsLastMsg.body == null ? "" : mmsLastMsg.body;
    		String contentType = mmsLastMsg.contentType;

    		if (contentType != null) {
    			if (ContentType.isAudioType(contentType)) {
    				mmsData.mAttachmentType = WorkingMessage.AUDIO;
    			} else if (ContentType.isVideoType(contentType)) {
    				mmsData.mAttachmentType = WorkingMessage.VIDEO;
    			} else if (ContentType.isImageType(contentType)) {
    				mmsData.mAttachmentType = WorkingMessage.IMAGE;
    			} else if (ContentType.isVcardTextType(contentType)) {
    				if (MessageUtils.hasLocation(context, mmsLastMsg.uri)) {
    					mmsData.mAttachmentType = WorkingMessage.LOCATION;
    				} else {
    					mmsData.mAttachmentType = WorkingMessage.VCARD;
    				}
    			} else if (ContentType.isDrmType(contentType)) {
    				//ignore the drm type messages for now
    				//we might add and icon in future to show the message is DRM protected
    				mmsData.mAttachmentType = WorkingMessage.DRM_TYPE;
    			}
    		}
    	} catch (Exception e) {
    		Logger.error(e);
    	}
    	return mmsData;
    }

	public boolean hasMedia() {
		for (SlideModel slide : mSlides) {
			if (slide.hasMedia()) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasLocation(){
		for (SlideModel slide : mSlides) {
			if (slide.hasLocation()) {
				return true;
			}
		}
		return false;
	}

	public boolean isSlideShowLoaded(Rect rect) {
    	SlideModel slide = this.get(0);
    	boolean loaded = true;
    	if (slide != null) {
    		if (slide.hasLocation()) {
    			slide.getLocation().isLoaded(rect);
    		} else if (slide.hasImage()) {
    			loaded = slide.getImage().isLoaded(rect);
    		} else if (slide.hasVideo()) {
    			loaded = slide.getVideo().isLoaded(rect);
    		} else if (slide.hasVCard()) {
                loaded = slide.getVCard().isLoaded(rect);
            } 
    	}
    	return loaded;
    }
    
    public void loadFirstSlide(Rect rect) {
    	SlideModel slide = this.get(0);
    	if (slide != null) {
    		if (slide.hasLocation()) {
    			slide.getLocation().getFormattedMsg();
    			ImageModel im = slide.getImage();
    			if (im != null) {
    				im.resetIfLoadRequired();
    				im.getBitmap();
    			}
    		} else if (slide.hasImage()) {
    			ImageModel im = slide.getImage();
    			im.resetIfLoadRequired();
    			im.getBitmap(rect);
    		} else if (slide.hasVideo()) {
    			slide.getVideo().getBitmap();
    		} else if (slide.hasVCard()) {
                slide.getVCard().loadVcard();
            }  
    	}
    }

    /**
     * This function checks to see if two models hav the same first slide
     * @param model1
     * @param model2
     * @return
     */
	public static boolean isSame(SlideshowModel model1, SlideshowModel model2) {
		boolean flag = false;
		
		if (model1 != null && model2 != null) {
			SlideModel slide1 = model1.get(0);
			SlideModel slide2 = model2.get(0);
			
			MediaModel media1 = null;
			MediaModel media2 = null;
			if (slide1 != null && slide2 != null) {
				if (slide1.hasLocation() && slide2.hasLocation()) {
					media1 = slide1.getLocation();
					media2 = slide2.getLocation();
				} else if (slide1.hasImage() && slide2.hasImage()) {
					media1 = slide1.getImage();
					media2 = slide2.getImage();
				} else if (slide1.hasVideo() && slide2.hasVideo()) {
					media1 = slide1.getVideo();
					media2 = slide2.getVideo();
				} else if (slide1.hasVCard() && slide2.hasVCard()) {
					media1 = slide1.getVCard();
					media2 = slide2.getVCard();
				}  
				
				if (media1 != null && media2 != null) {
					String oldLocation = media1.getSrc();
					String newLocation = media2.getSrc();
					
					if (oldLocation != null && newLocation != null && oldLocation.equals(newLocation)) {
						flag = true;
					}
				}
			}
		}
		return flag;
	}
}
