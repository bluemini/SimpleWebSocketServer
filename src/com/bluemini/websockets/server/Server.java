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

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class Server implements Runnable {
    
    public boolean keepServing = true;
    
    private ServerSocket server;

    private static int defaultPort = 88;
    private int listenPort;
    public SWSSHandler handler;
    private List<Thread> connections = new LinkedList<Thread>();
    private HashSet<String> Hosts = new HashSet<String>();
    
    public Server(SWSSHandler handler)
    {
        this(defaultPort, handler);
    }
    
    public Server(int port, SWSSHandler handler)
    {
        listenPort = port;
        this.handler = handler;
    }
    
    public void setHost(String host)
    {
        Hosts.add(host);
    }
    
    @Override
    public void run() {
        // set up the HTTP server to listen for connections..
        try
        {
            server = new ServerSocket(listenPort);
            System.out.println("listening for connections on port "+listenPort);
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            return;
        }
        
        serve();
    }
    
    /* wait for incoming connection. When received, we process it to
     * see if we have a matching url to service. 
     */
    private void serve()
    {
        Socket socket = null;
        try
        {
            while (keepServing) {
                socket = server.accept();
                System.out.println("Accepting a new session");
                Thread t = new Thread(new WSRequest(this, socket));
                t.start();
                connections.add(t);
                cleanseThreads();
            }
        }
        catch (Exception e)
        {
            System.out.println("Server Error: "+e.getMessage());
        }
        finally
        {
            if (socket != null)
            {
                try
                {
                    socket.close();
                }
                catch (Exception e)
                {
                    System.out.println("Error closing Server socket. " + e.getMessage());
                }
            }
        }
    }
    
    synchronized public boolean hasHost(String host)
    {
        if (Hosts.contains("*") || Hosts.contains(host))
        {
            return true;
        }
        return false;
    }
    
    public void stop()
    {
        Iterator<Thread> i = connections.iterator();
        while (i.hasNext())
        {
            Thread tt = i.next();
            tt.interrupt();
        }
        
        // to close down the main server thread, we close the socket
        try
        {
            server.close();
        }
        catch (Exception e) {
            System.out.println("closing socket on Server errored. " + e.getMessage());
        }
    }
    
    // each new connection, run through the list and remove any expired threads
    public void cleanseThreads()
    {
        Iterator<Thread> i = connections.iterator();
        while (i.hasNext())
        {
            Thread tt = i.next();
            if (!tt.isAlive())
            {
                i.remove();
            }
        }
    }
}
