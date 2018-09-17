$(document).ready(function() {
  var table = $("#myTable").DataTable({
    fixedHeader: {
      headerOffset: $("#table-header").outerHeight(true),
      header: true
    },
    paging: false,
    ordering: true,
    dom: "rtB",
    stateSave: true,
    stateDuration: -1,
    buttons: [
      {
        extend: "csv",
        filename: "errors"
      },
      {
        extend: "excel",
        filename: "errors"
      },
      {
        extend: "pdf",
        filename: "errors"
      }
    ],
    columns: [
      {
        name: "error",
        className: "column-error"
      },
      {
        name: "file",
        className: "column-file"
      },
      {
        name: "id",
        className: "column-error-id"
      },
      {
        name: "frequency",
        className: "column-error-frequency"
      },
      {
        name: "function",
        className: "column-function"
      }
    ],
    initComplete: function(settings, json) {
      var table = this.api();

      // search results highlight
      table.on("draw", function() {
        var body = $(table.table().body());
        body.unhighlight();
        var words = table.search().trim().split(/\s+/g)
        words.forEach(function(word) {
          body.highlight(word);
        })
        formatTableRows();
      });

      // Hack: Hide the column when performing columns reorder.
      function displayColumnThatWasHidden() {
        var $header = $(table.header()[0]);
        var $columns = $("th", $header);
        window.$columns = $columns;
        for (var i = 0; i < $columns.length; i++) {
          if ($columns[i].style.display === "none") {
            $columns[i].style.display = "table-cell";
            var columnIndex = parseInt(
              $columns[i].getAttribute("data-column-index")
            );
            var trs = document.getElementsByTagName("tr");
            for (var j = 0; j < trs.length; j++) {
              var td = trs[j].children[columnIndex];
              if (td && td.style.display === "none")
                td.style.display = "table-cell";
            }
            break;
          }
        }
      }

      var colReorder = new $.fn.dataTable.ColReorder(table, {
        realtime: false
      });
      var oldMouseDown = colReorder._fnMouseDown.bind(colReorder);
      colReorder._fnMouseDown = function(e, nTh) {
        if (e.target && e.target.classList.contains("drag-indicator")) {
          e.target = e.target.parentElement;
          oldMouseDown(e, nTh); // Call old _fnMouseDown function
        }
      };
      var oldMouseMove = colReorder._fnMouseMove.bind(colReorder);
      colReorder._fnMouseMove = function(e) {
        oldMouseMove(e); // Call old _fnMouseMove function

        var draggingTarget = colReorder.s.mouse.target;
        var columnIndex = parseInt(
          draggingTarget.getAttribute("data-column-index")
        );

        if (draggingTarget && colReorder.dom.drag) {
          if (draggingTarget.style.display !== "none") {
            // Hide column `columnIndex`
            var trs = document.getElementsByTagName("tr");
            for (var i = 0; i < trs.length; i++) {
              var td = trs[i].children[columnIndex];
              if (td && td.style.display !== "none") {
                td.style.display = "none";
              }
            }

            // Reinitialize aoTargets, which is used to locate pointer
            colReorder.s.aoTargets = [];
            colReorder._fnRegions();
          }

          draggingTarget.style.display = "none";

          if (colReorder.dom.drag) {
            var $th = $("th", colReorder.dom.drag);
            $th.show();
            $th.addClass("dragging");
            $("img", $th).attr(
              "src",
              "static/images/drag_indicator_yellow.svg"
            );
          }
        }
      };

      var oldMouseUp = colReorder._fnMouseUp.bind(colReorder);
      colReorder._fnMouseUp = function(e) {
        if (colReorder.dom.drag) {
          displayColumnThatWasHidden();
        }
        oldMouseUp(e);
      };

      // Hack: Disable default table header click event.
      function rebindTableHeaderClickEvents() {
        var $header = $(table.header()[0]);
        var $ths = $("th", $header);
        for (var i = 0; i < $ths.length; i++) {
          var $th = $($ths[i]);
          $th.off("click");
          $th.on(
            "click",
            (function($th) {
              return function(event) {
                var direction = $th.attr("data-direction") || "asc";
                if (direction === "asc") {
                  direction = "desc";
                } else {
                  direction = "asc";
                }
                $th.attr("data-direction", direction);
                sortColumns();
              };
            })($th)
          );
        }
      }

      var FIRST_COLUMN_HEADER = null
      var EXPANSION_STATE = {}
      try {
        if (typeof Storage !== "undefined") {
          FIRST_COLUMN_HEADER = JSON.parse(sessionStorage.getItem(location.href + "/FIRST_COLUMN_HEADER") || "null");
          EXPANSION_STATE = JSON.parse(sessionStorage.getItem(location.href + "/EXPANSION_STATE") || "{}")
        }
      } catch(error) {}
      function storeTableState() {
        try {
          if (typeof Storage !== "undefined") {
            sessionStorage.setItem(location.href + "/FIRST_COLUMN_HEADER", JSON.stringify(FIRST_COLUMN_HEADER));
            sessionStorage.setItem(location.href + "/EXPANSION_STATE", JSON.stringify(EXPANSION_STATE));
          }
        } catch(error) {}
      }
      /**
       * This function will collapse repetitive items based on the first column
       */
      function formatTableRows() {
        var $header = $(table.header()[0]);
        var $ths = $("th", $header);
        var firstColumnHeader = $ths[0].innerText;
        var $trs = $("tr", table.body()[0]);
        var exists = {};
        var order = [];
        for (var i = 0; i < $trs.length; i++) {
          var tr = $trs[i];
          tr.classList.remove("minor");
          tr.classList.remove("major");
          tr.classList.remove("minor-display");
          var $tds = $("td", $trs[i]);
          $(".td-button-wrapper", $tds).remove();
          var td = $tds[0];
          var content = td.innerText;
          if ($ths[0].classList.contains("column-file")) {
            // File:Line column
            var lastIndex = content.lastIndexOf(":");
            if (lastIndex > 0) {
              content = content.slice(0, lastIndex);
            }
          }
          if (content in exists) {
            exists[content] += 1;
            $trs[i].classList.add("minor");
          } else {
            exists[content] = 1;
            order.push([content, i]);
          }
        }
        order.forEach(function(o) {
          var content = o[0];
          var offset = o[1];
          var tr = $trs[offset];
          var td = $("td", tr)[0];
          if (exists[content] > 1) {
            tr.classList.add("major");
            var $tdBtn = $(
              '<div class="td-button-wrapper">' +
                '<div class="td-button">' +
                '<div class="count">' +
                '<span data-count="' +
                exists[content] +
                '">' +
                "</span>" +
                "</div>" +
                '<span class="sign"></span>' +
                "</div>" +
                "</div>"
            );
            $tdBtn.click(function() {
              if ($tdBtn.hasClass("expanded")) {
                for (var i = offset + 1; i < $trs.length; i++) {
                  if ($trs[i].classList.contains("major")) {
                    break;
                  } else {
                    $trs[i].classList.remove("minor-display");
                  }
                }
                $tdBtn.removeClass("expanded");
                delete(EXPANSION_STATE[content]);
              } else {
                for (var i = offset + 1; i < $trs.length; i++) {
                  if ($trs[i].classList.contains("major")) {
                    break;
                  } else {
                    $trs[i].classList.add("minor-display");
                  }
                }
                $tdBtn.addClass("expanded");
                EXPANSION_STATE[content] = true;
              }
              table.fixedHeader.adjust();
              storeTableState();
            });
            $(".td-content-wrapper", td).prepend($tdBtn);
          }
        });

        if (FIRST_COLUMN_HEADER === firstColumnHeader) { // Restore expansion state
          for (var content in EXPANSION_STATE) {
            if (EXPANSION_STATE[content] === true) {
              order.forEach(function(o) {
                var content2 = o[0];
                var offset = o[1];
                if (content === content2) {
                  var tr = $trs[offset];
                  var td = $("td", tr)[0];
                  $(".td-button-wrapper", td).click();
                }
              })
            }
          }
        } else {
          EXPANSION_STATE = {}
        }
        FIRST_COLUMN_HEADER = firstColumnHeader;
        table.fixedHeader.adjust(); // Redraw the fixed header
        storeTableState();
      }
      /**
       * Sort columns from left to right by attribute `data-direction`.
       */
      function sortColumns() {
        var $header = $(table.header()[0]);
        var $ths = $("th", $header);
        var orders = [];
        for (var i = 0; i < $ths.length; i++) {
          var direction = $ths[i].getAttribute("data-direction");
          orders.push([i, direction]);
        }
        $(".td-button-wrapper").remove();
        table.order(orders).draw();

        try {
          if (typeof Storage !== "undefined") {
            sessionStorage.myTable_columnOrders = JSON.stringify(orders);
          }
        } catch (error) {}
      }
      /**
       * Load and set `data-direction` attributes to table headers
       */
      function initColumnsSorting() {
        var $header = $(table.header()[0]);
        var $ths = $("th", $header);
        var orders = [];
        try {
          if (typeof Storage !== "undefined") {
            orders = JSON.parse(sessionStorage.myTable_columnOrders);
          }
        } catch (error) {}

        for (var i = 0; i < $ths.length; i++) {
          if (i < orders.length) {
            $ths[i].setAttribute("data-direction", orders[i][1]); // [offset, direction]
          } else {
            $ths[i].setAttribute("data-direction", "asc");
          }
        }
        sortColumns();
      }
      /**
       * Format `File` column to fix sorting the line numbers in lexical order
       */
      function formatFileColumn() {
        var $header = $(table.header()[0]);
        var $ths = $("th", $header);
        var offset = 0;
        for (; offset < $ths.length; offset++) {
          if ($ths[offset].classList.contains("column-file")) {
            // File:Line column
            break;
          }
        }
        var data = {}; // key is filename, value is max lineNumber
        var $trs = $("tr", table.body()[0]);
        for (var i = 0; i < $trs.length; i++) {
          var tr = $trs[i];
          var td = tr.children[offset];
          if (!td) {
            continue;
          }
          var text = td.innerText;
          var index = text.lastIndexOf(":");
          var filename = text.slice(0, index);
          var line = parseInt(text.slice(index + 1)) || 0;
          if (!(filename in data) || line > data[filename]) {
            data[filename] = line;
          }
        }
        for (var i = 0; i < $trs.length; i++) {
          var tr = $trs[i];
          var td = tr.children[offset];
          if (!td) {
            continue;
          }
          var text = td.innerText;
          var index = text.lastIndexOf(":");
          var filename = text.slice(0, index);
          var line = parseInt(text.slice(index + 1)) || 0;
          var difference =
            data[filename].toString().length - line.toString().length;
          var newText = filename + ":";
          for (var j = 0; j < difference; j++) {
            newText += String.fromCharCode(0);
          }
          newText += line;
          var a = td.querySelector("a");
          if (a) {
            a.innerText = newText;
            table.row(tr).data()[offset] = a.outerHTML;
          }
        }
      }

      function initDragIndicatorTooltip() {
        var $header = $(table.header()[0]);
        var $ths = $("th", $header);
        for (var i = 0; i < $ths.length; i++) {
          $(".table-header", $ths[i]).append(
            '<span class="tooltip"></span>'
          );
        }
        $(".drag-indicator").on("mousedown", function(event) { // Fix Firefox image dragging bug.
          event.preventDefault();
        })
      }

      function checkEmptyTable() {
        var $emptyTd = $(".dataTables_empty");
        if ($emptyTd.length && !table.data().count()) {
          $emptyTd.addClass("no-errors-celebration");
          $emptyTd.html(
            "<span>Let's celebrate! There are no errors in your report.</span>"
          );
        }
      }

      table.on("column-reorder", function(e, settings, details) {
        rebindTableHeaderClickEvents();
        sortColumns();
      });

      formatFileColumn();
      initDragIndicatorTooltip();
      initColumnsSorting();
      rebindTableHeaderClickEvents();
      checkEmptyTable();

      $(window).bind("beforeunload", function() {
        try {
          if (typeof Storage !== "undefined") {
            sessionStorage.myTable_searchValue = $("#table-search").val();
            sessionStorage.myTable_scrollTop = $(window).scrollTop();
          }
        } catch (error) {}
      });

      $(window).bind("resize", function() {
        table.fixedHeader.adjust();
      })

      try {
        if (typeof Storage !== "undefined") {
          if (sessionStorage.myTable_searchValue) {
            $("#table-search").val(sessionStorage.myTable_searchValue || "");
          }
          if (sessionStorage.myTable_scrollTop) {
            $(window).scrollTop(sessionStorage.myTable_scrollTop);
          }
        }
      } catch (error) {}

      $("#table-search").on("input", function(event) {
        var text = this.value.trim();
        var regex;
        var words = text.split(/\s/);
        var terms = {};
        var cols = table.settings().init().columns;
        for (var i = 0; i < cols.length; i++) {
          terms[cols[i].name] = "";
        }
        var global = "";
        for (var i = 0; i < words.length; i++) {
          var word = words[i];
          var idx = word.indexOf(":");
          if (idx !== -1) {
            var label = word.substring(0, idx);
            var term = word.substring(idx + 1);
            if (label in terms) {
              terms[label] = terms[label] + " " + term;
            } else {
              global = global + " " + word;
            }
          } else {
            global = global + " " + word;
          }
        }
        for (var col in terms) {
          table.column(col + ":name").search(terms[col]);
        }
        table.search(global).draw();
      });
    }
  });
});
