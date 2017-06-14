#!your-path-to-perl/perl

# Copyright in this TET Journal Reporting Software is owned by Hewlett-Packard 
# Australia Limited.
#      
# The owner hereby grants permission to use, copy, modify, distribute,
# and license this software and its documentation for any purpose, 
# provided that existing copyright notices are retained in all copies and 
# that this notice is included verbatim in any distributions. No written 
# agreement, license, or royalty fee is required for any of the authorized 
# uses. Ownership of copyright in modifications to this software will vest 
# in the authors and the licensing terms described here need not be 
# followed, provided that the new terms are clearly indicated on the first 
# page of each file where they apply.
#      
# TO THE EXTENT ALLOWED BY LOCAL LAW, IN NO EVENT SHALL THE COPYRIGHT 
# OWNER OR DISTRIBUTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, 
# SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE 
# OF THIS SOFTWARE, ITS DOCUMENTATION, OR ANY DERIVATIVES THEREOF, EVEN 
# IF THE OWNER HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#      
# TO THE EXTENT ALLOWED BY LOCAL LAW, THE COPYRIGHT OWNER AND DISTRIBUTORS 
# SPECIFICALLY DISCLAIM ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
# IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, 
# AND NON-INFRINGEMENT, THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, AND 
# THE COPYRIGHT OWNER AND DISTRIBUTORS HAVE NO OBLIGATION TO PROVIDE 
# MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
#      
# RESTRICTED RIGHTS: Use, duplication or disclosure by the government
# is subject to the restrictions as set forth in subparagraph (c) (1) (ii) 
# of the Rights in Technical Data and Computer Software Clause as DFARS 
# 252.227-7013 and FAR 52.227-19.
#
# MODIFICATIONS:
#
# Andrew Josey, The Open Group, November 10th 1997
# Require the classes to be in $TET_ROOT/lib/perl
# 

=head1 NAME

tetreport - Analyse TET journal file

=head1 DESCRIPTION

Formats one or more TET journal files into useful reports.  Several reports are
available, including Summary, Formatted and Error reports.

Usage:

=head2  tetreport [B<-s|f|e>] [B<-nt>] [B<-q>] journal [journal ...]

  Where 'journal' specifies one or more journal files to report.  

  If no journal file is specified, current directory and $TET_SUITE_ROOT
  environment variable is used to deduce most recent relevant journal file.

  Any combination of the following reports may be specified.  The report name
  may be abbreviated to the first letter:

    -s        Summary report detailing the test cases run and the pass rate
              for each test case.  This is the default report.

    -f        Present journal file information in sequence, but in a more
              readable structured format.

    -e        Present detailed error information from journal about each
              non-PASS.  Tests completing with a PASS are not reported.

  Other options:

    -nt       Blank out all times fields to enable reports run at different
              times to be compared

    -q        Print nothing to STDOUT, but return a failure count for the 
              specified journal file(s).  A zero return code means no failures.
              No reports may be specified together with the -q option.
    
=head1 EXAMPLES

Print a summary report:

  tetreport results/0001e/journal

Print a formatted report:

  tetreport -f results/0001e/journal

Print an error report for all journal files in results directory:

  tetreport -e results/*/journal

Compare two journal files for differences:

  tetreport -f -nt results/0010e/journal > xx
  tetreport -f -nt results/0011e/journal > yy
  diff xx yy

=head1 AUTHOR

Geoff Smith

=cut 
    ;

# Embedded Perl Classes.. in TET_ROOT/lib/perl
require "$ENV{\"TET_ROOT\"}/lib/perl/TetJournal.pl";
require "$ENV{\"TET_ROOT\"}/lib/perl/TetReports.pl";


# Initialise
$defaultReport="summary";
@journal=();
@reports=();
$failCount=0;
$timesFlg="";
$quiet=0;
$autoJournal=0;

#
# Process Arguments
#

foreach $arg (@ARGV) {
    if ($arg=~/^-s/i) {
	push(@reports,"summary");
    }
    elsif ($arg=~/^-e/i) {
	push(@reports,"error");
    }
    elsif ($arg=~/^-f/i) {
	push(@reports,"formatted");
    }
    elsif ($arg=~/^-nt$/i) {
	$timesFlg='NOTIMES';
    }
    elsif ($arg=~/^-q/i) {
	$quiet=1;
    }
    else {
	push(@journal,$arg);
    }
}

if ($quiet) {
    @reports=();
}
elsif (! @reports) {
    push(@reports,$defaultReport);
}

#
# If @journal is empty, try to work out what it should be
#

if (! @journal) {
    $autoJournal=1;
    
    if (-d "results") {                     # found results directory in pwd    
	chdir("results");                   # so cd into it
	$ENV{PWD}="$ENV{PWD}/results";      # and update PWD for next 'if'
    }
    if ($ENV{PWD} =~ /\/results$/) {        # current directory is results
	chop($journalDir=`ls | tail -1`);   # find most recent journal
	$journal="$journalDir/journal";
	if (-f $journal) {
	    push(@journal,$journal);
	}
    }
    if (-f "journal") {                     # journal file in current directory
	@journal=( "journal" );
    }
	
    if ($ENV{TET_SUITE_ROOT} and ! @journal) {  # last desparate attempt
	$cmd="find $ENV{TET_SUITE_ROOT} -path '*/results/*/journal' | tail -1";
	chop($journal=`$cmd`);
	push(@journal,$journal) if ($journal);
    }
	
    if (! @journal) {
	print "Error: Unable to locate a journal file\n";
	exit(1);
    }
}

#
# Process each journal file in turn.  Create object and pass execution to
# report routine
#

foreach $journal (@journal) {
    if ($jnl=TetJournal->new($journal,$timesFlg)) {

	# This is the first report for this journal
	$first=1;
	
	foreach $report (@reports) {
	    # Create Reports object - specifying type of report
	    next unless ( $rpt=TetReports->new($jnl,$report) );

	    # Print report header if more than one journal or report specified
            # or journal file was automatically derived
	    if (0 < $#journal + $#reports | $autoJournal) {
		$reportCaps=$report;
		$reportCaps=~s/^(.)/\U\1/; # Capitalise first character
		print "\n$reportCaps report for: $journal\n";
	    }

	    # Print the tcc invocation command if this is the first report
	    print ("Command: " . $jnl->tccCmd ."\n") if ($first);
	    $first=0;


	    # Run the report
	    eval "\$rpt->$report";
	}

	# Obtain the failcount
	$failCount+=$jnl->status;
    }
    else {
	print "Error: Cannot open journal file '$journal'\n";
    }
}

exit($failCount); 


