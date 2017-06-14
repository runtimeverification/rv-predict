#
# @(#)tetapi.tcl	1.4
#
# Copyright 1990, 1994 Open Software Foundation (OSF)
# Copyright 1990 Unix International (UI)
# Copyright 1990, 1997 The Open Group (X/Open)
#
# Permission to use, copy, modify, and distribute this software and its
# documentation for any purpose and without fee is hereby granted, provided
# that the above copyright notice appear in all copies and that both that
# copyright notice and this permission notice appear in supporting
# documentation, and that the name of OSF, UI or X/Open not be used in 
# advertising or publicity pertaining to distribution of the software 
# without specific, written prior permission.  OSF, UI and X/Open make 
# no representations about the suitability of this software for any purpose.
# It is provided "as is" without express or implied warranty.
#
# OSF, UI and X/Open DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, 
# INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO 
# EVENT SHALL OSF, UI or X/Open BE LIABLE FOR ANY SPECIAL, INDIRECT OR 
# CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF 
# USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR 
# OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
# PERFORMANCE OF THIS SOFTWARE.
# 
# 
# HISTORY
# 18 November 1997, Andrew Josey, The Open Group
# set TET_ABACTION with a default value
#
# 8 November 1997, Andrew Josey, The Open Group
# Make tet_reason consistent with the perl api, i.e. to return
# the reason string or "" otherwise
#
# 22 October 1997, Andrew Josey, The Open Group
# Minor speedup changes and tet_delete handling
#
# 10th October 1997, Andrew Josey, The Open Group
# add missing close-bracket "]" at line 309 for use with tcl8
# as per bug report.
#
# 10th February 1997, Andrew Josey, The Open Group
# Apply known bug fixes
#
# $Log: tetapi.tcl,v $
# Revision 1.1.2.2  1994/03/07  21:35:25  rousseau
# 	Use '-c' option to sh for portability (CR 10085).
# 	[1994/03/07  21:35:10  rousseau]
#
# Revision 1.1.2.1  1994/02/10  22:54:42  rousseau
# 	Initial version
# 	[1994/02/10  22:53:53  rousseau]
# 
# $EndLog$


# DESCRIPTION:
#	This file contains tcl functions for use with the tcl API.
#	It is sourced automatically by the tcl TCM.
#	In addition it should be sourced by test purposes that are written as
#	separate tcl scripts, by means of the tcl "source" command.
#
#	The following functions are provided:
#
#		tet_setcontext
#		tet_setblock
#		tet_infoline
#		tet_result
#		tet_delete
#		tet_reason
#

# set current context and reset block and sequence
# usage: tet_setcontext
proc tet_setcontext {} {
  global TET_CONTEXT TET_BLOCK TET_SEQUENCE

  set TET_CONTEXT [exec sh -c {(:)& echo $!}]
  set TET_BLOCK 1
  set TET_SEQUENCE 1
}


# increment the current block ID, reset the sequence number to 1
# usage: tet_setblock
proc tet_setblock {} {
  global TET_BLOCK TET_SEQUENCE

  incr TET_BLOCK
  set TET_SEQUENCE 1
}


# print an information line to the execution results file
# and increment the sequence number
# usage: tet_infoline args [...]
proc tet_infoline args {
  global TET_TPCOUNT TET_CONTEXT TET_BLOCK TET_SEQUENCE

  tet_output 520 "$TET_TPCOUNT $TET_CONTEXT $TET_BLOCK $TET_SEQUENCE" $args
  incr TET_SEQUENCE
}


# record a test result for later emmision to the execution results file
# by tet_tpend
# usage: tet_result result_name
# (note that a result name is expected, not a result code number)
proc tet_result {result_name} {
    global TET_TMPRES

    if {([catch {set result_name}]) || 
        ([string length $result_name] == 0)} {
        puts stderr "tet_result: no result name specified"
        exit
    }
    if {[tet_getcode $result_name]} {
        tet_error "tet_result: invalid result name $result_name"
        set result_name NORESULT
    }
    
    if {([catch {set TET_TMPRES}]) || 
        ([string length $TET_TMPRES] == 0)} {
        puts stderr "tet_result: TET_TMPRES is not defined"
        exit
    }
    
    exec echo $result_name >> $TET_TMPRES
}


# mark a test purpose as deleted
# usage: tet_delete test_name reason [...]
proc tet_delete {test_name args} {
  global TET_DELETES

  if {([catch {set test_name}] == 1) || 
      ([string length $test_name] == 0)} {
    puts stderr "tet_delete: no test_name specified"
    exit
  }
  
  if {[string length $args] == 0} {
    tet_undelete $test_name
    return
  }

  if {[tet_reason $test_name ] != "" } {
    tet_undelete $test_name
  }

  if {([catch {set TET_DELETES}] == 1) || 
      ([string length $TET_DELETES] == 0)} {
    puts stderr "tet_delete: TET_DELETES is not defined"
    exit
  }
  
  exec echo "$test_name $args" >> $TET_DELETES
}


