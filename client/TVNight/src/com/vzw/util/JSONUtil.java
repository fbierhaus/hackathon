/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

import com.vzw.annotations.JsonIgnoreField;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import net.sf.json.*;
import org.apache.log4j.Logger;

/**
 * This class uses JsonIgnoreField annotation for jsonconfig
 * @author hud
 */
public class JSONUtil {
	private static final Logger				logger = Logger.getLogger(JSONUtil.class);
	
	public static final JsonConfig			DEFAULT_JSONCONFIG = new JsonConfig();
	
	static {
		DEFAULT_JSONCONFIG.addIgnoreFieldAnnotation(JsonIgnoreField.class);
	}
	
	public static JsonConfig newJsonConfig() {
		return DEFAULT_JSONCONFIG.copy();
	}
	
	public static <T extends JSON> T fromObject(Object obj) {
		return obj == null ? (T)new JSONObject() : (T)JSONSerializer.toJSON(obj, DEFAULT_JSONCONFIG);
	}
	
	/**
	 * Alias of fromObject
	 * @param <T>
	 * @param obj
	 * @return 
	 */
	public static <T extends JSON> T toJson(Object obj) {
		return obj == null ? (T)new JSONObject() : (T)fromObject(obj);
	}	
	
	public static <T> T toJava(String jsonStr, Object...params) {
		if (jsonStr == null) {
			return null;
		}
		else {
			JSON json = fromObject(jsonStr);
			return (T)toJava(json, params);
		}
	}
	
	/**
	 * A utility to convert json object to java object
	 * @param <T>
	 * @param json
	 * @param params
	 *		[rootClass] [,array_mode] [,collection class] [,attr_regex,class] [,attr_regex,class] ...
	 * @return 
	 */
	public static <T> T toJava(JSON json, Object...params) {
	
		if (json == null || json instanceof JSONNull) {
			return null;
		}
		
		JsonConfig jc;
		if (params.length == 0) {
			jc = DEFAULT_JSONCONFIG;
		}
		else {
			jc = DEFAULT_JSONCONFIG.copy();
			
			int i = 0;
			if (params[i] instanceof Class) {
				// it's root class
				jc.setRootClass((Class<?>)params[i]);
				++ i;
			}
			
			if (params.length > i) {
				if (params[i] instanceof Integer) {
					jc.setArrayMode((Integer)params[i]);
					++ i;
				}

				if (params.length > i) {
					if (params[i] == null) {
						++ i;
					}
					else if (params[i] instanceof Class) {
						jc.setCollectionType((Class<?>)params[i]);
						++ i;
					}

					if (i + 1 < params.length) {
						Map<String, Class<?>> classMap = new HashMap<String, Class<?>>();
						for (; i < params.length - 1; i += 2) {
							classMap.put(params[i].toString(), (Class<?>)params[i + 1]);
						}

						jc.setClassMap(classMap);
					}
				}
			}
		}
		
		return (T)JSONSerializer.toJava(json, jc);
	}
	
	/**
	 * 
	 * @param json
	 * @param params 
	 */
	public static String toString(JSON json, Integer...params) {
		if (json == null) {
			return null;
		}
		
		int indentFactor = 0;
		int indent = 0;
		
		if (params.length > 0) {
			indentFactor = params[0];
		}
		
		if (params.length > 1) {
			indent = params[1];
		}
		
		return json.toString(indentFactor, indent);
	}
	
	
	public static <T> String toJsonString(T obj, Integer...params) {
		if (obj == null) {
			return null;
		}
		
		int indentFactor = 0;
		int indent = 0;
		
		if (params.length > 0) {
			indentFactor = params[0];
		}
		
		if (params.length > 1) {
			indent = params[1];
		}

		return toString(fromObject(obj), indentFactor, indent);
	}
	
	
	
	public static String normalize(String val) {
		return val == null ? null : (org.apache.commons.lang3.StringUtils.equals(val, "null") || 
				org.apache.commons.lang3.StringUtils.equals(val, "undefined") ? null : val);
		
	}
	
