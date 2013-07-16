/*
 * Copyright (C) 2008 SocialMuse, Inc.
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

package com.verizon.mms.util;

public class MemoryCacheObject<T> {
	private T obj;
	private long time;


	public MemoryCacheObject(T obj, long time) {
		this.obj = obj;
		this.time = time;
	}

	public MemoryCacheObject(T obj) {
		this.obj = obj;
	}

	public T get() {
		return obj;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}
}