# return the reason string why a test purpose has been deleted
# otherwise return an empty string
# usage: tet_reason test_name
proc tet_reason {test_name} {
    global TET_DELETES

    set fd [open $TET_DELETES r]
    
    while {![eof $fd]} {
	set line [gets $fd]
	if {[lindex $line 0] == $test_name} {
	    set reasonstr [lindex $line 1]
	    close $fd
	    return $reasonstr
	}
    }
    close $fd
    return  ""
}



# ******************************************************************

#
# "private" functions for internal use by the tcl API
# these are not published interfaces and may go away one day
#
proc tet_getcode {code_name} {
    global TET_ABORT TET_RESNUM TET_CODE TET_ABACTION
    
    set TET_ABACTION ""
    set TET_ABORT NO
    set TET_RESNUM -1
    
    if {[catch {set TET_CODE}]} {
	puts stderr "tet_getcode: TET_CODE is not defined"
	exit
    }

    if {[catch {set code_name}]} {
	puts stderr "tet_getcode: no code name specified"
	exit
    }

    set fd [open $TET_CODE r]
    seek $fd 0
    
    while {![eof $fd]} {
	set line [gets $fd]
	if {[regexp ^# $line]} {
	    continue
	}

	if {[regexp ^$ $line]} {
	    continue
	}

	if {[lindex $line 1] == $code_name} {
	    set TET_RESNUM [lindex $line 0]
	    set TET_ABACTION [lindex $line 2]
	    regsub -all \" $TET_ABACTION "" $TET_ABACTION
	    break
	}
    }
    
    if {$TET_RESNUM == -1} {
	unset TET_ABACTION
	return 1
    }
    
    switch $TET_ABACTION {
	"" -
	Continue {
	    set TET_ABORT NO
	}
	Abort {
	    set TET_ABORT YES
	}
	default {
	    tet_error "invalid action field $TET_ABACTION in file $TET_CODE"
	    set TET_ABORT NO
	}
    }
    
    unset TET_ABACTION
    return 0
}


# tet_undelete - undelete a test purpose
proc tet_undelete {test_name} {
    global TET_DELETES
    
    set fd [open $TET_DELETES r]
    set tfd [open "/tmp/tet_deletes.tmp" w+]
    seek $fd 0
    
    while {![eof $fd]} {
	set line [gets $fd]
	
	if {[string first $test_name $line] == 0} {
	    continue;
	} else {
	    puts $tfd $line
	}
    } 
    
    close $fd
    close $tfd
    exec cp /tmp/tet_deletes.tmp $TET_DELETES
    exec rm /tmp/tet_deletes.tmp
}


# tet_error - print an error message to stderr and on TCM Message line
proc tet_error {args} {
    global TET_PNAME TET_RESFILE TET_ACTIVITY

    puts stderr "$TET_PNAME: $args"
    
    if {[catch {set TET_ACTIVITY}]} {
	set TET_ACTIVITY 0
    }
    
    if {([catch {set TET_ACTIVITY}] == 1) || 
        ([string length $TET_ACTIVITY] == 0)} {
	set TET_ACTIVITY 0
    }
    
    if {([catch {set TET_RESFILE}] == 1) || 
        ([string length $TET_RESFILE] == 0)} {
	puts stderr "tet_error: TET_RESFILE is not defined"
	exit
    }
    
    exec echo "510|$TET_ACTIVITY|$args" >> $TET_RESFILE
}


# tet_output - print a line to the execution results file
proc tet_output {args} {
    global TET_STDERR TET_RESFILE TET_ACTIVITY

    if {[catch {set TET_ACTIVITY}]} {
	set TET_ACTIVITY 0
    }
    
    if {[string length [lindex $args 1]] > 0} {
	set tet_sp " "
    } else {
	set tet_sp ""
    }
    
    set line [format "%d|%s%s%s|%s" [lindex $args 0] $TET_ACTIVITY $tet_sp \
            [lindex $args 1] [lindex $args 2]]
    
    regsub -all \n $line " " line2
    set line $line2
    
    if {[string length $line] > 511} {
	puts stderr [format "warning: results file line truncated: prefix: %d|%s%s%s|" [lindex $args 0] $TET_ACTIVITY $TET_SP [lindex $args 1]]
	set line [string range $line 0 511]
    }
    
    if {([catch {set TET_RESFILE}] == 1) || 
        ([string length $TET_RESFILE] == 0)} {
	puts stderr "tet_error: TET_RESFILE is not defined"
	exit
    }
    
    exec echo $line >> $TET_RESFILE
    
    if {([file exists $TET_STDERR]) && ([file size $TET_STDERR] > 0)} {
	tet_error "[exec cat $TET_STDERR]"
    }
}


