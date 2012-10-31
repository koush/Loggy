if (!WebSocket) {
	$('#no-websocket').show();
	return;
}

// Let the library know where WebSocketMain.swf is:
console.log("attempting connection...");

// Write your code in the same way as for native WebSocket:
var ws = new WebSocket(window.location.href.replace('http', 'ws') + '/stream');
console.log(WebSocket);
ws.onopen = function() {
	console.log('connected');
};

ws.onmessage = function(e) {
	if (e && e.data) {
		var data = JSON.parse(e.data);
		var image = data.image;
		console.log(image);
		$('#image').empty();
		$('#image').append($('<img></img>').attr('src', '/sdcard/' + image));
	}
};

ws.onclose = function() {
  console.log("closed");
};
ws.onerror = function() {
  console.log(arguments);
}
