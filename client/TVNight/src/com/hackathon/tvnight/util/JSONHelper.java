package com.hackathon.tvnight.util;

import java.io.Reader;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import android.util.Log;

/**
 * Helper class to (de)serialize JSON text data to and fro Java Objects
 */
@SuppressWarnings("deprecation")
public class JSONHelper {

    final static String TAG = "[JSON-TRACE]==>> ";

    // static object
    // static final Gson gson;
    static final ObjectMapper mapper;
    static {
        // GsonBuilder gsonBuilder = new GsonBuilder();
        // gsonBuilder.registerTypeAdapter(Object.class, new ObjectDeserializer());
        // gson = gsonBuilder.create();

        mapper = new ObjectMapper();
        mapper.configure(org.codehaus.jackson.map.SerializationConfig.Feature.USE_ANNOTATIONS, true);
        mapper.configure(org.codehaus.jackson.map.SerializationConfig.Feature.INDENT_OUTPUT, true);
        mapper.configure(org.codehaus.jackson.map.SerializationConfig.Feature.WRITE_NULL_PROPERTIES, false);
        mapper.configure(org.codehaus.jackson.map.SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, true);
        mapper.configure(org.codehaus.jackson.map.SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY,
                true);
        mapper.configure(org.codehaus.jackson.map.SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(org.codehaus.jackson.map.DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
        mapper.configure(org.codehaus.jackson.map.DeserializationConfig.Feature.USE_BIG_INTEGER_FOR_INTS,
                true);

    }

    /**
     * Creates object from json text data
     * 
     * @param <T>
     * @param json
     * @param t
     * @return
     */
    public static <T> T fromJson(String json, Class<T> t) {
        long start = System.currentTimeMillis();
        try {
            // return gson.fromJson(json, t);
            return mapper.readValue(json, t);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            long diff = System.currentTimeMillis() - start;
            Log.d(TAG, " json to " + t + " ===>>> " + diff + "ms.");
        }
    }

    public static <T> T fromJson(Reader json, Class<T> t) {
        long start = System.currentTimeMillis();
        try {
            // return gson.fromJson(json, t);
            return mapper.readValue(json, t);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            long diff = System.currentTimeMillis() - start;
            Log.d(TAG, " json to " + t + " ===>>> " + diff + "ms.");
        }
    }

    /**
     * Returns map object
     * 
     * @param json
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Map<String, ?> fromJson(String json) {
        long start = System.currentTimeMillis();
        try {
            // return (Map<String, ?>) gson.fromJson(json, Object.class);
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            long diff = System.currentTimeMillis() - start;
            Log.d(TAG, " json to Map<String, ?> " + " ===>>> " + diff + "ms.");
        }
    }

    /**
     * Gets json string data
     * 
     * @param obj
     * @return
     */
    public static String toJson(Object obj) {
        long start = System.currentTimeMillis();
        try {
            // return gson.toJson(obj);
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            long diff = System.currentTimeMillis() - start;
            Log.d(TAG, obj.getClass() + " to json ===>>> " + diff + "ms.");
        }
    }
}
