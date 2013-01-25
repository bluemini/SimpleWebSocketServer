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
package com.bluemini.webrtc;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluemini.websockets.server.SWSSHandler;
import com.bluemini.websockets.server.WSRequest;
import com.bluemini.websockets.server.WSResponse;

public class WebRTCHandler extends SWSSHandler {
	
	private ArrayList<WebRTCSession> sessions = new ArrayList<WebRTCSession>();
    private HashMap<String, WebRTCClient> clients = new HashMap<String, WebRTCClient>();
	
    @Override
	public void response(WSRequest request)
	{
        JSONObject jsonResponse = new JSONObject();
	    try
	    {
	        boolean success = false;
	        String message = request.getMessageString();
	        JSONObject jsonRequest = new JSONObject(message);
	        System.out.println("message received from "+request.getId() + ": " + message);
	        
	        if (jsonRequest.has("login")) {
	            JSONObject loginRequest = jsonRequest.getJSONObject("login");
	            if (loginRequest != null) {
	                success = registerClient(request, loginRequest);
	                jsonResponse.put("success", success);
	            }
	        } else if (jsonRequest.has("users")) {
	            jsonResponse.put("users", new JSONArray());
	            try
	            {
	            	Iterator<String> iter = clients.keySet().iterator();
	            	while (iter.hasNext()) {
	            	    String clientId = iter.next();
	            	    JSONObject client = clients.get(clientId).toJSON();
	            	    client.append("ID", clientId);
	            		jsonResponse.append("users", client);
	            	}
	            }
	            catch (JSONException jsone)
	            {
	            	System.out.println("Unable to serialize clients. " + jsone.getMessage());
	            }
	        /*
	        } else if (jsonRequest.has("sdp")) {
	        	// if the request's id exists in clients, then we set it's sdp data
	        	if (clients.containsKey(request.getId()))
	        	{
	        		String sdp = jsonRequest.getJSONObject("sdp").getString("sdp");
	        		WebRTCSession session = clients.get(request.getId()).getOtherClient();
	        		response.put("success", false);
	        	}
	        	else
	        	{
	        		response.put("success", false);
	        	} */
	        } else if (jsonRequest.has("call")) {
	            success = callUser(request, jsonRequest);
	            if (success) {
	                jsonResponse.put("sessionstarted", success);
	            } else {
	                jsonResponse.put("success", success);
	            }
            } else if (clients.containsKey(request.getId())) {
            	// TODO: verify that this client is connected to something
                WebRTCClient outClient = clients.get(request.getId()).getOtherClient();
                if (outClient != null)
                {
                    System.out.println("Forwarding message from " + request.getId() + ": " + message);
                    int opcode = request.getOpcode();
                    byte[] inmessage = request.getMessage();
                    WSResponse response = new WSResponse(opcode, inmessage);
                    WSRequest client = outClient.getChannel(); 
                    sendResponse(client, response);
                }
                else
                {
                	System.out.println("Got back a null for the outClient object");
                }
            } else {
                jsonResponse.put("success", false);
	        }

	        if (jsonResponse.length() > 0) {
		        System.out.println("JSON response: " + jsonResponse.toString(2));
		    	sendResponse(request, new WSResponse(WSRequest.OPCODE_TEXT_FRAME, jsonResponse.toString() ) );
	        }
	        
	    }
	    catch (JSONException jsone)
	    {
	        System.out.println("Unable to extract the nature of the request. " + jsone.getMessage());
	        jsone.printStackTrace(System.out);
	    }
	    catch (UnsupportedEncodingException uee)
	    {
	    	System.out.println("ERROR: encoding/decoding between byte array and string");
	    }
	}
	
	@Override
	public void upgrade(WSRequest request)
	{
		// nothing to do..
	}
	
	@Override
	public void onClose(WSRequest request)
	{
	    // TODO: need to remove closed websocket sessions from the clients list and any sessions
	    if (clients.containsKey(request.getId()))
	    {
	        // disconnect this client from any sessions and then drop the client
	        WebRTCClient client = clients.get(request.getId()); 
	        client.disconnect(true);
	        clients.remove(request.getId());
	    }
	}
	
