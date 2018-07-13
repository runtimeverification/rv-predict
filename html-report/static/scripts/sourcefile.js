$(function () {
  /*
  $("div.line-with-errors").click(function() {
    $("div.errors", this).slideToggle(200)
  })
  */
  $('div.line-with-errors div.errors').show()
  
  // jump to `linno` if specified
  // for example: 2-buffer-overflow.c.html?lineno=148 will then jump to line 148
  var match = window.location.search.match(/\?lineno=(\d+)/)
  if (match) {
    var lineno = match[1]
    // var $el = $('.line-with-errors.line-' + lineno)
    var $el = $('.line-' + lineno)
    if ($el.length) {
      $el.addClass('highlight')
      var el = $el[0]
      el.scrollIntoView()
      $('.errors', $el).show()
      var container = $('.container')[0]
      if (container) {
        container.scrollTop = Math.max(0, (container.scrollTop - container.offsetHeight * 0.382))
      }
    }
  }
});