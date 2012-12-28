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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Properties;

import sun.misc.BASE64Encoder;


public class WSRequest
{
	
	public String pathFull = null;
	public String[] pathDetails = null;
	public Properties queryDetails = null;
	public String method = "";
	public String httpVersion = ""; 

	private ArrayList<String> pathDynamics = new ArrayList<String>();
	private Properties headers = new Properties();
	private StringBuilder body;
	
	private boolean FIN			= false;
	private boolean RSV1		= false;
	private boolean RSV2		= false;
	private boolean RSV3		= false;
	private byte opcode			= 0;
	private boolean masked		= false;
	private int mask			= 0;
	private long payloadSize	= -1;
	private String payload;
	
	private byte OPCODE_CONTINUATION_FRAME	= 0;
	private byte OPCODE_TEXT_FRAME			= 1;
	private byte OPCODE_BINARY_FRAME		= 2;
	private byte OPCODE_CONNECTION_CLOSE	= 8;
	private byte OPCODE_PING				= 9;
	private byte OPCODE_PONG				= 10;
	
	public static final String WSGUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	
	/**
	 * The constructor takes an input stream and parses out the meaning. You
	 * do NOT construct the WSRequest to generate the appropriate headers
	 * for a connection upgrade. These should all be called statically.
	 * @param request
	 */
	public WSRequest(InputStream request)
	{
		String line;
		int read;
		byte[] buff = new byte[1024];
		long bytesLeft = 0;
		
		try
		{
			// get the first byte
			setStatus(request.read());
			
			// figure out the payload length
			setPayload(request);
			
			// figure out the masking
			if (masked)
			{
				mask = 0;
				for (int i=4; i>0; i--)
				{
					mask += request.read() << ((i-1)*8);
				}
				System.out.println("Mask: "+mask);
			}
			
			// read in the rest ..
			do
			{
				int bytesRead = request.read(buff);
				bytesLeft = payloadSize-bytesRead;
				System.out.println("read bytes: " + bytesRead + ", to read: " + bytesLeft);
				
				// unmask the data (if masked)
				if (masked)
				{
					buff = unmask(buff, bytesRead, mask);
				}
				
			}
			while (bytesLeft <= 0);
		} catch (IOException ioe) {
			// anything
		}
	}
	
	/**
	 * Reads the first byte of the incoming data stream and establishes the
	 * type of the request and the sets the various flags.
	 * @param status
	 */
	private void setStatus(int status)
	{
		if ((status & 1) == 1)
			FIN = true;
		if ((status & 2) == 2)
			RSV1 = true;
		if ((status & 4) == 4)
			RSV2 = true;
		if ((status & 8) == 8)
			RSV3 = true;
		opcode = (byte) ((status & (128+4+32+16)) >> 4);
	}
	
	/**
	 * Parse the payload length data so that we know how much data to read in during
	 * the following actions.
	 * @param request
	 * @throws IOException
	 */
	private void setPayload(InputStream request) throws IOException
	{
		byte payloadHeader = (byte) request.read();
		
		// first establish any masking..
		if ((payloadHeader & 128) == 128)
			masked = true;
		
		// then get the intial payload size value
		payloadSize = payloadHeader & 127;
		
		// if the payload size is bigger than 125, we need to parse it in..
		if (payloadSize == 126) // 126 means the length is contained in the next 2 bytes
		{
			payloadSize = request.read() << 8 + request.read();
		}
		else if (payloadSize == 127) // 127 means the length is in the next 8 bytes
		{
			payloadSize = 0;
			for (int i=0; i<8; i++)
			{
				payloadSize += request.read() << i;
			}
		}
		
		// a little debug
		System.out.println("Payload: " + payloadSize);
	}
	
	private byte[] unmask(byte[] buffer, int length, int mask)
	{
		byte[] message = new byte[length];
		byte[] newMask = intToByteArray(mask);
		
		for (int i=0; i<length; i++)
		{
			message[i] = (byte) (newMask[i%4] ^ buffer[i]);
			// System.out.println(Byte.toString(buffer[i]) + ", " + Byte.toString(message[i]) + ", " + Byte.toString(newMask[i%4]));
		}
		
		return message;
	}
	
	/**
	 * helper function to convert an int into a 4 byte array
	 * @param a
	 * @return
	 */
	public static byte[] intToByteArray(int a)
	{
	    return new byte[] {
	        (byte) ((a >> 24) & 0xFF),
	        (byte) ((a >> 16) & 0xFF),   
	        (byte) ((a >> 8) & 0xFF),   
	        (byte) (a & 0xFF)
	    };
	}

}