	/**
	 * This is only a workaround for some clients that do not convert javascript array
	 * to JSONArray properly. So the jo looks like:
	 * "0": object
	 * "1": object
	 * ...
 	 * @param jo
	 * @return i
	 */
	public static JSONArray toJsonArray(JSONObject jo) {
		if (jo == null) {
			return null;
		}
		
		JSONArray ja = new JSONArray();
		
		for (int i = 0; i < jo.size(); ++ i) {
			Object obj = jo.opt(Integer.toString(i));
			if (obj != null) {
				ja.add(obj);
			}
		}
		
		return ja;
	}
	
	/**
	 * workaround for some browser which convert array to object
	 * This method retrieve json array from jo.key
	 * @param jo
	 * @param key
	 * @return 
	 */
	public static JSONArray getJsonArray(JSONObject joData, String key) {
		if (joData == null || key == null) {
			return null;
		}
		
		
		JSONArray ja = joData.optJSONArray(key);
		if (ja == null) {
			JSONObject jo = joData.optJSONObject(key);
			if (jo != null) {
				ja = toJsonArray(jo);
			}
		}
		
		return ja;
	}
	
	
	public static JSONObject fromProperties(Properties props) throws Exception {
		
		if (props == null) {
			return null;
		}
		
		JSONObject joRes = new JSONObject();
		
		final String GLOBAL_PREFIX = "global.";
		
		Set<String> keySet = props.stringPropertyNames();

		Map<String, Object> mapRes = new HashMap<String, Object>();
		for (String key : keySet) {
			String _key;
			Map<String, Object> _mc = mapRes;

			// check whether it's global
			if (key.startsWith(GLOBAL_PREFIX)) {
				_key = key.substring(GLOBAL_PREFIX.length());
			}
			else {
				_key = key;
			}

			// device with "\\."
			String[] ncArr = _key.split("\\.");
			for (int ic = 0; ic < ncArr.length; ++ ic) {
				String _kc = ncArr[ic];
				Object _o = _mc.get(_kc);
				if (_o != null) {
					// the key already exists
					// check whether it's string or object
					if (_o instanceof String) {
						// check whether this key's value is a string
						if (ic + 1 < ncArr.length) {
							// wrong, it's a map
//							LogUtil.errorAndThrow(logger, new SimpleException("Invalid resource for key: {0} ", key));
						}
						else {
							// simply put it
							_mc.put(_kc, props.getProperty(key)); // should automaticall break out
						}
						
					}
					else if (_o instanceof Map) {
						// check whether this key's value is string
						if (ic + 1 >= ncArr.length) {
							// wrong, it's a string
//							LogUtil.errorAndThrow(logger, new SimpleException("Invalid resource for key: {0} ", key));
						}
						else {
							// move to next component
							_mc = (Map<String, Object>)_o;
						}
					}
					else {
//						LogUtil.errorAndThrow(logger, new SimpleException("Invalid resource: {0}", props.toString()));
					}
				}
				else {
					// if _o does not exist
					
					// check string
					if (ic + 1 >= ncArr.length) {
						// this is a string, done here
						_mc.put(_kc, props.getProperty(key));
					}
					else {
						// it's an object
						_o = new HashMap<String, Object>();
						_mc.put(_kc, _o);
						_mc = (Map<String, Object>)_o;
					}
				}
			}

		}
		
		// once done, put it in json
		joRes.putAll(mapRes);
		
		return joRes;
		
	}
	
	/**
	 * Convert a resource file to a JSON Object
	 * 
	 * 1. For global.<a>.<b>, global is removed
	 * 2. For any <a>.<b>
	 * 
	 * @param resStr
	 * @return 
	 */
	public static JSONObject fromResource(String resStr) throws Exception {
		if (resStr == null) {
			return null;
		}
		
		StringReader sr = new StringReader(resStr);
		Properties props = new Properties();
		props.load(sr);
		return fromProperties(props);
	}
}
