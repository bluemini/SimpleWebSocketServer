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

public class WSResponse {
	
	private byte[] response;
	
	private boolean FIN			= false;
	private boolean RSV1		= false;
	private boolean RSV2		= false;
	private boolean RSV3		= false;
	public boolean closing		= false;
	private byte opcode			= 0;
	private boolean masked		= false;
	private int mask			= 0; // server responses shouldn't be masked
	private long payloadSize	= 0;
	
	private String message;
	
	public WSResponse(int opcode, String message)
	{
		this.message = message;
		this.opcode = (byte) opcode;
		this.payloadSize = message.length();
		if (this.opcode == 8)
			this.closing = true;
		this.FIN = true;
	}
	
	public byte[] getResponse() {
		serialiseResponse();
		return response;
	}
	
	private void serialiseResponse()
	{
		byte[] tBuff = new byte[1024];
		
		// set the bits for the first byte
		tBuff[0] = 0;
		if (FIN)
			tBuff[0] = (byte) (tBuff[0] | (byte) (1 << 7) );
		if (RSV1)
			tBuff[0] = (byte) (tBuff[0] | (byte) (1 << 6) );
		if (RSV2)
			tBuff[0] = (byte) (tBuff[0] | (byte) (1 << 5) );
		if (RSV3)
			tBuff[0] = (byte) (tBuff[0] | (byte) (1 << 4) );
		tBuff[0] = (byte) (tBuff[0] | (byte) (opcode) );
		
		// set the payload length
		if (payloadSize <= 125)
		{
			tBuff[1] = (byte) payloadSize;
			for (int i=0; i<payloadSize; i++)
			{
				tBuff[i+2] = (byte) (message.charAt(i));
				// System.out.println("setting tBuff " + (i+2) + message.charAt(i));
			}
		}
		
		response = new byte[(int) (payloadSize + 2)];
		for (int i=0; i<(payloadSize+2); i++)
		{
			response[i] = tBuff[i];
		}
	}

}
