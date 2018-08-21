function showNLines(n, startLine, context) {
  for (var i = startLine; i < startLine + n; i++) {
    showline(i, context)
  }
}

function showline(line, context) {
  $(".line-"+line, context).show()
}

/**
 * Hide n lines. If there is `hll` class line, then return false and don't hide; 
 * otherwise, return false and hide lines.  
 */ 
function hideNLines(n, startLine, context) {
  var canHide = true 
  for (var i = startLine; i < startLine + n; i++) {
    var $line = $(".line-"+i, context)
    if ($line.hasClass('hll') || $line.hasClass('hll3')) {
      canHide = false
      break
    }
  }

  if (canHide) {
    for (var i = startLine; i < startLine + n; i++) {
      hideLine(i, context)
    }
  }
  return canHide
}

function hideLine(line, context) {
  $(".line-"+line, context).hide()
}

function displayNLines(startLine, endLine, context) {
  for (var i = startLine; i < endLine; i++) {
    showline(i, context)
  }
}

function expandUp(fileId, line, btn) {
  var $div = $("[data-fileid=" + fileId + "][data-line=" + line + "]")
  var oldStart = parseInt($div.attr("data-context-start"))
  if (oldStart < 0) return
  var dStart = parseInt($div.attr("data-context-dstart"))
  if (isNaN(dStart)) { dStart = 10 }
  var start = oldStart - dStart
  showNLines(oldStart-start, Math.max(0, start+1), $div)
  $div.attr("data-context-start", start)
  $div.attr("data-context-dstart", 2*dStart)

  btn.nextElementSibling.classList.remove('disabled')

  // check whehter to disable the button
  if (start < 0) {
    btn.classList.add('disabled')
  } else {
    btn.classList.remove('disabled')
  }
}

function collapseUp(fileId, line, btn) {
  var $div = $("[data-fileid=" + fileId + "][data-line=" + line + "]")
  var oldStart = parseInt($div.attr("data-context-start"))
  var size = parseInt($div.attr("size"))
  var dStart = parseInt($div.attr("data-context-dstart"))
  if (isNaN(dStart)) { dStart = 10}
  else {dStart = dStart / 2}
  var start = oldStart + dStart
  if (start < 0) {
    start = 0;
  }
  if (start >= size) return
  if (hideNLines(dStart, oldStart + 1, $div)) {
    $div.attr("data-context-start", start)
    $div.attr("data-context-dstart", dStart)

    btn.previousElementSibling.classList.remove('disabled')

    if (parseInt($div.attr('data-line')) - parseInt($div.attr('data-context-start')) <= 8) {
      btn.classList.add('disabled')
    }
  }
}

function expandDown(fileId, line, btn) {
  var $div = $("[data-fileid=" + fileId + "][data-line=" + line + "]")
  var oldEnd = parseInt($div.attr("data-context-end"))
  var size = parseInt($div.attr("size"))
  var dEnd = parseInt($div.attr("data-context-dend"))
  if (oldEnd > size) return
  if (isNaN(dEnd)) { dEnd = 10 }
  var end = oldEnd + dEnd
  showNLines(Math.min(end, size + 1)-oldEnd, oldEnd, $div)
  $div.attr("data-context-end", end)
  $div.attr("data-context-dend", 2*dEnd)

  btn.nextElementSibling.classList.remove('disabled')

  // check whehter to disable the button
  if (end > size) {
    btn.classList.add('disabled')
  } else {
    btn.classList.remove('disabled')
  }
}

function collapseDown(fileId, line, btn) {
  var $div = $("[data-fileid=" + fileId + "][data-line=" + line + "]")
  var oldEnd = parseInt($div.attr("data-context-end"))
  var size = parseInt($div.attr("size"))
  var dEnd = parseInt($div.attr("data-context-dend"))
  if (isNaN(dEnd)) { dEnd = 10 }
  else {dEnd = dEnd / 2}
  var end = oldEnd - dEnd
  if (end > size + 1) {
    end = size + 1;
  }
  if (hideNLines(dEnd, end, $div)) {
    $div.attr("data-context-end", end)
    $div.attr("data-context-dend", dEnd)

    btn.previousElementSibling.classList.remove('disabled')

    if (parseInt($div.attr('data-context-end')) - parseInt($div.attr('data-line')) <= 8) {
      btn.classList.add('disabled')
    }
  }
}

