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
package com.bluemini.websockets;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;

import com.bluemini.websockets.server.SWSSHandler;
import com.bluemini.websockets.server.Server;

/**
 * Launch the Engine from a variety of sources, either through a main() or invoked through
 * Apache Daemon.
 */
public class ServerWrapper implements Daemon {
    
    private static final Logger log = Logger.getLogger("SWSS");
    private static Server engine = null;
    private static Thread mainThread = null;
    private static Properties swssProperties = null;
    private static String socketHandlerClass = "com.bluemini.websockets.EchoHandler";


    private static ServerWrapper engineLauncherInstance = new ServerWrapper();


    public ServerWrapper()
    {
        try {
            Appender l = new FileAppender(new org.apache.log4j.SimpleLayout(), System.getProperty("user.home") + "\\swss.log");
            log.addAppender(l);
        } catch (Exception e) {
            //
        }
    }

    /**
     * The Java entry point.
     * @param args Command line arguments, all ignored.
     */
    public static void main(String[] args) {
        loadProperties();
        // the main routine is only here so I can also run the app from the command line
        engineLauncherInstance.initialize();
        
        log.info("initialisation complete");

        Scanner sc = new Scanner(System.in);
        // wait until receive stop command from keyboard
        System.out.printf("Enter 'stop' to halt: ");
        while(true)
        {
            String comm = sc.nextLine().toLowerCase();
            if (comm.equals("stop"))
            {
                log.info("breaking out of main method");
                break;
            }
            log.info("User entered some text: " + comm);
        }

        engineLauncherInstance.terminate();

    }
    
    private static void loadProperties()
    {
        try {    
            swssProperties = new Properties();
            InputStream propertyStream = ServerWrapper.class.getResourceAsStream("/setting.properties");
            if (propertyStream != null)
            {
                swssProperties.load(ServerWrapper.class.getResourceAsStream("/setting.properties"));  //Assuming it is at top level, not under any package
                if (swssProperties.contains("handlerclass"))
                {
                        swssProperties.getProperty("handlerclass");
                }
            }
        } catch (IOException ex) {
            System.out.println("Could not open Config file");    
        }  
    }
    
    /**
     * Static methods called by prunsrv to start/stop
     * the Windows service.  Pass the argument "start"
     * to start the service, and pass "stop" to
     * stop the service.
     *
     * Taken lock, stock and barrel from Christopher Pierce's blog at http://blog.platinumsolutions.com/node/234
     *
     * @param args Arguments from prunsrv command line
     **/
    public static void windowsService(String args[])
    {
        String cmd = "start";
        if (args.length > 0) {
            cmd = args[0];
        }

        if ("start".equals(cmd)) {
            log.info("Calling windowsStart from windowsService");
            engineLauncherInstance.windowsStart();
        }
        else {
            log.info("Calling windowsStop from windowsService");
            engineLauncherInstance.windowsStop();
        }
    }

    public void windowsStart()
    {
        log.debug("windowsStart called");
        initialize();
        while (engine != null && !engine.keepServing) {
            // don't return until stopped
            synchronized(this) {
                try {
                    this.wait(60000);  // wait 1 minute and check if stopped
                }
                catch(InterruptedException ie){}
            }
        }
    }

    public void windowsStop()
    {
        terminate();
        synchronized(this) {
            // stop the start loop
            this.notify();
        }
    }

    // Implementing the Daemon interface is not required for Windows but is for Linux
    @Override
    public void init(DaemonContext arg0) throws Exception
    {
    }

    @Override
    public void start()
    {
        //log.debug("Daemon start");
        initialize();
    }

    @Override
    public void stop()
    {
        //log.debug("Daemon stop");
        terminate();
    }

    @Override
    public void destroy()
    {
        //log.debug("Daemon destroy");
    }

    /**
     * Do the work of starting the engine
     */
    private void initialize()
    {
        if (engine == null) {
            log.info("Starting the Engine");
            
            // We're going to use the echo handler to simply echo back the message
            try {
                SWSSHandler handler = (SWSSHandler) Class.forName(socketHandlerClass).newInstance();
                engine = new Server(handler);
                
                // we'll accept connections from all domains
                engine.setHost("*");
                
                // start the WebSocket server
                mainThread = new Thread(engine, "WebSocketServer");
                mainThread.start();
            }
            catch (ClassNotFoundException cnfe)
            {
                log.error("Class not found: " + socketHandlerClass);
            }
            catch (Exception e)
            {
                log.error("Other problem: " + e.getMessage());
            }
            log.info("returning from the initialize method");
        }
    }

    /**
     * Cleanly stop the engine.
     */
    public void terminate()
    {
        log.info("Terminating the server");
        if (mainThread.isAlive())
        {
            log.info("server thread is currently alive, attempting to call stop()");
            engine.stop();
            log.info("server engine stop() called, has been stopped");
        }
    }
}
