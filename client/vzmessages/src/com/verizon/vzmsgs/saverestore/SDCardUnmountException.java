package com.verizon.vzmsgs.saverestore;

public class SDCardUnmountException extends Exception{
	private static final long serialVersionUID = 7897629575469020615L;
	private static String exceptionMsg = "sd card unmounted exception";
	public SDCardUnmountException(String exceptionMessage) {
		super(exceptionMessage);
	}
	
	public SDCardUnmountException() {
		super(exceptionMsg);
	}

}
