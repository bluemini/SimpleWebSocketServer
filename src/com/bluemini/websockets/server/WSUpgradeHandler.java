package com.bluemini.websockets.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
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
	
	public static final String WSGUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	
	public WSUpgradeHandler(BufferedReader request)
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
			
			System.out.println("Request Path    :"+pathFull);
			System.out.println("Query String    :"+queryDetails);
			Enumeration en = headers.keys();
			while (en.hasMoreElements())
			{
				String key = (String) en.nextElement();
				System.out.println("Request headers :"+key+" :: "+headers.getProperty(key));
			}
		}
		catch (IOException ioe)
		{
			// anything
		}
		catch (Exception e)
		{
			// anything else
		}
	}
	
	/**
	 * converts the request into a path and a querystring. These are then fed into
	 * an HttpRequest object.
	 * TODO: process more than just GET requests 
	 * @param request
	 */
	private void parseFirstLine(String request)
	{
		String path = "";
		Properties query = new Properties();
		
		Pattern p = Pattern.compile("^(GET|POST|PUT)[ ]+/([^ ]*)[ ]+HTTP/1\\.([01])$");
		Matcher m = p.matcher(request);

		if (m.find())
		{
			method = request.substring(m.start(1), m.end(1));
			String uri = request.substring(m.start(2), m.end(2));
			method = "1."+request.substring(m.start(3), m.end(3));

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
		
		pathFull = path;
		pathDetails = path.split("/");
		queryDetails = query;
	}
	
	public boolean isUpgradeRequest()
	{
		if (isUpgrade != null)
		{
			return isUpgrade;
		}
		isUpgrade = false;
		
		// check all WebSocket upgrade headers are set and appropriate
		if (headers.containsKey("Upgrade") &&
				headers.containsKey("Sec-WebSocket-Key") &&
				// TODO: handle the protocol header..
				/* headers.containsKey("Sec-WebSocket-Protocol") && */
				headers.containsKey("Sec-WebSocket-Version") &&
				headers.containsKey("Connection") )
		{
			if (headers.getProperty("Upgrade").equals("websocket") &&
					headers.getProperty("Connection").contains("Upgrade"))
			{
				isUpgrade = true;
			}
			else
			{
				System.out.println("Unable to find Upgrade==websocket and Connection==Upgrade");
			}
		}
		else
		{
			System.out.println("All requisite headers not found");
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
			System.out.println("Incoming key: " + key);
			byte[] value = WSUpgradeHandler.hashString(key + WSUpgradeHandler.WSGUID);
			BASE64Encoder enc = new BASE64Encoder();
			String encoded = enc.encode(value);
			System.out.println("Encoded string: " + encoded);
			return encoded;
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
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
		byte[] hashed = md.digest(toHash.getBytes("iso-8859-1"));
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

	public void addDynamic(String var)
	{
		pathDynamics.add(var);
	}
	
	public ArrayList<String> getDynamics()
	{
		return pathDynamics;
	}

}
