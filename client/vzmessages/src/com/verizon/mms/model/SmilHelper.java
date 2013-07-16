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

import static com.verizon.mms.dom.smil.SmilMediaElementImpl.SMIL_MEDIA_END_EVENT;
import static com.verizon.mms.dom.smil.SmilMediaElementImpl.SMIL_MEDIA_PAUSE_EVENT;
import static com.verizon.mms.dom.smil.SmilMediaElementImpl.SMIL_MEDIA_SEEK_EVENT;
import static com.verizon.mms.dom.smil.SmilMediaElementImpl.SMIL_MEDIA_START_EVENT;
import static com.verizon.mms.dom.smil.SmilParElementImpl.SMIL_SLIDE_END_EVENT;
import static com.verizon.mms.dom.smil.SmilParElementImpl.SMIL_SLIDE_START_EVENT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.smil.SMILDocument;
import org.w3c.dom.smil.SMILElement;
import org.w3c.dom.smil.SMILLayoutElement;
import org.w3c.dom.smil.SMILMediaElement;
import org.w3c.dom.smil.SMILParElement;
import org.w3c.dom.smil.SMILRegionElement;
import org.w3c.dom.smil.SMILRegionMediaElement;
import org.w3c.dom.smil.SMILRootLayoutElement;
import org.xml.sax.SAXException;

import android.content.Context;
import android.drm.mobile1.DrmException;
import android.net.Uri;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.ContentType;
import com.verizon.mms.MmsException;
import com.verizon.mms.dom.smil.SmilDocumentImpl;
import com.verizon.mms.dom.smil.parser.SmilXmlParser;
import com.verizon.mms.dom.smil.parser.SmilXmlSerializer;
import com.verizon.mms.drm.DrmWrapper;
import com.verizon.mms.pdu.PduBody;
import com.verizon.mms.pdu.PduPart;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.ui.MessageUtils;

public class SmilHelper {
    private static final String TAG = "Mms/smil";

    public static final String ELEMENT_TAG_TEXT = "text";
    public static final String ELEMENT_TAG_IMAGE = "img";
    public static final String ELEMENT_TAG_AUDIO = "audio";
    public static final String ELEMENT_TAG_VIDEO = "video";
    public static final String ELEMENT_TAG_REF = "ref";
    public static final String ELEMENT_TAG_VCARD = "vcard";
    public static final String ELEMENT_TAG_LOCATION = "location";
    
    public static final String LOCATION_IMG_PREFIX = "location";
    public static final String LOCATION_VCARD_PREFIX = "VCARD_";

    public static final int CONTENT_TYPE_TEXT = 0;
    public static final int CONTENT_TYPE_IMAGE = 1;
    public static final int CONTENT_TYPE_VIDEO = 2;
    public static final int CONTENT_TYPE_AUDIO = 3;
    public static final int CONTENT_TYPE_LOCATION = 5;
    public static final int CONTENT_TYPE_LOCATION_IMAGE = 6;
    public static final int CONTENT_TYPE_VCARD = 7;
    
    private SmilHelper() {
        // Never instantiate this class.
    }

    public static SMILDocument getDocument(Context context, PduBody pb) {
        // Find SMIL part in the message.
        PduPart smilPart = findSmilPart(pb);
        SMILDocument document = null;
        
        //boolean hasVcard = findPart(pb, ContentType.TEXT_VCARD) != null;
        boolean createDocument = shouldCreateDocument(pb);
        
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug("SmilHelper.getDocument creating new smil document " + createDocument);
        }
        // Try to load SMIL document from existing part.
		// if we have vcard present we have to create the document from the part
        // as vcard part will not be included in the smil AND
        // if the Pdu has only one media part and/or one text part
        // we construct our own document (New VZ Requirement. If we 
        // get a Image and text in an MMS from an IPhone it sends it as 
        // 2 slides instead of one i.e 2 parts in the smil document Ex
        /*<smil><head><layout><root-layout width="100%" height="100%"/><region id="Image" width="100%" height="83%" top="0%" left="0%"/><region id="Text" width="100%" height="17%" top="84%" left="0%"/></layout></head><body>
        <par dur="2000ms"><text region="Text" src="Part0.txt"/></par>
        <par dur="2000ms"><img region="Image" src="device-2012-07-31-015250.png"/></par>
        </body></smil>*/
        
        if (smilPart != null && !createDocument) {
            document = getSmilDocument(smilPart);
        }

        if (document == null) {
            // Create a new SMIL document.
            document = createSmilDocument(context, pb);
        }

