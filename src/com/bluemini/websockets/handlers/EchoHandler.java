package com.bluemini.websockets.handlers;

import com.bluemini.websockets.server.WSRequest;
import com.bluemini.websockets.server.WSResponse;

public class EchoHandler extends SWSSHandler {
	
	public WSResponse response(WSRequest request)
	{
	    String mess = request.getMessage();
		return new WSResponse(WSRequest.OPCODE_TEXT_FRAME, mess);
	}

}
