#!/bin/sh
# A template test case for the Shell API
# Examples : see contrib/SHELL-API

# ---Initialize TCM data structures--- #
# these have to be fixed names ic1, ic2 , ic3 etc
iclist="ic1 ic2 ic3"			# list invocable components

# ---TET test purposes--- #
# map the icnames to the test purpose functions
ic1="tp1"				# functions for ic1
ic2="tp2"				# functions for ic2
ic3="tp3"				# functions for ic3

#---TET startup functions---#
tet_startup=""				# no startup function
tet_cleanup="cleanup"			# cleanup function

#---TET test functions follow--- #


tp1()
{

	# tp1() code follows

}

tp2()
{
	# tp2() code follows

}

tp3() 
{
	# tp3() code follows

}

# --- TET cleanup function --- #

cleanup() # clean-up function
{
	# cleanup code follows     
}

# --- Test Suite Specific common shell functions ---#
# if you have any test suite specific
# common shell functions you could shell them in here
. $TET_EXECUTE/lib/shfuncs

#--- Include the TCM--- #
# execute shell test case manager - must be last line
. $TET_ROOT/lib/xpg3sh/tcm.sh
