function prepareList() {
	$('#expList').find('li:has(ul)').unbind('click').click(function(event) {
		if(this == event.target) {
			$(this).toggleClass('expanded');
			$(this).children('ul').toggle('medium');
		}
		return false;
	}).not(".expanded").addClass('collapsed').removeClass('expanded').children('ul').hide();
};

$(document).ready(function() {
  prepareList();
});
