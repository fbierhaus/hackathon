/*
 * Copyright (C) 2010 SocialMuse, Inc.
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

package com.verizon.mms.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.os.Handler;
import android.os.Message;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.util.ArrayDeque;


public class ListDataWorker extends Thread {
	private static int id;
	private Object reqLock = new Object();
	private ArrayList<DataQueue> queues;
	private boolean stop;
	private int currentJob;
	private int clearedJob;


	public interface ListDataJob {
		public Object run(int pos, Object data);
	}

	public interface ListDataOwner {
		/**
		 * Returns true if the given position is still valid, e.g. is still being displayed.
		 * Returning false means the job for that position will not be run and no result will
		 * be returned.
		 *
		 * @param pos The position specified with the request
		 * @param data The data specified with the request
		 */
		public boolean isValidRequest(int pos, Object data);
	}

	private class DataReq {
		private int pos;
		private ListDataJob job;
		private Object data;

		private DataReq(int pos, ListDataJob job, Object data) {
			this.pos = pos;
			this.job = job;
			this.data = data;
		}

		public String toString() {
			return super.toString() + ": pos = " + pos + ", job = " + job + ", data = " + data;
		}
	}

	private class DataQueue {
		private Handler handler;
		private int msgid;
		private int size;
		private ListDataOwner owner;
		private ArrayDeque<DataReq> reqQueue;
		private HashMap<Integer, DataReq> posToReq;

		private DataQueue(Handler handler, int msgid, int size, ListDataOwner owner) {
			this.handler = handler;
			this.msgid = msgid;
			this.size = size;
			this.owner = owner;
			reqQueue = new ArrayDeque<DataReq>(size);
			posToReq = new HashMap<Integer, DataReq>(size);
		}
	}


	public ListDataWorker() {
		super("DataWorker-" + id++);
		setPriority(Thread.NORM_PRIORITY - 1);
	}

	public ListDataWorker(Handler handler, int msgid, int size) {
		this();
		addQueue(handler, 0, msgid, size, null);
	}

	public void addQueue(Handler handler, int queueNum, int msgid, int size, ListDataOwner owner) {
		final DataQueue queue = new DataQueue(handler, msgid, size, owner);
		synchronized (reqLock) {
			if (queues == null) {
				queues = new ArrayList<DataQueue>(queueNum > 2 ? queueNum : 2);
			}
			final int qsize = queues.size();
			if (queueNum < qsize) {
				queues.remove(queueNum);
			}
			else {
				while (queueNum > queues.size()) {
					queues.add(null);
				}
			}
			queues.add(queueNum, queue);
		}
	}

	public void resizeQueue(int queueNum, int size) {
		synchronized (reqLock) {
			if (queueNum < queues.size()) {
				queues.get(queueNum).size = size;
			}
		}
	}

	public void resizeQueues(int size) {
		synchronized (reqLock) {
			for (DataQueue queue : queues) {
				queue.size = size;
			}
		}
	}

	@Override
	public void run() {
		final int numQueues = queues.size();
		do {
			// check the queues in order; if a request is ready then dequeue it (LIFO), otherwise wait
			DataQueue queue;
			DataReq req;
			synchronized (reqLock) {
				int i = 0;
				do {
					queue = queues.get(i++);
					req = queue.reqQueue.pollLast();
				} while (req == null && i < numQueues);

//				if (MMSLogger.IS_DEBUG_ENABLED) {
//					log.debug("DataWorker.run: req = " + req);
//				}

				if (req != null) {
					if (queue.posToReq.remove(req.pos) == null) {
						Logger.error(getClass(),"DataWorker.run: no mapping for pos " + req.pos);
					}
				}
				else {
					try {
						reqLock.wait();
					}
					catch (InterruptedException e) {
					}
					if (stop) {
						break;
					}
					continue;
				}

				++currentJob;
			}

			// check if this is still a valid request
			final int pos = req.pos;
			final ListDataOwner owner = queue.owner;
			final Object data = req.data;
			if (owner == null || owner.isValidRequest(pos, data)) {
//				if (MMSLogger.IS_DEBUG_ENABLED) {
//					log.debug(getClass(), "executing job for " + pos);
//				}

				// run the task and send the results to the handler if there are any
				// and it's still a valid job and request
				final Object obj = req.job.run(pos, data);
	
				synchronized (reqLock) {
					if ((owner == null || owner.isValidRequest(pos, data)) && currentJob > clearedJob && obj != null) {
//						if (MMSLogger.IS_DEBUG_ENABLED) {
//							log.debug(getClass(), "returning result for " + pos);
//						}
						final Handler handler = queue.handler;
						final Message msg = Message.obtain(handler, queue.msgid, obj);
						msg.arg1 = pos;
						handler.sendMessage(msg);
					}
//					else if (MMSLogger.IS_DEBUG_ENABLED) {
//						log.debug(getClass(), "dropping result for " + pos + ": cur/cleared job = " +
//							currentJob + " / " + clearedJob + ", obj = " + obj);
//					}
				}
			}
//			else if (MMSLogger.IS_DEBUG_ENABLED) {
//				log.debug(getClass(), "dropping job for " + pos);
//			}
		}
		while (!stop);
	}

	public void request(int pos, ListDataJob job, Object data) {
		request(0, pos, job, data);
	}

	public void request(int queueNum, int pos, ListDataJob job, Object data) {
		try {
			synchronized (reqLock) {
				if (queueNum < queues.size()) {
					DataReq old = null;
					final ArrayDeque<DataReq> reqs;
					final DataQueue queue = queues.get(queueNum);
					final HashMap<Integer, DataReq> posToReq = queue.posToReq;
	
					// trim oldest entry if full
					reqs = queue.reqQueue;
					if (reqs.size() >= queue.size) {
						final DataReq req = reqs.removeFirst();
						posToReq.remove(req.pos);
//						if (MMSLogger.IS_DEBUG_ENABLED) {
//							log.debug("DataWorker.request: removed " + req);
//						}
					}
	
					// remove this request if already in the queue
					old = posToReq.remove(pos);
					if (old != null) {
						reqs.remove(old);
					}
	
					// add request to head
					final DataReq req = new DataReq(pos, job, data);
					reqs.add(req);
					posToReq.put(pos, req);
					reqLock.notify();

//					if (MMSLogger.IS_DEBUG_ENABLED) {
//						log.debug("DataWorker.request: " + (old == null ? "added" : "duplicate") + " request for " + pos + ", size = " + reqs.size());
//					}
				}
			}
		}
		catch (Exception e) {
			Logger.error(getClass(),e);
		}
	}

	public void clear() {
		synchronized (reqLock) {
			for (final DataQueue queue : queues) {
				queue.reqQueue.clear();
				queue.posToReq.clear();
				queue.handler.removeCallbacksAndMessages(null);
				clearedJob = currentJob;
			}
		}
	}

	public void exit() {
		synchronized (reqLock) {
			clear();
			stop = true;
			reqLock.notify();
		}
	}
}
