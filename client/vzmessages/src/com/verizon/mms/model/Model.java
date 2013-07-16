/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.verizon.mms.model;

import java.util.ArrayList;
import java.util.List;

public class Model {
    private final ArrayList<IModelChangedObserver> mModelChangedObservers =
    	new ArrayList<IModelChangedObserver>();

    public void registerModelChangedObserver(IModelChangedObserver observer) {
    	synchronized (mModelChangedObservers) {
	        if (!mModelChangedObservers.contains(observer)) {
	            mModelChangedObservers.add(observer);
	            registerModelChangedObserverInDescendants(observer);
	        }
//	        if (Logger.IS_DEBUG_ENABLED) {
//	        	Logger.debug(id(this) + ".registerModelChangedObserver: after: " + dump());
//	        }
    	}
    }

    public void unregisterModelChangedObserver(IModelChangedObserver observer) {
    	synchronized (mModelChangedObservers) {
	        mModelChangedObservers.remove(observer);
	        unregisterModelChangedObserverInDescendants(observer);
//	        if (Logger.IS_DEBUG_ENABLED) {
//	        	Logger.debug(id(this) + ".unregisterModelChangedObserver: after: " + dump());
//	        }
    	}
    }

    public void unregisterAllModelChangedObservers() {
//        if (Logger.IS_DEBUG_ENABLED) {
//        	Logger.debug(id(this) + ".unregisterAllModelChangedObservers");
//        }
    	synchronized (mModelChangedObservers) {
	        unregisterAllModelChangedObserversInDescendants();
	        mModelChangedObservers.clear();
    	}
    }

    protected void notifyModelChanged(boolean dataChanged) {
    	synchronized (mModelChangedObservers) {
//	        if (Logger.IS_DEBUG_ENABLED) {
//	        	Logger.debug(id(this) + ".notifyModelChanged: notifying: " + dump());
//	        }
	        for (IModelChangedObserver observer : mModelChangedObservers) {
	            observer.onModelChanged(this, dataChanged);
	        }
    	}
    }

    @SuppressWarnings("unchecked")
	protected List<IModelChangedObserver> getModelChangedObservers() {
    	synchronized (mModelChangedObservers) {
    		return (List<IModelChangedObserver>)mModelChangedObservers.clone();
    	}
    }

    protected void registerModelChangedObserverInDescendants(IModelChangedObserver observer) {
    }

    protected void unregisterModelChangedObserverInDescendants(IModelChangedObserver observer) {
    }

    protected void unregisterAllModelChangedObserversInDescendants() {
    }

//	private String id(Object o) {
//		return o.getClass().getName() + '@' + Integer.toHexString(o.hashCode());
//	}

//	private String dump() {
//		final StringBuilder sb = new StringBuilder("[");
//		final int num = mModelChangedObservers.size();
//		for (int i = 0; i < num; ++i) {
//			if (i > 0) {
//				sb.append(", ");
//			}
//			sb.append(id(mModelChangedObservers.get(i)));
//        }
//		sb.append("]");
//		return sb.toString();
//	}
}
