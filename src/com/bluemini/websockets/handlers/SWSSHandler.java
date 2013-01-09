package com.bluemini.websockets.handlers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.bluemini.websockets.server.WSRequest;
import com.bluemini.websockets.server.WSResponse;

public abstract class SWSSHandler {
	
	public void response(WSRequest request)
	{
		// sendResponse(request, new WSResponse(WSRequest.OPCODE_TEXT_FRAME, "DEFAULT RESPONSE!") );
	}
	
	public void upgrade(WSRequest request)
	{
		// Implement this..
	}

    public void onClose(WSRequest request)
    {
        // Implement this..
    }

    public void sendResponse(WSRequest request, WSResponse response)
	{
		System.out.println("Sending a response");
		try
		{
			System.out.println(response.toString());
			request.sendResponse(response);
		}
		catch (UnsupportedEncodingException uee)
		{
			System.out.println("Unable to (en|de)code the response. " + uee.getMessage());
		}
		catch (IOException ioe)
		{
			System.out.println("Unable to send response. " + ioe.getMessage());
		}
	}

}
