package com.bluemini.websockets.handlers;

import java.util.ArrayList;
import java.util.Calendar;

import com.bluemini.websockets.server.WSRequest;
import com.bluemini.websockets.server.WSResponse;

public class WebRTCHandler extends SWSSHandler {
	
	private ArrayList<WebRTCSession> sessions = new ArrayList<WebRTCSession>();
	
	public WSResponse response(WSRequest request)
	{
		return new WSResponse(WSRequest.OPCODE_TEXT_FRAME, request.getMessage());		
	}

}

class WebRTCSession
{
	private Calendar LastAccess = Calendar.getInstance();
	private int ID;
	private ArrayList<WebRTCClient> clients = new ArrayList<WebRTCClient>();
}

class WebRTCClient
{
	private String host;
	private int cookie;
}