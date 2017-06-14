#!/usr/bin/tcl
# @(#)test2.tcl	1.1
# a very simple test case

set tet_startup ""
set tet_cleanup ""
set iclist "ic1"
set ic1 "tp1"	


proc tp1 {} {

    set result 0
    switch -- $result {
      0 {
        tet_result FATAL
      }
      1 {
        tet_result FAIL
      }
	}

}

# initialize test environment: must be last line
source $env(TET_ROOT)/lib/tcl/tcl.tcm.dat
