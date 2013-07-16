/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.verizon.vcard.android.provider;

/**
 * The Contacts provider stores all information about contacts.
 */
public class VCardContacts {
    private static final String TAG = "Contacts";

    public static final String AUTHORITY = "contacts";

    /** Signifies an email address row that is stored in the ContactMethods table */
    public static final int KIND_EMAIL = 1;
    /** Signifies a postal address row that is stored in the ContactMethods table */
    public static final int KIND_POSTAL = 2;
    /** Signifies an IM address row that is stored in the ContactMethods table */
    public static final int KIND_IM = 3;
    /** Signifies an Organization row that is stored in the Organizations table */
    public static final int KIND_ORGANIZATION = 4;
    /** Signifies an Phone row that is stored in the Phones table */
    public static final int KIND_PHONE = 5;
    /** When the type of address is not specified */
    public static final int KIND_NONE = 6;

    /**
     * no public constructor since this is a utility class
     */
    private VCardContacts() {}

    /**
     * Columns from the People table that other tables join into themselves.
     */
    public interface PeopleColumns {
        /**
         * The person's name.
         * <P>Type: TEXT</P>
         */
        public static final String NAME = "name";

        /**
         * Phonetic equivalent of the person's name, in a locale-dependent
         * character set (e.g. hiragana for Japanese).
         * Used for pronunciation and/or collation in some languages.
         * <p>Type: TEXT</P>
         */
        public static final String PHONETIC_NAME = "phonetic_name";

        /**
         * The display name. If name is not null name, else if number is not null number,
         * else if email is not null email.
         * <P>Type: TEXT</P>
         */
        public static final String DISPLAY_NAME = "display_name";

        /**
         * The field for sorting list phonetically. The content of this field
         * may not be human readable but phonetically sortable.
         * <P>Type: TEXT</p>
         * @hide Used only in Contacts application for now.
         */
        public static final String SORT_STRING = "sort_string";

        /**
         * Notes about the person.
         * <P>Type: TEXT</P>
         */
        public static final String NOTES = "notes";

        /**
         * The number of times a person has been contacted
         * <P>Type: INTEGER</P>
         */
        public static final String TIMES_CONTACTED = "times_contacted";

        /**
         * The last time a person was contacted.
         * <P>Type: INTEGER</P>
         */
        public static final String LAST_TIME_CONTACTED = "last_time_contacted";

        /**
         * A custom ringtone associated with a person. Not always present.
         * <P>Type: TEXT (URI to the ringtone)</P>
         */
        public static final String CUSTOM_RINGTONE = "custom_ringtone";

        /**
         * Whether the person should always be sent to voicemail. Not always
         * present.
         * <P>Type: INTEGER (0 for false, 1 for true)</P>
         */
        public static final String SEND_TO_VOICEMAIL = "send_to_voicemail";

        /**
         * Is the contact starred?
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String STARRED = "starred";

        /**
         * The server version of the photo
         * <P>Type: TEXT (the version number portion of the photo URI)</P>
         */
        public static final String PHOTO_VERSION = "photo_version";       
    }
    /**
     * A sub directory of a single person that contains all of their Phones.
     */
    public static final class Phones implements BaseColumns, PhonesColumns,
    PeopleColumns {
        /**
         * no public constructor since this is a utility class
         */
        private Phones() {}

        /**
         * The directory twig for this sub-table
         */
        public static final String CONTENT_DIRECTORY = "phones";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "number ASC";
    }

    /**
     * A subdirectory of a single person that contains all of their
     * ContactMethods.
     */
    public static final class ContactMethods
    implements BaseColumns, ContactMethodsColumns, PeopleColumns {
        /**
         * no public constructor since this is a utility class
         */
        private ContactMethods() {}

        /**
         * The directory twig for this sub-table
         */
        public static final String CONTENT_DIRECTORY = "contact_methods";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "data ASC";
    }
    /**
     * Columns from the Phones table that other columns join into themselves.
     */
    public interface PhonesColumns {
        /**
         * The type of the the phone number.
         * <P>Type: INTEGER (one of the constants below)</P>
         */
        public static final String TYPE = "type";

