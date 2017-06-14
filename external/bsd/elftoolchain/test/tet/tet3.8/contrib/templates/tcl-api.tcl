#!/usr/bin/tcl
# This needs to be tclX for signal support
# A template test case for the TCL API


# ---Initialize TCM data structures--- #
# these have to be fixed names ic1, ic2 , ic3 etc
set iclist "ic1 ic2 ic3"

# ---TET test purposes--- #
# map the icnames to the test purpose functions
set ic1 "tp1"	
set ic2 "tp2"
set ic3 "tp3"

#---TET startup/cleanup functions---#

set tet_startup "startup"
set tet_cleanup "cleanup"

# startup routines typically check for dependencies
# and can cancel tests using tet_delete

proc startup {} {
	tet_infoline "Entering the startup routine"


	set TET_TCL_DEPEND ""
	if {([catch {set TET_TCL_DEPEND $env(TET_TCL_DEPEND}] == 1) ||
    	([string length TET_TCL_DEPEND] == 0)} {
		tet_delete tp3 "tp3 deleted since TET_TCL_DEPEND not set in the environment"
	}
}

proc cleanup {} {
	tet_infoline "This is the cleanup routine"
}

#---TET test functions follow--- #

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

#--- Include the TCM--- #
# execute tcl test case manager - must be last line
source $env(TET_ROOT)/lib/tcl/tcl.tcm.dat
