package com.verizon.mms.transaction;

import java.io.IOException;

@SuppressWarnings("serial")
public class HTTPException extends IOException {
	private int statusCode;

	public HTTPException(String string, int statusCode) {
		super(string);
		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}
}
