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