        public static final int TYPE_CUSTOM = 0;
        public static final int TYPE_HOME = 1;
        public static final int TYPE_MOBILE = 2;
        public static final int TYPE_WORK = 3;
        public static final int TYPE_FAX_WORK = 4;
        public static final int TYPE_FAX_HOME = 5;
        public static final int TYPE_PAGER = 6;
        public static final int TYPE_OTHER = 7;

        /**
         * The user provided label for the phone number, only used if TYPE is TYPE_CUSTOM.
         * <P>Type: TEXT</P>
         */
        public static final String LABEL = "label";

        /**
         * The phone number as the user entered it.
         * <P>Type: TEXT</P>
         */
        public static final String NUMBER = "number";

        /**
         * The normalized phone number
         * <P>Type: TEXT</P>
         */
        public static final String NUMBER_KEY = "number_key";

        /**
         * Whether this is the primary phone number
         * <P>Type: INTEGER (if set, non-0 means true)</P>
         */
        public static final String ISPRIMARY = "isprimary";
    }
    /**
     * Columns from the ContactMethods table that other tables join into
     * themseleves.
     */
    public interface ContactMethodsColumns {
        /**
         * The kind of the the contact method. For example, email address,
         * postal address, etc.
         * <P>Type: INTEGER (one of the values below)</P>
         */
        public static final String KIND = "kind";

        /**
         * The type of the contact method, must be one of the types below.
         * <P>Type: INTEGER (one of the values below)</P>
         */
        public static final String TYPE = "type";
        public static final int TYPE_CUSTOM = 0;
        public static final int TYPE_HOME = 1;
        public static final int TYPE_WORK = 2;
        public static final int TYPE_OTHER = 3;
        public static final int TYPE_MOBILE = 4;

        /**
         * @hide This is temporal. TYPE_MOBILE should be added to TYPE in the future.
         */
        public static final int MOBILE_EMAIL_TYPE_INDEX = 2;

        /**
         * @hide This is temporal. TYPE_MOBILE should be added to TYPE in the future.
         * This is not "mobile" but "CELL" since vCard uses it for identifying mobile phone.
         */
        public static final String MOBILE_EMAIL_TYPE_NAME = "_AUTO_CELL";

        /**
         * The user defined label for the the contact method.
         * <P>Type: TEXT</P>
         */
        public static final String LABEL = "label";

        /**
         * The data for the contact method.
         * <P>Type: TEXT</P>
         */
        public static final String DATA = "data";

        /**
         * Auxiliary data for the contact method.
         * <P>Type: TEXT</P>
         */
        public static final String AUX_DATA = "aux_data";

        /**
         * Whether this is the primary organization
         * <P>Type: INTEGER (if set, non-0 means true)</P>
         */
        public static final String ISPRIMARY = "isprimary";
    }
    /**
     * Columns from the Organizations table that other columns join into themselves.
     */
    public interface OrganizationColumns {
        /**
         * The type of the organizations.
         * <P>Type: INTEGER (one of the constants below)</P>
         */
        public static final String TYPE = "type";

        public static final int TYPE_CUSTOM = 0;
        public static final int TYPE_WORK = 1;
        public static final int TYPE_OTHER = 2;

        /**
         * The user provided label, only used if TYPE is TYPE_CUSTOM.
         * <P>Type: TEXT</P>
         */
        public static final String LABEL = "label";

        /**
         * The name of the company for this organization.
         * <P>Type: TEXT</P>
         */
        public static final String COMPANY = "company";

        /**
         * The title within this organization.
         * <P>Type: TEXT</P>
         */
        public static final String TITLE = "title";

        /**
         * The person this organization is tied to.
         * <P>Type: TEXT</P>
         */
        public static final String PERSON_ID = "person";

        /**
         * Whether this is the primary organization
         * <P>Type: INTEGER (if set, non-0 means true)</P>
         */
        public static final String ISPRIMARY = "isprimary";
    }
}
