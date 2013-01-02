package com.bluemini.websockets.handlers;

import com.bluemini.websockets.server.WSRequest;
import com.bluemini.websockets.server.WSResponse;

public abstract class SWSSHandler {
	
	public WSResponse response(WSRequest request)
	{
		return new WSResponse(WSRequest.OPCODE_TEXT_FRAME, "DEFAULT RESPONSE!");
	}

}