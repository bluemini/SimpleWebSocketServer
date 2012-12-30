package com.bluemini.websockets.server;

public class SWSSUpgradeException extends Exception {

	public SWSSUpgradeException()
	{
		super("Unknown error");
	}
	
	public SWSSUpgradeException(String err)
	{
		super(err);
	}
	
}
