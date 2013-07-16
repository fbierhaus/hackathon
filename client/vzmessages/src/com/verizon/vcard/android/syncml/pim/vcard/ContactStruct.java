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

package com.verizon.vcard.android.syncml.pim.vcard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.graphics.Bitmap;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Base64;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.util.BitmapManager;
import com.verizon.vcard.android.provider.VCardContacts;
import com.verizon.vcard.android.syncml.pim.PropertyNode;
import com.verizon.vcard.android.syncml.pim.VNode;

/**
 * The parameter class of VCardComposer.
 * This class standy by the person-contact in
 * Android system, we must use this class instance as parameter to transmit to
 * VCardComposer so that create vCard string.
 */
// TODO: rename the class name, next step
public class ContactStruct {
    // Note: phonetic name probably should be "LAST FIRST MIDDLE" for European languages, and
    //       space should be added between each element while it should not be in Japanese.
    //       But unfortunately, we currently do not have the data and are not sure whether we should
    //       support European version of name ordering.
    //
    // TODO: Implement the logic described above if we really need European version of
    //        phonetic name handling. Also, adding the appropriate test case of vCard would be
    //        highly appreciated.
    private static final int NAME_ORDER_TYPE_ENGLISH = 0;
    private static final int NAME_ORDER_TYPE_JAPANESE = 1;

    public static final String VZW_LOCATION = "X-VZW-NGM-LOC";
    
    /** MUST exist */
    private String name;
    private String phoneticName;
    /** maybe folding */
    private List<String> notes = new ArrayList<String>();
    /** maybe folding */
    private String title;
    
    /** The type of Photo (e.g. JPEG, BMP, etc.) */
    private String photoType;
    /** Only for GET. Use addPhoneList() to PUT. */
    private List<PhoneData> phoneList;
    /** Only for GET. Use addContactmethodList() to PUT. */
    private List<ContactMethod> contactmethodList;
    /** Only for GET. Use addOrgList() to PUT. */
    private List<OrganizationData> organizationList;
    /** Only for GET. Use addExtension() to PUT */
    private Map<String, List<String>> extensionMap;

    private String url;
    
    private String geo;
    
    private String vzwLocation;
    
    private Bitmap contactPicture;
    
    public static final String ADDRESS_SEPERATOR = ", ";

    // Use organizationList instead when handling ORG.
    @Deprecated
    private String company;
    
    public static class PhoneData {
        public int type;
        /** maybe folding */
        public String data;
        public String label;
        public boolean isPrimary; 
    }

    public static class ContactMethod {
        // Contacts.KIND_EMAIL, Contacts.KIND_POSTAL
        public int kind;
        // e.g. Contacts.ContactMethods.TYPE_HOME, Contacts.PhoneColumns.TYPE_HOME
        // If type == Contacts.PhoneColumns.TYPE_CUSTOM, label is used.
        public int type;
        public String data;
        // Used only when TYPE is TYPE_CUSTOM.
        public String label;
        public boolean isPrimary;
    }
    
    public static class OrganizationData {
        public int type;
        public String companyName;
        public String positionName;
        public boolean isPrimary;
    }
    
    public String getName(){
        return name;
    }
    public String getCompany(){
        return company;
    }
    
    public List<String> getNotes(){
        return notes;
    }
    
    public String getTitle(){
        return title;
    }
    
    public List<PhoneData> getPhoneList(){
        return phoneList;
    }
    
    public List<ContactMethod> getContactMethodsList(){
        return contactmethodList;
    }
    
    public List<OrganizationData> getOrganizationalData(){
        return organizationList;
    }
    public Bitmap getContactPicture(){
        return contactPicture;
    }
    /**
     * Add a phone info to phoneList.
     * @param data phone number
     * @param type type col of content://contacts/phones
     * @param label lable col of content://contacts/phones
     */
    public void addPhone(int type, String data, String label, boolean isPrimary){
        if (phoneList == null) {
            phoneList = new ArrayList<PhoneData>();
        }
        PhoneData phoneData = new PhoneData();
        phoneData.type = type;
        
        StringBuilder builder = new StringBuilder();
        String trimed = data.trim();
        int length = trimed.length();
        for (int i = 0; i < length; i++) {
            char ch = trimed.charAt(i);
            if (('0' <= ch && ch <= '9') || (i == 0 && ch == '+')) {
                builder.append(ch);
            }
        }
        phoneData.data = PhoneNumberUtils.formatNumber(builder.toString());
        phoneData.label = label;
        phoneData.isPrimary = isPrimary;
        phoneList.add(phoneData);
    }

