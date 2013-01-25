Simple WebSocket Server
=======================

A simple, Java based, WebSocket server. Written mostly for fun, but also as a backend to 
some WebSocket play.

At present, it doesn't handle fragmented messages, and there isn't a control interface yet
so all it does is send back a static reply. However, it is now implementing the 'core'
functions, so it will upgrade the connection to a WebSocket connection, receive, decode
and respond (with a static message) to an incoming message, and close the socket when
asked.

Still a lot to do, but it's been fun.


Running it
----------

To run it, start com.bluemini.websocket.SimpleServer

For Windows users, there's a startWSServer.bat to help


Handlers
--------

The basic server simply decodes and defragments incoming WebSocket data. To make any of
this useful you need to do something with the incoming data and respond back to the client
in some meaningful way.

The Server constructor takes a SWSSHandler (abstract) which implements the 'response' method, 
taking a WSRequest and returning a WSResponse. It can also handle the upgrade event and
the onClose event to take necessary actions when these occur.

An example EchoHandler that sends back the data you send it is available in the server package.


Other Stuff
-----------

Working from RFC 6455 (http://tools.ietf.org/html/rfc6455)
