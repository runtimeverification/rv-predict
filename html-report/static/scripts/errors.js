var column_names = ["Id", "Error", "Function", "File", "Line"];
var sortAsc = {Id: false, Error: false, Function: false, File: false, Line: false};
var sortKeys = {Id: "index", Error: "errdesc", Function: "function", File: "file", Line: "line"};

function magicSort(a,b) {
  return (a > b) - (b > a);
}

function sortBy(rows, column, sortFn=magicSort) {
  sortAsc[column] = ! sortAsc[column];
  if (sortAsc[column]) {
    rows.sort(sortFn);
  } else {
    rows.sort(function(a,b) {return -sortFn(a,b)});
  }
}

function fillData(table, data) {
  // data bind with new data
  rows = table
    .select("tbody").selectAll("tr")
    .data(data, function(d){ return d.index; });

  // enter the rows
  rows
   .enter()
   .append("tr");

  // enter td's in each row
  row_entries = rows
      .selectAll("td")
      .data(function(d) {
        return [{column: "Id", index: d.index},
                {column: "Error", error: d.errdesc, index: d.index},
                {column: "Function", fn: d["function"]},
                {column: "File", file: d.file},
                {column: "Line", line: d.line}];
       })
      .enter()
      .append("td");

  var entries={};
  for (column in sortKeys) {
    entries[column] = row_entries.filter(function (d) { return d.column == column; });
  }
  entries["Id"]
    .append("a")
    .attr("href", function (d) { return "../error-" + d.index + ".html"; })
    .text(function(d) { return d.index; });

  entries["Error"]
    .append("a")
    .attr("href", function (d) { return "../error-" + d.index + ".html"; })
    .text(function(d) { return d.error; });

  entries["Function"].text(function (d) { return d.fn; });

  entries["File"]
    .append("a")
    .attr("href", function (d) { return "../" + d.file + ".html"; })
    .text(function (d) { return d.file; });

  entries["Line"].text(function (d) { return d.line; });

  // exit
  rows.exit().remove();
$(window).resize(function() {

  // Change the selector if needed
  var $table = $('table'),
      $bodyCells = $table.find('tbody tr:first').children(),
      colWidth;

  // Get the tbody columns width array
  colWidth = $bodyCells.map(function() {
      return $(this).width();
  }).get();

  // Set the width of thead columns
  $table.find('thead tr').children().each(function(i, v) {
      $(v).width(colWidth[i]);
  });
});
}

// draw the table
d3.select("body").append("div")
  .attr("id", "container")

d3.select("#container").append("div")
  .attr("id", "FilterableTable");

d3.select("#FilterableTable").append("h1")
  .attr("id", "title")
  .text("RV-Match report")

d3.select("#FilterableTable").append("div")
  .attr("id", "SearchBar")
  .append("p")
    .attr("class", "SearchBar")
    .text("Search:");

d3.select(".SearchBar")
  .append("input")
    .attr("class", "SearchBar")
    .attr("id", "search")
    .attr("type", "text")
    .attr("placeholder", "Search...");

var table = d3.select("#FilterableTable").append("table");
table.append("thead").append("tr");

var headers = table.select("tr").selectAll("th")
    .data(column_names)
  .enter()
    .append("th")
    .text(function(d) { return d; });

var rows, row_entries, row_entries_no_anchor, row_entries_with_anchor;
data = JSON.parse(json_data);

// draw table body with rows
table.append("tbody")

fillData(table, data);

/**  search functionality **/
  d3.select("#search")
    .on("keyup", function() { // filter according to key pressed
      var searched_data = data;
      var text = this.value.trim();
      var regex;
      if ( text.charAt(0) == ':' ) {
        fields = text.split(':');
        for (var i = 0; i < fields.length; i++) {
          for (var column in sortKeys) {
            if (fields[i].startsWith(column+"=")) {
              regex = new RegExp(fields[i].trim().substr(column.length+1), "i");
              searched_data = searched_data.filter(function(r) { return regex.test(r[sortKeys[column]]); });
            }
          }
        }
      } else {
        regex = new RegExp(text, "i");
        searched_data = searched_data.filter(function(r) { return regex.test(r.errdesc); });
      }

      fillData(table, searched_data);
    })

/**  sort functionality **/
headers
  .on("click", function(d) { sortBy(rows, d, function(a,b){ return magicSort(a[sortKeys[d]],b[sortKeys[d]])}); }) // end of click listeners
//});
d3.select(self.frameElement).style("height", "780px").style("width", "1150px");

// Change the selector if needed
var $table = $('table.scroll'),
    $bodyCells = $table.find('tbody tr:first').children(),
    colWidth;

// Adjust the width of thead cells when window resizes
$(window).resize(function() {
    // Get the tbody columns width array
    colWidth = $bodyCells.map(function() {
        return $(this).width();
    }).get();

    // Set the width of thead columns
    $table.find('thead tr').children().each(function(i, v) {
        $(v).width(colWidth[i]);
    });
}).resize(); // Trigger resize handler
