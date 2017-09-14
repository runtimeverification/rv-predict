This prototype demonstrates some handling of multiple errors in a codebase.

To rebuild the report (in a subdirectory output/ ), run 'python build.py',
or just './build.sh'. This requires Python 2.7 with the jinja2 and pygments
libraries installed (both can be installed with pip).

The set of errors is taken from real RV-Match errors recorded in "errors.json".
The list of source files to generate pages for is taken from "files.json".
