TETware "rpt" report writer  contrib tools
------------------------------------------

To build on win32 systems, assuming MS C++ and
the MKS toolkit installed, use the command line
version of the compiler.

Just type 
	make
	make install


To build on UNIX systems, you need to edit the
makefile 

	1) to remove the extra ^Ms in the file.
	2) to change the compiler definitions, and the
	   definitions for the .obj and .exe file extensions


A description of the utilities files.


NAME 

rpt - TET report writer utility

SYNOPSIS


rpt [-f filename] [-j journal] [-d detail] [-s scope] [-t test] 
       [-u] [ -h ] [-v] [-w]

DESCRIPTION

The rpt utility produces a report for a raw Test Environment
Toolkit journal file. The TET_ROOT environment variable  needs to
be set.

The following flags are supported as arguments to the rpt utility.

      -f journal file name
      -j journal file # (default is latest)
      -d detail
         0 - everything
         1 - reasonable detail (default)
         2 - nothing
         3 - results only
         4 - assertions and results only
      -s scope of detail
         0 - all result codes
         1 - errors, FIP (default)
         2 - reserved for future use
         3 - UNSUPPORTED
         4 - FIP
         5 - NOTINUSE
         6 - UNTESTED
         7 - NORESULT
      -t detail only on specified Test Case
      -u use latest journal file for current user
      -h display this usage message
      -v display program version
      -w summarize journals


NAME

rptm - comparative report writer

SYNOPSIS

rptm [-s test] [-a] [-h] [-u] [-f] journal1 journal2 [journal3] 
           [journal4 ] [journal5] [journal6]

DESCRIPTION

The rptm utility produces a comparative report for two or
more Test Environment Toolkit journal files.

The following flags are supported as arguments to the rptm utility:

      -a show all results (not just variations)
      -h print the usage message
      -s report only on specified tests
      -u show variations plus build failures common to all reports
      -f show variations plus failures in any report

