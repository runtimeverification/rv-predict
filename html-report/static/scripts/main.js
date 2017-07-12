function prepareList() {
	$('#expList').find('li:has(ul)').click(function(event) {
		if(this == event.target) {
			$(this).toggleClass('expanded');
			$(this).children('ul').toggle('medium');
		}
		return true;
	}).not(".expanded").addClass('collapsed').removeClass('expanded').children('ul').hide();
};

$(document).ready(function() {
  prepareList();
});
