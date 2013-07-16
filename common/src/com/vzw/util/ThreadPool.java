/******************************************************
 * File: ThreadPool.java
 * created May 18, 2002 12:11:08 PM by ron
 *
 * $Id: ThreadPool.java,v 1.1 2011/05/11 16:36:00 linj Exp $
 * $Log: ThreadPool.java,v $
 * Revision 1.1  2011/05/11 16:36:00  linj
 * *** empty log message ***
 *
 * Revision 1.2  2004/10/29 23:55:54  fletcherm
 * Merging all changes from MMG_VERIZON_2-x to MMG, per Thameem. The goal of this merge is to end up with an MMG HEAD that is identical to the MMG_VERIZON_2-x branch.
 *
 * Revision 1.1.1.1.2.2.2.1  2003/12/18 03:59:32  smithr
 * provide better timeout support under smpp 3.3
 * and better smpp 3.4 support
 *
 * Revision 1.6  2003/08/05 17:04:01  rsmith
 * corrected problems with handling Invalid Dest Addr that crept into this build during the last merge.
 *
 * Revision 1.1.1.1.2.1  2003/04/30 00:52:10  jasonc
 * Cleanup: standardizing code formatting
 *
 * Revision 1.1.1.1  2003/02/19 00:00:05  ssaroha
 * Sources for Mobile Messaging GateWay
 *
 * Revision 1.1.1.1  2003/02/15 00:25:43  serg
 * The latest version of MMG Software, includes changes for msisdn routing and mms
 *
 * Revision 1.4  2002/09/23 14:36:41  rsmith
 * additional JavaDoc comments.
 *
 * Revision 1.3  2002/09/09 13:09:47  rsmith
 * Updates from Serg; javadoc
 *
 * Revision 1.1.1.1  2002/08/21 17:41:44  serg
 * Imported version 1.0
 *
 * Revision 1.2  2002/08/02 17:16:42  openpath
 * updated logging for reports
 *
 * Revision 1.2  2002/05/20 19:59:00  ron
 * added in element limit
 *
 * Revision 1.1  2002/05/20 00:17:22  ron
 * no message
 *
 *
 */
package com.vzw.util;


/**
 * A threadPool that has a defined number of threads wait on
 * a blockingQueue for Runnable objects.
 *
 * @version $Revision: 1.1 $
 * @author  VGP P&S
 */
public class ThreadPool extends ThreadGroup {
  private static int groupNumber = 0;
  private static int threadId = 0;
  private final BlockingQueue pool = new BlockingQueue();
  private int maxSize;
  private int poolSize;
  private int maxElements;
  private boolean limitElements = false;
  private boolean hasClosed = false;

  //---------------------------------------

  /**
   * Setup the thread pool with the initialize size. <p>
   * @param initialThreadCount  how many threads to establish initially
   * @param maxThreadCount    the maximum number of threads that can be added to the pool
   */
  public ThreadPool(int initialThreadCount, int maxThreadCount) {
    super("ThreadPool" + groupNumber++);

    maxSize = (maxThreadCount > 0) ? maxThreadCount : Integer.MAX_VALUE;

    poolSize = Math.min(initialThreadCount, maxSize);

    for (int i = poolSize; --i >= 0;)
      new PooledThread().start();
  }

  //---------------------------------------

  /**
   *
   * Create a dynamic Thread pool with no upper limit.
   *
   */
  public ThreadPool() {
    super("ThreadPool" + groupNumber++);
    this.maxSize = 0;
  }

  //---------------------------------------

  /**
   * Limits the number of elements that can be added
   * to the underlying blockingQueue
   * @param me  the max element count
   */
  public void setMaxElements(int me) {
    limitElements = true;
    maxElements = me;
  }

  //---------------------------------------

  /**
   * turns of element limiting
   */
  public void turnOffElementLimit() {
    limitElements = false;
  }

  //---------------------------------------

  /**
   * Runs an "action" in a thread by added more threads to
   * the pool if needed and then enqeueing the action <p>
   *
   * @param   action   the action to perform in one of the pooled threads
   */
  public synchronized void run(Runnable action) throws Closed, Full {
    if (hasClosed) {
      throw new Closed();
    }

    if (poolSize < maxSize) {
      synchronized (pool) {
        if (pool.getElementCount() != 0) {
          poolSize++;
          new PooledThread().start(); // Add thread to pool
        }
      }
    }

    // if maxed out on elements, stop enqueuing
    if ((limitElements) && (pool.getElementCount() >= maxElements)) {
      throw new Full();
    }

    pool.addElement(action); // Attach action to it.
  }

  //---------------------------------------

  /**
   * Kills all the threads waiting in the blocking queue
   *
   */
  public synchronized void close() {
    hasClosed = true;
    pool.close(); // release all waiting threads
  }

  //---------------------------------------
  //---------------------------------------

  /**
   * The threads that live in the ThreadPool are
   * created here.  Each thread waits on the blockingQueue
   * to have a new element added.  When this element is
   * is added, it get's it and runs it.
   */
  private class PooledThread extends Thread {
    public PooledThread() {
      super(ThreadPool.this, "T" + threadId);
    }

    public void run() {
      try {
        while (!hasClosed) {
          ((Runnable) (pool.waitForNextElement())).run();
        }
      } catch (Exception e) {
      }
    }
  }

  //---------------------------------------
  //---------------------------------------

  /**
   * Imbedded Exception that is thrown if you try to
   * do something with a closed ThreadPool
   */
  public class Closed extends RuntimeException {
    Closed() {
      super("Tried to execute operation on a closed ThreadPool");
    }
  }

  //---------------------------------------
  //---------------------------------------

  /**
   * Imbedded Exception that is thrown if the threadpool
   * is too full
   */
  public class Full extends RuntimeException {
    Full() {
      super("Threadpool is full");
    }
  }
}
