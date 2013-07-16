/******************************************************
 * File: BlockingQueue.java
 * created Apr 23, 2002 10:21:36 PM by ron
 *
 * $Id: BlockingQueue.java,v 1.1 2011/05/11 16:36:00 linj Exp $
 * $Log: BlockingQueue.java,v $
 * Revision 1.1  2011/05/11 16:36:00  linj
 * *** empty log message ***
 *
 * Revision 1.2  2004/10/29 23:55:54  fletcherm
 * Merging all changes from MMG_VERIZON_2-x to MMG, per Thameem. The goal of this merge is to end up with an MMG HEAD that is identical to the MMG_VERIZON_2-x branch.
 *
 * Revision 1.1.1.1.2.2.2.1  2003/12/18 03:59:31  smithr
 * provide better timeout support under smpp 3.3
 * and better smpp 3.4 support
 *
 * Revision 1.5  2003/08/05 17:04:01  rsmith
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
 * Revision 1.3  2002/09/23 14:36:41  rsmith
 * additional JavaDoc comments.
 *
 * Revision 1.2  2002/09/09 13:09:47  rsmith
 * Updates from Serg; javadoc
 *
 * Revision 1.1.1.1  2002/08/21 17:41:44  serg
 * Imported version 1.0
 *
 * Revision 1.2  2002/08/02 17:16:42  openpath
 * updated logging for reports
 *
 * Revision 1.1  2002/05/20 00:17:22  ron
 * no message
 *
 * Revision 1.1  2002/05/02 17:58:59  ron
 * no message
 *
 *
 */
package com.vzw.util;

import java.util.Vector;


/**
 * This is a queue wrapper that allows us to add objects to a Vector.
 * A method is provided that enables clients to wait while blocking
 * until there is content to receive. <p>
 *
 *
 * @version $Revision: 1.1 $
 * @author  VGP P&S
 */
public class BlockingQueue {
  protected Vector queue = new Vector();

  //------------------------------------------
  public BlockingQueue() {
  }

  //------------------------------------------

  /**
   * Add this element to our container and notify so that
   *  anyone waiting with waitForNextElement() can get it. <p>
   *
   * @param  obj  The object to insert into the vector and notifyAll()
   */
  public void addElement(Object obj) {
    synchronized (this) {
      queue.addElement(obj);
      notifyAll();
    }
  }

  //------------------------------------------

  /**
   * Performs a notifyAll() to notify all the waiting threads so they get a chance to end
   * gracefully, if they need to.
   */
  public void close() {
    synchronized (this) {
      notifyAll();
    }
  }

  //------------------------------------------

  /**
   * Get the first element in the Vector, but DO NOT BLOCK!! <p>
   * This is will return null if nothing is there. <p>
   *
   * @return the first object in the vector or null if none are present
   */
  public Object getNextElement() {
    Object obj = null;

    if (queue.size() > 0) {
      synchronized (this) {
        try {
          obj = queue.elementAt(0);
          queue.removeElementAt(0);
        } catch (IndexOutOfBoundsException io) {
          //this should never happen
          io.printStackTrace();
        }
      }
    }

    return obj;
  }

  //------------------------------------------

  /**
   * Wait up to the specified amount for an object to show up
   *  in the queue. <p>
   *  Be careful; this will return a null if the time is exceeded <p>
   *
   * @return the first object in the vector or null if the wait time was exceeded and nothing is still in the vector.
   */
  public Object waitForNextElement(long waitTime) {
    Object obj = getNextElement();

    if (obj == null) {
      synchronized (this) {
        try {
          wait(waitTime);
          obj = getNextElement();
        } catch (InterruptedException ie) {
          //return the null
        }
      }
    }

    return obj;
  }

  //------------------------------------------

  /**
   * Wait forever for an object to show up
   *  in the queue. <p>
   *  It is still a good idea to check for nulls.  <p>
   * @return the first object in the queue when the queue has something in it.
   */
  public Object waitForNextElement() {
    return waitForNextElement(0);
  }

  //------------------------------------------

  /**
   * @return the number of elements in the container
   */
  public int getElementCount() {
    return queue.size();
  }

  //------------------------------------------

  /**
   * Remove all elements from the container
   */
  public void removeAllElements() {
    queue.clear();
  }

  //------------------------------------------

  /**
   * Remove an element at this position
   */
  protected void removeElement(int pos) {
    queue.removeElementAt(pos);
  }

  //------------------------------------------

  /**
   * How big is the underlying queue? <p>
   * @return vector's size().
   */
  public int getQueueSize() {
    return queue.size();
  }
}