    /**
     * Add a contactmethod info to contactmethodList.
     * @param kind integer value defined in Contacts.java
     * (e.g. Contacts.KIND_EMAIL)
     * @param type type col of content://contacts/contact_methods
     * @param data contact data
     * @param label extra string used only when kind is Contacts.KIND_CUSTOM.
     */
    public void addContactmethod(int kind, int type, String data,
            String label, boolean isPrimary){
        if (contactmethodList == null) {
            contactmethodList = new ArrayList<ContactMethod>();
        }
        ContactMethod contactMethod = new ContactMethod();
        contactMethod.kind = kind;
        contactMethod.type = type;
        contactMethod.data = data;
        contactMethod.label = label;
        contactMethod.isPrimary = isPrimary;
        contactmethodList.add(contactMethod);
    }
    
    /**
     * Add a Organization info to organizationList.
     */
    public void addOrganization(int type, String companyName, String positionName,
            boolean isPrimary) {
        if (organizationList == null) {
            organizationList = new ArrayList<OrganizationData>();
        }
        OrganizationData organizationData = new OrganizationData();
        organizationData.type = type;
        organizationData.companyName = companyName;
        organizationData.positionName = positionName;
        organizationData.isPrimary = isPrimary;
        organizationList.add(organizationData);
    }

    /**
     * Set "position" value to the appropriate data. If there's more than one
     * OrganizationData objects, the value is set to the last one. If there's no
     * OrganizationData object, a new OrganizationData is created, whose company name is
     * empty.  
     * 
     * TODO: incomplete logic. fix this:
     * 
     * e.g. This assumes ORG comes earlier, but TITLE may come earlier like this, though we do not
     * know how to handle it in general cases...
     * ----
     * TITLE:Software Engineer
     * ORG:Google
     * ----
     */
    public void setPosition(String positionValue) {
        if (organizationList == null) {
            organizationList = new ArrayList<OrganizationData>();
        }
        int size = organizationList.size();
        if (size == 0) {
            addOrganization(VCardContacts.OrganizationColumns.TYPE_OTHER, "", null, false);
            size = 1;
        }
        OrganizationData lastData = organizationList.get(size - 1);
        lastData.positionName = positionValue;
    }
    
    public void addExtension(PropertyNode propertyNode) {
        if (propertyNode.propValue.length() == 0) {
            return;
        }
        // Now store the string into extensionMap.
        List<String> list;
        String name = propertyNode.propName;
        if (extensionMap == null) {
            extensionMap = new HashMap<String, List<String>>();
        }
        if (!extensionMap.containsKey(name)){
            list = new ArrayList<String>();
            extensionMap.put(name, list);
        } else {
            list = extensionMap.get(name);
        }        
        
        list.add(propertyNode.encode());
    }
    
    private static String getNameFromNProperty(List<String> elems, int nameOrderType) {
        // Family, Given, Middle, Prefix, Suffix. (1 - 5)
        int size = elems.size();
        if (size > 1) {
            StringBuilder builder = new StringBuilder();
            boolean builderIsEmpty = true;
            // Prefix
            if (size > 3 && elems.get(3).length() > 0) {
                builder.append(elems.get(3));
                builderIsEmpty = false;
            }
            String first, second;
            if (nameOrderType == NAME_ORDER_TYPE_JAPANESE) {
                first = elems.get(0);
                second = elems.get(1);
            } else {
                first = elems.get(1);
                second = elems.get(0);
            }
            if (first.length() > 0) {
                if (!builderIsEmpty) {
                    builder.append(' ');
                }
                builder.append(first);
                builderIsEmpty = false;
            }
            // Middle name
            if (size > 2 && elems.get(2).length() > 0) {
                if (!builderIsEmpty) {
                    builder.append(' ');
                }
                builder.append(elems.get(2));
                builderIsEmpty = false;
            }
            if (second.length() > 0) {
                if (!builderIsEmpty) {
                    builder.append(' ');
                }
                builder.append(second);
                builderIsEmpty = false;
            }
            // Suffix
            if (size > 4 && elems.get(4).length() > 0) {
                if (!builderIsEmpty) {
                    builder.append(' ');
                }
                builder.append(elems.get(4));
                builderIsEmpty = false;
            }
            return builder.toString();
        } else if (size == 1) {
            return elems.get(0);
        } else {
            return "";
        }
    }
    
