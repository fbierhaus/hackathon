/**
 * SyncEvents.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.messaging.vzmsgs.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * This class is used to
 * 
 * @author Jegadeesan M
 * @Since Nov 26, 2012
 */
public class SyncItem implements BaseColumns {

    public static final String TABLE_NAME = "sync_items";
    public static final Uri CONTENT_URI = Uri.parse("content://" + ApplicationProvider.AUTHORITY + "/"
            + TABLE_NAME);
    public static final String _ITEM_ID = "luid";
    public static final String _TYPE = "type";
    public static final String _ACTION = "action";
    public static final String _PRIORITY = "priority";
    public static final String _RETRY_COUNT = "retry_count";
    public static final String _LAST_PRIORITY = "last_priority";

    public long id;
    public long itemId;
    public ItemType type;
    public ItemAction action;
    public ItemPriority priority;
    public ItemPriority lastPriority;
    public int retryCount;

    // public final class ItemPriority {
    // public static final int CRITICAL = 1;
    // public static final int HIGH = 2;
    // public static final int MEDIUM = 3;
    // public static final int LOW = 4;
    // public static final int UNKNOWN = 0;
    // }
    //
    // public final class ItemType {
    // public static final int UNKNOWN = 0;
    // public static final int SMS = 1;
    // public static final int MMS = 2;
    // public static final int CONVERSATION = 3;
    // public static final int VMA_MSG = 4;
    // }
    //
    // public final class ItemAction {
    // // UNKNOWN, SEND, READ, DELETE, FETCH, FETCH_ATTACHMENT;
    // public static final int UNKNOWN = 0;
    // public static final int SEND = 1;
    // public static final int READ = 2;
    // public static final int DELETE = 3;
    // public static final int FETCH = 4;
    // public static final int FETCH_ATTACHMENT = 5;
    // }

    public enum ItemPriority {

        /*
         * This is unused by SyncItem
         */
        MAX_PRIORITY(100),

        /*
         * Foreground uses these values.
         */
        CRITICAL(99), // Any UI action
        HIGH(75), // Read or delete events

        /*
         * Not used by SyncItem
         */
        MIN_PRIORITY(50),

        /*
         * Partial changes more than 10 times while doing enqueue XMCR.
         */
        CRITICAL_UPDATES(49),

        /*
         * Partial sync attachments download
         */
        CRITICAL_ATTACHEMENT(48),
        /*
         * Background uses priority from below
         */
        MEDIUM(40), // Full sync horizontal top 25
        LOW(20), // Full sync & Attachments

        /*
         * These priority is used to filter out no UID or LUID
         */
        NO_UID_OR_LUID(-2),

        ONDEMAND_MAX(1099), ONDEMAND_CRITICAL(1010), ONDEMAND_ATTACHMENT(1008), ONDEMAND_MIN(1006),

        FULLSYNC_MAX(799), FULLSYNC_CRITICAL(710), FULLSYNCOLDER_MESSAGES(708),

        FULLSYNC_ATTACHMENT(706), FULLSYNCOLDER_ATTACHMENT(704), FULLSYNC_MIN(700),

        SEND_MAX(499),

        SENDSMS_CRITICAL(410), // SEND_SMS
        SENDMMS_CRITICAL(408), // SEND_MMS
        SENDMMS_ATTACHEMENT(406), //

        SEND_MIN(400),

        INITIALSYNC_MAX(299), 
        INITIALSYNC_CRITICAL(294),
        INITIALSYNC_MIN(200),

        /*
         * These two are not picked by any thread
         */
        UNKNOWN(0), DEFFERED(-1), PERMANENT_FAILURE(-3);

        private int value;

        private ItemPriority(int value) {
            this.value = value;
        }

        public static ItemPriority toEnum(int value) {
            for (ItemPriority pri : values()) {
                if (pri.getValue() == value) {
                    return pri;
                }
            }
            return UNKNOWN;
        }

        public int getValue() {
            return value;
        }

        public ItemPriority getPriorityForAttachment() {
            String me = this.toString();
            String[] strings = me.split("_");
            return ItemPriority.valueOf(strings[0] + "_ATTACHMENT");
        }

        public static boolean isFullSyncItemPriority(ItemPriority priority) {
            return (priority.getValue() >= FULLSYNC_MIN.getValue() && priority.getValue() <= FULLSYNC_MAX
                    .getValue());
        }

        /**
         * This Method
         * 
         * @param itemPriority
         * @return
         */
        public static boolean isFetchPriority(ItemPriority priority) {
            return (priority.getValue() >= FULLSYNC_MIN.getValue() && priority.getValue() <= ONDEMAND_MAX
                    .getValue());
        }

        /**
         * This Method
         * 
         * @param itemPriority
         * @return
         */
        public static boolean isSendPriority(ItemPriority priority) {
            return (priority.getValue() >= SEND_MIN.getValue() && priority.getValue() <= SEND_MAX.getValue());
        }

        /**
         * This Method is used verify the given is intial sync priority
         * 
         * @param itemPriority
         * @return
         */
        public static boolean isInitialSyncPriority(ItemPriority priority) {
            return (priority.getValue() >= INITIALSYNC_MIN.getValue() && priority.getValue() <= INITIALSYNC_MAX
                    .getValue());
        }
    }

    public enum ItemType {
        UNKNOWN(0), SMS(1), MMS(2), CONVERSATION(3), VMA_MSG(4);
        private int value;

        /**
         * 
         * Constructor
         */
        private ItemType(int value) {
            this.value = value;
        }

        public static ItemType toEnum(int value) {
            for (ItemType type : values()) {
                if (type.getValue() == value) {
                    return type;
                }
            }
            return UNKNOWN;
        }

        public int getValue() {
            return value;
        }
    }

    public enum ItemAction {
        UNKNOWN(0), SEND(1), READ(2), DELETE(3), FETCH(4), FETCH_ATTACHMENT(5), FETCH_CHANGES(6), FULLSYNC(7), SEND_SMS(
                8), SEND_MMS(9), FETCH_MESSAGE(10), FETCH_MESSAGE_HEADERS(11), SENDMMS_ATTACHEMENT(12);
        private int value;

        private ItemAction(int value) {
            this.value = value;
        }

        public static ItemAction toEnum(int value) {
            for (ItemAction action : values()) {
                if (action.getValue() == value) {
                    return action;
                }
            }
            return UNKNOWN;
        }

        public int getValue() {
            return value;
        }
    }

    public boolean isConversation() {
        return type == ItemType.CONVERSATION;
    }

    public boolean isSMS() {
        return type == ItemType.SMS;
    }

    public boolean isMMS() {
        return type == ItemType.MMS;
    }

    public boolean isDeleted() {
        return action == ItemAction.DELETE;
    }

    public boolean isRead() {
        return action == ItemAction.READ;
    }

    public boolean isSend() {
        return action == ItemAction.SEND_SMS || action == ItemAction.SEND_MMS ;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("id=" + id);
        builder.append(",itemId=" + itemId);
        builder.append(",type=" + type);
        builder.append(",action=" + action);
        builder.append(",priority=" + priority);
        builder.append(",retryCount=" + retryCount);
        return builder.toString();
    }

    /**
     * This Method
     * 
     * @return
     */
    public boolean isFullSyncItem() {
        return ItemPriority.isFullSyncItemPriority(priority);
    }

}
