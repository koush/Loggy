(function() {
  jade.renderFile = function(file, args, fn) {
    $.get(file, function(data) {
      jade.render(data, args, fn);
    })
  }
  $(document).ready(function() {
	  var templates = $('.template');
	  var pending = templates.length;
	  var views = {};
	  
	  function render() {
		  if (pending != 0)
			  return;
		  for (var view in views) {
			  var rendered = views[view];
			  $('#' + view).html(rendered);
		  }
	  }
	  
	  $.each(templates, function(index, element) {
		  var view = 'views/' + element.id + '.jade';
		  
		  jade.renderFile(view, {}, function(err, rendered) {
			  views[element.id] = rendered;
			  pending--;
			  render();
		  });
	  });
  });
})();

