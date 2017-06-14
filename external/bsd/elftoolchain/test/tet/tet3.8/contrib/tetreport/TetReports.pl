package TetReports;

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

=head1 NAME

TetReports - Reports for tetreport

=head1 DESCRIPTION

Build reports from TET journal files.  Uses the TetJournal class to manage all
the Journal file domain knowledge.  This class simply builds the reports.

The reports are included in this TetReports class, rather than as routines 
living in tetreport, to allow new reports to be added without changing the
code.

=head1 METHODS

=head2 new(I<TetJournal_Object>)

TetJournal_Object is the handle for an existing TetJournal object.
Returns a TetReports object handle.

=head2 summary

Generate a summary report

=head2 formatted

Generate a formatted report

=head2 error

Generate an error report

=head1 AUTHOR

Geoff Smith

=cut 
    ;

# ===========================================================================
# Method     : new
# Description: Constructor.
# Inputs     : TetJournal object
# Returns    : TetReports object or null if report not known
# Example    : $rpt=TetReports->new($jnl);

sub new {
    $class=shift;
    my $this={};

    # Save TetJournal handle in TetReports object
    $this->{jnl}=shift;

    # Return TetReports handle
    bless $this,$class; 
}


# ===========================================================================
# Method     : summary
# Description: Generate a summary report
# Inputs     : none
# Returns    : zero 

sub summary {
    my $this=shift;
    my $jnl=$this->{jnl};  # TetJournal object reference
    my $percentPass=0;
    my $TCpassTot=0;
    my $TCrunTot=0;

    #
    # Print body of report for each test case
    #
    foreach $TC ($jnl->TClist) {
	print "$TC\n";
	print "          Run Count   Pass Count for each TP   Pass Rate\n";
	@IC=$jnl->IClist($TC);
	print "  No IC's found\n" unless (@IC);

	foreach $IC (@IC) {
	    my $runCt=$jnl->ICrunCount($TC,$IC);
	    my $passCt="";
	    my $passTot=0;
	    my $TPcount=0;
	    my $suffix="%";
	    my $first=1;

	    foreach $_ ($jnl->ICpassCount($TC,$IC)) {
		if ($first) {
		    $passCt.=$_;
		    $first=0;
		} else {
		    $passCt.=",$_";
		}
		$passTot+=$_;
		$TPcount++;
	    }
	    my $testCount=$runCt * $TPcount;
	    if ($testCount != 0) {
		$percentPass=100 * $passTot / $testCount;
	    }
	    else {
		$percentPass=0;
	    }
	    $suffix="% *" if ($percentPass != 100);

	    printf("  IC%-6s%-12s%-28s%3d%-3s\n",$IC,$runCt,$passCt,$percentPass,$suffix);
	    $TCrunTot+=$testCount;
	    $TCpassTot+=$passTot;
	}
    }
    
    #
    # Print summary line
    #
    print "Summary                         +----------------------+\n";
    $percentPass=($TCrunTot == 0)? 0 : 100 * $TCpassTot / $TCrunTot;
    my $rate="$TCpassTot/$TCrunTot";
    my ($start,$end)=$jnl->times;
    printf("  Start: %8s End: %8s |Passed: %-9s%3d%% |\n",$start,$end,$rate,$percentPass);
    print "                                +----------------------+\n";
    return(0);
}



# ===========================================================================
# Method     : formatted
# Description: Generate a formatted report
# Inputs     : none
# Returns    : zero 

sub formatted {
    my $this=shift;
    my $jnl=$this->{jnl};
    $jnl->resetBlockPtr;  # Reset pointer in case several reports are run

    my @block, $typeCaps, $type, $TCname, $start, $end;
    my @IC, @ICstring, $icstart, $icend, $TPcount, $result, $tpstart, $tpend;

    #
    # Loop through each block in turn, formatting the data
    #
    while (@block=$jnl->nextBlock) {
	$typeCaps=$type=$jnl->blockType;
	$typeCaps=~s/^(.)/\U\1/;  # Uppercase the first character

	$TCname=$jnl->TCname;

	($start,$end)=$jnl->blockTimes;
	
	if ($type eq 'build' or $type eq 'clean') { 
	    print "$start\t$typeCaps for Test Case $TCname\n";
	    foreach $_ ($jnl->blockOutput) {
		print "\t\t$_\n";
	    }
	}
	elsif ($type eq 'scenario') {
	    foreach $_ (@block) {
		print " $_\n";
	    }
	}
	elsif ($type eq 'TC') {
	    @IC=$jnl->IClist;
	    $ICstring=join(",",@IC);
	    print "$start\tExecuting Test Case $TCname  ICs={$ICstring}\n";
	    foreach $text ($jnl->blockOutput) {
		print "\t\t$text\n";
	    }
	    
	    print "\t\tNo IC's found\n" unless (@IC);

	    foreach $IC (@IC) {
		($icstart,$icend)=$jnl->ICtimes($IC);
		print "$icstart\t\tExecuting IC$IC\n";
		
		$TPcount=$jnl->TPcount($IC);
		print "\t\t\tNo TP's found\n" unless ($TPcount);
		for ($TP=1;$TP <= $TPcount;$TP++) {
		    $result=$jnl->TPresult($IC,$TP);
		    ($tpstart,$tpend)=$jnl->TPtimes($IC,$TP);
		    foreach $_ ($jnl->TPoutput($IC,$TP)) {
			print "        \t\t\tTP$TP $_\n";
		    }
		    print "$tpend\t\t\tTP$TP $result\n";
		}
	    }
	}
    }
    return(0);
}


# ===========================================================================
# Method     : error
# Description: Produce Error Report
# Inputs     : none
# Returns    : zero 

sub error { 
    my $this=shift;
    my $jnl=$this->{jnl}; # TetJournal object 

    my @block, $TCname, @IC;

    $jnl->resetBlockPtr;  # Reset pointer in case several reports run

    #
    # Loop through each block in turn, looking for Test Cases
    #

    my $errorCount=0;

    while (@block=$jnl->nextBlock) {
	next unless ('TC' eq $jnl->blockType);
	
	$TCname=$jnl->TCname;
	@IC=$jnl->IClist;

	# Check for no IC's in Test Case
	unless (@IC) {
	    $errorCount++;
	    &prtErrorHeader($errorCount);
	    print "$TCname  No IC's found\n";
	    
	    foreach $_ ($jnl->blockOutput) {
		print "  $_\n";
	    }
	}

	foreach $IC ($jnl->IClist) {
	    # Skip IC unless errors are found
	    next unless ($jnl->ICstatus($IC));

	    # This IC has errors so dig in
	    $errorCount++;
	    &prtErrorHeader($errorCount);
	    print "$TCname  IC=$IC\n";

	    my $TPcount=$jnl->TPcount($IC);

	    if($TPcount == 0) {
		print "  No TP's found\n";
	    }

	    for ($TP=1; $TP <= $TPcount; $TP++) {
		$result=$jnl->TPresult($IC,$TP);
		next if ($result eq 'PASS');
		
		# I've found the failing TP.  Print the gory details.
		print "  $result from TP$TP:\n";
		foreach $_ ($jnl->getTP($IC,$TP)) {
		    print "    $_\n";
		}
	    }
	}
    }
    
    if ($errorCount == 0) {
	print "  -> No errors\n";
    }
    return(0);
}


# ===========================================================================
# Routine    : prtErrorHeader
# Description: Print Error Report Header
# Input: Error number

sub prtErrorHeader {
    my $errorCount=shift;
    print "----------------- Error Report $errorCount ------------------\n";
}

1;
