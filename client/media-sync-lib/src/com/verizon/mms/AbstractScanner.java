package com.verizon.mms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.SpannableString;
import android.text.style.URLSpan;
import android.text.util.Linkify;

/**
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * @Since Mar 28, 2012
 */
public abstract class AbstractScanner {

    // log
    // private static final Logger log = LoggerFactory.getLogger(AbstractScanner.class.getSimpleName());
   
    // Pattern to match digits
    static final Pattern DIGIT_PATTERN = Pattern.compile("\\D+", Pattern.CASE_INSENSITIVE);

    interface Emitter {
        void emit(Media media);
    }

    abstract int getNumberOfMessages(long thread);

    abstract void scan(String selection, Emitter emitter);

    abstract void delete(long id);
    
    /**
     * This Method removes non-digit cahracters from a given string.
     * 
     * @param input
     * @return
     */
    String removeNonDigitsFromText(String input) {
        // Replace all non-number character with ""
        StringBuffer sb = new StringBuffer();
        Matcher matcher = DIGIT_PATTERN.matcher(input);
        while (matcher.find()) {
            matcher.appendReplacement(sb, "");
        }
        matcher.appendTail(sb);
        return sb.toString().trim();
    }

    /**
     * This Method performs join on a list of string.
     * 
     * @param input
     * @return
     */
    static String join(List<String> input) {
        StringBuilder _add = new StringBuilder();
        if (null != input) {
            for (String add : input) {
                _add.append(add);
                _add.append(", ");
            }
        }
        String address = _add.toString().trim();

        // remove trailing comma
        if (address.endsWith(",")) {
            address = address.substring(0, address.length() - 1);
        }
        return address;
    }

    public static Map<String, String> extractUris(String text) {
       
        Map<String, String> map = new HashMap<String, String>();
        if (text == null)
            return map;
        //while syncing got senario where text was null

        SpannableString msg = new SpannableString(text);
        // Possible fix for: http://50.17.243.155/bugzilla/show_bug.cgi?id=60
        // Problem ==> Linkify.MAP_ADDRESSES was trying to execute on UI thread.
        Linkify.addLinks(msg, Linkify.PHONE_NUMBERS | Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

        List<String> uris = new ArrayList<String>();

        // Spans
        URLSpan[] spans = msg.getSpans(0, msg.length(), URLSpan.class);
        for (URLSpan span : spans) {
            uris.add(span.getURL());
        }

        // log.info("!=> Extractd uris={}", uris);

  
        while (uris.size() > 0) {
            String mPartCt = null;
            String uriString = uris.remove(0);
            // Remove any duplicates so they don't get added multiple times
            while (uris.contains(uriString)) {
                uris.remove(uriString);
            }

            // Check for link
            if (uriString != null && uriString.length() > 0 && (uriString.startsWith("http:") || uriString.startsWith("https:"))) {
                mPartCt = Media.M_LINK_CT;
            } else {
                int sep = uriString.indexOf(":");
                String prefix = null;

                if (sep >= 0) {
                    prefix = uriString.substring(0, sep);
                    uriString = uriString.substring(sep + 1);
                }
                if ("mailto".equalsIgnoreCase(prefix)) {
                    mPartCt = Media.M_EMAIL_CT;
                } else if ("tel".equalsIgnoreCase(prefix) && uriString.length() > 6 && uriString.length() < 16) {
                    mPartCt = Media.M_PHONE_CT;
                }
            }

            if (null != mPartCt) {
                map.put(uriString, mPartCt);
            }
        }
        // log.info("!=> map={}", map);
        return map;
    }

}