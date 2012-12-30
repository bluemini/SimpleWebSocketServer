package com.bluemini.websockets.server;

public class SWSSException extends Exception {
	
	public SWSSException()
	{
		super("Unknown error");
	}
	
	public SWSSException(String err)
	{
		super(err);
	}
	
}