    public static ContactStruct constructContactFromVNode(VNode node,
            int nameOrderType) {
        //TODO ishaque use the latest types from ContactContract 
        // while assigning the type of vcard field
        if (!node.VName.equals("VCARD")) {
            // Impossible in current implementation. Just for safety.
            Logger.error(ContactStruct.class, "Non VCARD data is inserted.");
            return null;
        }

        // For name, there are three fields in vCard: FN, N, NAME.
        // We prefer FN, which is a required field in vCard 3.0 , but not in vCard 2.1.
        // Next, we prefer NAME, which is defined only in vCard 3.0.
        // Finally, we use N, which is a little difficult to parse.
        String fullName = null;
        String nameFromNProperty = null;

        // Some vCard has "X-PHONETIC-FIRST-NAME", "X-PHONETIC-MIDDLE-NAME", and
        // "X-PHONETIC-LAST-NAME"
        String xPhoneticFirstName = null;
        String xPhoneticMiddleName = null;
        String xPhoneticLastName = null;
        
        ContactStruct contact = new ContactStruct();

        // Each Column of four properties has ISPRIMARY field
        // (See android.provider.Contacts)
        // If false even after the following loop, we choose the first
        // entry as a "primary" entry.
        boolean prefIsSetAddress = false;
        boolean prefIsSetPhone = false;
        boolean prefIsSetEmail = false;
        boolean prefIsSetOrganization = false;
        
        for (PropertyNode propertyNode: node.propList) {
            String name = propertyNode.propName;

            if (TextUtils.isEmpty(propertyNode.propValue)) {
                continue;
            }
            
            if (name.equals("VERSION")) {
                // vCard version. Ignore this.
            } else if (name.equals("FN")) {
                fullName = propertyNode.propValue;
            } else if (name.equals("NAME") && fullName == null) {
                // Only in vCard 3.0. Use this if FN does not exist.
                // Though, note that vCard 3.0 requires FN.
                fullName = propertyNode.propValue;
            } else if (name.equals("N")) {
                nameFromNProperty = getNameFromNProperty(propertyNode.propValue_vector,
                        nameOrderType);
            } else if (name.equals("SORT-STRING")) {
                contact.phoneticName = propertyNode.propValue;
            } else if (name.equals("SOUND")) {
                if (propertyNode.paramMap_TYPE.contains("X-IRMC-N") &&
                        contact.phoneticName == null) {
                    // Some Japanese mobile phones use this field for phonetic name,
                    // since vCard 2.1 does not have "SORT-STRING" type.
                    // Also, in some cases, the field has some ';' in it.
                    // We remove them.
                    StringBuilder builder = new StringBuilder();
                    String value = propertyNode.propValue;
                    int length = value.length();
                    for (int i = 0; i < length; i++) {
                        char ch = value.charAt(i);
                        if (ch != ';') {
                            builder.append(ch);
                        }
                    }
                    contact.phoneticName = builder.toString();
                } else {
                    contact.addExtension(propertyNode);
                }
            } else if (name.equals("ADR")) {
                List<String> values = propertyNode.propValue_vector;
                boolean valuesAreAllEmpty = true;
                for (String value : values) {
                    if (value.length() > 0) {
                        valuesAreAllEmpty = false;
                        break;
                    }
                }
                if (valuesAreAllEmpty) {
                    continue;
                }

                int kind = VCardContacts.KIND_POSTAL;
                int type = -1;
                String label = "";
                boolean isPrimary = false;
                for (String typeString : propertyNode.paramMap_TYPE) {
                    if (typeString.equals("PREF") && !prefIsSetAddress) {
                        // Only first "PREF" is considered.
                        prefIsSetAddress = true;
                        isPrimary = true;
                    } else if (typeString.equalsIgnoreCase("HOME")) {
                        type = ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME;
                        label = "";
                    } else if (typeString.equalsIgnoreCase("WORK") || 
                            typeString.equalsIgnoreCase("COMPANY")) {
                        // "COMPANY" seems emitted by Windows Mobile, which is not
                        // specifically supported by vCard 2.1. We assume this is same
                        // as "WORK".
                        type = ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK;
                        label = "";
                    } else if (typeString.equalsIgnoreCase("POSTAL")) {
                        kind = VCardContacts.KIND_POSTAL;
                    } else if (typeString.equalsIgnoreCase("PARCEL") || 
                            typeString.equalsIgnoreCase("DOM") ||
                            typeString.equalsIgnoreCase("INTL")) {
                        // We do not have a kind or type matching these.
                        // TODO: fix this. We may need to split entries into two.
                        // (e.g. entries for KIND_POSTAL and KIND_PERCEL)
                    } else if (typeString.toUpperCase().startsWith("X-") &&
                            type < 0) {
                        type = ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM;
                        label = typeString.substring(2);
                    } else if (type < 0) {
                        // vCard 3.0 allows iana-token. Also some vCard 2.1 exporters
                        // emit non-standard types. We do not handle their values now.
                        type = ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM;
                        label = typeString;
                    }
                }
                // We use "HOME" as default
                if (type < 0) {
                    type = ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME;
                }
                                
                // adr-value    = 0*6(text-value ";") text-value
                //              ; PO Box, Extended Address, Street, Locality, Region, Postal
                //              ; Code, Country Name
                String address;
                List<String> list = propertyNode.propValue_vector;
                int size = list.size();
                if (size > 1) {
                    StringBuilder builder = new StringBuilder();
                    boolean builderIsEmpty = true;
                    if (Locale.getDefault().getCountry().equals(Locale.JAPAN.getCountry())) {
                        // In Japan, the order is reversed.
                        for (int i = size - 1; i >= 0; i--) {
                            String addressPart = list.get(i);
                            if (addressPart.length() > 0) {
                                if (!builderIsEmpty) {
                                    builder.append(' ');
                                }
                                builder.append(addressPart);
                                builderIsEmpty = false;
                            }
                        }
                    } else {
                        for (int i = 0; i < size; i++) {
                            String addressPart = list.get(i);
                            if (addressPart.length() > 0) {
                                if (!builderIsEmpty) {
                                    builder.append(ADDRESS_SEPERATOR);
                                }
                                builder.append(addressPart);
                                builderIsEmpty = false;
                            }
                        }
                    }
                    address = builder.toString().trim();
                } else {
                    address = propertyNode.propValue; 
                }
                contact.addContactmethod(kind, type, address, label, isPrimary);
            } else if (name.equals("ORG")) {
                // vCard specification does not specify other types.
                int type = VCardContacts.OrganizationColumns.TYPE_WORK;
                boolean isPrimary = false;
                
                for (String typeString : propertyNode.paramMap_TYPE) {
                    if (typeString.equals("PREF") && !prefIsSetOrganization) {
                        // vCard specification officially does not have PREF in ORG.
                        // This is just for safety.
                        prefIsSetOrganization = true;
                        isPrimary = true;
                    }
                    // XXX: Should we cope with X- words?
                }

                List<String> list = propertyNode.propValue_vector; 
                StringBuilder builder = new StringBuilder();
                for (Iterator<String> iter = list.iterator(); iter.hasNext();) {
                    builder.append(iter.next());
                    if (iter.hasNext()) {
                        builder.append(' ');
                    }
                }

                contact.addOrganization(type, builder.toString(), "", isPrimary);
            } else if (name.equals("TITLE")) {
                contact.setPosition(propertyNode.propValue);
            } else if (name.equals("ROLE")) {
                contact.setPosition(propertyNode.propValue);
            } else if (name.equals("PHOTO")) {
                // We prefer PHOTO to LOGO.
                String valueType = propertyNode.paramMap.getAsString("VALUE");
                if (valueType != null && valueType.equals("URL")) {
                    // TODO: do something.
                } else {
                    // Assume PHOTO is stored in BASE64. In that case,
                    // data is already stored in propValue_bytes in binary form.
                    // It should be automatically done by VBuilder (VDataBuilder/VCardDatabuilder) 
                    byte[] imageAsBytes = propertyNode.propValue.getBytes();
                    if (imageAsBytes != null) {
                        byte[] decodedBytes = Base64.decode(imageAsBytes, Base64.DEFAULT);
                        contact.contactPicture = BitmapManager.INSTANCE.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        decodedBytes = null;
                    }
                    String type = propertyNode.paramMap.getAsString("TYPE");
                    if (type != null) {
                        contact.photoType = type;
                    }
                }
            } else if (name.equals("LOGO")) {
                // When PHOTO is not available this is not URL,
                // we use this instead of PHOTO.
                String valueType = propertyNode.paramMap.getAsString("VALUE");
                if (valueType != null && valueType.equals("URL")) {
                    // TODO: do something.
                } else if (contact.contactPicture == null) {
                    byte[] imageAsBytes = propertyNode.propValue_bytes;
                    if (imageAsBytes != null) {
                        byte[] decodedBytes = Base64.decode(imageAsBytes, Base64.DEFAULT);
                        contact.contactPicture = BitmapManager.INSTANCE.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        decodedBytes = null;
                    }
                    
                    String type = propertyNode.paramMap.getAsString("TYPE");
                    if (type != null) {
                        contact.photoType = type;
                    }
                }
            } else if (name.equals("EMAIL")) {
                int type = -1;
                String label = null;
                boolean isPrimary = false;
                for (String typeString : propertyNode.paramMap_TYPE) {
                    if (typeString.equals("PREF") && !prefIsSetEmail) {
                        // Only first "PREF" is considered.
                        prefIsSetEmail = true;
                        isPrimary = true;
                    } else if (typeString.equalsIgnoreCase("HOME")) {
                        type = ContactsContract.CommonDataKinds.Email.TYPE_HOME;
                    } else if (typeString.equalsIgnoreCase("WORK")) {
                        type = ContactsContract.CommonDataKinds.Email.TYPE_WORK;
                    } else if (typeString.equalsIgnoreCase("CELL")) {
                        // We do not have Contacts.ContactMethodsColumns.TYPE_MOBILE yet.
                        type = ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM;
                        label = VCardContacts.ContactMethodsColumns.MOBILE_EMAIL_TYPE_NAME;
                    } else if (typeString.toUpperCase().startsWith("X-") &&
                            type < 0) {
                        type = ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM;
                        label = typeString.substring(2);
                    } else if (type < 0) {
                        // vCard 3.0 allows iana-token.
                        // We may have INTERNET (specified in vCard spec),
                        // SCHOOL, etc.
                        type = ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM;
                        label = typeString;
                    }
                }
                // We use "OTHER" as default.
                if (type < 0) {
                    type = ContactsContract.CommonDataKinds.Email.TYPE_OTHER;
                }
                contact.addContactmethod(VCardContacts.KIND_EMAIL,
                        type, propertyNode.propValue,label, isPrimary);
            } else if (name.equals("TEL")) {
                int type = -1;
                String label = null;
                boolean isPrimary = false;
                boolean isFax = false;
                for (String typeString : propertyNode.paramMap_TYPE) {
                    if (typeString.equals("PREF") && !prefIsSetPhone) {
                        // Only first "PREF" is considered.
                        prefIsSetPhone = true;
                        isPrimary = true;
                    } else if (typeString.equalsIgnoreCase("HOME")) {
                        type = ContactsContract.CommonDataKinds.Phone.TYPE_HOME;
                    } else if (typeString.equalsIgnoreCase("WORK")) {
                        type = ContactsContract.CommonDataKinds.Phone.TYPE_WORK;
                    } else if (typeString.equalsIgnoreCase("CELL")) {
                        type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
                    } else if (typeString.equalsIgnoreCase("PAGER")) {
                        type = ContactsContract.CommonDataKinds.Phone.TYPE_PAGER;
                    } else if (typeString.equalsIgnoreCase("FAX")) {
                        isFax = true;
                    } else if (typeString.equalsIgnoreCase("VOICE") ||
                            typeString.equalsIgnoreCase("MSG")) {
                        // Defined in vCard 3.0. Ignore these because they
                        // conflict with "HOME", "WORK", etc.
                        // XXX: do something?
                    } else if (typeString.toUpperCase().startsWith("X-") &&
                            type < 0) {
                        type = ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM;
                        label = typeString.substring(2);
                    } else if (type < 0){
                        // We may have MODEM, CAR, ISDN, etc...
                        type = ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM;
                        label = typeString;
                    }
                }
                // We use "HOME" as default
                if (type < 0) {
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_HOME;
                }
                if (isFax) {
                    if (type == ContactsContract.CommonDataKinds.Phone.TYPE_HOME) {
                        type = ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME; 
                    } else if (type == ContactsContract.CommonDataKinds.Phone.TYPE_WORK) {
                        type = ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK; 
                    }
                }

                contact.addPhone(type, propertyNode.propValue, label, isPrimary);
            } else if (name.equals("NOTE")) {
                contact.notes.add(propertyNode.propValue);
            } else if (name.equals("BDAY")) {
                contact.addExtension(propertyNode);
            } else if (name.equals("URL")) {
                contact.url = propertyNode.propValue;
                contact.addExtension(propertyNode);
            } else if (name.equals("REV")) {                
                // Revision of this VCard entry. I think we can ignore this.
                contact.addExtension(propertyNode);
            } else if (name.equals("UID")) {
                contact.addExtension(propertyNode);
            } else if (name.equals("KEY")) {
                // Type is X509 or PGP? I don't know how to handle this...
                contact.addExtension(propertyNode);
            } else if (name.equals("MAILER")) {
                contact.addExtension(propertyNode);
            } else if (name.equals("TZ")) {
                contact.addExtension(propertyNode);
            } else if (name.equals("GEO")) {
                contact.geo = propertyNode.propValue;
                contact.addExtension(propertyNode);
            } else if (name.equals("NICKNAME")) {
                // vCard 3.0 only.
                contact.addExtension(propertyNode);
            } else if (name.equals("CLASS")) {
                // vCard 3.0 only.
                // e.g. CLASS:CONFIDENTIAL
                contact.addExtension(propertyNode);
            } else if (name.equals("PROFILE")) {
                // VCard 3.0 only. Must be "VCARD". I think we can ignore this.
                contact.addExtension(propertyNode);
            } else if (name.equals("CATEGORIES")) {
                // VCard 3.0 only.
                // e.g. CATEGORIES:INTERNET,IETF,INDUSTRY,INFORMATION TECHNOLOGY
                contact.addExtension(propertyNode);
            } else if (name.equals("SOURCE")) {
                // VCard 3.0 only.
                contact.addExtension(propertyNode);
            } else if (name.equals("PRODID")) {
                // VCard 3.0 only.
                // To specify the identifier for the product that created
                // the vCard object.
                contact.addExtension(propertyNode);
            } else if (name.equals("X-PHONETIC-FIRST-NAME")) {
                xPhoneticFirstName = propertyNode.propValue;
            } else if (name.equals("X-PHONETIC-MIDDLE-NAME")) {
                xPhoneticMiddleName = propertyNode.propValue;
            } else if (name.equals("X-PHONETIC-LAST-NAME")) {
                xPhoneticLastName = propertyNode.propValue;
            } else if (name.equals(VZW_LOCATION)) {
                contact.vzwLocation = propertyNode.propValue;
            }
            else {
                // Unknown X- words and IANA token.
                contact.addExtension(propertyNode);
            }
        }

        if (fullName != null) {
            contact.name = fullName;
        } else if(nameFromNProperty != null) {
            contact.name = nameFromNProperty;
        } else {
            contact.name = "";
        }

        if (contact.phoneticName == null &&
                (xPhoneticFirstName != null || xPhoneticMiddleName != null ||
                        xPhoneticLastName != null)) {
            // Note: In Europe, this order should be "LAST FIRST MIDDLE". See the comment around
            //       NAME_ORDER_TYPE_* for more detail.
            String first;
            String second;
            if (nameOrderType == NAME_ORDER_TYPE_JAPANESE) {
                first = xPhoneticLastName;
                second = xPhoneticFirstName;
            } else {
                first = xPhoneticFirstName;
                second = xPhoneticLastName;
            }
            StringBuilder builder = new StringBuilder();
            if (first != null) {
                builder.append(first);
            }
            if (xPhoneticMiddleName != null) {
                builder.append(xPhoneticMiddleName);
            }
            if (second != null) {
                builder.append(second);
            }
            contact.phoneticName = builder.toString();
        }
        
        // Remove unnecessary white spaces.
        // It is found that some mobile phone emits  phonetic name with just one white space
        // when a user does not specify one.
        // This logic is effective toward such kind of weird data.
        if (contact.phoneticName != null) {
            contact.phoneticName = contact.phoneticName.trim();
        }

        // If there is no "PREF", we choose the first entries as primary.
        if (!prefIsSetPhone &&
                contact.phoneList != null && 
                contact.phoneList.size() > 0) {
            contact.phoneList.get(0).isPrimary = true;
        }

        if (!prefIsSetAddress && contact.contactmethodList != null) {
            for (ContactMethod contactMethod : contact.contactmethodList) {
                if (contactMethod.kind == VCardContacts.KIND_POSTAL) {
                    contactMethod.isPrimary = true;
                    break;
                }
            }
        }
        if (!prefIsSetEmail && contact.contactmethodList != null) {
            for (ContactMethod contactMethod : contact.contactmethodList) {
                if (contactMethod.kind == VCardContacts.KIND_EMAIL) {
                    contactMethod.isPrimary = true;
                    break;
                }
            }
        }
        if (!prefIsSetOrganization &&
                contact.organizationList != null &&
                contact.organizationList.size() > 0) {
            contact.organizationList.get(0).isPrimary = true;
        }
        
        return contact;
    }
    
