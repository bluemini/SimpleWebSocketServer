	var signalingChannel = new WebSocket('ws://localhost:88/');
	var pc;
	var configuration = {"iceServers": []};
	//console.log(webkitRTCPeerConnection);
	//RTCPeerConnection = RTCPeerConnection || webkitRTCPeerConnection || mozRTCPeerConnection;
	navigator.getUserMedia = navigator.getUserMedia || navigator.webkitGetUserMedia || navigator.mozGetUserMedia;

	// run start(true) to initiate a call
	function start(isCaller) {
	    pc = new RTCPeerConnection(configuration);

	    // send any ice candidates to the other peer
	    pc.onicecandidate = function (evt) {
	        signalingChannel.send(JSON.stringify({ "candidate": evt.candidate }));
	    };

	    // once remote stream arrives, show it in the remote video element
	    pc.onaddstream = function (evt) {
	        remoteView.src = URL.createObjectURL(evt.stream);
	    };

	    // get the local stream, show it in the local video element and send it
	    navigator.getUserMedia({ "audio": true, "video": true }, function (stream) {
	    	var selfView = document.getElementById("video");
	    	selfView.autoplay = true;
	        selfView.src = URL.createObjectURL(stream);
	        pc.addStream(stream);

	        if (isCaller)
	            pc.createOffer(gotDescription);
	        else
	            pc.createAnswer(pc.remoteDescription, gotDescription);

	        function gotDescription(desc) {
	            pc.setLocalDescription(desc);
	            signalingChannel.send(JSON.stringify({ "sdp": desc }));
	        }
	    });
	}

	signalingChannel.onmessage = function (evt) {
	    if (!pc)
	        start(false);

	    var signal = JSON.parse(evt.data);
	    if (signal.sdp)
	        pc.setRemoteDescription(new RTCSessionDescription(signal.sdp));
	    else
	        pc.addIceCandidate(new RTCIceCandidate(signal.candidate));
	};
	
	start(true);
