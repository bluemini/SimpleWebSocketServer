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
	
}
