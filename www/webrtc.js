	var signalingChannel;
	var pc;
	// var configuration = {"iceServers":[{"url":"stun:stun.l.google.com:19302"}]};
	var configuration = {'iceServers': []};
	var selfView;
	var remoteView;
	var localStream;
	var WSServer = "127.0.0.1";
	// var WSServer = "10.168.0.59";
	var caller = false;
	
	//console.log(webkitRTCPeerConnection);
	var RTCPeerConnection = RTCPeerConnection || webkitRTCPeerConnection || mozRTCPeerConnection;
	navigator.getUserMedia = navigator.getUserMedia || navigator.webkitGetUserMedia || navigator.mozGetUserMedia;

	// run start(true) to initiate a call
	function start(isCaller) {
		
		console.log("Starting PC");
		caller = isCaller;
		
	    pc = new RTCPeerConnection(configuration);

	    // send any ice candidates to the other peer
	    pc.onicecandidate = function (evt) {
	    	if (evt.candidate) {
		    	console.log("C->S: " + JSON.stringify(evt.candidate));
		        signalingChannel.send(JSON.stringify({ "candidate": evt.candidate }));
	    	} else {
	    		console.log("End of candidates");
	    	}
	    };
	    
	    pc.onopen = function(evt) {
	    	console.log("PC session is now open");
	    }
	    
	    pc.onconnecting = function(message) {
	    	console.log("PC Session connecting.");
	    }

	    // once remote stream arrives, show it in the remote video element
	    pc.onaddstream = function (evt) {
	    	console.log("Adding remote stream - setting remote view");
	        
	    	remoteView.autoplay = true;
	        remoteView.src = URL.createObjectURL(evt.stream);
	        // remoteView.src = selfView.src;
	    };
	    
	    pc.onremovestream = function(evt) {
	    	console.log("Removing stream");
	    	console.log(evt);
	    }

	    // get the local stream, show it in the local video element and send it
	    navigator.getUserMedia({ "audio": true, "video": true }, function (stream) {
	    	
	    	selfView.autoplay = true;
	        selfView.src = URL.createObjectURL(stream);
	        localStream = stream;

	        if (isCaller) { // getting hold of video on the caller, we can produce an offer
	        } else { // getting hold of video on the receiver allows us to answer an offer 
	        }

	    },
	    function(evt) {
	    	console.log("getUserMedia failed. " + evt);
	    });
	}
	
	function gotDescription(desc) {
		console.log("Setting local description");
        pc.setLocalDescription(desc); // appears to be the trigger to generate ICE candidates
        
        console.log("Sending message: " + JSON.stringify(desc))
        signalingChannel.send(JSON.stringify({ "sdp": desc }));
    }

    setStatus = function(message) {
		document.getElementById("status").innerHTML += message+'<br>';
	}
	
	login = function() {
		var loginRequest = {
			login: {
				name: document.getElementById("name").value,
				nick: document.getElementById("nick").value
			}
		};
		signalingChannel.send(JSON.stringify(loginRequest))
	}
	
	getUsers = function() {
		console.log("fetching users");
		signalingChannel.send('{"users":""}');
	}
	
	renderUsers = function(userData) {
		var userList = document.getElementById("userlist");
		var newHtml = "";
		for (user in userData) {
			newHtml += "<p onclick='callUser("+userData[user].ID+")'>" + userData[user].nick + "</p>";
		}
		userList.innerHTML = newHtml;
	}
	
	callUser = function(userId) {
		console.log("Creating pairing with the remote client (on the server)");
		var mssg = {
			call: ""+userId
		}
		signalingChannel.send(JSON.stringify(mssg));
	}
	
	window.onload = function() {
		selfView = document.getElementById("video");
    	remoteView = document.getElementById("remote");
		document.getElementById("login").onclick = function() {

			if (!pc)
		        start(false);

			if (document.getElementById("name").value && document.getElementById("nick")) {
				
				if (!signalingChannel) {
					signalingChannel = new WebSocket('ws://'+WSServer+':88/');
					console.log("connecting...");
				}

				if (signalingChannel) {
					signalingChannel.onmessage = function (evt) {
	
					    var signal = JSON.parse(evt.data);

					    if (signal.sdp) {
							console.log("WS: Received SDP offer");
							pc.addStream(localStream);
							pc.setRemoteDescription(new RTCSessionDescription(signal.sdp));
				    		console.log("Creating answer");
				    		pc.createAnswer(gotDescription, null);

					    } else if (signal.users) {
					    	if (!pc) {
					    		alert("You must allow access to audio/video before starting comms");
					    	} else {
						    	console.log("WS: Received some users..." + evt.data);
						    	renderUsers(signal.users);
					    	}

					    } else if (signal.hasOwnProperty("candidate")) {
					    	console.log("WS: S->C: Receiving a candidate: " + JSON.stringify(signal));
					    	pc.addIceCandidate(new RTCIceCandidate(signal.candidate));
					    
					    } else if (signal.hasOwnProperty("success")) {
					    	// show the success!?

					    } else if (signal.hasOwnProperty("sessionstarted")) {
					    	console.log("WS: Server has 'joined' us with the callee, session started, begin comms");
					    	// begin JSEP process
					        console.log("Adding local stream to PC");
					        pc.addStream(localStream);
				        	console.log("Creating an offer to the callee");
				            pc.createOffer(gotDescription, null);
					    
					    } else if (signal.type) {
					    	console.log("WS: signal received from callee with type: " + signal.type);
					    
					    } else {
					    	console.log("WS: Unknown response from RTC server...");
					    	console.log(signal);
					    }
					};
					
					// inform the UI
					signalingChannel.onopen = function(evt) {
						console.log("websocket connection open");
						login();
						getUsers();
					}
					
					// inform the UI
					signalingChannel.onerror = function(evt) {
						console.log("websocket connection failed");
						console.log(evt);
						// alert("failed to connect to the websocket" + evt.toString());
					}
					
					// inform the UI
					signalingChannel.onclose = function(evt) {
						console.log("websocket connection closed");
						selfView.src = "";
						signalingChannel = null;
					}
				}
			}
		}
		
		document.getElementById("users").onclick = function() {
			getUsers();
		}
		
		document.getElementById("close").onclick = function() {
			if (signalingChannel) {
				signalingChannel.close();
			}
		}
		
		document.getElementById("clear").onclick = function() {
			document.getElementById("status").innerHTML = "";
		}
		
	}
