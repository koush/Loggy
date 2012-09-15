(function() {
	var search = '/sdcard';
	var path = window.location.href.substring(window.location.href.indexOf(search) + search.length);
	if (path.length == '')
		path = '/';
	console.log(path);
	$.get('/json/sdcard'  + path, function(err, data) {
		console.log(arguments);
	})
})();