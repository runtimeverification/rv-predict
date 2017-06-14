#!/usr/bin/tcl
# This needs to be tclX for signal support
# @(#)test3.tcl	1.1
# By executing as 
# tcc -e -vmessage="mymessage" -y test3 contrib/tcldemo -p
# you can change the behaviour

set tet_startup "startup"
set tet_cleanup "cleanup"
set iclist "ic1 ic2"
set ic1 "tp1"	
set ic2 "tp2 tp3"

proc startup {} {
   tet_infoline "This message is from the startup function."
}

proc cleanup {} {
   tet_infoline "This message is from the cleanup function."
}

proc tp1 {} {
   tet_infoline "This message is from tp1."
   tet_infoline "The expected result is PASS."
   tet_result PASS
}

proc tp2 {} {
   global message

   tet_infoline "This message is from tp2."
   if [info exists message] {
      tet_infoline "$message"
   } else {
      tet_infoline "No message from configuration variable 'message'."
   }
   tet_infoline "The expected result is INSPECT."
   tet_result INSPECT
}

proc tp3 {} {
   tet_infoline "This message is from tp3."
   tet_infoline "The expected result is FAIL."
   tet_result FAIL
}

# execute tcl test case manager - must be last line
source $env(TET_ROOT)/lib/tcl/tcl.tcm.dat