        return document;
    }

    /**
     * This function checks to see if there is a VCard or if
     * the MMS has one media slide and/or one text slide
     * @param pb
     * @return true if there is a VCard in the PduBody or if  the Pdu has
     * one media and/or one text slide
     */
    private static boolean shouldCreateDocument(PduBody pb) {
        int num = pb.getPartsNum();
        int slideCount = 1;
        boolean createDocument = false;
        boolean hasMedia = false;
        boolean hasText = false;
        String partContentType = null;
        PduPart pduPart = null;
                
        for (int i = 0; i < num; i++) {
            pduPart = pb.getPart(i);
            partContentType = PduPersister.toIsoString(pduPart.getContentType());
            
            if (hasMedia && hasText) {
                slideCount++;
                hasMedia = hasText = false;
            }
            
            if (ContentType.isVcardTextType(partContentType)) {
                createDocument = true;
                break;
            } else if (partContentType.equalsIgnoreCase(ContentType.APP_SMIL)) {
                continue;
            } else if (ContentType.isImageType(partContentType) ||
                    ContentType.isAudioType(partContentType) || 
                    ContentType.isVideoType(partContentType)) {
                if (hasMedia) {
                    slideCount++;
                    hasMedia = hasText = false;
                    continue;
                }
                hasMedia = true;
            } else if (ContentType.isTextType(partContentType)) {
                if (hasText) {
                    slideCount++;
                    hasMedia = hasText = true;
                    continue;
                }
                hasText = true;
            } else {
                if (Logger.IS_ERROR_ENABLED) {
                    Logger.error(SmilHelper.class, "Unsupported media type " + partContentType);
                    slideCount++;
                }
            }
        }
        
        return (createDocument)? true : slideCount <= 1;
    }

    /**
     * 
     * This Method returns the PduPart of the specified contenttypes if present in the PduBody 
     * @param pb
     * @param contentType
     * @return
     */
    private static PduPart findPart(PduBody pb, String textVcard) {
        int num = pb.getPartsNum();
        PduPart part = null;
        
        for (int i = 0; i < num; i++) {
            PduPart pduPart = pb.getPart(i);
            
            if (PduPersister.toIsoString(pduPart.getContentType()).equalsIgnoreCase(textVcard)) {
                part = pduPart;
                break;
            }
        }
        return part;
    }

    public static SMILDocument getDocument(SlideshowModel model, Context context) {
        return createSmilDocument(model, context);
    }

    /**
     * Find a SMIL part in the MM.
     *
     * @return The existing SMIL part or null if no SMIL part was found.
     */
    private static PduPart findSmilPart(PduBody body) {
        int partNum = body.getPartsNum();
        for(int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            if (Arrays.equals(part.getContentType(),
                            ContentType.APP_SMIL.getBytes())) {
                // Sure only one SMIL part.
                return part;
            }
        }
        return null;
    }

    private static SMILDocument validate(SMILDocument in) {
        // TODO: add more validating facilities.
        return in;
    }

    /**
     * Parse SMIL message and retrieve SMILDocument.
     *
     * @return A SMILDocument or null if parsing failed.
     */
    private static SMILDocument getSmilDocument(PduPart smilPart) {
        try {
            byte[] data = smilPart.getData();
            if (data != null) {
                if (Logger.IS_DEBUG_ENABLED) {
                	Logger.debug(SmilHelper.class, "Parsing SMIL document.");
                	Logger.debug(SmilHelper.class, new String(data));
                }

                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                SMILDocument document = new SmilXmlParser().parse(bais);
                return validate(document);
            }
        } catch (IOException e) {
        	Logger.error(SmilHelper.class, "Failed to parse SMIL document.", e);
        } catch (SAXException e) {
        	Logger.error(SmilHelper.class, "Failed to parse SMIL document.", e);
        } catch (MmsException e) {
        	Logger.error(SmilHelper.class, "Failed to parse SMIL document.", e);
        }
        return null;
    }

    public static SMILParElement addPar(SMILDocument document) {
        SMILParElement par = (SMILParElement) document.createElement("par");
        // Set duration to eight seconds by default.
        par.setDur(8.0f);
        document.getBody().appendChild(par);
        return par;
    }

    public static SMILMediaElement createMediaElement(
            String tag, SMILDocument document, String src) {
        SMILMediaElement mediaElement =
                (SMILMediaElement) document.createElement(tag);
        
        mediaElement.setSrc(escapeXML(src));
        return mediaElement;
    }

    static public String escapeXML(String str) {
        return str.replaceAll("&","&amp;")
                  .replaceAll("<", "&lt;")
                  .replaceAll(">", "&gt;")
                  .replaceAll("\"", "&quot;")
                  .replaceAll("'", "&apos;");
    }
    
    static public String removeEscapeChar(String str) {
        return str.replaceAll("&amp;","&")
                  .replaceAll("&lt;", "<")
                  .replaceAll("&gt;", ">")
                  .replaceAll("&quot;", "\"")
                  .replaceAll("&apos;", "'");
    }

    /**
     * 
     * This Method is used to create smildocument with simple slides
     * where a simple slide can be one of the following
     * 1) text
     * 2) text + image/audio/video
     * 3) text + vcard
     * 4) text + location
     * 5) text + location + image 
     * @param context
     * @param pb
     * @return
     */
    private static SMILDocument createSmilDocument(Context context, PduBody pb) {
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(SmilHelper.class, "Creating default SMIL document.");
        }

        PduBody orderedPduBody = reorderPartBody(pb);
        
        SMILDocument document = new SmilDocumentImpl();

        // Create root element.
        // FIXME: Should we create root element in the constructor of document?
        SMILElement smil = (SMILElement) document.createElement("smil");
        smil.setAttribute("xmlns", "http://www.w3.org/2001/SMIL20/Language");
        document.appendChild(smil);

        // Create <head> and <layout> element.
        SMILElement head = (SMILElement) document.createElement("head");
        smil.appendChild(head);

        SMILLayoutElement layout = (SMILLayoutElement) document.createElement("layout");
        head.appendChild(layout);

        // Create <body> element and add a empty <par>.
        SMILElement body = (SMILElement) document.createElement("body");
        smil.appendChild(body);
        SMILParElement par = addPar(document);

        // Create media objects for the parts in PDU.
        int partsNum = orderedPduBody.getPartsNum();
        if (partsNum == 0) {
            return document;
        }

        boolean hasText = false;
        boolean hasMedia = false;
        boolean hasLocation = false;
        for (int i = 0; i < partsNum; i++) {
            // Create new <par> element.
            if ((par == null) || (hasMedia && hasText)) {
                par = addPar(document);
                hasText = false;
                hasMedia = false;
                hasLocation = false;
            }

            PduPart part = orderedPduBody.getPart(i);
            String contentType = new String(part.getContentType());
            if (ContentType.isDrmType(contentType)) {
                DrmWrapper dw;
                try {
                    dw = new DrmWrapper(contentType, part.getDataUri(),
                            part.getData());
                    contentType = dw.getContentType();
                } catch (DrmException e) {
                	Logger.error(SmilHelper.class, e.getMessage(), e);
                } catch (IOException e) {
                	Logger.error(SmilHelper.class, e.getMessage(), e);
                }
            }

            if (contentType.equals(ContentType.TEXT_PLAIN)
                    || contentType.equalsIgnoreCase(ContentType.APP_WAP_XHTML)
                    || contentType.equals(ContentType.TEXT_HTML)) {
                SMILMediaElement textElement = createMediaElement(
                        ELEMENT_TAG_TEXT, document, part.generateLocation());

                if (hasLocation) {
                    String location = null; 
                    if (part.getContentLocation() != null) {
                        location = new String(part.getContentLocation());
                    }
                    
                    if (location != null && (location.equalsIgnoreCase("text_0.txt") || 
                            location.equalsIgnoreCase("text950.txt"))) {
                        par.appendChild(textElement);
                    } else {
                        //dont add text_1.txt it is not required if 
                        //we have location in this par element we can get it from the vcard
                        //this also avoids creation of two slides instead of one for location
                        hasText = true;
                    }
                    continue;
                }

                par.appendChild(textElement);
                hasText = true;
            } else if (ContentType.isImageType(contentType)) {
                if (hasMedia && !isLocationImage(hasLocation, part.getContentLocation())) {
                    par = addPar(document);
                    hasText = hasMedia = hasLocation = false;
                }
                SMILMediaElement imageElement = createMediaElement(
                        ELEMENT_TAG_IMAGE, document, part.generateLocation());
                par.appendChild(imageElement);
                hasMedia = true;
            } else if (ContentType.isVideoType(contentType)) {
                if (hasMedia || hasLocation) {
                    par = addPar(document);
                    hasText = hasMedia = hasLocation = false;
                }
                SMILMediaElement videoElement = createMediaElement(
                        ELEMENT_TAG_VIDEO, document, part.generateLocation());
                par.appendChild(videoElement);
                hasMedia = true;
            } else if (ContentType.isAudioType(contentType)) {
                if (hasMedia || hasLocation) {
                    par = addPar(document);
                    hasText = hasMedia = hasLocation = false;
                }
                SMILMediaElement audioElement = createMediaElement(
                        ELEMENT_TAG_AUDIO, document, part.generateLocation());
                par.appendChild(audioElement);
                hasMedia = true;
            } else if (ContentType.isVcardTextType(contentType)) {
                if (hasMedia || hasLocation) {
                    par = addPar(document);
                    hasText = hasMedia = hasLocation = false;
                }
                
                Uri vcardUri = part.getDataUri();
                boolean hasLocationField = false;
                if (vcardUri == null) {
                	hasLocationField = MessageUtils.hasLocation(context, part.getData());
                } else {
                	hasLocationField = MessageUtils.hasLocation(context, vcardUri);
                }
                if (hasLocationField) {
                    SMILMediaElement locationElement = createMediaElement(
                            ELEMENT_TAG_LOCATION, document, part.generateLocation());
                    par.appendChild(locationElement);
                    hasLocation = true;
                } else {
                    SMILMediaElement vcardElement = createMediaElement(
                            ELEMENT_TAG_VCARD, document, part.generateLocation());
                    par.appendChild(vcardElement);
                    //treat vcard as a media
                    hasMedia = true;
                }
            } else if (!contentType.equals(ContentType.APP_SMIL)) {
                // TODO enable after alpha build
                //            	throw new UnsupportedOperationException("Unsupported media type " + contentType);
                if (Logger.IS_ERROR_ENABLED) {
                    //lets log it so that we will know the contentTypes which we are not
                    //supporting
                    Logger.error(SmilHelper.class, "Unsupported media type " + contentType);
                }
            }
        }

        return document;
    }

    /**
     * This Method checks if the image is associated with location
     * message
     * @param hasLocation
     * @param bs
     * @return
     */
    private static boolean isLocationImage(boolean hasLocation, byte[] bs) {
        if (hasLocation) {
            String location = bs != null? new String(bs) : null;
            if (location != null && location.startsWith(LOCATION_IMG_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    private static SMILDocument createSmilDocument(SlideshowModel slideShowModel, Context context) {
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(SmilHelper.class, "Creating SMIL document from SlideshowModel.");
        }
        SlideshowModel slideshow = SlideshowModel.rebuildSlideShow(slideShowModel, context);
        SMILDocument document = new SmilDocumentImpl();

        // Create SMIL and append it to document
        SMILElement smilElement = (SMILElement) document.createElement("smil");
        document.appendChild(smilElement);

        // Create HEAD and append it to SMIL
        SMILElement headElement = (SMILElement) document.createElement("head");
        smilElement.appendChild(headElement);

        // Create LAYOUT and append it to HEAD
        SMILLayoutElement layoutElement = (SMILLayoutElement)
                document.createElement("layout");
        headElement.appendChild(layoutElement);

        // Create ROOT-LAYOUT and append it to LAYOUT
        SMILRootLayoutElement rootLayoutElement =
                (SMILRootLayoutElement) document.createElement("root-layout");
        LayoutModel layouts = slideshow.getLayout();
        rootLayoutElement.setWidth(layouts.getLayoutWidth());
        rootLayoutElement.setHeight(layouts.getLayoutHeight());
        String bgColor = layouts.getBackgroundColor();
        if (!TextUtils.isEmpty(bgColor)) {
            rootLayoutElement.setBackgroundColor(bgColor);
        }
        layoutElement.appendChild(rootLayoutElement);

        // Create REGIONs and append them to LAYOUT
        ArrayList<RegionModel> regions = layouts.getRegions();
        ArrayList<SMILRegionElement> smilRegions = new ArrayList<SMILRegionElement>();
        for (RegionModel r : regions) {
            SMILRegionElement smilRegion = (SMILRegionElement) document.createElement("region");
            smilRegion.setId(r.getRegionId());
            smilRegion.setLeft(r.getLeft());
            smilRegion.setTop(r.getTop());
            smilRegion.setWidth(r.getWidth());
            smilRegion.setHeight(r.getHeight());
            smilRegion.setFit(r.getFit());
            smilRegions.add(smilRegion);
        }

        // Create BODY and append it to the document.
        SMILElement bodyElement = (SMILElement) document.createElement("body");
        smilElement.appendChild(bodyElement);

        boolean txtRegionPresentInLayout = false;
        boolean imgRegionPresentInLayout = false;
        for (SlideModel slide : slideshow) {
            // Create PAR element.
            SMILParElement par = addPar(document);
            par.setDur(slide.getDuration() / 1000f);

            addParElementEventListeners((EventTarget) par, slide);

            // Add all media elements.
            for (MediaModel media : slide.getMedia()) {
                SMILMediaElement sme = null;
                String src = media.getSrc();
                if (media instanceof TextModel) {
                    TextModel text = (TextModel) media;
                    if (TextUtils.isEmpty(text.getText())) {
                        if (Logger.IS_DEBUG_ENABLED) {
                        	Logger.debug(SmilHelper.class, "Empty text part ignored: " + text.getSrc());
                        }
                        continue;
                    }
                    sme = SmilHelper.createMediaElement(SmilHelper.ELEMENT_TAG_TEXT, document, src);
                    txtRegionPresentInLayout = setRegion((SMILRegionMediaElement) sme,
                                                         smilRegions,
                                                         layoutElement,
                                                         LayoutModel.TEXT_REGION_ID,
                                                         txtRegionPresentInLayout);
                } else if (media instanceof ImageModel) {
                    sme = SmilHelper.createMediaElement(SmilHelper.ELEMENT_TAG_IMAGE, document, src);
                    imgRegionPresentInLayout = setRegion((SMILRegionMediaElement) sme,
                                                         smilRegions,
                                                         layoutElement,
                                                         LayoutModel.IMAGE_REGION_ID,
                                                         imgRegionPresentInLayout);
                } else if (media instanceof VideoModel) {
                    sme = SmilHelper.createMediaElement(SmilHelper.ELEMENT_TAG_VIDEO, document, src);
                    imgRegionPresentInLayout = setRegion((SMILRegionMediaElement) sme,
                                                         smilRegions,
                                                         layoutElement,
                                                         LayoutModel.IMAGE_REGION_ID,
                                                         imgRegionPresentInLayout);
                } else if (media instanceof AudioModel) {
                    sme = SmilHelper.createMediaElement(SmilHelper.ELEMENT_TAG_AUDIO, document, src);
                } else {
                	// TODO enable after alpha build
//                	throw new UnsupportedOperationException("Unsupported media type " + media + ": " + media.getContentType());
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(SmilHelper.class, "Unsupport media: " + media);
                    }
                    continue;
                }

                // Set timing information.
                int begin = media.getBegin();
                if (begin != 0) {
                    sme.setAttribute("begin", String.valueOf(begin / 1000));
                }
                int duration = media.getDuration();
                if (duration != 0) {
                    sme.setDur((float) duration / 1000);
                }
                par.appendChild(sme);

                addMediaElementEventListeners((EventTarget) sme, media);
            }
        }

        if (Logger.IS_DEBUG_ENABLED) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            SmilXmlSerializer.serialize(document, out);
            Logger.debug(SmilHelper.class, out.toString());
        }

        return document;
    }

    private static SMILRegionElement findRegionElementById(
            ArrayList<SMILRegionElement> smilRegions, String rId) {
        for (SMILRegionElement smilRegion : smilRegions) {
            if (smilRegion.getId().equals(rId)) {
                return smilRegion;
            }
        }
        return null;
    }

    private static boolean setRegion(SMILRegionMediaElement srme,
                                     ArrayList<SMILRegionElement> smilRegions,
                                     SMILLayoutElement smilLayout,
                                     String regionId,
                                     boolean regionPresentInLayout) {
        SMILRegionElement smilRegion = findRegionElementById(smilRegions, regionId);
        if (!regionPresentInLayout && smilRegion != null) {
            srme.setRegion(smilRegion);
            smilLayout.appendChild(smilRegion);
            return true;
        }
        return false;
    }

    static void addMediaElementEventListeners(
            EventTarget target, MediaModel media) {
        // To play the media with SmilPlayer, we should add them
        // as EventListener into an EventTarget.
        target.addEventListener(SMIL_MEDIA_START_EVENT, media, false);
        target.addEventListener(SMIL_MEDIA_END_EVENT, media, false);
        target.addEventListener(SMIL_MEDIA_PAUSE_EVENT, media, false);
        target.addEventListener(SMIL_MEDIA_SEEK_EVENT, media, false);
    }

    static void addParElementEventListeners(
            EventTarget target, SlideModel slide) {
        // To play the slide with SmilPlayer, we should add it
        // as EventListener into an EventTarget.
        target.addEventListener(SMIL_SLIDE_START_EVENT, slide, false);
        target.addEventListener(SMIL_SLIDE_END_EVENT, slide, false);
    }
    
	/**
	 * This method divides the pdu with more than one slide into different PduBody
	 * 
	 * @param context
	 * @param pb PduBody that contains the mms pdu
	 * @return
	 * @throws MmsException
	 */
    public static List<PduBody> createPartsFromPduBody(Context context, PduBody pb) throws MmsException {
        SMILDocument document = SmilHelper.getDocument(context, pb);

        // Create slide models.
        SMILElement docBody = document.getBody();
        NodeList slideNodes = docBody.getChildNodes();
        int slidesNum = slideNodes.getLength();
        
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug("Number of slides " + slidesNum);
        }
        
        //return if we have only one slide in the given pdu
        if (slidesNum <= 1) {
        	return null;
        }
        
        ArrayList<PduBody> pduSlides = new ArrayList<PduBody>(slidesNum);

        for (int i = 0; i < slidesNum; i++) {
            SMILParElement par = (SMILParElement) slideNodes.item(i);
            NodeList mediaNodes = par.getChildNodes();
            int mediaNum = mediaNodes.getLength();
            PduBody pduBody = new PduBody();
            
            boolean hasMedia = false;
            boolean hasText = false;
            for (int j = 0; j < mediaNum; j++) {
            	SMILMediaElement sme = (SMILMediaElement) mediaNodes.item(j);
            	String src = sme.getSrc();
            	PduPart part = null;

            	if (Logger.IS_DEBUG_ENABLED) {
            		Logger.debug("CreatePartFromPduBody source " + src);
            	}
            	try {
            		part = findPartOrThrow(pb, src);
            	} catch (IllegalArgumentException e) {
            		//when we are creating the smil document instead of parsing the
            		//smil there are chances where the source in the created smil has
            		//escaped characters try to find the part by removing those
            		String unescapedSource = SmilHelper.removeEscapeChar(src);
            		part = findPartOrThrow(pb, unescapedSource);
            	}
            	String contentType = new String(part.getContentType());

            	boolean createNewBody = false;
            	if (contentType.equalsIgnoreCase(ContentType.TEXT_PLAIN)
            			|| contentType.equalsIgnoreCase(ContentType.APP_WAP_XHTML)
            			|| contentType.equalsIgnoreCase(ContentType.TEXT_HTML)) {
            		if (hasText) {
            			//slide already has a text part create a new pdubody
            			createNewBody = true;
            			hasText = hasMedia = false;
            		} else {
            			hasText = true;
            		}
            	} else {
            		if (hasMedia) {
            			//slide already has a media part create a new pdubody
            			createNewBody = true;
            			hasText = hasMedia = false;
            		} else {
            			hasMedia = true;
            		}
            	}
            	
            	if (createNewBody) {
            		//create the smil part and add it to the pduBody at first position
                	PduPart smilPart = createSmilPart(context, pduBody);
                	pduBody.addPart(0, smilPart);

                	pduSlides.add(pduBody);
                	
                	pduBody = new PduBody();
            	}
            	pduBody.addPart(part);
            }

            if (pduBody.getPartsNum() > 0) {
            	//create the smil part and add it to the pduBody at first position
            	PduPart smilPart = createSmilPart(context, pduBody);
            	pduBody.addPart(0, smilPart);

            	pduSlides.add(pduBody);
            }
        }

        return pduSlides;
    }
    
    /*
     * This functions searches for the part in the PduBody and also handles
     * the source type begining with "cid:"
     */
    private static PduPart findPartOrThrow(PduBody pb, String src) {
        PduPart part = null;

        if (src != null) {
            if (src.startsWith("cid:")) {
                part = pb.getPartByContentId("<" + src.substring("cid:".length()) + ">");
                //seems to be a bug from the base code where instead of 
                //searching for <src.type> we are searching for <<src.type>>
                if (part == null) {
                	part = pb.getPartByContentId(src.substring("cid:".length()));
                }
            } else {
                part = pb.getPartByName(src);
                if (part == null) {
                    part = pb.getPartByFileName(src);
                    if (part == null) {
                        part = pb.getPartByContentLocation(src);
                    }
                }
            }
        }

        if (part != null) {
            return part;
        }

        throw new IllegalArgumentException("No part found for the model with src = " + src);
    }
    
    private static PduPart createSmilPart(Context context, PduBody pduBody) {
    	SMILDocument smilDocument = SmilHelper.getDocument(context, pduBody);
        // Create and insert SMIL part(as the first part) into the PduBody.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmilXmlSerializer.serialize(smilDocument, out);
        PduPart smilPart = new PduPart();
        smilPart.setContentId("smil".getBytes());
        smilPart.setContentLocation("smil.xml".getBytes());
        smilPart.setContentType(ContentType.APP_SMIL.getBytes());
        smilPart.setData(out.toByteArray());
        
        return smilPart;
	}
    
    public static class MMSMessage {
    	private ArrayList<PduPartContentType> attachments = new ArrayList<PduPartContentType>();

		public void addAttachment(PduPart attachment, String contentType, boolean location) {
			attachments.add(new PduPartContentType(attachment, contentType, location));
		}
		
		public ArrayList<PduPartContentType> getAttachment() {
			return attachments;
		}
	}
    
    public static class PduPartContentType {
    	public PduPart part;
    	public String contentType;
    	public boolean isLocation;
    	
    	public PduPartContentType(PduPart attachment, String contentType, boolean location) {
    		this.part = attachment;
    		this.contentType = contentType;
    		this.isLocation = location;
    	}
    }
    
    /**
     * 
     * This Method is Optimized way to create model list with simple slides
     * where a simple slide can be one of the following. This function is meant only for displaying the message in 
     * the MessageBubble
     * 1) text
     * 2) text + image/audio/video
     * 3) text + vcard
     * 4) text + location
     * 5) text + location + image 
     * @param context
     * @param pb
     * @return
     */
    public static List<MMSMessage> createModelList(Context context, PduBody pb) {
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(SmilHelper.class, "createModelList.");
        }

        List<PduPart> orderedPduBody = reorderPartBodyAsList(pb);
        
        boolean hasText = false;
        boolean hasMedia = false;
        boolean hasLocation = false;
        List<MMSMessage> msgs = new ArrayList<SmilHelper.MMSMessage>();
        int partsNum = orderedPduBody.size();
        MMSMessage par = null;
        for (int i = 0; i < partsNum; i++) {
            // Create new <par> element.
            if ((par == null) || (hasMedia && hasText)) {
                par = addPar(msgs);
                hasText = false;
                hasMedia = false;
                hasLocation = false;
            }

            PduPart part = orderedPduBody.get(i);
            String contentType = new String(part.getContentType());
            if (ContentType.isDrmType(contentType)) {
                DrmWrapper dw;
                try {
                    dw = new DrmWrapper(contentType, part.getDataUri(),
                            part.getData());
                    contentType = dw.getContentType();
                } catch (DrmException e) {
                	Logger.error(SmilHelper.class, e.getMessage(), e);
                } catch (IOException e) {
                	Logger.error(SmilHelper.class, e.getMessage(), e);
                }
            }

            if (contentType.equals(ContentType.TEXT_PLAIN)
                    || contentType.equalsIgnoreCase(ContentType.APP_WAP_XHTML)
                    || contentType.equals(ContentType.TEXT_HTML)) {
                if (hasLocation) {
                    String location = null; 
                    if (part.getContentLocation() != null) {
                        location = new String(part.getContentLocation());
                    }
                    
                    if (location != null && (location.equalsIgnoreCase("text_0.txt") || 
                            location.equalsIgnoreCase("text950.txt"))) {
                        par.addAttachment(part, contentType, false);
                    } else {
                        //dont add text_1.txt it is not required if 
                        //we have location in this par element we can get it from the vcard
                        //this also avoids creation of two slides instead of one for location
                        hasText = true;
                    }
                    continue;
                }

                par.addAttachment(part, contentType, false);
                hasText = true;
            } else if (ContentType.isImageType(contentType)) {
                if (hasMedia && !isLocationImage(hasLocation, part.getContentLocation())) {
                    par = addPar(msgs);
                    hasText = hasMedia = hasLocation = false;
                }
                par.addAttachment(part, contentType, false);
                hasMedia = true;
            } else if (ContentType.isVideoType(contentType)) {
                if (hasMedia || hasLocation) {
                    par = addPar(msgs);
                    hasText = hasMedia = hasLocation = false;
                }
                par.addAttachment(part, contentType, false);
                hasMedia = true;
            } else if (ContentType.isAudioType(contentType)) {
                if (hasMedia || hasLocation) {
                    par = addPar(msgs);
                    hasText = hasMedia = hasLocation = false;
                }
                par.addAttachment(part, contentType, false);
                hasMedia = true;
            } else if (ContentType.isVcardTextType(contentType)) {
                if (hasMedia || hasLocation) {
                    par = addPar(msgs);
                    hasText = hasMedia = hasLocation = false;
                }
                
                Uri vcardUri = part.getDataUri();
                boolean hasLocationField = false;
                if (vcardUri == null) {
                	hasLocationField = MessageUtils.hasLocation(context, part.getData());
                } else {
                	hasLocationField = MessageUtils.hasLocation(context, vcardUri);
                }
                if (hasLocationField) {
                	par.addAttachment(part, contentType, true);
                    hasLocation = true;
                } else {
                	par.addAttachment(part, contentType, false);
                    //treat vcard as a media
                    hasMedia = true;
                }
            } else if (!contentType.equals(ContentType.APP_SMIL)) {
                // TODO enable after alpha build
                //            	throw new UnsupportedOperationException("Unsupported media type " + contentType);
                if (Logger.IS_ERROR_ENABLED) {
                    //lets log it so that we will know the contentTypes which we are not
                    //supporting
                    Logger.error(SmilHelper.class, "Unsupported media type " + contentType);
                }
            }
        }

        return msgs;
    }

    
    private static MMSMessage addPar(List<MMSMessage> msgs) {
		MMSMessage msg = new MMSMessage();
		msgs.add(msg);

		return msg;
	}
    
    /*
     * This method reorders the PduBody to make sure that the location is associated with the 
     * appropriate image i.e the location image follows the corresponding location vcard
     */
 	public static PduBody reorderPartBody(PduBody pb) {
 		int size = pb.getPartsNum();
 		PduPart part;
 		String contentType;
 		PduBody orderedBody = new PduBody();
 	
 		Logger.debug(SmilHelper.class, "reorderPartBody");
 	
 		boolean hasLocation = false;
 		for (int i = 0; i < size; i++) {
 			part = pb.getPart(i);
			contentType = new String(part.getContentType());
 			
			if (ContentType.isVcardTextType(contentType)) {
					byte bs[] = part.getFilename();
					if (bs == null) {
						bs = part.getContentLocation();
					}
					String location = new String(bs);
					hasLocation = isLocationVcard(location);
					
					if (hasLocation) {
						break;
					}
 			}
 		}
 		
 		//if no location is present no need to reorder
 		if (!hasLocation) {
 			return pb;
 		}
 		
 		try {
 			for (int i = 0; i < size; i++) {
 				part = pb.getPart(i);
 				contentType = new String(part.getContentType());

 				if (ContentType.isVcardTextType(contentType)) {
 					byte bs[] = part.getFilename();
 					if (bs == null) {
 						bs = part.getContentLocation();
 					}
 					String location = new String(bs);
 					boolean hasLocationField = isLocationVcard(location);

 					if (hasLocationField) {
 						if (location != null && location.startsWith(LOCATION_VCARD_PREFIX)) {
 							int start = LOCATION_VCARD_PREFIX.length();
 							int end = location.lastIndexOf(".");
 							String id = location.substring(start);
 							if (end > 0) {
 								id = location.substring(start, end);
 							}
 							PduPart par = getLocationImagePart(pb, id);

 							//image corresponding to the location is found
 							if (par != null) {
 								//remove the old image part if already added to the orderedBody
 								int index = orderedBody.getPartIndex(par);
 								if (index != -1) {
 									orderedBody.removePart(index);
 								}

 								//add the location part followed by location image
 								orderedBody.addPart(part);
 								orderedBody.addPart(par);
 								continue;
 							}
 						}
 					}
 				} else if (ContentType.isImageType(contentType) && orderedBody.getPartIndex(part) != -1) {
 					continue;
 				}
 				orderedBody.addPart(part);
 			}
 		} catch (Exception e) {
 			Logger.error(SmilHelper.class, "reorderPartBody could not reorder the parts returning the unordered parts ", e);
			return pb;
 		}
 	
 		return orderedBody;
 	}

 	/*
 	 * This method reorders the PduBody to make sure that the location is associated with the 
 	 * appropriate image i.e the location image follows the corresponding location vcard
 	 */
 	public static List<PduPart> reorderPartBodyAsList(PduBody pb) {
 		int size = pb.getPartsNum();
 		PduPart part;
 		String contentType;
 		List<PduPart> partBody = new ArrayList<PduPart>(size);
 		List<PduPart> orderedBody = new ArrayList<PduPart>(size);
 		
 		Logger.debug(SmilHelper.class, "reorderPartBody");
 		
 		for (int i = 0; i < size; i++) {
 			part = pb.getPart(i);
 			partBody.add(part);
 		}
 		
 		try {
 			for (int i = 0; i < size; i++) {
 				part = pb.getPart(i);
 				contentType = new String(part.getContentType());
 				
 				if (ContentType.isVcardTextType(contentType)) {
 					byte bs[] = part.getFilename();
 					if (bs == null) {
 						bs = part.getContentLocation();
 					}
 					
 					if (bs != null) {
 						String location = new String(bs);
 						boolean hasLocationField = isLocationVcard(location);

 						if (hasLocationField) {
 							if (location != null && location.startsWith(LOCATION_VCARD_PREFIX)) {
 								int start = LOCATION_VCARD_PREFIX.length();
 								int end = location.lastIndexOf(".");
 								String id = location.substring(start);
 								if (end > 0) {
 									id = location.substring(start, end);
 								}
 								PduPart par = getLocationImagePart(pb, id);

 								//image corresponding to the location is found
 								if (par != null) {
 									//remove the old image part if already added to the orderedBody
 									orderedBody.remove(par);

 									//add the location part followed by location image
 									orderedBody.add(part);
 									orderedBody.add(par);
 									continue;
 								}
 							}
 						}
 					}
 				} else if (ContentType.isImageType(contentType) && orderedBody.contains(part)) {
 					continue;
 				}
 				orderedBody.add(part);
 			}
 		} catch (Exception e) {
 			Logger.error(SmilHelper.class, "reorderPartBody could not reorder the parts returning the unordered parts ", e);
 			return partBody;
 		}
 		
 		return orderedBody;
 	}

 	//returns the location image corresponding to the location vcard
 	private static PduPart getLocationImagePart(PduBody pb, String locationId) {
 		int size = pb.getPartsNum();
 		PduPart part;
 		String contentType;
 		if (Logger.IS_DEBUG_ENABLED) {
 			Logger.debug("SmilHelper.getLocationImagePart");
 		}

 		for (int i = 0; i < size; i++) {
 			part = pb.getPart(i);
 			contentType = PduPersister.toIsoString(part.getContentType());

 			if (ContentType.isImageType(contentType) && isLocationImageOfPart(locationId, part)) {
 				if (Logger.IS_DEBUG_ENABLED) {
 					Logger.debug("SmilHelper.getLocationImagePart found image corresponding to the location");
 				}
 				return part;
 			}
 		}
 		return null;
 	}
 	
 	/**
      * This Method checks if the image is associated with proper location
      * message
      * @param hasLocation
      * @param bs
      * @return
      */
     private static boolean isLocationImageOfPart(String locationId, PduPart part) {
     	byte[] bs = part.getFilename();
     	
     	if (bs == null) {
     		bs = part.getContentLocation();
     	}
     	String contentLocation = bs != null? new String(bs) : null;
     	if (locationId != null && contentLocation != null && contentLocation.startsWith(LOCATION_IMG_PREFIX + locationId)) {
     		return true;
     	}
     	return false;
     }
     
   //TODO: check if there is a need to opent the vcard file to verify if it is an location attachment
 	private static boolean isLocationVcard(String locationName) {
 		if (locationName.startsWith(LOCATION_VCARD_PREFIX)) {
 			return true;
 		}
 		return false;
 	}
}
