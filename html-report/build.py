# -*- coding: utf-8 -*-
from collections import defaultdict
import shutil
import json

from jinja2 import Environment, FileSystemLoader
from pygments import highlight
from pygments.lexers import CLexer
from pygments.formatters import HtmlFormatter

class LinkedHtmlFormatter(HtmlFormatter):
	def __init__(self, **kwargs):
		HtmlFormatter.__init__(self,**kwargs)
		self.hl_links = kwargs['hl_links']

	def wrap(self, source, outfile):
		return self._wrap_code(source)

	def _wrap_code(self, source):
		yield 0, '<div><pre>'
		lineno = 0
		for (i, txt) in source:
			if i == 1:
				lineno += 1
				if lineno in self.hl_lines:
					yield 0, '<div class="line-with-errors">'
					if len(self.hl_links[lineno]) == 1:
						err = self.hl_links[lineno][0]
						ix = err[u'index']
						tgt = 'error-%d.html' % ix
						yield 0, '<a class="hll" href="%s">'%tgt
						yield i, txt
						yield 0, '</a>'
					else:
						yield i, txt
					yield 0, '<div class="errors">'
					for err in self.hl_links[lineno]:
						yield 0,'<span class="lineno">   </span>'
						msg = '%s (%s)' % (err[u'description'],err[u'error_id'])
						ix = err[u'index']
						tgt = 'error-%d.html' % ix
						line = '<a href="%s"> %s Error #%d</a>\n' % (tgt, msg, ix)
						yield 0, line
					yield 0, '</div></div>'
				else:
					yield i, txt
			else:
				yield i, txt
		yield 0, '</pre></div>'

def highlightC(code, hl_links={}):
  hl_lines = hl_links.keys()
  return highlight(code,
      CLexer(),
      LinkedHtmlFormatter(hl_lines=hl_lines, hl_links=hl_links, linenos='inline'))

def load_json(filename):
   with open(filename, 'r') as f:
     return json.load(f)
files = load_json('files.json')
errors = load_json('errors.json')
datafiles=[]
global_data={item: load_json(item+'.json')
  for item in datafiles}

def listdict():
	return defaultdict(list)

errorsByFile = defaultdict(listdict)
for ix, err in enumerate(errors):
	err[u'index'] = ix
	err[u'errdesc'] = '%s (%s)' % (err[u'description'],err[u'error_id'])
	frame = err[u'traces'][0][u'frames'][0]
	loc = frame[u'loc']
	err[u'file'] = loc[u'rel_file']
	err[u'line'] = loc[u'line']
	err[u'function'] = frame[u'symbol']
	errorsByFile[err[u'file']][err[u'line']].append(err)

def render_code(fileinfo):
   filename = fileinfo[u'file']
   with open('code/'+filename, 'r') as f, open('output/'+filename+'.html', 'w') as target:
     code = f.read()
     template = env.get_template('sourcefile.html')
     errorlines = errorsByFile[filename]
     target.write(template.render(filename=filename,
								  errorlines=errorlines,
								  code=code,
								  info=fileinfo))

def render_error(err):
   template = env.get_template('error.html')
   with open('output/error-%d.html' % err[u'index'], 'w') as target:
	   target.write(template.render(err=err))

def render_template(filename):
  with open('output/'+filename, 'w') as target:
    template = env.get_template(filename)
    target.write(template.render())

env = Environment(
  autoescape=True,
  loader=FileSystemLoader('templates/')
)
env.filters[u'highlight'] = highlightC

env.globals.update(global_data)
env.globals[u'files'] = files
env.globals[u'errors'] = errors

shutil.rmtree('output/', True)
shutil.copytree('static/','output/static/')
for template in ['index.html','errors.html','nav.html','error-descriptions.html']:
  render_template(template)
for source in env.globals[u'files']:
  render_code(source)
for err in errors:
	render_error(err)