    public String displayString() {
        if (name.length() > 0) {
            return name;
        }
        if (contactmethodList != null && contactmethodList.size() > 0) {
            for (ContactMethod contactMethod : contactmethodList) {
                if (contactMethod.kind == VCardContacts.KIND_EMAIL && contactMethod.isPrimary) {
                    return contactMethod.data;
                }
            }
        }
        if (phoneList != null && phoneList.size() > 0) {
            for (PhoneData phoneData : phoneList) {
                if (phoneData.isPrimary) {
                    return phoneData.data;
                }
            }
        }
        return "";
    }
    
    public boolean isIgnorable() {
        return TextUtils.isEmpty(name) &&
                TextUtils.isEmpty(phoneticName) &&
                (phoneList == null || phoneList.size() == 0) &&
                (contactmethodList == null || contactmethodList.size() == 0);
    }
    
    public String getFirstNumber() {
        String number = null;
        
        if (phoneList != null && phoneList.size() > 0) {
            number = phoneList.get(0).data;
        }
        
        return number;
    }
    
    public String getFirstEmail() {
        String name = null;
        
        if (contactmethodList != null) {
            for (ContactMethod method : contactmethodList) {
                if (method.kind == VCardContacts.KIND_EMAIL) {
                   name = method.data;
                   break;
                }
            }
        }
        
        return name;
    }
    
    public String getGeoCoord() {
        return geo;
    }
    
    public String getVZWLocation() {
        return vzwLocation;
    }
    
    public String getURL() {
        return url;
    }
        
    public void setGeoCoord(String geo) {
        this.geo = geo;
    }
    
    public void setURL(String url) {
        this.url = url;
    }
   
    public void setVZWLocation(String locationFileName) {
        this.vzwLocation = locationFileName;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the amount of memory used by the contact data
     */
    public int getMemorySize() {
    	int size = 0;
    	// FIXME add other fields
    	if (contactPicture != null) {
			size = BitmapManager.getBitmapSize(contactPicture);
    	}
    	if (name != null) {
    		size += name.length() * 2;
    	}
    	return size;
    }
}
