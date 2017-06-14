#!/usr/bin/perl
@iclist=(ic1);
@ic1=("tp1");

sub tp1{

	&tet'infoline("This is the second test case"); 
	print("We have not set TET_OUTPUT_CAPTURE=True ");
        print("so all normal stdin/stdout/stderr\nfiles are available.  ");
        print("\nIf we had set output capture,  the results logged by");
        print(" the API would not be in the journal.\n");
        print("But these lines would.\n");
        &tet'result("PASS");
}

require "$ENV{\"TET_ROOT\"}/lib/perl/tcm.pl";
