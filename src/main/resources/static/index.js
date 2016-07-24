$(function(){
	var stompClient = null;
	var $connect = $('#connect');
	var $disconnect = $('#disconnect');
	var $messages = $('#messages');

	function setConnected(connected) {
		$connect.prop('disabled', connected);
		$disconnect.prop('disabled', !connected);
	    $messages.html('');
	}

	function connect() {
	    var socket = new SockJS('/messages');
	    stompClient = Stomp.over(socket);
	    stompClient.connect({}, function(frame) {
	        setConnected(true);
	        console.log('Connected: ' + frame);
	        
	        stompClient.subscribe('/topic/builds', showMessage);
	        stompClient.subscribe('/topic/logs', showMessage);
	        stompClient.subscribe('/topic/portal/state', showMessage);
	        stompClient.subscribe('/topic/portal/execute', showMessage);
	    });
	}

	function disconnect() {
	    if (stompClient != null) {
	        stompClient.disconnect();
	    }
	    
	    setConnected(false);
	    console.log("Disconnected");
	}

	function showMessage(message) {
		$messages.append('<pre>' + message.body + '</pre>')
	}
	
	$connect.on('click', connect);
	$disconnect.on('click', disconnect);
	
	connect();
});
