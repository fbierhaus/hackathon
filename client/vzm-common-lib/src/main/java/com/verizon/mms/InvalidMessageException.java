package com.verizon.mms;

@SuppressWarnings("serial")
public class InvalidMessageException extends MmsException {

	public InvalidMessageException(String msg) {
		super(msg);
	}

}
