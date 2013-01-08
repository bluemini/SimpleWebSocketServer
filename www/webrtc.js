	var signalingChannel;
	var pc;
	var configuration = {"iceServers": []};
	var selfView;
	var remoteView;
	//var WSServer = "127.0.0.1";
	var WSServer = "10.168.0.59";
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
	    	// console.log(evt);
	        signalingChannel.send(JSON.stringify({ "candidate": evt.candidate }));
	    };
	    
	    pc.onopen = function(evt) {
	    	// do nothing for now
	    }

	    // once remote stream arrives, show it in the remote video element
	    pc.onaddstream = function (evt) {
	    	console.log("Adding stream - setting remote view");
	    	remoteView = document.getElementById("remote");
	        remoteView.src = URL.createObjectURL(evt.stream);
	        remoteView.autoplay = true;
	    };
	    
	    pc.onremovestream = function(evt) {
	    	// do nothing for now
	    }

	    // get the local stream, show it in the local video element and send it
	    navigator.getUserMedia({ "audio": true, "video": true }, function (stream) {
	    	selfView.autoplay = true;
	        selfView.src = URL.createObjectURL(stream);
	        pc.addStream(stream);
	        console.log("Adding stream to PC");

	        if (isCaller) {
	        	console.log("Creating an offer to the callee");
	            pc.createOffer(gotDescription, null);
	        } else {
	    		console.log("Creating answer");
	    		pc.createAnswer(gotDescription, null);
	        }

	    });
	}
	
	function gotDescription(desc) {
        pc.setLocalDescription(desc);
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
		setStatus("fetching users");
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
		document.getElementById("login").onclick = function() {
			if (document.getElementById("name").value && document.getElementById("nick")) {
				
				if (!signalingChannel) {
					signalingChannel = new WebSocket('ws://'+WSServer+':88/');
					setStatus("connecting...");
				}

				if (signalingChannel) {
					signalingChannel.onmessage = function (evt) {
	
					    var signal = JSON.parse(evt.data);
					    // console.log(signal);
					    if (signal.sdp) {
							if (!pc)
						        start(false);
							console.log("setting remote description");
					    	pc.setRemoteDescription(new RTCSessionDescription(signal.sdp));

					    } else if (signal.users) {
					    	setStatus("got some users..." + evt.data);
					    	renderUsers(signal.users);

					    } else if (signal.candidate) {
					    	console.log("Receiving a candidate");
							if (!pc) {
						        start(false);
							}
					    	pc.addIceCandidate(new RTCIceCandidate(signal.candidate));
					    
					    } else if (signal.hasOwnProperty("success")) {
					    	// show the success!?

					    } else if (signal.hasOwnProperty("sessionstarted")) {
					    	console.log("Server has 'joined' us with the callee, session started, begin comms");
					    	setStatus("Session started");
					    	start(true);
					    
					    } else if (signal.type) {
					    	console.log("signal received from callee with type: " + signal.type);
					    
					    } else {
					    	setStatus("Bad response from RTC server");
					    	console.log("Unknown response from RTC server...follows")
					    	console.log(signal);
					    }
					};
					
					// inform the UI
					signalingChannel.onopen = function(evt) {
						setStatus("websocket connection open");
						login();
						getUsers();
					}
					
					// inform the UI
					signalingChannel.onerror = function(evt) {
						setStatus("websocket connection failed");
						console.log(evt);
						// alert("failed to connect to the webscoket" + evt.toString());
					}
					
					// inform the UI
					signalingChannel.onclose = function(evt) {
						setStatus("websocket connection closed");
						selfView.src = "";
						signalingChannel = null;
					}
				}
			}
		}
		
		document.getElementById("users").onclick = function() {
			getUsers();
		}
		
		document.getElementById("test").onclick = function() {
			getVideo();
		}
		
		document.getElementById("clear").onclick = function() {
			document.getElementById("status").innerHTML = "";
		}
		
	}
