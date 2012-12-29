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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Properties;

public class Server implements Runnable {
	
	private boolean keepServing = true;
	private PrintStream sys;
	
	private int listenPort = 88;
	private ArrayList<SocketAddress> connections = new ArrayList<SocketAddress>();
	private Properties props;
	
	@Override
	public void run() {
		ServerSocket server;
		Socket socket = null;
		
		// set up the HTTP server to listen for connections..
		try {
			server = new ServerSocket(listenPort);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return;
		}
		
		System.out.println("listening for connections on port "+listenPort);
		
		/* wait for incoming connection. When received, we process it to
		 * see if we have a matching url to service. 
		 */
		while (keepServing) {
			try {
				socket = server.accept();
				InputStream in = socket.getInputStream();
				String response = "";
				SocketAddress remote = socket.getRemoteSocketAddress();
				
				while (true) {
					try {
						if (connections.contains(remote)) {
							WSRequest request = processWSRequest(in);
							// System.out.println("Received: "+request.)
							
							sendResponse(request.response.getResponse(), socket);
							System.out.println(new String(request.response.getResponse()));

							if (request.closing)
							{
								System.out.println("Closing WebSocket");
								break;
							}
						} else {
							BufferedReader br = new BufferedReader(new InputStreamReader(in));
							response = startSession(br);
							sendResponse(response, socket);
							connections.add(remote);
						}
					} catch (Exception e) {
						response = "There was an error...";
						break;
					}
				}
				
			} catch (Exception e) {
				System.out.println("HttpServer: "+e.getMessage());
				return;
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (Exception e) {}
				}
			}
		}
	}
	
	/**
	 * takes the input from the socket and create
	 * @param br
	 * @return
	 */
	private String echoRequest(BufferedReader br) {
		String response;
		
		try { 
			response = br.readLine();
		} catch (Exception e) {
			response = "Unable to echo request...";
		}
		
		System.out.println("returning response.."+response);
		return response;
	}
	
	/**
	 * Starts a new WebSocket session by upgrading to the web socket
	 */
	private String startSession(BufferedReader br)
	{
		WSUpgradeHandler request = new WSUpgradeHandler(br);
		String sessionStarted = "Boo-Hoo";
		if (request.isUpgradeRequest() ) {
			// generate upgrade response
			StringBuilder resp = new StringBuilder();
			resp.append("HTTP/1.1 101 Switching Protocols\n");
			resp.append("Upgrade: websocket\n");
			resp.append("Connection: Upgrade\n");
			resp.append("Sec-WebSocket-Accept: " + request.getAcceptKey() + "\n");
			resp.append("\n");
			sessionStarted = resp.toString();
		} else {
			System.out.println("Not here!");
		}
		System.out.println(sessionStarted);
		return sessionStarted;
	}
	
	/**
	 * ensures that the data is a WS basic frame and extracts the essential data
	 */
	private WSRequest processWSRequest(InputStream in) {
		WSRequest request = new WSRequest(in);
		
		return request;
	}
	/***
	 * We take a responseBody (String) and push it out through the provided socket
	 * @param responseBody
	 * @param socket
	 * @throws IOException
	 */
	private void sendResponse(String responseBody, Socket socket) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
		bos.write(responseBody.getBytes());
		bos.flush();
	}
	
	private void sendResponse(byte[] responseBody, Socket socket) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
		bos.write(responseBody);
		bos.flush();
	}
	
}
