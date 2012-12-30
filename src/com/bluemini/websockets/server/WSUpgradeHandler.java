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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sun.misc.BASE64Encoder;


public class WSUpgradeHandler {
	
	public String pathFull = null;
	public String[] pathDetails = null;
	public Properties queryDetails = null;
	public String method = "";
	public String httpVersion = ""; 

	private ArrayList<String> pathDynamics = new ArrayList<String>();
	private Properties headers = new Properties();
	private StringBuilder body;
	
	private Boolean isUpgrade = null;
	public String failureReason = "";
	
	public static final String WSGUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	
	public WSUpgradeHandler(BufferedReader request)
	throws Exception
	{
		String line;
		int read;
		char[] buff = new char[1024];
		body = new StringBuilder();
		
		try {
			// read the first line
			parseFirstLine(request.readLine());
			
			// add each line to the headers, breaking on colon
			while (!(line = request.readLine()).trim().equals(""))
			{
				int colon = line.indexOf(":");
				if (colon > 0)
				{
					String headerName = line.substring(0, colon);
					String headerValue = line.substring(colon+1).trim();
					headers.setProperty(headerName, headerValue);
				}
				else
				{
					throw new Exception("Invalid header");
				}
			}
			
			// now the rest is body, if we haven't reached the end of the stream
			if (request.ready())
			{
				while ((read = request.read(buff)) >= 0)
				{
					body.append(buff, 0, read);
				}
			}
			
		}
		catch (IOException ioe)
		{
			// anything
		}
	}
	
	/**
	 * converts the request into a path and a querystring. These are then fed into
	 * an HttpRequest object.
	 * @param request
	 */
	private void parseFirstLine(String request)
	throws Exception
	{
		String path = "";
		Properties query = new Properties();
		
		Pattern p = Pattern.compile("^(GET)[ ]+/([^ ]*)[ ]+HTTP/1\\.1$");
		Matcher m = p.matcher(request);

		if (m.find())
		{
			method = request.substring(m.start(1), m.end(1));
			String uri = request.substring(m.start(2), m.end(2));

			System.out.println("URI: "+uri);
			int divider = uri.indexOf("?");
			if (divider >= 0)
			{
				path = uri.substring(0, divider);
				query = processQuery(uri.substring(divider+1));
			}
			else
			{
				path = uri;
			}
		}
		else
		{
			throw new Exception("A WebSocket connection MUST use GET and HTTP/1.1");
		}
		
		pathFull = path;
		pathDetails = path.split("/");
		queryDetails = query;
	}
	
	public boolean isUpgradeRequest(Server server)
	{
		if (isUpgrade != null)
		{
			return isUpgrade;
		}
		isUpgrade = false;
		
		// check all WebSocket upgrade headers are set and appropriate
		if (headers.containsKey("Upgrade") &&
				headers.containsKey("Origin") &&
				headers.containsKey("Sec-WebSocket-Key") &&
				// TODO: handle the protocol header..
				/* headers.containsKey("Sec-WebSocket-Protocol") && */
				headers.containsKey("Sec-WebSocket-Version") &&
				headers.containsKey("Connection") )
		{
			if (headers.getProperty("Upgrade").equals("websocket") &&
					headers.get("Sec-WebSocket-Version").equals("13") &&
					headers.getProperty("Connection").contains("Upgrade") &&
					server.hasHost((String) headers.get("Origin")))
			{
				isUpgrade = true;
			}
			else
			{
				failureReason = "Unable to find Upgrade==websocket, Connection==Upgrade, Version==13, Origin allowed";
			}
		}
		else
		{
			failureReason = "All requisite headers not found";
		}
		return isUpgrade;
	}
	
	/***
	 * 
	 */
	public String getAcceptKey()
	{
		String key = headers.getProperty("Sec-WebSocket-Key");
		return encodeHash(key);
	}
	
	public String encodeHash(String key)
	{
		try
		{
			byte[] value = WSUpgradeHandler.hashString(key + WSUpgradeHandler.WSGUID);
			BASE64Encoder enc = new BASE64Encoder();
			String encoded = enc.encode(value);
			return encoded;
		}
		catch (Exception e)
		{
			System.out.println("ERROR:"+e.getMessage());
			return e.getMessage();
		}
	}
	
	/***
	 * generate the correct SHA-1 hash of the incoming Sec-WebSocket-Key
	 */
	public static byte[] hashString(String toHash) 
			throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		// byte[] hashed = md.digest(toHash.getBytes("iso-8859-1"));
		byte[] hashed = md.digest(toHash.getBytes("UTF-8"));
		return hashed;
	}
	
	/**
	 * Creates a properties of the url query string. Breaking apart the
	 * query string on ampersands and equals signs to generate a set of
	 * name/value pairs.
	 */
	public static Properties processQuery(String s)
	{
		String[] params = s.split("&");
		Properties p = new Properties();
		for (String param: params) {
			String[] nv = param.split("=");
			if (nv.length == 2)
			{
				p.put(nv[0], nv[1]);
			}
			else if (nv.length == 1) 
			{
				p.put(nv[0], null);
			}
		}
		return p;
	}
	
	public String getFailure()
	{
		return failureReason;
	}

	public void addDynamic(String var)
	{
		pathDynamics.add(var);
	}
	
	public ArrayList<String> getDynamics()
	{
		return pathDynamics;
	}

}
