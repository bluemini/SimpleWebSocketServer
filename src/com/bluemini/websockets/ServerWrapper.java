package com.bluemini.websockets;

import java.util.Scanner;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
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

        if (!engine.keepServing) {
            log.info("Attempting to terminate SWSS");
            engineLauncherInstance.terminate();
        }
        engineLauncherInstance.terminate();

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
        String className = "com.bluemini.webrtc.WebRTCHandler";
        if (engine == null) {
            log.info("Starting the Engine");
            
            // We're going to use the echo handler to simply echo back the message
            try {
                SWSSHandler handler = (SWSSHandler) Class.forName(className).newInstance();
                engine = new Server(handler);
                
                // we'll accept connections from all domains
                engine.setHost("*");
                
                // start the WebSocket server
                mainThread = new Thread(engine, "WebSocketServer");
                mainThread.start();
            }
            catch (ClassNotFoundException cnfe)
            {
                log.error("Class not found: " + className);
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
