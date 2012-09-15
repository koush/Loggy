(function() {
  jade.renderFile = function(file, args, fn) {
    $.get(file, function(data) {
      jade.render(data, args, fn);
    })
  }
})();

jade.renderFile(view, {}, function(err, index) {
  $('body').html(index);
})
