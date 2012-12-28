package com.bluemini.websockets;

import com.bluemini.websockets.server.Server;

public class SimpleServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Thread(new Server(), "WebSocketServer").start();
	}

}
