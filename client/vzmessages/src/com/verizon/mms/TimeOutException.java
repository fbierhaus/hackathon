package com.verizon.mms;

import java.io.IOException;

public class TimeOutException extends IOException {
    private static final long serialVersionUID = -6402828801664465314L;

    /**
     * Creates a new TimeOutException.
     */
    public TimeOutException() {
        super();
    }
    
    /**
     * Creates new TimeOutException with the specified detail message.
     *
     * @param message the detail message.
     */
    public TimeOutException(String message) {
        super(message);
    }
}
