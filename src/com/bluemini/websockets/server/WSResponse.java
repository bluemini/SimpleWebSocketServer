/**
 * Copyright (c) 2012, Nick Harvey
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the <ORGANIZATION> nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *   
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *   ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *   LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *   INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *   CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *   ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE.
 */
package com.bluemini.websockets.server;

import java.io.UnsupportedEncodingException;

public class WSResponse {
    
    private byte[] response;
    
    private boolean FIN			= false;
    private boolean RSV1		= false;
    private boolean RSV2		= false;
    private boolean RSV3		= false;
    private boolean masked		= false;
    private byte opcode			= 0;
    private long payloadSize	= 0;
    private int mask			= 0; // server responses shouldn't be masked
    private String messageString;

    public boolean closing		= false;
    private byte[] message;
    
    public WSResponse(int opcode, String message)
    throws UnsupportedEncodingException
    {
        this(opcode, message.getBytes("UTF-8"));
        // System.out.println("ERROR: Unable to encode the message in UTF-8");
        // throw new SWSSException("Unable to encode the message in UTF-8");
    }

    public WSResponse(int opcode, byte[] message)
    throws UnsupportedEncodingException
    {
        this.messageString = new String(message, "UTF-8");
        this.opcode = (byte) opcode;
        if (this.opcode == 8)
            this.closing = true;
        this.FIN = true;
        this.message = message;
        payloadSize = message.length;
    }
    
    public byte[] getResponse() {
        serialiseResponse();
        return response;
    }
    
    private void serialiseResponse()
    {
        byte[] responseHeader = new byte[2];
        byte[] responsePayload;
        
        // set the bits for the first byte
        responseHeader[0] = 0;
        responseHeader[1] = 0;
        if (FIN)
            responseHeader[0] = (byte) (responseHeader[0] | (byte) (1 << 7) );
        if (RSV1)
            responseHeader[0] = (byte) (responseHeader[0] | (byte) (1 << 6) );
        if (RSV2)
            responseHeader[0] = (byte) (responseHeader[0] | (byte) (1 << 5) );
        if (RSV3)
            responseHeader[0] = (byte) (responseHeader[0] | (byte) (1 << 4) );
        responseHeader[0] = (byte) (responseHeader[0] | (byte) (opcode) );
        System.out.println("First byte of response. " + responseHeader[0]);
        
        // set the payload length
        if (payloadSize <= 125)
        {
            responseHeader[1] = (byte) payloadSize;
            responsePayload = new byte[0];
        }
        else if (payloadSize <= 65535)
        {
            responseHeader[1] = (byte) 126;
            responsePayload = new byte[2];
            responsePayload[0] = (byte) (payloadSize >> 8);
            responsePayload[1] = (byte) (payloadSize & 255);
        }
        else // TODO handle payloads over 65535 bytes
        {
            responsePayload = new byte[0];
        }
    
        response = new byte[(int) (2 + responsePayload.length + message.length)];
        for (int i=0; i<2; i++)
        {
            response[i] = responseHeader[i];
        }
        
        // append
        int i;
        for (i=0; i<responsePayload.length; i++)
        {
            response[2+i] = responsePayload[i];
        }
        int payloadStart = 2 + responsePayload.length;
        
        for (i=0; i<message.length; i++)
        {
            response[payloadStart + i] = message[i];
        }
        
    }
    
    @Override
    public String toString()
    {
        return "WSResponse: " +
            "\n  FIN: " + FIN +
            "\n  RSV1: " + RSV1 + 
            "\n  RSV2: " + RSV2 + 
            "\n  RSV3: " + RSV3 +
            "\n  Opcode: " + opcode +
            "\n  Masked: " + masked +
            "\n  Payload Size: " + payloadSize +
            "\n  Message: " + messageString +
            "\n  Mask: " + mask +
            "\n  Closing: " + closing;
    }

}
