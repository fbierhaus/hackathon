package com.verizon.mms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.helper.BitmapManager;
import com.verizon.mms.util.SqliteWrapper;

/**
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * @Since Mar 28, 2012
 */
public class MmsScanner extends AbstractScanner {

	// Projection
	static final String[] projection = new String[] { "_id", "thread_id",
			"read", "date", "ct_t", "m_type" };
	static final String M_CT = "application/vnd.wap.multipart.related";
	// context
	private final Context ctx;

	public MmsScanner(Context ctx) {
		this.ctx = ctx;
	}

	@Override
	int getNumberOfMessages(long thread) {
		int count = 0;
		// Query (sort by _id, that is process the latest ones first)
		Cursor cursor = SqliteWrapper.query(ctx,VZUris.getMmsUri(),
				new String[] { "_id" }, "thread_id = " + thread, null, null);
		if (null != cursor) {
			count = cursor.getCount();
			cursor.close();
		}

		return count;
	}

	@Override
	void delete(long id) {
		// First, delete this thread
		String where = MediaProvider.Helper.M_ID + " = " + id + " AND "	+ MediaProvider.Helper.M_TYPE + " > 2";
		
		int count = SqliteWrapper.delete(ctx,MediaSyncService.CACHE_URI, where, null);
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "==> delete, where=" + where + ", result=" + count);
		}
	}
	
	@Override
	void scan(String selection, Emitter emitter) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "scan: querying uri=" + VZUris.getMmsUri() + " selection=" + selection);
        }
		// Query (sort by _id, that is process the latest ones first)
		Cursor cursor = SqliteWrapper.query(ctx,VZUris.getMmsUri(),
				projection, selection, null, "_id");

		if (cursor == null) {
			Logger.error(getClass(), "scan: null cursor with selection (" + selection + ")");
			return;
		}
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "scan: count=" + cursor.getCount());
        }

        final BitmapManager mgr = BitmapManager.INSTANCE;
        try {
        	while (cursor.moveToNext()) {
				int mid = cursor.getInt(cursor.getColumnIndex("_id"));
				int threadId = cursor
						.getInt(cursor.getColumnIndex("thread_id"));
				int read = cursor.getInt(cursor.getColumnIndex("read"));
				String contentType = cursor.getString(cursor
						.getColumnIndex("ct_t"));
				long date = cursor.getLong(cursor.getColumnIndex("date")) * 1000; // date
				int type = cursor.getInt(cursor.getColumnIndex("m_type"));

				// Cache each part separately
				List<MmsPart> mmsParts = extractMmsParts(mid);
				if (null == mmsParts) {
					continue;
				}

				// fetch addresses
				String address = join(extractMmsAddresses(mid));
				for (MmsPart mmsPart : mmsParts) {
					// Ignore "application/smil" ==> we don't need it now.
					if (mmsPart.ct.equalsIgnoreCase("application/smil")) {
						continue;
					}

                    Media media = new Media(threadId, mid, mmsPart._id, address, contentType, mmsPart.ct,
                            mmsPart.text, type, read, date, 0, 0);

                    if (!media.isLocation() || media.hasLocation(ctx)) {
                        final boolean isImage = media.isImage();
                        if (isImage || media.isVideo()) {
                        	final Rect size;
                            if (isImage) {
                                size = mgr.getBitmapSize(ctx, media.getImageUri());
                            } else {
                                size = mgr.getVideoThumbnailSize(ctx, media.getVideoUri());
                            }
                            if (size != null) {
                    			media.setWidth(size.width());
                    			media.setHeight(size.height());
                            }
                        }
                    } else {
                    	// contact
                    	media.setmCt("application/vnd.wap.multipart.related");
                    	media.setmPartCt("text/namecard");
					}
					emitter.emit(media);

					if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "scan: added " + media);
                    }
                }
			}
		}
        catch (Exception e) {
        	Logger.error(getClass(), e);
        }
        finally {
        	cursor.close();
        }
	}

	/**
	 * Fetch all parts of this MMS
	 * 
	 * @param mid
	 *            MMS Id
	 * @return
	 */
	private List<MmsPart> extractMmsParts(int mid) {
		// Parts
		String selection = "mid=" + mid;
		Uri uri = Uri.parse("content://" + VZUris.getMmsUri().getAuthority()
				+ "/part");
		Cursor cursor = SqliteWrapper.query(ctx,uri, null, selection,
				null, null);
		if (cursor == null) {
			return null;
		}

		List<MmsPart> parts = new ArrayList<MmsPart>();
		try {
			// move to last
			if (cursor.moveToLast()) {
				boolean isLocation = false;
				do {
					String ct = cursor.getString(cursor.getColumnIndex("ct"));
					int _id = cursor.getInt(cursor.getColumnIndex("_id"));
					String text = cursor.getString(cursor.getColumnIndex("text"));
					if (ct.equalsIgnoreCase("text/x-vcard")) {
						isLocation = true;
						parts.add(new MmsPart(mid, _id, ct, text));
					}
					if (!isLocation) {
						parts.add(new MmsPart(mid, _id, ct, text));
					}
	
					// Check for possible links/emails
					if (ct.equals("text/plain") && null != text) {
						int counter = _id * 100; // initialize a counter for dummy
													// part-id
	
						Map<String, String> map = extractUris(text);
						for (String key : map.keySet()) {
							String mPartUri = key;
							String mPartCt = map.get(key);
							parts.add(new MmsPart(mid, counter++, mPartCt, mPartUri));
						}
					}
	
					if (isLocation) {
						for (int i = 0; i < parts.size(); i++) {
							if (!parts.get(i).ct.equals(Media.M_EMAIL_CT)
									&& !parts.get(i).ct.equals(Media.M_LINK_CT)
									&& !parts.get(i).ct.equals(Media.M_PHONE_CT)
									&& !parts.get(i).ct
											.equalsIgnoreCase("text/x-vcard")) {
								parts.remove(i);
							}
						}
					}
	
				} while (cursor.moveToPrevious());
			}
		}
		catch (Exception e) {
			Logger.error(getClass(), e);
		}
		finally {
			cursor.close();
		}

		return parts;
	}

	/**
	 * Fetch all relevant addresses for this MMS.
	 * 
	 * @param mid
	 *            MMS Id
	 * @return
	 */
	private List<String> extractMmsAddresses(int mid) {
		Uri uri = Uri.parse("content://" + VZUris.getMmsUri().getAuthority()
				+ "/" + mid + "/addr");
		String selectionAdd = "msg_id = " + mid + " AND type = " + 137;
		Cursor cursor = SqliteWrapper.query(ctx,uri, null, selectionAdd,null, null);

		if (cursor == null) {
			return null;
		}

		List<String> addresses = new ArrayList<String>();
		try {
			if (cursor.moveToFirst()) {
				do {
					// int _id = cursor.getInt(cursor.getColumnIndex("_id"));
					// int type = cursor.getInt(cursor.getColumnIndex("type"));
					// String contact_id =
					// cursor.getString(cursor.getColumnIndex("contact_id"));
					String address = cursor.getString(cursor
							.getColumnIndex("address"));
	
					// log.debug(" > Address: mid={}, _id={}, contact_id={}, address={}, type={}",
					// new Object[] {
					// mid, _id, contact_id, address, type });
	
					if ("insert-address-token".equalsIgnoreCase(address)) {
						address = "Me"; // Shared by me! :)
					}
	
					addresses.add(address);
	
					//
					// // Select Address with Digits
					// String _add = removeNonDigitsFromText(address);
					// if (_add.length() > 0) {
					// addresses.add(_add);
					// }
				} while (cursor.moveToNext());
			}
		}
		catch (Exception e) {
			Logger.error(getClass(), e);
		}
		finally {
			cursor.close();
		}

		return addresses;
	}

	/**
	 * Holder class for MMS Part information
	 * 
	 * @author "Animesh Kumar <animesh@strumsoft.com>"
	 * 
	 */
	private static class MmsPart {
		String ct;
		String text;
		int _id;
		int mid;

		public MmsPart(int mid, int _id, String ct, String text) {
			this.ct = ct;
			this._id = _id;
			this.mid = mid;
			this.text = text;
		}

		@Override
		public String toString() {
			return "MmsPart #==> _id=" + _id + ", mid=" + mid + ", ct=" + ct;
		}

	}
}