	/**
	 * Attempts to register the connecting client with the server. The details of the client
	 * will be saved in a session object and can later be returned to other clients so they
	 * can view a list of other connected users.
	 * @param jsonRequest
	 */
    private boolean registerClient(WSRequest request, JSONObject loginRequest)
    {
        if (loginRequest.has("name") && loginRequest.has("nick")) {
            WebRTCClient client;
            try
            {
                client = new WebRTCClient(request, loginRequest.getString("name"), loginRequest.getString("nick"));
                if (!clients.containsKey(request.getId()))
                {
                    clients.put(request.getId(), client);
                }
                return true;
            }
            catch (JSONException jsone)
            {
                System.out.println("Unable to add the client to the session. " + jsone.getMessage());
            }
        }
        return false;
    }
    
    public boolean callUser(WSRequest request, JSONObject jsonRequest)
    throws JSONException
    {
        // ensure the initiator is not attempting to contact themselves, that the
        // other party exists and we have their SDP data. Then return it!
    	if ( jsonRequest.has("call") ) 
    	{
    		// get the clientId and check it exists
    		String clientId = jsonRequest.getString("call");
			if ( clients.containsKey(clientId) && !clientId.equals(request.getId()) )
			{
	    		// get the client and check we have SDP data
                WebRTCClient inClient = clients.get(request.getId());
                WebRTCClient outClient = clients.get(clientId);
			    WebRTCSession session = new WebRTCSession(inClient, outClient);
	    		sessions.add(session);
	    		inClient.connect(session, true);
	    		outClient.connect(session, false);
	    		return true;
			}
    	}
        return false;
    }

}

class WebRTCSession
{
	private Calendar LastAccess = Calendar.getInstance();
	protected WebRTCClient caller;
	protected WebRTCClient callee;
	
	public WebRTCSession(WebRTCClient caller, WebRTCClient callee)
	{
	    this.caller = caller;
	    this.callee = callee;
	}
	
	public Calendar getInitDateTime()
	{
	    return this.LastAccess;
	}
	
	/**
	 * Closes a session, disconnecting clients if present. Removes internal
	 * references to caller/callee. A reference to a client is passed in and
	 * then the 'other' client is disconnected.
	 * @param client
	 */
	public void close(WebRTCClient client)
	{
	    if (this.caller != null && !this.caller.equals(client))
        {
            this.caller.disconnect(false);
        }
	    else if (this.callee != null && !this.callee.equals(client))
        {
            this.callee.disconnect(false);
        }
	    this.caller = null;
	    this.callee = null;
	}
}

class WebRTCClient
{
	private String name;
	private String nick;
	private WSRequest channel;
	public WebRTCSession session;
	public boolean isCaller;
	public String sdp = null;
	
	public WebRTCClient(WSRequest request, String name, String nick)
	{
	    this.channel = request;
	    this.name = name;
	    this.nick = nick;
	}
	
	public WSRequest getChannel()
	{
		return this.channel;
	}
	
	public void connect(WebRTCSession session, boolean isCaller)
	{
		this.session = session;
		this.isCaller = isCaller;
	}
	
	public void disconnect(boolean cascade)
	{
	    // if the client is attached to a session, we need to drop it
	    if (cascade && this.session != null)
	    {
	        this.session.close(this);
	    }
        this.session = null;
        System.out.println("Disconnected client/session");
	}
	
	/**
	 * If the client is in a session, return the 'other' client, otherwise
	 * return null.
	 * @return
	 */
	public WebRTCClient getOtherClient()
	{
	    if (session != null)
	    {
	        if (isCaller)
	        {
	            System.out.println("sending back the caller client");
	            return session.callee;
	        }
	        else
	        {
	            System.out.println("sending back the callee client");
	            return session.caller;
	        }
	    }
	    return null;
	}
	
	public JSONObject toJSON()
	throws JSONException
	{
		JSONObject clientJson = new JSONObject();
		clientJson.put("user", this.name);
		clientJson.put("nick", this.nick);
		if (this.sdp != null)
		{
			clientJson.put("sdp", this.sdp);
		}
		return clientJson;
	}
	
	public String toJSONString()
	throws JSONException
	{
	    return toJSON().toString();
	}
}