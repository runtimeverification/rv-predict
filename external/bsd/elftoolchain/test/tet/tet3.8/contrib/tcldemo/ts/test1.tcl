#!/usr/bin/tcl
# @(#)test1.tcl	1.4
# This needs to be tclX for signal support

set tet_startup "startup"
set tet_cleanup "cleanup"
set iclist "ic1 ic2 ic3"
set ic1 "tp1"	
set ic2 "tp2"
set ic3 "tp3"

proc startup {} {
	tet_infoline "Entering the startup routine"

# startup routines typically check for dependencies
# and can cancel tests using tet_delete
	set TET_TCL_DEPEND ""
	if {([catch {set TET_TCL_DEPEND $env(TET_TCL_DEPEND)}] == 1) ||
    	([string length TET_TCL_DEPEND] == 0)} {
		tet_delete tp3 "tp3 deleted since TET_TCL_DEPEND not set in the environment"
	}
}

proc cleanup {} {
	tet_infoline "This is the cleanup routine"
}

proc tp1 {} {
	tet_infoline "This is an infoline message from tcl"
	tet_result PASS
}

proc tp2 {} {
	tet_infoline "This test tp2 is expected to fail"
	tet_result FAIL
}

proc tp3 {} {
	tet_infoline "This test tp3 is expected to fail"
	tet_result FAIL
}

# execute tcl test case manager - must be last line
source $env(TET_ROOT)/lib/tcl/tcl.tcm.dat