/**
 * Create string that consists of `n` spaces.  
 */ 
function createNSpace(n) {
  n = n || 0
  var o = ''
  for (var i = 0; i < n; i++) {
    o += ' '
  }
  return o
}

/**
 * Get HTML string for local variable value.  
 */ 
function getValueString(value, type, space, oneline) {
  space = space || 0

  if (value instanceof Array) {
    if (value[0] === 'Struct' || value[0] === 'Union') {
      var o = '<span class="bracket">{</span> '
      var fields = value[1].fields,
          length = fields.length
      fields.forEach(function(field, i) {
        var val = fields[i].value
        var valString = getValueString(val.value, val.vtype, oneline ? space : (1 + val.id.length + ' = { '.length), oneline)
        if (val.id !== '<anonymous>') {
          o +=  createNSpace(i == 0 ? 0: space+2) + '<span class="var-id">.' + val.id + '</span> <span class="o">=</span> ' + valString
        } else { // anonymous struct inside a struct, remove curly brackets
          o +=  createNSpace(i == 0 ? 0: space+2) + valString.trim().replace(/^<span[^>]+>\{<\/span>/, '').replace(/<span[^>]+>\}<\/span>$/, '').trim()
        }
        if (i < length - 1) {
          o += ','
          if (!oneline) o += '\n'
        }
      })

      return o + ' <span class="bracket">}</span>'
    } else if (value[0] === 'Array') {
      var o = '{',
          arr = value[1]

      var data = [] // {valueString:number, count:number}
      for (var i = 0, length = arr.length; i < length; i++) {
        var valueString = getValueString(arr[i], type[1].vtype, space, oneline)
        if (!data.length) {
          data.push({valueString: valueString, count: 1})
        } else if (data[data.length - 1].valueString === valueString) {
          data[data.length - 1].count++
        } else {
          data.push({valueString: valueString, count: 1})
        }
      }
      for (var i = 0, length = data.length; i < length; i++) {
        var d = data[i],
            valueString = d.valueString,
            count = d.count
        o += valueString
        if (count >= 4) {
          o += '<span class="repeats"> (repeats ' + count + ' times)</span>'
        } else if (count > 1) {
          d.count--
          i--
        }
        if (i < length - 1) o += ', '
      }
      return o + '}'
    } else if (value[0] === 'Bytes') {
      var o = '',
          arr = value[1]
      for (var i = 0, length = arr.length; i < length; i++) {
        o += getValueString(arr[i].value, type, space, oneline)
        if (i < length - 1) o +=', '
      }
      return o + ''
    } else if (value[0] === 'Bitfield') {
      return getValueString(value[1].value[0].value, type[1].vtype, space, oneline)
    } else if (type === 'Char') {
      var charCode = parseInt(value[1]);
      if (!charCode) return "<span class=\"sc\">'\\0'</span> <span class=\"mi\">0</span>"
      return  '<span class=\"sc\">' + JSON.stringify(String.fromCharCode(charCode)).replace(/^"\\u00/, '"\\x').replace(/^\"(.+)\"$/, "'$1'</span>") + 
              ' <span class=\"mi\">' + charCode.toString() + '</span>'
    } else if (value[0] === 'Int') {
      return '<span class="mi">' + value[1].toString() + '</span>'
    } else if (value[0] === 'Float') {
      return '<span class="mi">' + parseFloat(value[1]) + '</span>'
    } else if (value[0] === 'Pointer') {
      var base = value[1].base[1].toString()
      var offset = value[1].offset || 0
      var baseString = base.startsWith('&') ? base : ('<span class="mi">' + base + '</span>')
      var offsetString = offset ? (' + offset(<span class="mi">' + offset + '</span>)') : ''
      var pointerValueString = 'base(' + baseString + ')' + offsetString 
      return '<span>' + pointerValueString + '</span>'
    } else {
      return value[1].toString()
    }
  } else if (value === 'Indeterminate') {
    return '__indeterminate'
  } else if (value === 'Unspecified') {
    return '__unspecified'
  } else if (value === 'NullPointer') {
    return 'NULL'
  } else {
    return value.toString()
  }
}

/**
 * Combine variable name and variable type to HTML string. 
 * eg
 * ('x', 'int [3]') => int x[3]
 */
function combineVariableNameAndType(id, type, typeString, value, space) {
  space = space || 0

  var index = typeString.indexOf('[')
  var o = '',
      sliceString = ''
  if (index > 0) {
    sliceString = typeString.slice(index, typeString.length).trim()
    typeString =  typeString.slice(0, index).trim()
  }

  if ((type[0] === 'Struct' || type[0] === 'Union') || 
      (type[0] === 'Array' && (type[1].vtype[0] === 'Struct' || type[1].vtype[0] === 'Union'))) { // Struct or Union
    typeString = escapeString(typeString).replace(/^(struct|union)\s+(.+)$/, '<span class="var-type"><span class="k">$1</span> <span class="var-struct">$2</span></span>')
    var o = typeString + createNSpace(space) + '<span class="nested"> {\n'
    
    var fields 
    if (type[0] === 'Struct' || type[0] === 'Union') {
      fields = value[1].fields 
    } else { // array of struct or union
      fields = value[1][0][1].fields
    }

    fields.forEach(function(field) {
      var id = field.value.id,
          typeString = field.value.vtype_str,
          type = field.value.vtype,
          value = field.value.value  
      o += createNSpace(space + 2) + combineVariableNameAndType(id, type, typeString, value, space + 2) + ';\n'
    })
    o += createNSpace(space) + '}</span><span class="spaces"> </span><span class="var-id">' + id + '</span>'
  } else {
    o = '<span class="kt var-type">' + escapeString(typeString).replace(/^(struct|union)\s+(.+)$/, '<span class="k">$1</span> <span class="var-struct">$2</span>')
                                                               .replace(/^(enum)\s+(.+)$/, '<span class="k">$1</span> <span class="var-enum">$2</span>')
                                                               .replace(/\*/g, '<span class="o">*</span>') + '</span>' + 
          '<span class="spaces">' + (typeString[typeString.length - 1] === '*' ? '':' ') + '</span>' + 
          '<span class="var-id">' + id + '</span>'
    if (type[0] === 'Bitfield') {
      o += '<span class="bitfield">:<span class="mi">' + type[1].size + '</span></span>'
    }
  }

  o += sliceString.replace(/\[(\d+)\]/g, '<span class="slice">[<span class="mi">$1</span>]</span>')
  return o 

}

function escapeString(str) {
  return str.replace(/\</g, '&lt;').replace(/\>/g, '&gt;')
}

/**
 * Generate memory table for displaying information of local variables
 */
function generateMemoryTable(localVariables, isStruct) {
  var $ul = $('<div class="variables">' +
  '</div>')
  if (isStruct) {
    $ul.addClass('struct')
  }

  function resizeDetail() {
     // resize .detail > .pre element 
     $details = $ul.closest('.details')
     $memory = $('.memory', $details)
     $('.pre', $details).css('min-height', $memory.height())
  }
  
  var li_arr = []
  localVariables.forEach(function(localVariable) {
    var id = localVariable.id,
        type = localVariable.vtype,
        typeString = localVariable.vtype_str,
        value = localVariable.value
          
    var varnameAndTypeString = combineVariableNameAndType(id, type, typeString, value)
    var valueString = getValueString(value, type, 0, true)   
    var $li = $('<pre class="variable">' + 
      '<span class="id">' + varnameAndTypeString + '</span>' + 
      ' <span class="o equal-sign" style="font-size:12px;">=</span> ' + 
      '<span class="value">' + valueString + '</span>' + 
    + '</pre>')
    
    // NOTE: need to be sorted
    li_arr.push($li)


    // check string data
    if (type[0] === 'Array' && type[1].vtype === 'Char') {
      var stringData = ''
      for (i = 0, length = value[1].length; i < length; i++) {
        var charCode = value[1][i][1]
        if (!charCode || isNaN(charCode)) break // 0 or not a number like `Indeterminate`
        stringData += JSON.stringify(String.fromCharCode(charCode)).replace(/^"\\u00/, '"\\x').replace(/^\"(.+)\"$/, "$1")
      }
      if (!stringData.length) {
        stringData = '<span class="sc"></span>'
      } else {
        stringData = ' <span class="sc">"' + stringData + '"</span> '
      }
      $li.children('.equal-sign').append(stringData)
    }

    // struct type tooltip 
    if ((type[0] === 'Union' || type[0] === 'Struct') ||
        (type[0] === 'Array' && (type[1].vtype[0] === 'Union' || type[1].vtype[0] === 'Struct'))) {
      var $tooltip = $('<span class="type-tooltip">' + varnameAndTypeString + '</span>')
      $('span.nested', $tooltip).removeClass('nested')
      $tooltip.children('.var-id').remove()
      $tooltip.children('.slice').remove()
      $li.children('span.id').append($tooltip)

      $li.children('span.id').children('.var-type').children('.var-struct').click(function(){
        $tooltip.toggleClass('display')
        resizeDetail()
      })    
    }

    // expansion
    // TODO: support pointer type in the future.  
    if (type instanceof Array && (type[0] === 'Union' || type[0] === 'Struct' /*|| type[0] === 'Pointer'*/ || type[0] === 'Array')) {
      $li.addClass('can-expand')
      var $expandBtn = $('<div class="memory-li-expand-btn btn"><div>+</div></div>')
      $li.append($expandBtn)

      var $expandedUl = null,
          $viewMoreBtn = null,
          arrayDisplayLength = 50,
          arrayLength = value[1].length

      $expandBtn.click(function(event) {
        event.stopPropagation()
        event.preventDefault()

        $expandBtn.toggleClass('expanded')
        if ($expandBtn.hasClass('expanded')) {
          $expandBtn.addClass('times') // times sign

          function expandStruct() {
            if ($expandedUl) {
              return $expandedUl.show()
            }

            var expandedVariables = value[1].fields.map(function(field){
              return field.value
            })
            $expandedUl = generateMemoryTable(expandedVariables, true)
            $expandedUl.addClass('expanded-ul')
            $li.append($expandedUl)
          }

          function expandArray() {
            // only show the first 50
            var i = 0
            var expandedVariables = []
            arrayDisplayLength = Math.min(arrayDisplayLength, arrayLength)

            for (i = 0; i < arrayDisplayLength; i++) {
              expandedVariables.push({
                id: '[' + i + ']' + createNSpace((arrayDisplayLength - 1).toString().length - i.toString().length),
                value: value[1][i],
                vtype: type[1].vtype,
                vtype_str: ''
              })
            }

            if ($expandedUl) $expandedUl.remove()
            $expandedUl = generateMemoryTable(expandedVariables, true)
            $expandedUl.addClass('expanded-ul')
            $li.append($expandedUl)

            if ($viewMoreBtn) $viewMoreBtn.remove()

            if (arrayDisplayLength < arrayLength) {
              $viewMoreBtn = $('<div class="btn" style="width: 256px; max-width: 100%; margin-bottom: 12px;">View more</div>')
              $expandedUl.append($viewMoreBtn)
              $viewMoreBtn.click(function() {
                arrayDisplayLength += 50
                expandArray()
                resizeDetail()
              })
            } else {
              // No need to view more
            }
          }

          // hide value 
          $li.children('.value').hide()
          
          // TODO: check pointer
          var expandedVariables = []
          if (type[0] === 'Union' || type[0] === 'Struct') {
            expandStruct()
            $li.children('.equal-sign').addClass('append-curly-bracket') // show left curly bracket
          } else if (type[0] === 'Array') {
            expandArray()
            $li.children('.equal-sign').addClass('append-curly-bracket') // show left curly bracket
          }
        } else {
          $expandBtn.removeClass('times') // plus sign
          if ($expandedUl) $expandedUl.hide();

          // show value 
          $li.children('.value').show()
          // hide curly bracket
          $li.children('.equal-sign').removeClass('append-curly-bracket')
        }

        resizeDetail()
      })
    }
  })


  // sort the records by the number of characters on the left of the variable name:  
  // eg:
  //   'int q'          => 4
  //   'char *a2'       => 6
  //   'int int_arr[3]' => 4
  li_arr.forEach(function($li) {
    var $id = $li.children('.id')
    // var $varId = $id.children('.var-id')
    var $varType = $id.children('.var-type')
    var $spaces = $id.children('.spaces')
    var charactersOnTheLeft = $varType.text().length + $spaces.text().length
    $li.data('characters-on-the-left', charactersOnTheLeft)
    $li.data('var-type-length', $varType.text().length)
  })

  // sort the records by `characters-on-the-left`
  li_arr = li_arr.sort(function($li1, $li2) {
    return $li1.data('characters-on-the-left') - $li2.data('characters-on-the-left')
  })

  // reset `spaces`
  if (li_arr.length) {
    var start = 0,
        minw = li_arr[0].data('characters-on-the-left'),
        maxw = minw
    for (var i = 0; i < li_arr.length; i++) {
      var newmaxw = Math.max(maxw, li_arr[i].data('characters-on-the-left'))
      if (newmaxw - minw >= minw) {
        for (var j = start; j < i; j++) {
          li_arr[j].children('.id').children('.spaces').text(createNSpace(
            maxw - li_arr[j].data('var-type-length')
          ))
        }
        start = i 
        minw = newmaxw
      }
      maxw = newmaxw
    }
    for (var j = start; j < li_arr.length; j++) {
      li_arr[j].children('.id').children('.spaces').text(createNSpace(
        maxw - li_arr[j].data('var-type-length')
      ))
    }
  }

  // sort `li_arr` rows alphabetically by type then by variable name
  li_arr = li_arr.sort(function($li1, $li2) {
    var $id1 = $li1.children('span.id')
    var $id2 = $li2.children('span.id')

    var varType1 = $id1.children('.var-type').text()
    var varSpaces1 = $id1.children('.spaces').text()
    var varType2 = $id2.children('.var-type').text()
    var varSpaces2 = $id2.children('.spaces').text()

    // sort by indentation level
    var s = (varType1.length + varSpaces1.length) - (varType2.length + varSpaces2.length)
    if (s !== 0) {
      return s
    }

    // sort by variable type
    var c = varType1.localeCompare(varType2)
    if (c !== 0) {
      return c
    }
    
    // sort by variable name
    var varId1 = $id1.children('.var-id').text()
    var varId2 = $id2.children('.var-id').text()
    if (varId1[0] === '[') { // array index
      return parseInt(varId1.slice(1, varId1.length - 1)) - parseInt(varId2.slice(1, varId2.length - 1))
    } else {
      return varId1.localeCompare(varId2)
    }
  })

  li_arr.forEach(function($li) {
    $ul.append($li)
  })

  return $ul
}

$(document).ready(function() {
  // load expand states history
  var EXPAND_STATE = {"0": true} // key is index, value is boolean
  try { // Safari will cause error, so wrap it inside `try...catch...`
    if (typeof(Storage) !== 'undefined' && localStorage['EXPAND_STATE_' + window.location.pathname]) {
      EXPAND_STATE = JSON.parse(localStorage['EXPAND_STATE_' + window.location.pathname])
    }
  } catch(error) {}

  // save loaded scripts
  // key is `fileid`, value is true/false
  var SCRIPTS = {} 
  function loadJavaScript(scriptPath) {
    return new Promise(function(resolve, reject) {
      if (SCRIPTS[scriptPath]) { // script already loaded 
        return resolve()
      } else {
        var script = document.createElement('script')
        script.setAttribute('type', 'text/javascript')
        script.setAttribute('src', scriptPath)
        script.onload = function() {
          SCRIPTS[scriptPath] = true 
          return resolve()
        }
        document.getElementsByTagName('head')[0].appendChild(script)
      }
    })
  }


  $("[data-fileid]").each(function(index){
    var obj=this,
        $obj = $(obj)
    var dat = this.dataset.fileid
    if (!dat) return // no source file found

    var line = +this.dataset.line // cast to integer       
    var localVariables = JSON.parse(decodeURIComponent(this.getAttribute("data-local-variables") || "[]")) // local variables
    var $stackFrame = $('.stack-frame', this)

    var scriptPath = dat + '.js'
    loadJavaScript(scriptPath).then(function() {
      $stackFrame.html(window['CODES'][dat] || '')

      var size = $(".line", $stackFrame).length
      $(".expand span.lineno", obj).width($(".line span.lineno", obj).width())
      $(".line", $stackFrame).hide()
      $obj.attr("size", size)
      var locks = obj.dataset.locks.split(" ")
      for (var i = 0; i < locks.length; i++) {
        $(".line-"+locks[i] + " .after", $stackFrame).append((i+1))
        $(".line-"+locks[i], $stackFrame).addClass("hll3")
      }
      $highlightLine = $(".line-"+line, $stackFrame) 
      $highlightLine.addClass("hll")        
      showNLines(7, line-3, $stackFrame)

      if (size <= parseInt($obj.attr('data-context-end'))) 
        $('.expand-down-btn', $obj).addClass('disabled')
      if ($obj.attr('data-context-start') < 0)
        $('.expand-up-btn', $obj).addClass('disabled')

      // create memory table for local variables
      var $memory = $('<div class="memory"><div class="header"></div></div>')
      if (localVariables.length) {
        $memory.append(generateMemoryTable(localVariables))

        var $toggleMemoryPanelBtn = $('<div class="btn toggle-memory-panel-btn">show variables</div>')
        $toggleMemoryPanelBtn.on('click', function() {
          var $snippetDetails = $('.snippet.details', $obj) 
          $snippetDetails.toggleClass('show-memory')

          if ($snippetDetails.hasClass('show-memory')) {
            $toggleMemoryPanelBtn.text('hide variables')

            var $memory = $('.memory', $snippetDetails)
            $('.pre', $snippetDetails).css('min-height', $memory.height())
          } else {
            $toggleMemoryPanelBtn.text('show variables')
            $('.pre', $snippetDetails).css('min-height', 'inherit')
          }
        })

        $('.header', $memory).append($toggleMemoryPanelBtn)
      }
      $($stackFrame).append($memory)
      $($stackFrame).append('<div class="clear"></div>')
      
      var $snippetDetails = $('.snippet.details', $obj)
      var $locksDetails = $('.locks.details', $obj)
      var $bar = $('.bar', $obj)
      var $expandBtn = $('.expand-btn', $bar)
      function setExpandBtnStatus() {
        if (!$snippetDetails.hasClass('displayed')) {
          $bar.removeClass('expanded')
          $expandBtn.removeClass('expanded')
          $expandBtn.removeClass('times') // plus sign
          EXPAND_STATE[index] = false
        } else { // expanded
          $bar.addClass('expanded')
          $expandBtn.addClass('expanded')
          $expandBtn.addClass('times') // times sign
          EXPAND_STATE[index] = true
        }

        try {
          if (typeof(Storage) !== 'undefined') { // store expand state to localStorage.  
            localStorage['EXPAND_STATE_' + window.location.pathname] = JSON.stringify(EXPAND_STATE)
          }
        } catch(error) {}
      }
      $expandBtn.on('click', function(event) {
        event.preventDefault()
        event.stopPropagation()
        if ($bar.hasClass("disabled")) {
          return
        } else {
          $snippetDetails.toggle('fast')
          $snippetDetails.toggleClass('displayed')
          $locksDetails.toggle('fast')
          setExpandBtnStatus()
        }
      })
      $bar.on('click', function(event) { $expandBtn.click() })

      if (EXPAND_STATE[index]) {
        $expandBtn.click()
      }

      // Window scroll event. We should always make the memory table stick to the top.
      var containerElem = document.documentElement
      window.addEventListener('scroll', function() {
        var detailElem = $snippetDetails[0],
            memoryElem = $('.memory', $snippetDetails)[0]
        if (!memoryElem) return 

        var containerScrollTop = containerElem.scrollTop || window.pageYOffset
        
        if (memoryElem.offsetHeight > containerElem.offsetHeight) return
        if (containerScrollTop >= detailElem.offsetTop + detailElem.offsetHeight) return

        if (containerScrollTop + memoryElem.offsetHeight >= detailElem.offsetTop + detailElem.offsetHeight) {
          memoryElem.style.position = 'absolute'
          memoryElem.style.right ='0'
          memoryElem.style.top = (detailElem.offsetHeight - memoryElem.offsetHeight) + 'px' 
        } else if (containerScrollTop > detailElem.offsetTop) {
          memoryElem.style.position = 'fixed'
          memoryElem.style.top = '0'
          memoryElem.style.right = '24px'
        } else {
          memoryElem.style.position = 'absolute'
          memoryElem.style.right = '0'  
          memoryElem.style.top = '0'
        }
      })
    })
  })
})