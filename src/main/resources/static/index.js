$(function(){
	var stompClient = null;
	
	var $connect = $('#connect');
	var $disconnect = $('#disconnect');
	
	var $state = $('#state');
	var $execute = $('#execute');
	var $log = $('#log');
	var $build = $('#build');

	function setConnected(connected) {
		$connect.prop('disabled', connected);
		$disconnect.prop('disabled', !connected);
	    $(".tab-pane").empty();
	}

	function connect() {
	    var socket = new SockJS('/messages');
	    stompClient = Stomp.over(socket);
	    stompClient.connect({}, function(frame) {
	        setConnected(true);
	        console.log('Connected: ' + frame);
	        
	        stompClient.subscribe('/topic/portal/state', onState);
	        stompClient.subscribe('/topic/portal/execute', onExecute);
	        stompClient.subscribe('/topic/logs', onLog);
	        stompClient.subscribe('/topic/builds', onBuild);
	    });
	}

	function disconnect() {
	    if (stompClient != null) {
	        stompClient.disconnect();
	    }
	    
	    setConnected(false);
	    console.log("Disconnected");
	}

	function onState(message) {
		$state.append('<pre>' + message.body + '</pre>')
	}
	function onExecute(message) {
		$execute.append('<pre>' + message.body + '</pre>')
	}
	function onLog(message) {
		$log.append('<pre>' + message.body + '</pre>')
	}
	function onBuild(message) {
		$build.append('<pre>' + message.body + '</pre>')
	}
	
	//$('#tabs').tab();
	
	$connect.on('click', connect);
	$disconnect.on('click', disconnect);
	
	connect();
});
