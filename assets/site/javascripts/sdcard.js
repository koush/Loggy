loadcontent = function() {
	$('#dir-rows').empty();
	$('#file-rows').empty();
	var search = '/sdcard';
	var path = window.location.href.substring(window.location.href.indexOf(search) + search.length);
	if (path.length == '')
		path = '/';
	console.log(path);
	$.get('/json/sdcard'  + path, function(data) {
		if (!data) {
			return;
		}

		for (var dir in data.dirs) {
			dir = data.dirs[dir];
			var format =
				".row.dir\n" +
				"  .span6\n" +
				"    span\n" +
				"      img.folder(src='/images/foldersmall.png')\n" +
				"    span.dirname\n" +
				"      a(href=window.location.href + '/' + dir.name)=dir.name\n";
			jade.render(format, {
				dir: dir
			}, function(err, data) {
				if (err) {
					console.log(err);
					return;
				}
				
				$('#dir-rows').append(data);
			})
		}
		
		for (var file in data.files) {
			file = data.files[file];
			var format =
				".row.file\n" +
				"  .span6\n" +
				"    span\n" +
				"      img.file(src='/images/file.png')\n" +
				"    span.filename\n" +
				"      a(href=window.location.href + '/' + file.name)=file.name\n" +
				"  .span1.filesize=file.size\n";
			jade.render(format, {
				file: file
			}, function(err, data) {
				if (err) {
					console.log(err);
					return;
				}
				
				$('#file-rows').append(data);
			})
		}
	})
}

loadcontent();



$("#sdcard a").live("click", function (event) {
	window.history.pushState({},"", $(this).attr("href"));
	loadcontent();
    event.preventDefault();
});

window.onpopstate = function(e){
    if(e.state){
    }
    loadcontent();
    
};
