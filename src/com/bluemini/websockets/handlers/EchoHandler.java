package com.bluemini.websockets.handlers;

import java.io.UnsupportedEncodingException;

import com.bluemini.websockets.server.WSRequest;
import com.bluemini.websockets.server.WSResponse;

public class EchoHandler extends SWSSHandler {
	
	public void response(WSRequest request)
	{
	    byte[] mess = request.getMessage();
	    try
	    {
			sendResponse(request, new WSResponse(WSRequest.OPCODE_TEXT_FRAME, mess) );
	    }
	    catch (UnsupportedEncodingException uee)
	    {
	    	System.out.println("ERROR: Problems encoding the message.");
	    }
	}

}
