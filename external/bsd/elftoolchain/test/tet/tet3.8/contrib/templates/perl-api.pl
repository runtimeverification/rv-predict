#!/usr/bin/perl
# A template test case for the Perl API


# ---Initialize TCM data structures--- #
# array of pointers to test purposes
# these have to be fixed names ic1, ic2 , ic3 etc
@iclist=(ic1);

# ---TET test purposes--- #
# map the icnames to the test purpose functions
@ic1=("tp1","tp2");

#---TET startup functions---#
$tet'startup="startup";
$tet'cleanup="cleanup";

sub startup {
	&tet'infoline("This is the startup");
	&tet'delete("tp2", "deleted in startup");
}

sub cleanup {
	&tet'infoline("This is the cleanup");
}

#---TET test functions follow--- #

sub tp1{

	&tet'infoline("This is the tp1 test case"); 
        &tet'result("PASS");
}

sub tp2{

	&tet'infoline("This is the tp2 test case "); 
        &tet'result("PASS");
}

#--- Include the TCM--- #
require "$ENV{\"TET_ROOT\"}/lib/perl/tcm.pl";