/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.verizon.internal.telephony;


/**
 * Internal interface used to control the phone; SDK developers cannot
 * obtain this interface.
 *
 * {@hide}
 *
 */
public interface Phone {

    /** used to enable additional debug messages */
    static final boolean DEBUG_PHONE = true;


    /**
     * The phone state. One of the following:<p>
     * <ul>
     * <li>IDLE = no phone activity</li>
     * <li>RINGING = a phone call is ringing or call waiting.
     *  In the latter case, another call is active as well</li>
     * <li>OFFHOOK = The phone is off hook. At least one call
     * exists that is dialing, active or holding and no calls are
     * ringing or waiting.</li>
     * </ul>
     */
    enum State {
        IDLE, RINGING, OFFHOOK;
    };

    /**
     * The state of a data connection.
     * <ul>
     * <li>CONNECTED = IP traffic should be available</li>
     * <li>CONNECTING = Currently setting up data connection</li>
     * <li>DISCONNECTED = IP not available</li>
     * <li>SUSPENDED = connection is created but IP traffic is
     *                 temperately not available. i.e. voice call is in place
     *                 in 2G network</li>
     * </ul>
     */
    enum DataState {
        CONNECTED, CONNECTING, DISCONNECTED, SUSPENDED;
    };

    public enum DataActivityState {
        /**
         * The state of a data activity.
         * <ul>
         * <li>NONE = No traffic</li>
         * <li>DATAIN = Receiving IP ppp traffic</li>
         * <li>DATAOUT = Sending IP ppp traffic</li>
         * <li>DATAINANDOUT = Both receiving and sending IP ppp traffic</li>
         * <li>DORMANT = The data connection is still active,
                                     but physical link is down</li>
         * </ul>
         */
        NONE, DATAIN, DATAOUT, DATAINANDOUT, DORMANT;
    };

    enum SuppService {
      UNKNOWN, SWITCH, SEPARATE, TRANSFER, CONFERENCE, REJECT, HANGUP;
    };

    static final String STATE_KEY = "state";
    static final String PHONE_NAME_KEY = "phoneName";
    static final String FAILURE_REASON_KEY = "reason";
    static final String STATE_CHANGE_REASON_KEY = "reason";
    static final String DATA_APN_TYPES_KEY = "apnType";
    static final String DATA_APN_KEY = "apn";

    static final String DATA_IFACE_NAME_KEY = "iface";
    static final String NETWORK_UNAVAILABLE_KEY = "networkUnvailable";
    static final String PHONE_IN_ECM_STATE = "phoneinECMState";

    /**
     * APN types for data connections.  These are usage categories for an APN
     * entry.  One APN entry may support multiple APN types, eg, a single APN
     * may service regular internet traffic ("default") as well as MMS-specific
     * connections.<br/>
     * APN_TYPE_ALL is a special type to indicate that this APN entry can
     * service all data connections.
     */
    static final String APN_TYPE_ALL = "*";
    /** APN type for default data traffic */
    static final String APN_TYPE_DEFAULT = "default";
    /** APN type for MMS traffic */
    static final String APN_TYPE_MMS = "mms";
    /** APN type for SUPL assisted GPS */
    static final String APN_TYPE_SUPL = "supl";
    /** APN type for DUN traffic */
    static final String APN_TYPE_DUN = "dun";
    /** APN type for HiPri traffic */
    static final String APN_TYPE_HIPRI = "hipri";

    // "Features" accessible through the connectivity manager
    static final String FEATURE_ENABLE_MMS = "enableMMS";
    static final String FEATURE_ENABLE_SUPL = "enableSUPL";
    static final String FEATURE_ENABLE_DUN = "enableDUN";
    static final String FEATURE_ENABLE_HIPRI = "enableHIPRI";

    /**
     * Return codes for <code>enableApnType()</code>
     */
    static final int APN_ALREADY_ACTIVE     = 0;
    static final int APN_REQUEST_STARTED    = 1;
    static final int APN_TYPE_NOT_AVAILABLE = 2;
    static final int APN_REQUEST_FAILED     = 3;


    /**
     * Optional reasons for disconnect and connect
     */
    static final String REASON_ROAMING_ON = "roamingOn";
    static final String REASON_ROAMING_OFF = "roamingOff";
    static final String REASON_DATA_DISABLED = "dataDisabled";
    static final String REASON_DATA_ENABLED = "dataEnabled";
    static final String REASON_GPRS_ATTACHED = "gprsAttached";
    static final String REASON_GPRS_DETACHED = "gprsDetached";
    static final String REASON_CDMA_DATA_ATTACHED = "cdmaDataAttached";
    static final String REASON_CDMA_DATA_DETACHED = "cdmaDataDetached";
    static final String REASON_APN_CHANGED = "apnChanged";
    static final String REASON_APN_SWITCHED = "apnSwitched";
    static final String REASON_APN_FAILED = "apnFailed";
    static final String REASON_RESTORE_DEFAULT_APN = "restoreDefaultApn";
    static final String REASON_RADIO_TURNED_OFF = "radioTurnedOff";
    static final String REASON_PDP_RESET = "pdpReset";
    static final String REASON_VOICE_CALL_ENDED = "2GVoiceCallEnded";
    static final String REASON_VOICE_CALL_STARTED = "2GVoiceCallStarted";
    static final String REASON_PS_RESTRICT_ENABLED = "psRestrictEnabled";
    static final String REASON_PS_RESTRICT_DISABLED = "psRestrictDisabled";
    static final String REASON_SIM_LOADED = "simLoaded";

    // Used for band mode selection methods
    static final int BM_UNSPECIFIED = 0; // selected by baseband automatically
    static final int BM_EURO_BAND   = 1; // GSM-900 / DCS-1800 / WCDMA-IMT-2000
    static final int BM_US_BAND     = 2; // GSM-850 / PCS-1900 / WCDMA-850 / WCDMA-PCS-1900
    static final int BM_JPN_BAND    = 3; // WCDMA-800 / WCDMA-IMT-2000
    static final int BM_AUS_BAND    = 4; // GSM-900 / DCS-1800 / WCDMA-850 / WCDMA-IMT-2000
    static final int BM_AUS2_BAND   = 5; // GSM-900 / DCS-1800 / WCDMA-850
    static final int BM_BOUNDARY    = 6; // upper band boundary


}
