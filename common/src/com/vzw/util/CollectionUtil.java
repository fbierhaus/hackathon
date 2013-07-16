/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vzw.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author hud
 */
public class CollectionUtil {
	public static boolean isEmpty(Collection<?> coll) {
		return coll == null || coll.isEmpty();
	}
	
	public static <T> boolean isEmpty(T[] arr) {
		return arr == null || arr.length == 0;
	}
	
	public static boolean isEmpty(Map map) {
		return map == null || map.isEmpty();
	}
	
	public static void clear(Set<?> set) {
		if (set != null) {
			set.clear();
		}
	}
	
	public static void clear(Collection<?> coll) {
		if (coll != null) {
			coll.clear();
		}
	}
	
	public static void clear(Map map) {
		if (map != null) {
			map.clear();
		}
	}
	
	public static int size(Collection<?> coll) {
		return isEmpty(coll) ? 0 : coll.size();
	}
	
	public static int size(Map map) {
		return isEmpty(map) ? 0 : map.size();
	}
	
	public static <T> int size(T[] arr) {
		return isEmpty(arr) ? 0 : arr.length;
	}
}
